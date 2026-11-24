<script setup>
import { reactive, ref, watch } from 'vue'
import { get } from '../api'
import { date, errorText, transactionAccountSelectable } from '../display'
import StatusBadge from '../components/StatusBadge.vue'
import OverlayDialog from '../components/OverlayDialog.vue'
import CardDetail from '../components/CardDetail.vue'
defineProps({ user: Object })
const form = reactive({ customerNo: '', accountNo: '', transactionType: 'PAYMENT', documentIssueDate: '', purpose: '' })
const selectionNotice = ref('')
const customer = ref(null), busy = ref(false), error = ref(''), queried = ref(false), rows = ref([]), selected = ref(null), submitted = ref(null)
watch(form, () => { queried.value = false; rows.value = []; selected.value = null })
const selectable = account => transactionAccountSelectable(account, form.transactionType, form.documentIssueDate)
watch([() => form.transactionType, () => form.documentIssueDate], () => {
  const account = customer.value?.accounts?.find(item => item.accountNo === form.accountNo)
  if (form.accountNo && !selectable(account)) form.accountNo = ''
})
async function verify() {
  busy.value = true; error.value = ''; customer.value = null; form.accountNo = ''; rows.value = []; queried.value = false
  try { customer.value = await get('/customers', { customerNo: form.customerNo.trim() }) }
  catch (e) { error.value = errorText(e) } finally { busy.value = false }
}
async function search() {
  busy.value = true; error.value = ''; rows.value = []; queried.value = false
  try {
    if (!customer.value || !form.accountNo) throw new Error('请先核验客户并选择实际付款账号')
    if (!selectable(customer.value.accounts?.find(item => item.accountNo === form.accountNo))) throw new Error('该账户不能用于当前查询条件')
    if (!form.purpose.trim()) throw new Error('请填写本次调阅的业务用途或流程编号')
    const criteria = { ...form, customerNo: customer.value.customerNo, purpose: form.purpose.trim() }
    if (criteria.transactionType !== 'CHECK') delete criteria.documentIssueDate
    if (!criteria.documentIssueDate) delete criteria.documentIssueDate
    const formSnapshot = JSON.stringify(form)
    const data = await get('/transactions/seals', criteria)
    if (formSnapshot !== JSON.stringify(form)) return
    submitted.value = criteria; rows.value = data.items || []; selectionNotice.value = data.selectionNotice || data.notice || ''; queried.value = true
  } catch (e) { error.value = errorText(e) } finally { busy.value = false }
}
</script>
<template><section class="business-page"><el-form :inline="true" :model="form" :disabled="busy" class="search-form transaction-search" @submit.prevent="search"><el-form-item label="客户号" required><el-input v-model="form.customerNo" placeholder="输入客户号" @input="customer=null; form.accountNo=''; queried=false"><template #append><el-button :loading="busy" @click="verify">核验</el-button></template></el-input></el-form-item><el-form-item v-if="customer" label="客户名称"><el-input :model-value="customer.customerName" readonly /></el-form-item><el-form-item label="付款账号" required><el-select v-model="form.accountNo" :disabled="!customer" placeholder="先核验客户"><el-option v-for="account in customer?.accounts" :key="account.accountNo" :value="account.accountNo" :label="`${account.accountNo}${account.status==='CLOSED'?' / 已销户':''}`" :disabled="!selectable(account)" /></el-select></el-form-item><el-form-item label="交易类型"><el-select v-model="form.transactionType" @change="form.documentIssueDate=''"><el-option label="付款业务" value="PAYMENT" /><el-option label="票据业务" value="CHECK" /></el-select></el-form-item><el-form-item v-if="form.transactionType==='CHECK'" label="凭证签发日期"><el-date-picker v-model="form.documentIssueDate" type="date" value-format="YYYY-MM-DD" placeholder="留空查询当前版本" clearable /></el-form-item><el-form-item label="业务用途" required><el-input v-model="form.purpose" maxlength="128" placeholder="业务用途或流程编号" /></el-form-item><el-form-item><el-button type="primary" native-type="submit" :loading="busy">查询</el-button></el-form-item></el-form><el-alert v-if="form.transactionType==='CHECK' && form.documentIssueDate" title="已销户账号仅可用于历史查询，适用性仍由后端校验。" type="info" :closable="false" /><el-alert v-if="error" :title="error" type="error" :closable="false" /><el-alert v-if="queried" :title="selectionNotice || '请按实际账户和业务规则核实候选印鉴。'" type="info" :closable="false" />
<el-table :data="rows" border stripe v-loading="busy" :empty-text="queried?'无符合条件的有效印鉴':'填写交易条件后查询'"><el-table-column type="index" label="序号" width="65" /><el-table-column prop="cardNo" label="印鉴卡号" min-width="190" /><el-table-column prop="customerNo" label="客户号" min-width="110" /><el-table-column prop="customerName" label="客户名称" min-width="180" /><el-table-column label="付款账号" min-width="180"><template #default>{{ submitted?.accountNo }}</template></el-table-column><el-table-column prop="versionNo" label="版本" width="75" /><el-table-column label="状态" width="100"><template #default="{ row }"><StatusBadge :value="row.status" /></template></el-table-column><el-table-column label="生效时间" min-width="180"><template #default="{ row }">{{ date(row.effectiveFrom) }}</template></el-table-column><el-table-column label="失效时间" min-width="180"><template #default="{ row }">{{ date(row.effectiveTo) }}</template></el-table-column><el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="selected=row">调阅影像</el-button></template></el-table-column></el-table><div v-if="queried" class="result-total">共 {{ rows.length }} 项候选</div><OverlayDialog v-if="selected" title="交易印鉴调阅" wide @close="selected=null"><el-descriptions :column="2" border class="section-block"><el-descriptions-item label="付款账号">{{ submitted.accountNo }}</el-descriptions-item><el-descriptions-item label="业务用途">{{ submitted.purpose }}</el-descriptions-item></el-descriptions><CardDetail :card-id="String(selected.cardId)" :initial-version-id="String(selected.versionId)" :user="user" :transaction="submitted" /></OverlayDialog></section></template>
