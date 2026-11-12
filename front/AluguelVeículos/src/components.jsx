import { useRef, useState } from 'react'

import { date, money, statuses } from './format'

export function Badge({ status }) { return <span className={`badge ${status.toLowerCase()}`}>{statuses[status]}</span> }
export function Empty({ title = 'Nada por aqui ainda', children }) {
  return <div className="empty"><span className="empty-line" aria-hidden="true" /><h3>{title}</h3><p>{children}</p></div>
}
export function Alert({ error }) {
  return error && <div className="alert error" role="alert"><strong>{error.message || error}</strong>
    {error.fields && <ul>{Object.entries(error.fields).map(([field]) => <li key={field}>Revise o campo: {fieldNames[field] || field}.</li>)}</ul>}
  </div>
}
const fieldNames = { username: 'usuário', password: 'senha', name: 'nome', rg: 'RG', cpf: 'CPF', address: 'endereço', profession: 'profissão', employers: 'empregadores', registration: 'matrícula', year: 'ano', brand: 'marca', model: 'modelo', plate: 'placa', dailyRate: 'diária', ownerId: 'proprietário', startDate: 'retirada', endDate: 'devolução', bankId: 'banco', creditAmount: 'crédito', installments: 'parcelas' }
export function Resource({ resource, children }) {
  if (resource.loading) return <div className="loading" role="status"><span className="loading-track" aria-hidden="true" />Carregando informações…</div>
  if (resource.error) return <Alert error={resource.error} />
  return <div className="resource-ready">{children(resource.data)}</div>
}
export function Form({ onSubmit, children, submit = 'Salvar', secondary, className = '' }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const errorRef = useRef(null)
  async function handle(event) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    setBusy(true); setError(null)
    try { await onSubmit(data) }
    catch (error) { setError(error); requestAnimationFrame(() => errorRef.current?.focus()) }
    finally { setBusy(false) }
  }
  return <form onSubmit={handle} className={className} aria-busy={busy}>
    <div ref={errorRef} tabIndex={-1}><Alert error={error} /></div>
    <fieldset disabled={busy}>{children}<div className="form-actions">
      <button className="button primary" type="submit">{busy && <span className="button-spinner" aria-hidden="true" />}{busy ? 'Aguarde…' : submit}</button>{secondary}
    </div></fieldset>
  </form>
}
export function Field({ label, hint, children, ...props }) {
  return <label className="field"><span>{label}</span>{children || <input {...props} />}{hint && <small>{hint}</small>}</label>
}
export function Password({ autoComplete = 'current-password' }) {
  const [visible, setVisible] = useState(false)
  return <Field label="Senha"><div className="password-field">
    <input name="password" type={visible ? 'text' : 'password'} required minLength={8} maxLength={64} autoComplete={autoComplete} />
    <button type="button" className="text-button" aria-pressed={visible} onClick={() => setVisible(!visible)}>{visible ? 'Ocultar' : 'Mostrar'}</button>
  </div></Field>
}
export function Heading({ eyebrow, title, children, action }) {
  return <div className="page-heading"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1>{children && <p className="muted">{children}</p>}</div>{action}</div>
}
export function BookingTable({ bookings }) {
  if (!bookings.length) return <Empty title="Nenhum pedido encontrado">Os pedidos aparecerão aqui conforme forem criados.</Empty>
  return <div className="table-scroll"><table><thead><tr><th>Pedido / cliente</th><th>Veículo</th><th>Período</th><th>Total</th><th>Situação</th><th><span className="sr-only">Ação</span></th></tr></thead>
    <tbody>{bookings.map(r => <tr key={r.id}>
      <td><strong>#{String(r.id).padStart(4, '0')}</strong><small>{r.client.name}</small></td>
      <td>{r.vehicle.brand} {r.vehicle.model}<small>{r.vehicle.plate}</small></td>
      <td>{date(r.startDate)}<small>até {date(r.endDate)}</small></td><td className="nowrap">{money(r.total)}</td>
      <td><Badge status={r.status} /></td><td><a className="table-link" href={`#/pedidos/${r.id}`} aria-label={`Ver pedido ${r.id}`}>Ver pedido →</a></td>
    </tr>)}</tbody></table></div>
}
export function CarDrawing() {
  return <svg viewBox="0 0 360 150" fill="none" className="car-drawing" aria-hidden="true">
    <path d="M31 102h294M67 91l16-30c5-9 14-14 24-14h103c12 0 20 3 29 12l29 29 38 7c8 2 12 6 12 14v9H48v-14c0-7 6-12 19-13Z" fill="currentColor" opacity=".15" />
    <path d="m99 59-17 31h166l-25-24c-5-5-12-7-20-7H99ZM158 59v30M47 104l24-3m222 1 23 5" stroke="currentColor" strokeWidth="3" strokeLinejoin="round" />
    <path d="M67 91l16-30c5-9 14-14 24-14h103c12 0 20 3 29 12l29 29 38 7c8 2 12 6 12 14v9H48v-14c0-7 6-12 19-13Z" stroke="currentColor" strokeWidth="3" />
    <circle cx="102" cy="117" r="18" fill="var(--surface)" stroke="currentColor" strokeWidth="4" /><circle cx="263" cy="117" r="18" fill="var(--surface)" stroke="currentColor" strokeWidth="4" />
    <circle cx="102" cy="117" r="6" fill="currentColor" /><circle cx="263" cy="117" r="6" fill="currentColor" />
  </svg>
}
