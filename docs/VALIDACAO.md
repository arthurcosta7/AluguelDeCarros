# Verificação da entrega

Verificação realizada em 04/11/2026, com Java 25 e Node 24.

| Verificação | Resultado |
| --- | --- |
| Compilação do backend e testes (`mvn test-compile`) | Passou |
| Cenários de integração com contexto Spring, Flyway e H2 | 10 passaram, 0 falharam |
| Frontend (`npm run lint`) | Passou |
| Frontend (`npm run build`) | Passou |
| Sintaxe do script (`bash -n aluguel/run.sh`) | Passou |

## Cenários executados

1. Login com sessão, logout e exigência de CSRF.
2. Obtenção de token CSRF real e renovação após autenticação, como faz o frontend.
3. Cadastro de veículo com banco proprietário, restrição por empresa e proteção de veículo com pedidos ativos.
4. Cadastro de cliente, limite de empregadores, formato do CPF e duplicidade.
5. Aprovação sem crédito, prevenção de aceite desatualizado e contrato com condições preservadas.
6. Aprovação pela empresa e banco, alteração pelo agente e retorno à análise.
7. Isolamento dos pedidos e permissões de clientes e agentes.
8. Bloqueio de sobreposição, liberação por cancelamento e aceitação de períodos adjacentes.
9. Datas e crédito inválidos, rejeição e reenvio para análise.
10. Duas tentativas simultâneas de reserva: exatamente uma aceita e outra rejeitada por conflito.

## Particularidade do ambiente

O cache Maven disponível estava incompleto para o plugin Surefire 3.5.6, e o ambiente não resolveu o host do Maven Central. Por isso, `mvn test` não concluiu pelo plugin. Os fontes de produção e teste foram compilados pelo Maven e os dez métodos JUnit foram executados por um executor temporário usando `TestContextManager` do Spring, preparação de dados antes de cada teste e MockMvc. Foi usado o mock maker por subclasses do Mockito, evitando a necessidade de anexar um agente à JVM neste ambiente.

Em uma máquina com acesso ao Maven Central, a execução padrão permanece `cd aluguel && ./mvnw test`. Não foram alteradas dependências para contornar a indisponibilidade do plugin.

## Verificações externas pendentes

A conexão real com o Supabase depende de preencher as credenciais locais. A migração específica de PostgreSQL, incluindo RLS, não foi executada contra um servidor PostgreSQL nesta sessão. H2 em modo PostgreSQL não substitui essa verificação.

A interface passou por lint e build; não foi executado teste interativo em navegador nesta sessão. O roteiro manual para validar cliente, empresa e banco está no README principal.

## Configuração Supabase — atualização

O `aluguel/run.sh` agora carrega as quatro variáveis do `.env` por um launcher Python, preservando senhas literais e variáveis exportadas. Na ausência de `DB_URL`, deriva a conexão direta com TLS; para Session pooler, aceita a URL JDBC explícita e preserva o usuário com sufixo do projeto.

Validados: sintaxe Bash, leitura de senha com caracteres especiais, derivação da URL direta, manutenção do usuário no pooler, lint e build do frontend.

A conexão remota não foi confirmada: o ambiente retornou falha de resolução DNS para o endpoint Supabase. A inicialização pelo Maven também foi impedida pelo sandbox (`mkdir: Read-only file system`). Portanto, a execução de Flyway, a consulta real e o fluxo de cadastro/login contra Supabase permanecem pendentes. O host do Session pooler precisa ser copiado do painel caso a máquina de execução não tenha IPv6.
