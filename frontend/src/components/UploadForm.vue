<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { get, upload } from '../api'
import { bytes, errorText } from '../display'
const props = defineProps({ card: Object })
const emit = defineEmits(['complete', 'cancel', 'busy'])
const busy = ref(false), error = ref(''), customer = ref(null), files = ref([])
const form = reactive({ customerNo: props.card?.customerNo || '', accountNos: [...(props.card?.accountNos || [])], remark: props.card?.remark || '', authorizationRef: '', changeReason: '' })
const selected = computed(() => customer.value?.accounts?.filter(account => form.accountNos.includes(account.accountNo)) || [])
let signature = '', idemKey = ''
watch(busy, value => emit('busy', value))
async function verify() {
  error.value = ''; busy.value = true
  try {
    if (!form.customerNo.trim()) throw new Error('请输入客户号')
    customer.value = await get('/customers', { customerNo: form.customerNo.trim() })
    form.customerNo = customer.value.customerNo
    form.accountNos = form.accountNos.filter(no => customer.value.accounts?.some(account => account.accountNo === no && ['ACTIVE', 'NORMAL', 'ENABLED'].includes(account.status)))
  } catch (e) { customer.value = null; error.value = errorText(e) } finally { busy.value = false }
}
function addSelected(entry) {
  error.value = ''
  const file = entry.raw
  if (!file) return
  if (!['image/png','image/jpeg'].includes(file.type)) { error.value = '仅支持PNG、JPEG影像'; return }
  if (file.size > 10 * 1024 * 1024) { error.value = '单页不能超过10MB'; return }
  if (files.value.length >= 20) { error.value = '最多上传20页'; return }
  files.value.push({ file, url: URL.createObjectURL(file), id: crypto.randomUUID() })
}
function remove(index) { URL.revokeObjectURL(files.value[index].url); files.value.splice(index, 1) }
function move(index, offset) { const item = files.value.splice(index, 1)[0]; files.value.splice(index + offset, 0, item) }
async function submit() {
  busy.value = true; error.value = ''
  try {
    if (!customer.value || !form.accountNos.length) throw new Error('请先核验客户并选择结算账户')
    if (form.accountNos.length > 1 && !form.authorizationRef.trim()) throw new Error('多账户共用须填写授权依据')
    if (!files.value.length) throw new Error('请上传至少一页影像')
    if (props.card && !form.changeReason.trim()) throw new Error('请填写变更原因')
    const metadata = { ...form, customerNo: form.customerNo.trim() }
    const current = JSON.stringify([metadata, files.value.map(({ file }) => [file.name, file.size, file.lastModified])])
    if (signature !== current) { signature = current; idemKey = crypto.randomUUID() }
    const result = await upload(props.card ? `/seal-cards/${props.card.cardId}/versions` : '/seal-cards', metadata, files.value.map(entry => entry.file), idemKey)
    emit('complete', result)
  } catch (e) { error.value = errorText(e) } finally { busy.value = false }
}
onBeforeUnmount(() => files.value.forEach(entry => URL.revokeObjectURL(entry.url)))
if (props.card) verify()
</script>

<template><el-alert v-if="error" :title="error" type="error" :closable="false" /><el-form :model="form" label-width="110px" :disabled="busy" @submit.prevent="submit"><el-form-item label="客户号" required><el-input v-model="form.customerNo" :readonly="!!card" placeholder="输入客户号" @input="customer=null"><template #append><el-button :loading="busy" @click="verify">核验客户</el-button></template></el-input></el-form-item><el-form-item v-if="customer" label="客户名称"><el-input :model-value="`${customer.customerName} / ${customer.branchNo}`" readonly /></el-form-item><el-form-item label="适用结算账户" required><el-select v-model="form.accountNos" multiple filterable :disabled="!customer" placeholder="先核验客户，再选择账户"><el-option v-for="account in customer?.accounts" :key="account.accountNo" :value="account.accountNo" :label="`${account.accountNo} / ${account.accountName}${account.status==='CLOSED'?' / 已销户':''}`" :disabled="!['ACTIVE','NORMAL','ENABLED'].includes(account.status)" /></el-select></el-form-item><el-form-item label="授权依据" :required="form.accountNos.length>1"><el-input v-model="form.authorizationRef" maxlength="128" placeholder="多账户共用时必填" /></el-form-item><el-form-item v-if="card" label="变更原因" required><el-input v-model="form.changeReason" type="textarea" maxlength="500" show-word-limit /></el-form-item><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit /></el-form-item><el-form-item label="印鉴卡影像" required><el-upload :auto-upload="false" :show-file-list="false" accept="image/png,image/jpeg" multiple :on-change="addSelected"><el-button type="primary" plain>选择影像</el-button><template #tip><div class="el-upload__tip">PNG / JPEG，单页不超过10MB，最多20页。</div></template></el-upload></el-form-item><el-table :data="files" border stripe empty-text="请选择影像文件"><el-table-column label="页码" width="70"><template #default="{ $index }">{{ $index+1 }}</template></el-table-column><el-table-column label="预览" width="95"><template #default="{ row }"><el-image :src="row.url" :preview-src-list="files.map(item=>item.url)" preview-teleported fit="contain" class="upload-thumbnail" /></template></el-table-column><el-table-column label="文件名" min-width="180"><template #default="{ row }">{{ row.file.name }}</template></el-table-column><el-table-column label="大小" width="100"><template #default="{ row }">{{ bytes(row.file.size) }}</template></el-table-column><el-table-column label="操作" width="190"><template #default="{ $index }"><el-button link type="primary" :disabled="$index===0 || busy" @click="move($index,-1)">上移</el-button><el-button link type="primary" :disabled="$index===files.length-1 || busy" @click="move($index,1)">下移</el-button><el-button link type="danger" :disabled="busy" @click="remove($index)">移除</el-button></template></el-table-column></el-table><el-form-item class="section-block"><el-button type="primary" :loading="busy" native-type="submit">提交复核</el-button><el-button @click="$emit('cancel')">取消</el-button></el-form-item></el-form></template>
