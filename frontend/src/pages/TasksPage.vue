<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { get, post } from '../api'
import { date, errorText } from '../display'
import StatusBadge from '../components/StatusBadge.vue'
import Pager from '../components/Pager.vue'
import OverlayDialog from '../components/OverlayDialog.vue'
import CardDetail from '../components/CardDetail.vue'
const props = defineProps({ user: Object, notify: Function })
const filter = ref(''), page = ref(1), rows = ref([]), total = ref(0), busy = ref(false), error = ref(''), selected = ref(null), retrying = ref(null)
let timer
async function load(next = page.value, silent = false) {
  if (busy.value) return
  busy.value = true; if (!silent) error.value = ''
  try { const data = await get('/upload-tasks', { status: filter.value, page: next, pageSize: 20 }); rows.value = data.items || []; total.value = data.total; page.value = next }
  catch (e) { error.value = errorText(e) } finally { busy.value = false }
}
async function retry(task) {
  retrying.value = task.taskId; error.value = ''
  try { await post(`/upload-tasks/${task.taskId}/retry`); props.notify('重试申请已受理，请关注任务状态'); await load() }
  catch (e) { error.value = errorText(e) } finally { retrying.value = null }
}
onMounted(() => { load(1); timer = setInterval(() => { if (rows.value.some(row => ['PENDING', 'PROCESSING', 'RETRY'].includes(row.status))) load(page.value, true) }, 5000) })
onBeforeUnmount(() => clearInterval(timer))
</script>
<template><section class="business-page"><el-form inline class="search-form" @submit.prevent="load(1)"><el-form-item label="任务状态"><el-select v-model="filter" clearable placeholder="全部状态"><el-option v-for="(text,value) in { PENDING:'待处理',PROCESSING:'处理中',RETRY:'待重试',FAILED:'失败',SUCCESS:'成功' }" :key="value" :value="value" :label="text" /></el-select></el-form-item><el-form-item><el-button type="primary" native-type="submit" :loading="busy">查询</el-button><el-button @click="filter=''; load(1)">重置</el-button><el-button :loading="busy" @click="load()">刷新</el-button></el-form-item></el-form><el-alert v-if="error" :title="error" type="error" :closable="false" /><el-table :data="rows" border stripe v-loading="busy" empty-text="暂无上传任务"><el-table-column prop="taskId" label="任务ID" min-width="100" /><el-table-column prop="cardId" label="印鉴卡ID" min-width="100" /><el-table-column prop="versionId" label="版本ID" min-width="100" /><el-table-column label="状态" width="110"><template #default="{ row }"><StatusBadge :value="row.status" /></template></el-table-column><el-table-column prop="retryCount" label="重试次数" width="95" /><el-table-column prop="lastErrorCode" label="最近错误码" min-width="170" /><el-table-column prop="lastErrorMessage" label="错误说明" min-width="220" show-overflow-tooltip /><el-table-column label="下次重试" width="180"><template #default="{ row }">{{ date(row.nextRetryAt) }}</template></el-table-column><el-table-column label="更新时间" width="180"><template #default="{ row }">{{ date(row.updatedAt || row.createdAt) }}</template></el-table-column><el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button v-if="row.cardId" link type="primary" @click="selected=String(row.cardId)">详情</el-button><el-button v-if="['FAILED','RETRY'].includes(row.status)" link type="primary" :loading="retrying===row.taskId" :disabled="!!retrying" @click="retry(row)">重试</el-button></template></el-table-column></el-table><Pager :page="page" :total="total" :busy="busy" @change="load" /><OverlayDialog v-if="selected" title="任务关联档案" wide @close="selected=null"><CardDetail :card-id="selected" :user="user" /></OverlayDialog></section></template>
