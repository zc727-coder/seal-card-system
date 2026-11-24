<script setup>
import { computed, onMounted, ref } from 'vue'
import { get } from '../api'
import { can, date, label, errorText } from '../display'
import StatusBadge from '../components/StatusBadge.vue'
const props = defineProps({ user: Object })
const emit = defineEmits(['navigate'])
const data = ref(null), error = ref(''), busy = ref(false)
const stats = computed(() => [{ label: '印鉴档案', value: data.value?.totalCards, route: 'archive', hint: '授权机构内的全部资料' }, { label: '待复核申请', value: data.value?.pendingApproval, route: 'review', hint: '资料就绪，等待独立复核' }, { label: '当前生效版本', value: data.value?.activeVersions, route: 'archive', hint: '交易仍须校验适用账户' }, { label: '失败上传任务', value: data.value?.failedTasks, route: 'tasks', hint: '需要恢复或人工核对' }])
async function load() { busy.value = true; error.value = ''; try { data.value = await get('/dashboard') } catch (e) { error.value = errorText(e) } finally { busy.value = false } }
onMounted(load)
</script>
<template><section class="business-page"><div class="page-toolbar"><h2>业务概览</h2><el-button :loading="busy" @click="load">刷新</el-button></div><el-alert v-if="error" :title="error" type="error" :closable="false" /><el-table :data="data ? [data] : []" border stripe v-loading="busy" empty-text="暂无统计"><el-table-column prop="totalCards" label="印鉴档案总数" /><el-table-column prop="pendingApproval" label="待复核申请" /><el-table-column prop="activeVersions" label="当前生效版本" /><el-table-column prop="failedTasks" label="失败上传任务" /></el-table><div class="section-heading">最近操作</div><el-table :data="data?.recentAudits || []" border stripe v-loading="busy" empty-text="暂无可见操作记录"><el-table-column label="操作" min-width="140"><template #default="{ row }">{{ label(row.action) }}</template></el-table-column><el-table-column label="结果" width="100"><template #default="{ row }"><StatusBadge :value="row.result" /></template></el-table-column><el-table-column prop="operatorId" label="操作人员" min-width="140" /><el-table-column prop="branchNo" label="机构" width="100" /><el-table-column label="发生时间" min-width="190"><template #default="{ row }">{{ date(row.createdAt) }}</template></el-table-column></el-table></section></template>
