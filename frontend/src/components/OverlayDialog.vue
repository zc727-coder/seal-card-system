<script setup>
defineProps({ title: String, wide: Boolean, busy: Boolean })
defineEmits(['close'])
</script>
<template><el-dialog :model-value="true" :title="title" :width="wide ? '1100px' : '650px'" align-center append-to-body destroy-on-close :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" @close="$emit('close')"><slot /></el-dialog></template>
