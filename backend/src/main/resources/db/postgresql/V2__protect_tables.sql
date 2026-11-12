alter table locadora.accounts enable row level security;
alter table locadora.employers enable row level security;
alter table locadora.vehicles enable row level security;
alter table locadora.rentals enable row level security;
alter table locadora.contracts enable row level security;
alter table locadora.rental_events enable row level security;
revoke all on schema locadora from public;
