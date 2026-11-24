<script setup>
defineProps({ page: Number, total: Number, size: { type: Number, default: 20 }, busy: Boolean })
defineEmits(['change'])
</script>
<template><div class="pagination-row"><el-pagination background :current-page="page" :page-size="size" :total="Number(total || 0)" :disabled="busy" layout="total, prev, pager, next, jumper" @current-change="$emit('change', $event)" /></div></template>
