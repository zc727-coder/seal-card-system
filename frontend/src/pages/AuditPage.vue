<script setup>
import { onMounted, reactive, ref } from 'vue'
import { get } from '../api'
import { date, label, errorText, auditActions } from '../display'
import StatusBadge from '../components/StatusBadge.vue'
import Pager from '../components/Pager.vue'
const filter = reactive({ cardId: '', action: '', result: '', requestId: '' })
const page = ref(1), rows = ref([]), total = ref(0), busy = ref(false), error = ref('')
async function load(next = page.value) {
  busy.value = true; error.value = ''
  try { const data = await get('/audits', { ...filter, page: next, pageSize: 20 }); rows.value = data.items || []; total.value = data.total; page.value = next }
  catch (e) { error.value = errorText(e); rows.value = []; total.value = 0 } finally { busy.value = false }
}
onMounted(() => load(1))
</script>
<template><section class="business-page"><el-form :inline="true" :model="filter" class="search-form" @submit.prevent="load(1)"><el-form-item label="印鉴卡ID"><el-input v-model="filter.cardId" clearable /></el-form-item><el-form-item label="操作"><el-select v-model="filter.action" clearable placeholder="全部"><el-option v-for="value in auditActions" :key="value" :value="value" :label="label(value)" /></el-select></el-form-item><el-form-item label="结果"><el-select v-model="filter.result" clearable placeholder="全部"><el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAIL" /></el-select></el-form-item><el-form-item label="请求编号"><el-input v-model="filter.requestId" clearable /></el-form-item><el-form-item><el-button type="primary" native-type="submit" :loading="busy">查询</el-button><el-button @click="Object.assign(filter,{ cardId:'',action:'',result:'',requestId:'' }); load(1)">重置</el-button></el-form-item></el-form><el-alert v-if="error" :title="error" type="error" :closable="false" /><el-table :data="rows" border stripe v-loading="busy" empty-text="暂无审计记录"><el-table-column type="index" label="序号" width="65" /><el-table-column label="操作" min-width="120"><template #default="{ row }">{{ label(row.action) }}</template></el-table-column><el-table-column label="结果" width="95"><template #default="{ row }"><StatusBadge :value="row.result" /></template></el-table-column><el-table-column prop="sealCardId" label="印鉴卡ID" min-width="110" /><el-table-column prop="versionId" label="版本ID" min-width="100" /><el-table-column prop="operatorId" label="操作人员" min-width="120" /><el-table-column prop="branchNo" label="机构" width="90" /><el-table-column prop="purpose" label="业务用途" min-width="170" show-overflow-tooltip /><el-table-column prop="errorCode" label="错误码" min-width="160" /><el-table-column prop="requestId" label="请求编号" min-width="220" show-overflow-tooltip /><el-table-column label="发生时间" min-width="190"><template #default="{ row }">{{ date(row.createdAt) }}</template></el-table-column></el-table><Pager :page="page" :total="total" :busy="busy" @change="load" /></section></template>
