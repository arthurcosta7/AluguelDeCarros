# Complementação do LAB02 — 11/11/2026

Este documento atualiza a [auditoria inicial](RELATORIO_CONFORMIDADE_LAB02.md), preservada como registro das lacunas encontradas.

## Implementado

| Pendência | Entrega |
| --- | --- |
| CRUD de cliente | Cadastro e consulta existentes; edição própria em Minha conta; exclusão sem vínculos com confirmação e senha atual. API `PUT/DELETE /api/clients/me`, com autenticação, CSRF e validação. |
| Contrato de crédito independente | Entidade/tabela/repositório próprios, identificador, banco, valor, parcelas, data e texto estável. Um por contrato de aluguel; criado atomicamente no aceite após pareceres. Consulta e impressão nos detalhes do pedido. |
| Migração de dados existentes | V3 acrescenta tabela e preserva texto de contratos antigos com crédito; V4 ativa RLS e revoga acesso público à nova tabela. V1/V2 não foram reescritas. |
| Casos de uso | [UML PlantUML](modelos/v2/casos-de-uso.puml). |
| Histórias de usuário | [13 histórias com critérios de aceite](modelos/v2/historias.md). |
| Classes | [Mermaid com entidades, atributos, enumerações e multiplicidades](modelos/v2/classes.mmd). |
| Pacotes/MVC | [PlantUML com frontend, API, segurança, serviço, domínio, repositórios e banco](modelos/v2/pacotes.puml). |
| Evolução dos modelos | [Base reconstruída do commit 5cfb58f](modelos/v1/BASE.md) e [comparação com a versão atual](modelos/EVOLUCAO.md). Não se trata de modelos originais de aula. |
| README | [README principal](../README.md), com stack, arquitetura, execução, variáveis de exemplo, preparação de agentes, roteiro de demonstração, testes e limitações. |
| Verificação de ponta a ponta | [Script e instruções](TESTE_NAVEGADOR.md) para navegador + API real + H2 isolado, com dados fictícios. |

## Decisões de funcionamento

- O próprio cliente administra seu cadastro. O papel e as credenciais não são modificados pela edição de dados pessoais.
- A exclusão física exige senha correta e ausência de qualquer pedido, inclusive cancelado, ou veículo de propriedade do cliente. Contratos e histórico não são apagados para permitir a exclusão.
- A exclusão bem-sucedida encerra a sessão. O backend identifica o cliente pela autenticação, sem aceitar um identificador de terceiro enviado pelo navegador.
- O contrato de crédito é formalizado junto ao aluguel, após aprovação da empresa e do banco e aceite do cliente. O pedido anterior ao aceite é apenas uma solicitação.
- Mudanças posteriores no cadastro não reescrevem os textos aceitos. Não há pagamento ou desembolso bancário real.
- A política de propriedade continua permitindo cliente, empresa e banco como proprietários, sem transferência automática de titularidade.

## Validação realizada

- **16 testes backend aprovados (15 de negócio + 1 de migração), sem falhas ou erros.** Suíte de integração ampliada com cenários de edição, CPF duplicado, limite de empregadores, proteção de perfil, exclusão com senha/sessão, bloqueio por histórico e proprietário, acesso cruzado entre dois clientes/bancos e estabilidade/unicidade do contrato de crédito.
- Teste de migração V1 → V3 com contrato antigo com crédito, verificando a preservação do texto aceito e a reaplicação sem duplicidade.
- Frontend: lint, build e três testes da camada de API aprovados.
- Navegador com API real em H2: cadastro, login, edição de cliente, criação com crédito, aprovação da empresa, aprovação do banco, aceite, consulta de dois contratos, bloqueio da exclusão com histórico, exclusão de conta sem vínculos e layout de edição em 375 px. Não houve erros JavaScript no cenário final.
- Supabase: inicialização real aplicou V3/V4; consultas JDBC somente leitura confirmaram ambas as migrações com sucesso, acesso à nova tabela e `relrowsecurity = true`. Não foram criados clientes/pedidos de demonstração no banco remoto para a validação do navegador.
- O backend principal na porta 8080 foi reiniciado com a implementação atual.

Capturas com dados fictícios: [contratos](imagens/contratos-demonstracao.png) e [edição de cliente em celular](imagens/editar-cliente-mobile.png).

## Limites e passos externos

1. A jornada completa de três perfis passou contra H2, não contra o Supabase. A conexão e as migrações do Supabase foram verificadas separadamente; isso não comprova todos os comportamentos de concorrência no PostgreSQL.
2. Os modelos originais/feedback de aula não estavam disponíveis. A versão-base foi identificada honestamente como reconstrução. Se existirem artefatos históricos externos, devem ser incorporados sem alterar suas datas/autoria.
3. Integrantes adicionais e turma não foram informados. O README registra apenas o mantenedor conhecido, sem inventar participantes.
4. Os arquivos foram preparados na cópia local. Commit/push e disponibilidade pública não são comprovados por esta documentação; nenhum deploy público foi criado.

Não é possível prometer nota ou conformidade acadêmica absoluta sem comparar com os modelos efetivamente apresentados em aula e os critérios do professor.
