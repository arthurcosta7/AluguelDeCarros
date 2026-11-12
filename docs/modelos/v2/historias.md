# Histórias de usuário — versão atual

| ID | Como / quero / para | Critérios de aceite |
| --- | --- | --- |
| HU01 | Como visitante, quero cadastrar-me como cliente para usar o sistema. | Usuário único, senha, RG, CPF, nome, endereço, profissão; zero a três empregadores com renda não negativa; sem escolher papel de agente. |
| HU02 | Como usuário cadastrado, quero entrar e sair para proteger minha sessão. | Login válido cria sessão; inválido é rejeitado; operações exigem autenticação e CSRF; sair encerra sessão. |
| HU03 | Como cliente, quero consultar e editar meus dados para manter meu cadastro atualizado. | Somente conta própria; CPF único; até três empregadores; papel, usuário e senha não podem ser alterados por esse formulário; contratos preservam texto aceito. |
| HU04 | Como cliente, quero excluir minha conta sem vínculos para encerrar meu acesso. | Confirmação explícita e senha atual; bloquear se houver qualquer pedido ou veículo próprio; sessão encerrada após exclusão. |
| HU05 | Como usuário, quero consultar veículos e disponibilidade para escolher o período. | Matrícula, ano, marca, modelo, placa, diária e proprietário; datas válidas; sem sobreposição de períodos ativos. |
| HU06 | Como empresa, quero cadastrar e editar minha frota para oferecer carros. | Somente própria frota; proprietário pode ser cliente, empresa ou banco; matrícula/placa únicas; bloquear edição com pedidos ativos. |
| HU07 | Como cliente, quero criar e consultar pedidos para acompanhar meu aluguel. | Veículo, datas, total calculado no servidor, crédito opcional; novo pedido em análise; acesso somente ao próprio pedido. |
| HU08 | Como cliente ou agente vinculado, quero modificar pedidos para corrigir condições. | Não modificar cancelado/contratado; exigir versão atual; agente mantém empresa e banco mantém associação; voltar à análise e registrar histórico. |
| HU09 | Como cliente, quero cancelar meu pedido para liberar as datas. | Confirmação; somente próprio pedido antes da contratação; manter histórico. |
| HU10 | Como empresa, quero avaliar pedidos para aprovar ou rejeitar o aluguel. | Somente pedidos da empresa em análise; justificativa obrigatória; com crédito encaminhar ao banco; sem crédito liberar aceite. |
| HU11 | Como banco, quero avaliar crédito atribuído para registrar meu parecer. | Somente banco vinculado e após empresa aprovar; justificativa; aprovação libera aceite; rejeição libera datas. |
| HU12 | Como cliente, quero aceitar condições aprovadas para contratar. | Versão atual, disponibilidade e datas verificadas; gerar exatamente um contrato; com crédito gerar documento independente vinculado na mesma transação. |
| HU13 | Como participante do aluguel, quero consultar e imprimir os contratos para guardar as condições. | Cliente, empresa e banco vinculado acessam documento; cópia textual estável mesmo após edição cadastral. |
