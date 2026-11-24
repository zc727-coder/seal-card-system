<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { get, post, imageBlob } from '../api'
import { can, date, bytes, errorText, localTimestamp } from '../display'
import StatusBadge from './StatusBadge.vue'
const props = defineProps({ cardId: String, user: Object, review: Boolean, transaction: Object, initialVersionId: String })
const emit = defineEmits(['change', 'version-upload'])
const detailTab = ref('basic')
const card = ref(null), loading = ref(true), busy = ref(false), error = ref(''), notice = ref(''), versionId = ref(''), action = ref(''), customer = ref(null)
let imageRequest = 0, disposed = false
const image = ref(''), imagePage = ref(null), imageBusy = ref(false), checked = ref(false)
const input = reactive({ reason: '', effectiveFrom: '', accountNos: [], authorizationRef: '', changeReason: '' })
const currentVersionLabel = computed(() => {
  const current = card.value?.versions?.find(item => String(item.versionId) === String(card.value.currentVersionId))
  return current ? `V${current.versionNo}` : '未生效'
})
const version = computed(() => card.value?.versions?.find(item => String(item.versionId) === String(versionId.value)))
const canReview = computed(() => can(props.user, 'CHECKER') && version.value?.status === 'PENDING_APPROVAL' && version.value.createdBy !== props.user.operatorId)
const canSeeImages = computed(() => can(props.user, 'CHECKER') || (can(props.user, 'MAKER') && version.value?.createdBy === props.user.operatorId) || (can(props.user, 'TELLER') && props.transaction))
function clearImage() { imageRequest++; if (image.value) URL.revokeObjectURL(image.value); image.value = ''; imagePage.value = null }
function selectVersion(id) { versionId.value = id; clearImage(); imageBusy.value = false; checked.value = false; action.value = ''; error.value = '' }
async function load() {
  loading.value = true; error.value = ''
  try {
    card.value = await get(`/seal-cards/${props.cardId}`)
    if (!card.value.versions?.some(item => String(item.versionId) === String(versionId.value))) {
      const target = props.initialVersionId || (props.review ? card.value.versions?.find(item => item.status === 'PENDING_APPROVAL')?.versionId : card.value.currentVersionId) || card.value.versions?.[0]?.versionId
      versionId.value = target
      if (props.review) detailTab.value = 'images'
    }
  } catch (e) { error.value = errorText(e) } finally { loading.value = false }
}
async function viewImage(pageNo) {
  imageBusy.value = true; error.value = ''; clearImage()
  const requestId = imageRequest, targetVersion = version.value.versionId
  try {
    const blob = await imageBlob(props.cardId, targetVersion, pageNo, props.transaction || { purpose: props.review ? '建库复核' : '建档资料核对' })
    if (disposed || requestId !== imageRequest) return
    image.value = URL.createObjectURL(blob); imagePage.value = pageNo
  } catch (e) { if (!disposed && requestId === imageRequest) error.value = errorText(e) } finally { if (requestId === imageRequest) imageBusy.value = false }
}
async function startAction(type) {
  action.value = type; error.value = ''; notice.value = ''; input.reason = ''; checked.value = false
  if (type === 'accounts') {
    busy.value = true
    try { customer.value = await get('/customers', { customerNo: card.value.customerNo }); input.accountNos = [...(version.value?.accountNos || card.value.accountNos || [])] }
    catch (e) { error.value = errorText(e) } finally { busy.value = false }
  }
}
async function act() {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    const root = `/seal-cards/${props.cardId}`
    if (action.value === 'approve') {
      if (!checked.value) throw new Error('请确认已核对全部影像、适用账户及授权依据')
      await post(`${root}/versions/${version.value.versionId}/approve`, { effectiveFrom: localTimestamp(input.effectiveFrom) })
    } else if (action.value === 'reject') {
      if (!input.reason.trim()) throw new Error('请填写驳回原因')
      await post(`${root}/versions/${version.value.versionId}/reject`, { reason: input.reason.trim() })
    } else if (action.value === 'disable') {
      if (!input.reason.trim()) throw new Error('请填写停用原因')
      await post(`${root}/disable`, { reason: input.reason.trim() })
    } else if (action.value === 'accounts') {
      if (!input.accountNos.length || !input.changeReason.trim()) throw new Error('请选择账户并填写范围变更原因')
      if (input.accountNos.length > 1 && !input.authorizationRef.trim()) throw new Error('多账户共用需填写授权依据')
      await post(`${root}/accounts`, { accountNos: input.accountNos, authorizationRef: input.authorizationRef, changeReason: input.changeReason })
    }
    notice.value = action.value === 'accounts' ? '账户范围变更申请已提交，复核后生效' : '操作已完成'
    action.value = ''; clearImage(); await load(); emit('change')
  } catch (e) { error.value = errorText(e) } finally { busy.value = false }
}
onMounted(load)
onBeforeUnmount(() => { disposed = true; clearImage() })
</script>

