<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { get } from '../api'
import { can, date, errorText, label } from '../display'
import StatusBadge from '../components/StatusBadge.vue'
import Pager from '../components/Pager.vue'
import OverlayDialog from '../components/OverlayDialog.vue'
import UploadForm from '../components/UploadForm.vue'
import CardDetail from '../components/CardDetail.vue'
const props = defineProps({ user: Object, review: Boolean, notify: Function })
const filter = reactive({ customerNo: '', accountNo: '', status: props.review ? 'PENDING_APPROVAL' : '' })
const uploadBusy = ref(false)
const rows = ref([]), total = ref(0), page = ref(1), busy = ref(false), error = ref(''), selected = ref(null), uploadCard = ref(null), uploadOpen = ref(false), progress = ref(null)
let timer
async function load(next = page.value) {
  busy.value = true; error.value = ''
  try { const data = await get('/seal-cards', { ...filter, page: next, pageSize: 20 }); rows.value = data.items || []; total.value = data.total; page.value = next }
  catch (e) { error.value = errorText(e); rows.value = []; total.value = 0 } finally { busy.value = false }
}
function create(card = null) { uploadCard.value = card; selected.value = null; uploadOpen.value = true }
async function uploaded(result) {
  uploadOpen.value = false; props.notify('资料已受理，影像就绪后进入复核流程')
  progress.value = result; load(1)
  if (result.taskId && ['UPLOADING', 'PENDING', 'PROCESSING', 'RETRY'].includes(result.status)) {
    clearInterval(timer)
    timer = setInterval(async () => {
      try {
        const task = await get(`/upload-tasks/${result.taskId}`)
        progress.value = { ...task, cardId: result.cardId }
        if (['SUCCESS', 'FAILED'].includes(task.status)) { clearInterval(timer); load(1) }
      } catch { clearInterval(timer) }
    }, 3000)
  }
}
watch(() => props.review, value => { filter.status = value ? 'PENDING_APPROVAL' : ''; load(1) })
onMounted(() => load(1))
onBeforeUnmount(() => clearInterval(timer))
</script>

<template><section class="business-page">
  <el-form :inline="true" :model="filter" class="search-form" @submit.prevent="load(1)"><el-form-item label="客户号"><el-input v-model="filter.customerNo" clearable placeholder="输入客户号" /></el-form-item><el-form-item label="结算账号"><el-input v-model="filter.accountNo" clearable placeholder="输入结算账号" /></el-form-item><el-form-item label="状态"><el-select v-model="filter.status" clearable placeholder="全部状态"><el-option v-for="status in ['PENDING_APPROVAL','APPROVED','ACTIVE','UPLOADING','REJECTED','FAILED','DISABLED']" :key="status" :value="status" :label="label(status)" /></el-select></el-form-item><el-form-item><el-button type="primary" native-type="submit" :loading="busy">查询</el-button><el-button @click="Object.assign(filter, { customerNo: '', accountNo: '', status: review ? 'PENDING_APPROVAL' : '' }); load(1)">重置</el-button><el-button v-if="!review && can(user,'MAKER')" type="primary" @click="create()">新增</el-button></el-form-item></el-form>
  <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-alert v-if="progress" type="info" :closable="false"><template #title>最近申请：<StatusBadge :value="progress.status" /><el-button link @click="selected = String(progress.cardId)">查看档案</el-button></template>{{ progress.lastErrorMessage }}</el-alert>
  <el-table :data="rows" border stripe highlight-current-row v-loading="busy" empty-text="暂无符合条件的记录"><el-table-column type="index" label="序号" width="65" align="center" /><el-table-column label="印鉴卡号" min-width="190" show-overflow-tooltip><template #default="{ row }"><el-button link type="primary" @click="selected = String(row.cardId)">{{ row.cardNo }}</el-button></template></el-table-column><el-table-column prop="customerNo" label="客户号" min-width="110" /><el-table-column prop="customerName" label="客户名称" min-width="180" show-overflow-tooltip /><el-table-column label="适用结算账户" min-width="180"><template #default="{ row }"><div v-for="no in row.accountNos" :key="no">{{ no }}</div></template></el-table-column><el-table-column label="版本" width="75"><template #default="{ row }">{{ row.versionNo ? 'V' + row.versionNo : '—' }}</template></el-table-column><el-table-column label="状态" width="110"><template #default="{ row }"><StatusBadge :value="row.status" /></template></el-table-column><el-table-column prop="branchNo" label="机构" width="90" /><el-table-column label="建档时间" width="180"><template #default="{ row }">{{ date(row.createdAt) }}</template></el-table-column><el-table-column label="操作" width="120" fixed="right"><template #default="{ row }"><el-button size="small" type="primary" link @click="selected = String(row.cardId)">{{ review ? '查看 / 复核' : '详情' }}</el-button></template></el-table-column></el-table>
  <Pager :page="page" :total="total" :busy="busy" @change="load" />
  <OverlayDialog v-if="selected" title="印鉴档案详情" wide @close="selected = null"><CardDetail :card-id="selected" :user="user" :review="review" @change="load()" @version-upload="create" /></OverlayDialog>
  <OverlayDialog v-if="uploadOpen" :title="uploadCard ? '印鉴版本变更' : '新增印鉴档案'" wide :busy="uploadBusy" @close="uploadOpen = false"><UploadForm :card="uploadCard" @busy="uploadBusy = $event" @cancel="uploadOpen = false" @complete="uploaded" /></OverlayDialog>
</section></template>
