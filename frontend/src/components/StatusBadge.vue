<script setup>
import { computed } from 'vue'
import { label } from '../display'
const props = defineProps({ value: String })
const type = computed(() => ['ACTIVE','SUCCESS','READY'].includes(props.value) ? 'success' : ['FAILED','FAIL','REJECTED','DISABLED'].includes(props.value) ? 'danger' : ['PENDING_APPROVAL','APPROVED','PROCESSING','RETRY','UPLOADING'].includes(props.value) ? 'warning' : 'info')
</script>
<template><el-tag :type="type" size="small" effect="light">{{ label(value) }}</el-tag></template>
