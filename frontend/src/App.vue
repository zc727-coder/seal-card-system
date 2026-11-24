<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { get, post } from './api'
import { can, label, errorText } from './display'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import DashboardPage from './pages/DashboardPage.vue'
import ArchivePage from './pages/ArchivePage.vue'
import TransactionPage from './pages/TransactionPage.vue'
import AuditPage from './pages/AuditPage.vue'
import TasksPage from './pages/TasksPage.vue'

const user = ref(null), ready = ref(false), loginBusy = ref(false), loginError = ref(''), username = ref(''), password = ref(''), demoUsers = ref([])
const route = ref(location.hash.slice(1) || 'dashboard'), menuOpen = ref(false), toast = ref('')
let toastTimer
const nav = computed(() => [
  { id: 'dashboard', name: '工作台', allowed: true },
  { id: 'archive', name: '印鉴档案', allowed: can(user.value, 'MAKER', 'CHECKER', 'TELLER', 'ADMIN') },
  { id: 'review', name: '复核中心', allowed: can(user.value, 'CHECKER') },
  { id: 'transaction', name: '交易调阅', allowed: can(user.value, 'TELLER') },
  { id: 'audit', name: '审计中心', allowed: can(user.value, 'AUDITOR') },
  { id: 'tasks', name: '上传任务', allowed: can(user.value, 'MAKER', 'ADMIN') },
].filter(item => item.allowed))
const current = computed(() => nav.value.find(item => item.id === route.value))
function navigate(id) { route.value = id; location.hash = id; menuOpen.value = false }
function hashChanged() { route.value = location.hash.slice(1) || 'dashboard' }
function notify(text) { toast.value = text; clearTimeout(toastTimer); toastTimer = setTimeout(() => { toast.value = '' }, 7000) }
function expired() { user.value = null; password.value = ''; loginError.value = '登录状态已失效，请重新登录'; menuOpen.value = false }
async function login() {
  if (loginBusy.value) return
  loginBusy.value = true; loginError.value = ''
  try {
    if (!username.value.trim() || !password.value) throw new Error('请输入账号和密码')
    user.value = await post('/auth/login', { username: username.value.trim(), password: password.value })
    password.value = ''; navigate('dashboard')
  } catch (e) { loginError.value = errorText(e) } finally { loginBusy.value = false }
}
async function logout() {
  try { await post('/auth/logout'); user.value = null; password.value = ''; location.hash = ''; route.value = 'dashboard' }
  catch (e) { notify(errorText(e)) }
}
onMounted(async () => {
  window.addEventListener('session-expired', expired); window.addEventListener('hashchange', hashChanged)
  try { user.value = await get('/auth/me') } catch (e) { if (e.status !== 401) loginError.value = errorText(e) }
  finally { ready.value = true }
})
onBeforeUnmount(() => { window.removeEventListener('session-expired', expired); window.removeEventListener('hashchange', hashChanged); clearTimeout(toastTimer) })
</script>

<template><el-config-provider :locale="zhCn" size="small">
  <div v-if="!ready" class="boot-screen">正在恢复会话…</div>
  <main v-else-if="!user" class="login-layout"><section class="login-card"><h1>印鉴卡管理系统</h1><el-form label-position="top" @submit.prevent="login"><el-form-item label="账号" required><el-input v-model="username" autocomplete="username" placeholder="输入账号" /></el-form-item><el-form-item label="密码" required><el-input v-model="password" type="password" show-password autocomplete="current-password" placeholder="输入密码" /></el-form-item><el-alert v-if="loginError" :title="loginError" type="error" :closable="false" /><el-button type="primary" class="full-width" :loading="loginBusy" native-type="submit">登录</el-button></el-form></section></main>
  <el-container v-else class="admin-layout"><el-aside class="admin-aside" :class="{ 'mobile-open': menuOpen }" width="200px"><div class="system-name">印鉴卡管理系统</div><el-menu :default-active="route" background-color="#334157" text-color="#d5dce7" active-text-color="#fff" @select="navigate"><el-menu-item v-for="item in nav" :key="item.id" :index="item.id">{{ item.name }}</el-menu-item></el-menu></el-aside><div v-if="menuOpen" class="menu-backdrop" @click="menuOpen = false" /><el-container class="admin-main-container"><el-header class="admin-header" height="50px"><div><el-button class="mobile-menu" @click="menuOpen = !menuOpen">菜单</el-button><span>{{ current?.name || '无访问权限' }}</span></div><div class="identity"><el-tag type="info" size="small">演示环境</el-tag><span>{{ user.branchNo }}</span><span>{{ user.displayName || user.operatorId }}</span><span>{{ user.roles?.map(label).join(' / ') }}</span><el-button link @click="logout">退出</el-button></div></el-header><el-main class="admin-content"><el-breadcrumb separator="/"><el-breadcrumb-item>首页</el-breadcrumb-item><el-breadcrumb-item>{{ current?.name || '无访问权限' }}</el-breadcrumb-item></el-breadcrumb><DashboardPage v-if="route === 'dashboard'" :user="user" @navigate="navigate" /><ArchivePage v-else-if="(route === 'archive' && can(user, 'MAKER', 'CHECKER', 'TELLER', 'ADMIN')) || (route === 'review' && can(user, 'CHECKER'))" :key="route" :user="user" :review="route === 'review'" :notify="notify" /><TransactionPage v-else-if="route === 'transaction' && can(user, 'TELLER')" :user="user" /><AuditPage v-else-if="route === 'audit' && can(user, 'AUDITOR')" /><TasksPage v-else-if="route === 'tasks' && can(user, 'MAKER', 'ADMIN')" :user="user" :notify="notify" /><el-empty v-else description="当前岗位无权访问此页面"><el-button @click="navigate('dashboard')">返回工作台</el-button></el-empty></el-main></el-container></el-container>
  <div v-if="toast" class="toast-message" role="status">{{ toast }}<el-button link @click="toast = ''">关闭</el-button></div>
</el-config-provider></template>
