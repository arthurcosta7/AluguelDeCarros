import { date, money, roles, statuses, timestamp, today } from './format'
import { useState } from 'react'
import { api } from './api'
import { useResource } from './hooks'
import { Badge, BookingTable, CarDrawing, Empty, Field, Form, Heading, Resource } from './components'

export function Dashboard({ user, revision }) {
  const resource = useResource('/rentals', revision)
  return <><Heading eyebrow="VISÃO GERAL" title={`Olá, ${user.name.split(' ')[0]}.`}>{user.role === 'CLIENTE' ? 'Seu próximo caminho começa por aqui.' : 'Acompanhe os pedidos e mantenha tudo em movimento.'}</Heading>
    <Resource resource={resource}>{bookings => <>
      <div className="stats-grid">{[
        ['Pedidos', bookings.length, 'Todos os seus pedidos'],
        ['Em análise', bookings.filter(r => ['EM_ANALISE', 'AGUARDANDO_BANCO'].includes(r.status)).length, 'Aguardando parecer'],
        ['Aprovados', bookings.filter(r => r.status === 'APROVADO').length, 'Prontos para confirmação'],
        ['Contratos', bookings.filter(r => r.status === 'CONTRATADO').length, 'Condições confirmadas'],
      ].map(([label, value, hint]) => <article className="stat" key={label}><p>{label}</p><strong>{value.toString().padStart(2, '0')}</strong><small>{hint}</small></article>)}</div>
      {user.role === 'CLIENTE' && <section className="welcome-banner"><div><p className="eyebrow">VAMOS ENCONTRAR SEU CARRO?</p><h2>Um novo destino espera por você.</h2><p>Consulte os veículos e escolha o período do seu aluguel.</p><a className="button primary" href="#/veiculos">Explorar veículos →</a></div><CarDrawing /></section>}
      <section className="panel"><div className="section-heading"><h2>Pedidos recentes</h2><a className="text-button" href="#/pedidos">Ver todos →</a></div><BookingTable bookings={bookings.slice(0, 5)} /></section>
    </>}</Resource>
  </>
}

