alter table locadora.credit_contracts enable row level security;
revoke all on locadora.credit_contracts from public, anon, authenticated;
