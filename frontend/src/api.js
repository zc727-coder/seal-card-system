const BASE = '/api/v1'

export class ApiError extends Error {
  constructor(message, status, code, requestId, details) {
    super(message)
    Object.assign(this, { status, code, requestId, details })
  }
}

export function query(values = {}) {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params.set(key, String(value))
  })
  return params.toString() ? `?${params}` : ''
}

export async function request(path, options = {}) {
  const headers = new Headers(options.headers)
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  let response
  try {
    response = await fetch(`${BASE}${path}`, { credentials: 'same-origin', ...options, headers,
      signal: options.signal || AbortSignal.timeout(options.body instanceof FormData ? 120000 : 30000) })
  } catch (error) {
    throw new ApiError(error.name === 'TimeoutError' ? '请求超时，请稍后重试' : '无法连接服务，请检查服务是否启动', 0)
  }
  let result
  try { result = await response.json() } catch { throw new ApiError(`服务返回了无法识别的响应（HTTP ${response.status}）`, response.status) }
  if (!response.ok || result.code !== '0') {
    if (response.status === 401 && path !== '/auth/login' && path !== '/auth/me') window.dispatchEvent(new Event('session-expired'))
    throw new ApiError(result.message || `请求失败（HTTP ${response.status}）`, response.status, result.code, result.requestId, result.data)
  }
  return result.data
}

export const get = (path, params) => request(path + query(params))
export const post = (path, body) => request(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })
export async function upload(path, metadata, files, key) {
  const body = new FormData()
  body.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
  files.forEach(file => body.append('files', file))
  return request(path, { method: 'POST', body, headers: { 'Idempotency-Key': key } })
}

export async function imageBlob(cardId, versionId, pageNo, context) {
  const path = `/seal-cards/${encodeURIComponent(cardId)}/versions/${encodeURIComponent(versionId)}/images/${pageNo}`
  const response = await fetch(BASE + path + query(context), { credentials: 'same-origin', signal: AbortSignal.timeout(30000) })
  if (!response.ok) {
    let result = {}
    try { result = await response.json() } catch { /* upstream may return plain text */ }
    if (response.status === 401) window.dispatchEvent(new Event('session-expired'))
    throw new ApiError(result.message || `影像读取失败（HTTP ${response.status}）`, response.status, result.code, result.requestId)
  }
  return response.blob()
}
