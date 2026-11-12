import { test } from 'node:test'
import assert from 'node:assert/strict'
import { api } from '../src/api.js'

for (const path of ['/auth/me', '/auth/login']) {
  test(`backend indisponível em ${path} informa como recuperar`, async () => {
    const previous = globalThis.fetch
    globalThis.fetch = async () => new Response('', { status: 502 })
    try {
      await assert.rejects(api(path, path.endsWith('login') ? { method: 'POST', body: {} } : {}), /servidor.*backend/i)
    } finally { globalThis.fetch = previous }
  })
}
test('credenciais inválidas preservam a mensagem do backend', async () => {
  const previous = globalThis.fetch
  globalThis.fetch = async () => Response.json({ message: 'Usuário ou senha inválidos.' }, { status: 401 })
  try { await assert.rejects(api('/auth/me'), /Usuário ou senha inválidos/) }
  finally { globalThis.fetch = previous }
})
