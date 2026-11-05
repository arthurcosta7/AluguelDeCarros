import { useState } from 'react'
import { api } from './api'
import { Alert, Field, Form, Password } from './components'

export default function Auth({ onLogin, message }) {
  const [register, setRegister] = useState(false)
  const [notice, setNotice] = useState('')
  const [employers, setEmployers] = useState(1)
  return <main className="auth-shell" id="content" tabIndex={-1}>
    <section className="auth-intro"><a href="#/painel" className="brand"><span className="brand-mark">r.</span>rota<span className="brand-dot">/</span></a>
      <div><p className="eyebrow">SEU PRÓXIMO CAMINHO</p><h1>Menos burocracia.<br />Mais caminhos.</h1><p>Escolha seu carro, acompanhe seu pedido e confirme seu contrato em um só lugar.</p>
        <ol className="intro-steps"><li><span>01</span> Escolha o veículo e as datas</li><li><span>02</span> Acompanhe a análise do pedido</li><li><span>03</span> Confirme e consulte seu contrato</li></ol>
      </div><small>Sistema acadêmico de aluguel de veículos</small>
    </section>
    <section className="auth-panel"><div className="auth-form">
      <p className="eyebrow">BEM-VINDO À ROTA</p><h2>{register ? 'Crie sua conta' : 'Bom ter você aqui.'}</h2>
      <p className="muted">{register ? 'Cadastre seus dados para solicitar um aluguel.' : 'Entre para acessar seus pedidos e contratos.'}</p>
      {message && <Alert error={message} />}{notice && <p className="alert success" role="status">{notice}</p>}
      <Form key={register ? 'register' : 'login'} submit={register ? 'Criar conta' : 'Entrar na conta'} onSubmit={async data => {
        if (!register) {
          await api('/auth/login', { method: 'POST', form: true, body: { username: data.get('username'), password: data.get('password') } })
          onLogin(await api('/auth/me')); return
        }
        const body = Object.fromEntries(['username', 'password', 'name', 'rg', 'cpf', 'address', 'profession'].map(key => [key, data.get(key)]))
        body.employers = Array.from({ length: employers }, (_, i) => ({ name: data.get(`employer-${i}`), income: Number(data.get(`income-${i}`)) }))
        await api('/auth/register', { method: 'POST', body })
        setRegister(false); setNotice('Cadastro concluído. Entre com seu usuário e senha.')
      }}>
        {register && <Field label="Nome completo" name="name" autoComplete="name" required maxLength={120} />}
        <Field label="Usuário" name="username" autoComplete="username" required pattern="[a-zA-Z0-9_.-]{3,40}" title="Use de 3 a 40 letras, números, pontos, traços ou sublinhados." hint={register ? 'De 3 a 40 caracteres, sem espaços.' : undefined} />
        <Password autoComplete={register ? 'new-password' : 'current-password'} />
        {register && <>
          <div className="form-grid"><Field label="RG" name="rg" required maxLength={20} /><Field label="CPF" name="cpf" inputMode="numeric" pattern="[0-9]{11}" title="Informe os 11 dígitos do CPF, sem pontuação." required maxLength={11} hint="11 dígitos, sem pontuação." /></div>
          <Field label="Endereço completo" name="address" autoComplete="street-address" required maxLength={250} />
          <Field label="Profissão" name="profession" required maxLength={100} />
          <div className="section-heading"><h3>Empregadores e rendimentos</h3><span className="muted">Máximo de 3</span></div>
          {Array.from({ length: employers }, (_, i) => <div className="form-grid" key={i}>
            <Field label={`Empregador ${i + 1}`} name={`employer-${i}`} required maxLength={120} />
            <Field label="Renda mensal (R$)" name={`income-${i}`} type="number" min="0" max="9999999999.99" step="0.01" required />
          </div>)}
          <div className="inline-actions">{employers < 3 && <button type="button" className="text-button" onClick={() => setEmployers(employers + 1)}>+ Adicionar empregador</button>}
            {employers > 0 && <button type="button" className="text-button" onClick={() => setEmployers(employers - 1)}>Remover último</button>}</div>
        </>}
      </Form>
      <p className="auth-switch">{register ? 'Já tem cadastro?' : 'Primeira vez aqui?'} <button className="text-button" type="button" onClick={() => { setRegister(!register); setNotice('') }}>{register ? 'Entrar' : 'Criar uma conta'}</button></p>
    </div></section>
  </main>
}
