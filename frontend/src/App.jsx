import { roles } from './format'
import { useEffect, useState } from 'react'
import { api } from './api'
import { useRoute } from './hooks'
import Auth from './Auth'
import { Alert, Empty } from './components'
import { Dashboard, Bookings, Vehicles, VehicleForm, RentalForm, BookingDetail, Profile } from './pages'
import './App.css'
import './polish.css'

export default function App() {
  const [user, setUser] = useState(undefined)
  const [message, setMessage] = useState('')
  const [revision, setRevision] = useState(0)
  const [logoutBusy, setLogoutBusy] = useState(false)
  const [profileBusy, setProfileBusy] = useState(false)
  const route = useRoute()
  useEffect(() => {
    let active = true
    api('/auth/me').then(user => { if (active) setUser(user) }).catch(error => {
      if (active) { setUser(null); if (error.status !== 401) setMessage(error.message) }
    })
    const expired = () => { setUser(null); setMessage('Sua sessão expirou. Entre novamente para continuar.') }
    window.addEventListener('session-expired', expired)
    return () => { active = false; window.removeEventListener('session-expired', expired) }
  }, [])
  function saved(path) { setRevision(r => r + 1); window.location.hash = path }
  if (user === undefined) return <main className="boot" role="status">Carregando Rota…</main>
  if (!user) return <Auth message={message} onLogin={user => { setUser(user); setMessage(''); setRevision(r => r + 1); window.location.hash = '/painel' }} />
  const [path, query = ''] = route.split('?')
  const detailMatch = path.match(/^\/pedidos\/(\d+)$/)
  const editMatch = path.match(/^\/pedidos\/(\d+)\/editar$/)
  const vehicleMatch = path.match(/^\/veiculos\/(\d+)\/editar$/)
  let page
  const props = { user, revision, onSaved: saved }
  if (path === '/painel') page = <Dashboard {...props} />
  else if (path === '/pedidos') page = <Bookings {...props} />
  else if (path === '/contratos') page = <Bookings {...props} contractsOnly />
  else if (path === '/veiculos') page = <Vehicles {...props} />
  else if (path === '/veiculos/novo' || vehicleMatch) page = <VehicleForm {...props} id={vehicleMatch?.[1]} />
  else if (path === '/novo' || editMatch) page = <RentalForm {...props} id={editMatch?.[1]} query={query} />
  else if (detailMatch) page = <BookingDetail {...props} id={detailMatch[1]} />
  else if (path === '/conta') page = <Profile user={user} onUpdated={setUser} onDeleted={() => { setUser(null); setMessage('Conta excluída.'); window.location.hash = '/painel' }} />
  else page = <Empty title="Página não encontrada"><a href="#/painel">Voltar ao painel</a></Empty>
  const nav = [['/painel', 'Visão geral', '01'], ['/veiculos', 'Veículos', '02'], ['/pedidos', user.role === 'CLIENTE' ? 'Meus pedidos' : 'Pedidos', '03'], ['/contratos', 'Contratos', '04'], ['/conta', 'Minha conta', '05']]
  return <div className="app-shell"><a className="skip-link" href="#content" onClick={e => { e.preventDefault(); document.getElementById('content').focus() }}>Pular para o conteúdo</a>
    <aside className="sidebar"><a href="#/painel" className="brand"><span className="brand-mark">r.</span>rota<span className="brand-dot">/</span></a>
      <p className="nav-caption">ÁREA DO {roles[user.role].toUpperCase()}</p><nav aria-label="Navegação principal">{nav.map(([href, label, number]) => <a key={href} href={`#${href}`} className={path === href || href === '/pedidos' && (detailMatch || editMatch || path === '/novo') || href === '/veiculos' && (vehicleMatch || path === '/veiculos/novo') ? 'active' : ''} aria-current={path === href ? 'page' : undefined}><span aria-hidden="true">{number}</span>{label}</a>)}</nav>
      <div className="sidebar-footer"><p>Seu caminho,<br /><strong>mais simples.</strong></p><small>Projeto acadêmico · 2026</small></div>
    </aside>
    <div className="workspace"><header className="topbar"><span className="topbar-label">GESTÃO DE ALUGUÉIS</span><div className="account-menu">{user.presentationRoles?.length > 0 && <label className="presentation-switch"><span>Perfil da apresentação</span><select aria-label="Perfil da apresentação" value={user.role} disabled={profileBusy || logoutBusy} onChange={async event => {
        const role = event.target.value
        setProfileBusy(true); setMessage('')
        try { setUser(await api('/auth/presentation-role', { method: 'POST', body: { role } })); setRevision(r => r + 1); window.location.hash = '/painel' }
        catch (error) { setMessage(error.message) }
        finally { setProfileBusy(false) }
      }}>{user.presentationRoles.map(role => <option key={role} value={role}>{roles[role]}</option>)}</select></label>}<span className="avatar" aria-hidden="true">{user.name.charAt(0).toUpperCase()}</span><div><strong>{user.name}</strong><small>{roles[user.role]}</small></div><button className="text-button logout" disabled={logoutBusy} onClick={async () => {
        setLogoutBusy(true)
        try { await api('/auth/logout', { method: 'POST' }); setUser(null); setMessage(''); window.location.hash = '/painel' }
        catch (error) { setMessage(error.message) }
        finally { setLogoutBusy(false) }
      }}>{logoutBusy ? 'Saindo…' : 'Sair'}</button></div></header>
      <main id="content" tabIndex={-1} className="content"><Alert error={message} /><div className="page-stage" key={path}>{page}</div></main>
      <footer className="workspace-footer">Rota / Sistema de aluguel de carros<span>Uma etapa de cada vez.</span></footer>
    </div>
  </div>
}
