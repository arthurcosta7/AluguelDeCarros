export const money = value => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value ?? 0)
export const date = value => value ? new Intl.DateTimeFormat('pt-BR').format(new Date(`${value}T12:00:00`)) : '—'
export const timestamp = value => new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
export const today = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}
export const statuses = {
  EM_ANALISE: 'Em análise', AGUARDANDO_BANCO: 'Aguardando banco', APROVADO: 'Aprovado',
  REJEITADO: 'Rejeitado', CANCELADO: 'Cancelado', CONTRATADO: 'Contratado',
}
export const roles = { CLIENTE: 'Cliente', EMPRESA: 'Empresa', BANCO: 'Banco' }

