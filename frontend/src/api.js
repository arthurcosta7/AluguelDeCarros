const unavailable = 'Não foi possível conectar ao servidor. Verifique se o backend está em execução e tente novamente.'

async function request(url, options) {
  let response
  try { response = await fetch(url, options) }
  catch { throw new Error(unavailable) }
  if ([502, 503, 504].includes(response.status)) {
    const error = new Error(unavailable)
    error.status = response.status
    throw error
  }
  return response
}

export async function api(path, { method = 'GET', body, form = false } = {}) {
  const headers = { Accept: 'application/json' }
  if (method !== 'GET') {
    const response = await request('/api/auth/csrf', { credentials: 'same-origin' })
    if (!response.ok) throw new Error('Não foi possível iniciar a operação. Tente novamente.')
    const csrf = await response.json()
    headers[csrf.headerName] = csrf.token
    if (body) headers['Content-Type'] = form ? 'application/x-www-form-urlencoded' : 'application/json'
  }
  const response = await request(`/api${path}`, {
      method, headers, credentials: 'same-origin',
      body: body ? (form ? new URLSearchParams(body) : JSON.stringify(body)) : undefined,
  })
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    const error = new Error(data.message || 'Não foi possível concluir a operação. Tente novamente.')
    error.fields = data.fields
    error.status = response.status
    if (response.status === 401 && !path.startsWith('/auth/')) window.dispatchEvent(new Event('session-expired'))
    throw error
  }
  return response.status === 204 ? null : response.json()
}