<template>
  <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-alert v-if="notice" :title="notice" type="success" :closable="false" />
  <div v-if="loading && !card" v-loading="loading" class="loading-area" />
  <template v-if="card"><el-alert v-if="card.status==='DISABLED'" :title="`已停用：${card.disableReason || '—'}，${date(card.disabledAt)}`" type="warning" :closable="false" />
    <div class="detail-toolbar"><el-button v-if="can(user,'MAKER') && card.status!=='DISABLED'" type="primary" @click="$emit('version-upload',card)">变更影像</el-button><el-button v-if="can(user,'MAKER') && card.status!=='DISABLED'" @click="startAction('accounts')">变更账户范围</el-button><el-button v-if="can(user,'CHECKER') && card.status!=='DISABLED'" type="danger" plain @click="startAction('disable')">停用 / 挂失</el-button><span class="muted">当前版本：{{ currentVersionLabel }}</span></div>
    <el-tabs v-model="detailTab" type="border-card">
      <el-tab-pane label="基本信息" name="basic"><el-descriptions :column="2" border><el-descriptions-item label="印鉴卡号">{{ card.cardNo }}</el-descriptions-item><el-descriptions-item label="客户号">{{ card.customerNo }}</el-descriptions-item><el-descriptions-item label="客户名称">{{ card.customerName }}</el-descriptions-item><el-descriptions-item label="管理机构">{{ card.branchNo }}</el-descriptions-item><el-descriptions-item label="状态"><StatusBadge :value="card.status" /></el-descriptions-item><el-descriptions-item label="录入人员">{{ card.createdBy }}</el-descriptions-item><el-descriptions-item label="建档时间">{{ date(card.createdAt) }}</el-descriptions-item><el-descriptions-item label="备注">{{ card.remark || '—' }}</el-descriptions-item></el-descriptions></el-tab-pane>
      <el-tab-pane label="版本记录" name="versions"><el-table :data="card.versions || []" border stripe highlight-current-row><el-table-column prop="versionNo" label="版本" width="75" /><el-table-column label="状态" width="110"><template #default="{ row }"><StatusBadge :value="row.status" /></template></el-table-column><el-table-column prop="createdBy" label="录入人" width="110" /><el-table-column prop="approvedBy" label="复核人" width="110" /><el-table-column label="生效时间" min-width="175"><template #default="{ row }">{{ date(row.effectiveFrom) }}</template></el-table-column><el-table-column label="失效时间" min-width="175"><template #default="{ row }">{{ date(row.effectiveTo) }}</template></el-table-column><el-table-column prop="changeReason" label="变更原因" min-width="140" show-overflow-tooltip /><el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="selectVersion(row.versionId); detailTab='images'">查看</el-button></template></el-table-column></el-table></el-tab-pane>
      <el-tab-pane label="账户适用范围" name="accounts"><el-form inline class="version-select"><el-form-item label="版本"><el-select v-model="versionId" @change="selectVersion"><el-option v-for="item in card.versions" :key="item.versionId" :value="item.versionId" :label="`V${item.versionNo}`" /></el-select></el-form-item><el-form-item v-if="version"><StatusBadge :value="version.status" /></el-form-item></el-form><el-table :data="version?.bindings || []" border stripe><el-table-column type="index" label="序号" width="65" /><el-table-column prop="accountNo" label="适用结算账号" min-width="180" /><el-table-column label="关系状态" width="110"><template #default="{ row }"><StatusBadge :value="row.bindingStatus" /></template></el-table-column><el-table-column prop="approvedBy" label="复核人" width="110" /><el-table-column label="生效时间" min-width="180"><template #default="{ row }">{{ date(row.effectiveFrom) }}</template></el-table-column><el-table-column label="失效时间" min-width="180"><template #default="{ row }">{{ date(row.effectiveTo) }}</template></el-table-column></el-table><el-descriptions v-if="version" :column="1" border class="section-block"><el-descriptions-item label="授权依据">{{ version.authorizationRef || '—' }}</el-descriptions-item><el-descriptions-item label="变更原因">{{ version.changeReason || '—' }}</el-descriptions-item></el-descriptions></el-tab-pane>
      <el-tab-pane label="影像资料 / 复核" name="images"><el-form inline class="version-select"><el-form-item label="版本"><el-select v-model="versionId" @change="selectVersion"><el-option v-for="item in card.versions" :key="item.versionId" :value="item.versionId" :label="`V${item.versionNo}`" /></el-select></el-form-item><el-form-item v-if="version"><StatusBadge :value="version.status" /></el-form-item></el-form>
        <template v-if="version"><el-descriptions :column="2" border><el-descriptions-item label="录入人">{{ version.createdBy }}</el-descriptions-item><el-descriptions-item label="复核人">{{ version.approvedBy || '未复核' }}</el-descriptions-item><el-descriptions-item label="适用账户">{{ version.accountNos?.join('、') || '—' }}</el-descriptions-item><el-descriptions-item label="授权依据">{{ version.authorizationRef || '—' }}</el-descriptions-item><el-descriptions-item label="生效时间">{{ date(version.effectiveFrom) }}</el-descriptions-item><el-descriptions-item label="失效时间">{{ date(version.effectiveTo) }}</el-descriptions-item><el-descriptions-item label="变更原因">{{ version.changeReason || '—' }}</el-descriptions-item><el-descriptions-item label="驳回原因">{{ version.rejectReason || '—' }}</el-descriptions-item></el-descriptions><el-alert v-if="!canSeeImages" title="当前角色或流程无影像读取权限。交易柜员请从交易调阅入口进入。" type="info" :closable="false" class="section-block" /><el-table :data="version.images || []" border stripe class="section-block"><el-table-column prop="pageNo" label="页码" width="70" /><el-table-column prop="mimeType" label="文件类型" width="130" /><el-table-column label="大小" width="100"><template #default="{ row }">{{ bytes(row.fileSize) }}</template></el-table-column><el-table-column prop="sha256" label="SHA-256摘要" min-width="220" show-overflow-tooltip /><el-table-column label="操作" width="115"><template #default="{ row }"><el-button link type="primary" :disabled="!canSeeImages || imageBusy" @click="viewImage(row.pageNo)">查看影像</el-button></template></el-table-column></el-table><div v-if="imageBusy" v-loading="imageBusy" class="loading-area" /><div v-if="image" class="image-preview"><div class="section-heading">第 {{ imagePage }} 页 <el-button link @click="clearImage">关闭影像</el-button></div><el-image :src="image" fit="contain" :alt="`版本${version.versionNo}第${imagePage}页`" /></div><div v-if="review && version.status==='PENDING_APPROVAL'" class="detail-toolbar section-block"><el-alert v-if="!canReview" title="录入人与复核人必须分离，当前身份不可复核此申请。" type="warning" :closable="false" /><template v-else><el-button type="primary" :disabled="busy" @click="startAction('approve')">复核通过</el-button><el-button type="danger" plain :disabled="busy" @click="startAction('reject')">驳回</el-button></template></div></template>
      </el-tab-pane>
    </el-tabs>
    <section v-if="action" class="action-section"><div class="section-heading">{{ { approve:'复核生效',reject:'驳回申请',disable:'停用 / 挂失',accounts:'账户范围变更' }[action] }}</div><el-form :model="input" label-width="110px" :disabled="busy" @submit.prevent="act"><template v-if="action==='approve'"><el-form-item label="生效时间"><el-date-picker v-model="input.effectiveFrom" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss.SSS" placeholder="留空表示立即生效" clearable /></el-form-item><el-form-item><el-checkbox v-model="checked">已核对全部影像、适用账户和授权依据。</el-checkbox></el-form-item></template><template v-else-if="action==='accounts'"><el-form-item label="完整账户范围" required><el-select v-model="input.accountNos" multiple filterable><el-option v-for="account in customer?.accounts" :key="account.accountNo" :value="account.accountNo" :label="account.accountNo" :disabled="!['ACTIVE','NORMAL','ENABLED'].includes(account.status)" /></el-select></el-form-item><el-form-item label="授权依据"><el-input v-model="input.authorizationRef" maxlength="128" /></el-form-item><el-form-item label="变更原因" required><el-input v-model="input.changeReason" type="textarea" maxlength="500" show-word-limit /></el-form-item></template><template v-else><el-alert v-if="action==='disable'" title="停用将终止当前版本及适用关系，历史资料保留。请确认后提交。" type="warning" :closable="false" /><el-form-item :label="action==='reject'?'驳回原因':'停用原因'" required><el-input v-model="input.reason" type="textarea" maxlength="500" show-word-limit /></el-form-item></template><el-form-item><el-button type="primary" :loading="busy" native-type="submit">确认提交</el-button><el-button @click="action=''">取消</el-button></el-form-item></el-form></section>
  </template>
</template>
