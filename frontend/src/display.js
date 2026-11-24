export const labels = {
  QUERY_CUSTOMER: '客户账户核验', QUERY_TRANSACTION: '交易印鉴查询', ACCESS_DENIED: '访问被拒绝', RETRY_UPLOAD: '上传任务重试', INVALID_REQUEST: '无效请求', ACCOUNT_RECONCILE: '账户状态核对',
  ENABLED: '允许使用', DISABLED: '已停用', UPLOADING: '影像处理中', PENDING_APPROVAL: '待复核', APPROVED: '预约生效', ACTIVE: '已生效', ARCHIVED: '已归档', FAILED: '失败', REJECTED: '已驳回', PENDING: '待处理', PROCESSING: '处理中', SUCCESS: '成功', RETRY: '待重试', ENDED: '已终止', READY: '已就绪', FAIL: '失败', NORMAL: '正常', CLOSED: '已销户', MAKER: '录入柜员', CHECKER: '复核柜员', TELLER: '交易柜员', AUDITOR: '审计人员', ADMIN: '系统管理员', UPLOAD: '上传影像', SUBMIT: '提交复核', QUERY: '查询资料', APPROVE: '复核通过', REJECT: '驳回申请', ACTIVATE: '版本生效', DISABLE: '停用印鉴', BIND: '维护账户', UNBIND: '解除关联', VIEW_IMAGE: '调阅影像', LOGIN: '登录', PAYMENT: '付款业务', CHECK: '票据业务',
}
export const auditActions = ['LOGIN', 'UPLOAD', 'SUBMIT', 'QUERY', 'QUERY_CUSTOMER', 'QUERY_TRANSACTION', 'APPROVE', 'REJECT', 'ACTIVATE', 'DISABLE', 'BIND', 'UNBIND', 'VIEW_IMAGE', 'RETRY_UPLOAD', 'ACCESS_DENIED', 'INVALID_REQUEST', 'ACCOUNT_RECONCILE']
export function transactionAccountSelectable(account, transactionType, documentIssueDate) {
  if (!account) return false
  if (['ACTIVE', 'NORMAL', 'ENABLED'].includes(account.status)) return true
  return account.status === 'CLOSED' && transactionType === 'CHECK' && !!documentIssueDate
}
export const label = value => labels[value] || value || '—'
export const date = value => value ? String(value).replace('T', ' ').replace(/\.\d+(?:[+-].*)?$/, '').replace(/Z$/, '') : '—'
export const bytes = value => Number(value) >= 1048576 ? `${(Number(value) / 1048576).toFixed(1)} MB` : `${Math.ceil(Number(value || 0) / 1024)} KB`
export const can = (user, ...roles) => roles.some(role => user?.roles?.includes(role))
export const errorText = error => {
  const fields = error.details?.fieldErrors
  const detail = fields && typeof fields === 'object' ? Object.entries(fields).map(([key, value]) => `${key}：${value}`).join('；') : ''
  return `${error.message || '操作失败'}${detail ? ` · ${detail}` : ''}${error.requestId ? ` · 请求编号 ${error.requestId}` : ''}`
}
export function localTimestamp(value) { return value ? `${value}${value.length === 16 ? ':00.000' : ''}` : null }