export function Bookings({ user, revision, contractsOnly = false }) {
  const resource = useResource('/rentals', revision)
  const [filter, setFilter] = useState('')
  return <><Heading eyebrow="ACOMPANHAMENTO" title={contractsOnly ? 'Contratos' : 'Pedidos'} action={!contractsOnly && user.role === 'CLIENTE' && <a className="button primary" href="#/novo">+ Novo pedido</a>}>
    {contractsOnly ? 'Consulte as condições confirmadas de cada aluguel.' : 'Consulte a situação e os detalhes dos pedidos de aluguel.'}</Heading>
    {!contractsOnly && <div className="filter-row"><Field label="Filtrar por situação"><select value={filter} onChange={e => setFilter(e.target.value)}><option value="">Todas as situações</option>{Object.entries(statuses).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></Field></div>}
    <section className="panel"><Resource resource={resource}>{bookings => <BookingTable bookings={bookings.filter(r => contractsOnly ? r.status === 'CONTRATADO' : !filter || r.status === filter)} />}</Resource></section>
  </>
}

export function Vehicles({ user, revision }) {
  const [period, setPeriod] = useState('')
  const resource = useResource(`/vehicles${period}`, revision)
  return <><Heading eyebrow="FROTA" title="Encontre seu próximo carro" action={user.role === 'EMPRESA' && <a className="button primary" href="#/veiculos/novo">+ Cadastrar veículo</a>}>Escolha um veículo e consulte a disponibilidade para o seu período.</Heading>
    <div className="panel availability"><Form submit="Consultar disponibilidade" onSubmit={async data => setPeriod(`?start=${data.get('start')}&end=${data.get('end')}`)} secondary={period && <button className="button secondary" type="button" onClick={() => setPeriod('')}>Limpar filtro</button>}>
      <div className="form-grid"><Field label="Retirada" name="start" type="date" min={today()} required /><Field label="Devolução" name="end" type="date" min={today()} required /></div>
    </Form><p className="muted">A devolução deve ser posterior à retirada. A data da devolução não conta como diária.</p></div>
    <Resource resource={resource}>{cars => cars.length ? <div className="cars-grid">{cars.map(car => <article className="car-card" key={car.id}>
      <div className="car-art"><span className="car-year">{car.year}</span><CarDrawing /></div>
      <div className="car-body"><p className="eyebrow">{car.brand}</p><h2>{car.model}</h2><p className="muted">{car.plate} · Matrícula {car.registration}</p>
        <dl className="car-info"><div><dt>Empresa</dt><dd>{car.company.name}</dd></div><div><dt>Proprietário</dt><dd>{car.owner.name} · {roles[car.owner.role]}</dd></div></dl>
        <div className="car-bottom"><p><strong>{money(car.dailyRate)}</strong><small>por diária</small></p>
          {user.role === 'CLIENTE' ? <a className="button primary" href={`#/novo?vehicle=${car.id}${period ? `&${period.slice(1)}` : ''}`}>Solicitar →</a> : user.role === 'EMPRESA' && car.company.id === user.id ? <a className="button secondary" href={`#/veiculos/${car.id}/editar`}>Editar</a> : null}
        </div>
      </div></article>)}</div> : <Empty title="Nenhum veículo disponível">Tente outro período ou aguarde o cadastro de veículos pela empresa.</Empty>}</Resource>
  </>
}

export function VehicleForm({ user, id, revision, onSaved }) {
  const cars = useResource('/vehicles', revision)
  const owners = useResource('/owners', revision)
  if (user.role !== 'EMPRESA') return <Empty title="Acesso restrito">Somente empresas podem cadastrar veículos.</Empty>
  return <><Heading eyebrow="GESTÃO DA FROTA" title={id ? 'Editar veículo' : 'Cadastrar veículo'}>A matrícula é o identificador do registro do veículo, separado da placa.</Heading>
    <Resource resource={cars}>{list => <Resource resource={owners}>{people => {
      const car = id ? list.find(v => v.id === Number(id)) : null
      if (id && !car) return <Empty title="Veículo não encontrado" />
      return <section className="panel form-panel"><Form key={id || 'new'} submit="Salvar veículo" secondary={<a className="button secondary" href="#/veiculos">Voltar</a>} onSubmit={async data => {
        const body = Object.fromEntries(data)
        body.year = Number(body.year); body.dailyRate = Number(body.dailyRate); body.ownerId = Number(body.ownerId)
        await api(id ? `/vehicles/${id}` : '/vehicles', { method: id ? 'PUT' : 'POST', body }); onSaved('/veiculos')
      }}><div className="form-grid">
        <Field label="Marca" name="brand" defaultValue={car?.brand} required maxLength={60} /><Field label="Modelo" name="model" defaultValue={car?.model} required maxLength={80} />
        <Field label="Ano" name="year" type="number" min="1900" max={new Date().getFullYear() + 1} defaultValue={car?.year || new Date().getFullYear()} required />
        <Field label="Placa" name="plate" defaultValue={car?.plate} pattern="[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}" title="Use o formato ABC1234 ou ABC1D23." required maxLength={7} hint="Sem espaços ou hífen." />
        <Field label="Matrícula" name="registration" defaultValue={car?.registration} required maxLength={40} />
        <Field label="Diária (R$)" name="dailyRate" type="number" min="0.01" max="9999999999.99" step="0.01" defaultValue={car?.dailyRate} required />
        <Field label="Proprietário"><select name="ownerId" defaultValue={car?.owner.id || user.id} required>{people.map(p => <option key={p.id} value={p.id}>{p.name} — {roles[p.role]}</option>)}</select></Field>
      </div></Form></section>
    }}</Resource>}</Resource>
  </>
}

export function RentalForm({ user, id, query, revision, onSaved }) {
  const cars = useResource('/vehicles', revision)
  const banks = useResource('/banks', revision)
  const detail = useResource(id ? `/rentals/${id}` : '/auth/me', revision)
  return <><Heading eyebrow="PEDIDOS DE ALUGUEL" title={id ? `Alterar pedido #${id}` : 'Novo pedido'}>As condições serão encaminhadas para análise antes da contratação.</Heading>
    <Resource resource={cars}>{vehicles => <Resource resource={banks}>{agents => <Resource resource={detail}>{data => <RentalFields key={id ? `${id}-${data.rental.version}` : 'new'}
      user={user} booking={id ? data.rental : null} vehicles={vehicles} banks={agents} query={query} onSaved={onSaved} />}</Resource>}</Resource>}</Resource>
  </>
}
function RentalFields({ user, booking, vehicles, banks, query, onSaved }) {
  const params = new URLSearchParams(query)
  const [vehicleId, setVehicleId] = useState(String(booking?.vehicle.id || params.get('vehicle') || vehicles[0]?.id || ''))
  const [start, setStart] = useState(booking?.startDate || params.get('start') || '')
  const [end, setEnd] = useState(booking?.endDate || params.get('end') || '')
  const [credit, setCredit] = useState(Boolean(booking?.bank))
  const selected = vehicles.find(v => v.id === Number(vehicleId))
  const days = start && end ? Math.round((new Date(end) - new Date(start)) / 86400000) : 0
  const total = days > 0 && selected ? days * selected.dailyRate : 0
  const availableCars = user.role === 'CLIENTE' ? vehicles : booking ? vehicles.filter(v => v.company.id === booking.company.id) : []
  if (!vehicles.length) return <Empty title="Nenhum veículo cadastrado">A empresa precisa cadastrar um veículo antes do primeiro pedido.</Empty>
  if (!booking && user.role !== 'CLIENTE') return <Empty title="Acesso restrito">A criação de pedidos é exclusiva para clientes.</Empty>
  if (booking && ['CANCELADO', 'CONTRATADO'].includes(booking.status)) return <Empty title="Pedido encerrado">Este pedido está disponível apenas para consulta.</Empty>
  return <section className="panel form-panel"><Form submit={booking ? 'Salvar e enviar para nova análise' : 'Enviar pedido para análise'} secondary={<a href={booking ? `#/pedidos/${booking.id}` : '#/veiculos'} className="button secondary">Voltar</a>} onSubmit={async data => {
    const body = { vehicleId: Number(vehicleId), startDate: start, endDate: end,
      bankId: credit ? Number(data.get('bankId')) : null,
      creditAmount: credit ? Number(data.get('creditAmount')) : null,
      installments: credit ? Number(data.get('installments')) : null,
      note: data.get('note') || null }
    const saved = await api(booking ? `/rentals/${booking.id}?version=${booking.version}` : '/rentals', { method: booking ? 'PUT' : 'POST', body })
    onSaved(`/pedidos/${saved.id}`)
  }}>
    <Field label="Veículo"><select value={vehicleId} onChange={e => setVehicleId(e.target.value)} required>{availableCars.map(v => <option key={v.id} value={v.id}>{v.brand} {v.model} · {v.plate} · {money(v.dailyRate)}/dia</option>)}</select></Field>
    <div className="form-grid"><Field label="Retirada"><input name="startDate" type="date" min={today()} required value={start} onChange={e => setStart(e.target.value)} /></Field>
      <Field label="Devolução"><input name="endDate" type="date" min={start || today()} required value={end} onChange={e => setEnd(e.target.value)} /></Field></div>
    <div className="quote"><span>{days > 0 ? `${days} diária(s) · ${selected?.company.name || ''}` : 'Selecione o período para calcular o valor'}</span><strong>{money(total)}</strong></div>
    <p className="muted">A devolução não conta como diária. O período será reservado enquanto o pedido estiver em análise ou aprovado.</p>
    <label className="check-field"><input type="checkbox" checked={credit} disabled={user.role === 'BANCO'} onChange={e => setCredit(e.target.checked)} /> Solicitar crédito bancário</label>
    {credit && <div className="credit-panel"><Field label="Banco concedente"><select name="bankId" defaultValue={booking?.bank?.id || ''} required>
      <option value="" disabled>Selecione um banco</option>{banks.filter(a => user.role !== 'BANCO' || a.id === user.id).map(a => <option key={a.id} value={a.id}>{a.name}</option>)}</select></Field>
      <div className="form-grid"><Field label="Valor do crédito (R$)" name="creditAmount" type="number" min="0.01" max={total || undefined} step="0.01" defaultValue={booking?.creditAmount || ''} required />
        <Field label="Quantidade de parcelas" name="installments" type="number" min="1" max="60" defaultValue={booking?.installments || 1} required /></div>
      <p className="muted">Crédito sem juros para demonstração. O banco analisa após a aprovação da empresa. Não há cobrança pelo sistema.</p>
    </div>}
    {booking && <Field label="Motivo da alteração"><textarea name="note" maxLength={1000} required rows={3} /></Field>}
  </Form></section>
}

export function BookingDetail({ user, id, revision, onSaved }) {
  const resource = useResource(`/rentals/${id}`, revision)
  return <Resource resource={resource}>{detail => <DetailContent key={`${id}-${detail.rental.version}`} user={user} detail={detail} onSaved={onSaved} />}</Resource>
}
function DetailContent({ user, detail, onSaved }) {
  const { rental: r, events, contract } = detail
  const [confirm, setConfirm] = useState('')
  const editable = !['CANCELADO', 'CONTRATADO'].includes(r.status)
  const canEvaluate = user.role === 'EMPRESA' && r.status === 'EM_ANALISE' || user.role === 'BANCO' && r.status === 'AGUARDANDO_BANCO'
  async function act(action, body) {
    await api(`/rentals/${r.id}/${action}?version=${r.version}`, { method: 'POST', body })
    onSaved(`/pedidos/${r.id}`)
  }
  return <><Heading eyebrow="DETALHES DO PEDIDO" title={`Pedido #${String(r.id).padStart(4, '0')}`} action={<Badge status={r.status} />}>Criado em {timestamp(r.createdAt)}</Heading>
    <div className="detail-layout"><section className="panel detail-main"><h2>{r.vehicle.brand} {r.vehicle.model}</h2><p className="muted">{r.vehicle.plate} · {r.vehicle.year} · Matrícula {r.vehicle.registration}</p>
      <dl className="detail-grid"><div><dt>Retirada</dt><dd>{date(r.startDate)}</dd></div><div><dt>Devolução</dt><dd>{date(r.endDate)}</dd></div>
        <div><dt>Empresa responsável</dt><dd>{r.company.name}</dd></div><div><dt>Proprietário do veículo</dt><dd>{r.vehicle.owner.name} ({roles[r.vehicle.owner.role]})</dd></div>
        <div><dt>Total do aluguel</dt><dd className="price">{money(r.total)}</dd></div><div><dt>Modalidade</dt><dd>{r.bank ? 'Com crédito' : 'Sem crédito'}</dd></div>
        {r.bank && <><div><dt>Banco concedente</dt><dd>{r.bank.name}</dd></div><div><dt>Crédito solicitado</dt><dd>{money(r.creditAmount)} em {r.installments} parcela(s), sem juros</dd></div></>}
      </dl>
      <h3>Dados do contratante</h3><dl className="detail-grid"><div><dt>Nome</dt><dd>{r.client.name}</dd></div><div><dt>Profissão</dt><dd>{r.client.profession}</dd></div><div><dt>CPF</dt><dd>{r.client.cpf}</dd></div><div><dt>RG</dt><dd>{r.client.rg}</dd></div><div className="span-2"><dt>Endereço</dt><dd>{r.client.address}</dd></div></dl>
      <h3>Empregadores e rendimentos</h3>{r.client.employers.length ? <ul className="income-list">{r.client.employers.map((e, i) => <li key={i}><span>{e.name}</span><strong>{money(e.income)}/mês</strong></li>)}</ul> : <p className="muted">Nenhum empregador informado.</p>}
      {editable && <div className="inline-actions"><a className="button secondary" href={`#/pedidos/${r.id}/editar`}>Alterar pedido</a>{user.role === 'CLIENTE' && <button className="button danger" onClick={() => setConfirm('cancel')}>Cancelar pedido</button>}</div>}
      {user.role === 'CLIENTE' && r.status === 'APROVADO' && <div className="approval-box"><h3>Seu pedido foi aprovado.</h3><p>Confira as condições acima. Ao confirmar, o contrato será registrado e ficará disponível para consulta.</p><button className="button primary" onClick={() => setConfirm('accept')}>Confirmar condições e contratar</button></div>}
      {confirm && <div className="confirmation"><h3>{confirm === 'cancel' ? 'Cancelar este pedido?' : 'Confirmar a contratação?'}</h3><p>{confirm === 'cancel' ? 'O período será liberado e este pedido não poderá ser reaberto.' : 'Você aceita o veículo, as datas, o valor e as condições de crédito apresentados. Depois, o pedido ficará disponível apenas para consulta.'}</p>
        <Form submit={confirm === 'cancel' ? 'Sim, cancelar pedido' : 'Sim, aceito as condições'} onSubmit={() => act(confirm)} secondary={<button className="button secondary" type="button" onClick={() => setConfirm('')}>Voltar</button>} />
      </div>}
      {canEvaluate && <div className="evaluation"><h3>{user.role === 'BANCO' ? 'Análise de crédito' : 'Análise do aluguel'}</h3><p className="muted">Confira os dados e rendimentos do cliente e registre seu parecer.</p>
        <Form submit="Registrar parecer" onSubmit={data => act('evaluate', { approve: data.get('decision') === 'approve', note: data.get('note') })}>
          <Field label="Parecer"><select name="decision" required><option value="" disabled>Selecione o parecer</option><option value="approve">Aprovar</option><option value="reject">Rejeitar</option></select></Field>
          <Field label="Justificativa"><textarea name="note" maxLength={1000} rows={3} required /></Field>
        </Form></div>}
    </section><aside className="panel history"><h2>Histórico do pedido</h2><ol>{events.map(e => <li key={e.id}><span className="timeline-dot" aria-hidden="true" /><strong>{e.action.replaceAll('_', ' ').toLowerCase()}</strong><small>{e.actorName} · {timestamp(e.createdAt)}</small><p>{e.note}</p></li>)}</ol></aside></div>
    {contract && <section className="panel contract"><div className="section-heading"><div><p className="eyebrow">CONTRATO #{contract.id}</p><h2>Condições confirmadas</h2></div><button className="button secondary" onClick={() => window.print()}>Imprimir contrato</button></div><p className="muted">Formalizado em {timestamp(contract.acceptedAt)}</p><pre>{contract.terms}</pre></section>}
    <a className="text-button back-link" href="#/pedidos">← Voltar aos pedidos</a>
  </>
}

export function Profile({ user }) {
  return <><Heading eyebrow="MINHA CONTA" title="Seus dados">Informações do cadastro e perfil de acesso.</Heading><section className="panel form-panel"><dl className="detail-grid">
    <div><dt>Nome</dt><dd>{user.name}</dd></div><div><dt>Usuário</dt><dd>{user.username}</dd></div><div><dt>Perfil</dt><dd>{roles[user.role]}</dd></div>
    {user.role === 'CLIENTE' && <><div><dt>CPF</dt><dd>{user.cpf}</dd></div><div><dt>RG</dt><dd>{user.rg}</dd></div><div><dt>Profissão</dt><dd>{user.profession}</dd></div><div><dt>Endereço</dt><dd>{user.address}</dd></div></>}
  </dl>{user.role === 'CLIENTE' && <><h3>Empregadores e rendimentos</h3>{user.employers.length ? <ul className="income-list">{user.employers.map((e, i) => <li key={i}><span>{e.name}</span><strong>{money(e.income)}/mês</strong></li>)}</ul> : <p className="muted">Nenhum empregador informado.</p>}</>}</section></>
}
