# Evolução e alinhamento com o código

| Aspecto | Base observada (5cfb58f) | V2 atual | Código |
| --- | --- | --- | --- |
| Cliente | Cadastro e leitura | Edição própria e exclusão sem vínculos com senha | Profile, ProfileInput, DeleteAccount, ApiController, RentalService |
| Crédito | Campos de Rental e texto de Contract | CreditContract independente, um por Contract, gerado no aceite | Model.CreditContract, CreditContractRepository, Views.CreditAgreement |
| Banco | Avalia antes da contratação | Regra preservada, documento gerado após aceite | RentalService.evaluate/accept |
| Histórico | Texto estável no contrato de aluguel | Texto estável também no contrato de crédito; exclusão bloqueada para cliente com pedidos | RentalService.deleteProfile/accept |
| Persistência | Migrações V1/V2 | V3 adiciona documento e converte contratos antigos com crédito; V4 protege nova tabela no PostgreSQL | db/migration e db/postgresql |
| MVC | React/API/serviço/modelo/repositório | Separação preservada e explicitada no diagrama de pacotes | src/ do frontend e pacotes api/service/domain/repository/config |

Decisões de implementação: clientes administram o próprio cadastro; agentes são provisionados fora do cadastro público; pedidos finalizados preservam o histórico; não há juros ou desembolso integrado; propriedade é cadastrada, sem transferência automática; o crédito só se formaliza junto ao aceite do aluguel.

A entidade Account representa os três papéis por enum, sem herança JPA. Employer é objeto embutido em coleção. RentalEvent guarda ator por identificador/nome, sem relação JPA de navegação. O diagrama descreve essas escolhas, e não classes abstratas inexistentes.

Não há evidência de diagramas anteriores à base ou revisões orientadas em aula. Esses materiais deverão ser incorporados se existirem; não é possível recriá-los como se fossem históricos.
