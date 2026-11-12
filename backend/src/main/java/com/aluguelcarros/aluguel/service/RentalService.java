package com.aluguelcarros.aluguel.service;

import com.aluguelcarros.aluguel.api.*;
import com.aluguelcarros.aluguel.domain.Model.*;
import com.aluguelcarros.aluguel.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class RentalService {
    private static final List<Status> ACTIVE = List.of(Status.EM_ANALISE,Status.AGUARDANDO_BANCO,Status.APROVADO,Status.CONTRATADO);
    private final AccountRepository accounts;
    private final VehicleRepository vehicles;
    private final RentalRepository rentals;
    private final ContractRepository contracts;
    private final EventRepository events;
    private final CreditContractRepository creditContracts;
    private final PasswordEncoder encoder;
    public RentalService(AccountRepository accounts, VehicleRepository vehicles, RentalRepository rentals,
                         ContractRepository contracts, EventRepository events, CreditContractRepository creditContracts, PasswordEncoder encoder) {
        this.accounts=accounts; this.vehicles=vehicles; this.rentals=rentals;
        this.contracts=contracts; this.events=events; this.creditContracts=creditContracts; this.encoder=encoder;
    }
    public Account account(String username) {
        return accounts.findByUsername(username).orElseThrow(() -> fail(HttpStatus.UNAUTHORIZED,"Sessão inválida."));
    }
    @Transactional
    public Views.User register(Requests.Registration input) {
        String username=input.username().toLowerCase(Locale.ROOT);
        if (input.password().getBytes(StandardCharsets.UTF_8).length>72)
            throw bad("A senha deve ter no máximo 72 bytes; reduza seu tamanho.");
        if (accounts.existsByUsername(username) || accounts.existsByCpf(input.cpf()))
            throw fail(HttpStatus.CONFLICT,"Usuário ou CPF já cadastrado.");
        Account a=new Account(); a.username=username; a.passwordHash=encoder.encode(input.password());
        a.role=Role.CLIENTE; a.name=input.name().trim(); a.rg=input.rg().trim(); a.cpf=input.cpf();
        a.address=input.address().trim(); a.profession=input.profession().trim();
        a.employers=input.employers().stream().map(e->new Employer(e.name().trim(),e.income())).toList();
        return Views.User.of(accounts.saveAndFlush(a));
    }
    @Transactional
    public Views.User updateProfile(Account actor, Requests.ProfileInput input) {
        requireRole(actor,Role.CLIENTE);
        Account a=accounts.findById(actor.id).orElseThrow(()->notFound("Cliente"));
        if (!Objects.equals(a.cpf,input.cpf()) && accounts.existsByCpf(input.cpf()))
            throw fail(HttpStatus.CONFLICT,"CPF já cadastrado.");
        a.name=input.name().trim(); a.rg=input.rg().trim(); a.cpf=input.cpf();
        a.address=input.address().trim(); a.profession=input.profession().trim();
        a.employers.clear(); input.employers().forEach(e->a.employers.add(new Employer(e.name().trim(),e.income())));
        return Views.User.of(accounts.saveAndFlush(a));
    }
    @Transactional
    public void deleteProfile(Account actor, String password) {
        requireRole(actor,Role.CLIENTE);
        if (!encoder.matches(password,actor.passwordHash)) throw bad("Senha atual incorreta.");
        if (rentals.existsByClientId(actor.id) || vehicles.existsByOwnerId(actor.id))
            throw fail(HttpStatus.CONFLICT,"Sua conta possui pedidos ou veículos vinculados e não pode ser excluída. O histórico deve ser preservado.");
        accounts.delete(accounts.findById(actor.id).orElseThrow(()->notFound("Cliente"))); accounts.flush();
    }
    public List<Views.Person> banks() { return accounts.findByRole(Role.BANCO).stream().map(Views.Person::of).toList(); }
    public List<Views.Person> owners(Account actor) {
        requireRole(actor,Role.EMPRESA);
        return accounts.findAll().stream().map(Views.Person::of).toList();
    }
    public List<Views.Car> cars(LocalDate start,LocalDate end) {
        if (start!=null || end!=null) dates(start,end);
        return vehicles.findAll().stream()
            .filter(v->start==null || rentals.countOverlaps(v.id,null,start,end,ACTIVE)==0)
            .map(Views.Car::of).toList();
    }
    @Transactional
    public Views.Car saveCar(Account actor,Long id,Requests.VehicleInput input) {
        requireRole(actor,Role.EMPRESA);
        if (input.year()>Year.now().getValue()+1) throw bad("Ano do veículo inválido.");
        Vehicle v=id==null ? new Vehicle() : vehicles.lockById(id).orElseThrow(()->notFound("Veículo"));
        if (id!=null) {
            if (!v.company.id.equals(actor.id)) throw forbidden();
            if (rentals.findAll().stream().anyMatch(r->r.vehicle.id.equals(id) && ACTIVE.contains(r.status)))
                throw fail(HttpStatus.CONFLICT,"O veículo tem pedidos em andamento e não pode ser alterado.");
        }
        v.registration=input.registration().trim(); v.year=input.year(); v.brand=input.brand().trim();
        v.model=input.model().trim(); v.plate=input.plate().toUpperCase(Locale.ROOT); v.dailyRate=input.dailyRate();
        v.owner=accounts.findById(input.ownerId()).orElseThrow(()->notFound("Proprietário")); v.company=actor;
        return Views.Car.of(vehicles.saveAndFlush(v));
    }
    public List<Views.Booking> bookings(Account actor) {
        return rentals.findAll().stream().filter(r->canRead(actor,r))
            .sorted(Comparator.comparing((Rental r)->r.id).reversed()).map(Views.Booking::of).toList();
    }
    public Views.Detail detail(Account actor,Long id) {
        Rental r=rentals.findById(id).orElseThrow(()->notFound("Pedido"));
        requireRead(actor,r);
        return new Views.Detail(Views.Booking.of(r),events.findByRentalIdOrderByCreatedAtAscIdAsc(id).stream().map(Views.Event::of).toList(),
            contracts.findByRentalId(id).map(Views.Agreement::of).orElse(null),
            creditContracts.findByContractRentalId(id).map(Views.CreditAgreement::of).orElse(null));
    }
    @Transactional
    public Views.Booking create(Account actor,Requests.RentalInput input) {
        requireRole(actor,Role.CLIENTE);
        Rental r=new Rental(); r.client=actor; r.createdAt=Instant.now();
        apply(actor,r,input); r=rentals.saveAndFlush(r);
        event(actor,r,"PEDIDO_CRIADO",conditions(r));
        return Views.Booking.of(r);
    }
    @Transactional
    public Views.Booking update(Account actor,Long id,long version,Requests.RentalInput input) {
        Rental r=locked(actor,id,version);
        editable(r);
        if (actor.role==Role.BANCO && !Objects.equals(input.bankId(),actor.id)) throw forbidden();
        String before=conditions(r);
        apply(actor,r,input); rentals.flush();
        event(actor,r,"PEDIDO_ALTERADO","Antes: "+before+" | Depois: "+conditions(r)+" | "+Objects.toString(input.note(),"Nova análise necessária."));
        return Views.Booking.of(r);
    }
    @Transactional
    public Views.Booking cancel(Account actor,Long id,long version) {
        requireRole(actor,Role.CLIENTE);
        Rental r=locked(actor,id,version); editable(r); r.status=Status.CANCELADO; rentals.flush();
        event(actor,r,"PEDIDO_CANCELADO","Cancelado pelo cliente."); return Views.Booking.of(r);
    }
    @Transactional
    public Views.Booking evaluate(Account actor,Long id,long version,Requests.Evaluation input) {
        if (actor.role==Role.CLIENTE) throw forbidden();
        Rental r=locked(actor,id,version);
        if (actor.role==Role.EMPRESA && r.status!=Status.EM_ANALISE ||
            actor.role==Role.BANCO && r.status!=Status.AGUARDANDO_BANCO)
            throw fail(HttpStatus.CONFLICT,"Este pedido não está aguardando sua avaliação.");
        vehicles.lockById(r.vehicle.id).orElseThrow(()->notFound("Veículo"));
        if (input.approve()) available(r.vehicle.id,r.id,r.startDate,r.endDate);
        r.status=!input.approve() ? Status.REJEITADO :
            actor.role==Role.EMPRESA && r.bank!=null ? Status.AGUARDANDO_BANCO : Status.APROVADO;
        rentals.flush(); event(actor,r,input.approve()?"PARECER_POSITIVO":"PARECER_NEGATIVO",input.note());
        return Views.Booking.of(r);
    }
    @Transactional
    public Views.Agreement accept(Account actor,Long id,long version) {
        requireRole(actor,Role.CLIENTE);
        Rental r=locked(actor,id,version);
        if (r.status!=Status.APROVADO) throw fail(HttpStatus.CONFLICT,"Apenas pedidos aprovados podem gerar contrato.");
        dates(r.startDate,r.endDate);
        vehicles.lockById(r.vehicle.id).orElseThrow(()->notFound("Veículo"));
        available(r.vehicle.id,r.id,r.startDate,r.endDate);
        Contract c=new Contract(); c.rental=r; c.acceptedAt=Instant.now();
        c.terms="CONTRATO DE ALUGUEL — Pedido #"+r.id+"\n"+
            "Contratante: "+r.client.name+" | CPF: "+r.client.cpf+" | RG: "+r.client.rg+"\n"+
            "Endereço: "+r.client.address+" | Profissão: "+r.client.profession+"\n"+
            "Empresa: "+r.company.name+"\n"+
            "Veículo: "+r.vehicle.brand+" "+r.vehicle.model+" | Ano: "+r.vehicle.year+
            " | Placa: "+r.vehicle.plate+" | Matrícula: "+r.vehicle.registration+"\n"+
            "Proprietário: "+r.vehicle.owner.name+" ("+r.vehicle.owner.role+")\n"+
            conditions(r)+"\n"+
            (r.bank==null ? "Modalidade: sem crédito." : "Modalidade: com crédito. Banco concedente: "+r.bank.name+
                ". Crédito sem juros: R$ "+r.creditAmount+" em "+r.installments+" parcelas. Saldo fora do crédito: R$ "+r.total.subtract(r.creditAmount)+".")+"\n"+
            "Aceito eletronicamente pelo cliente em "+c.acceptedAt+". Registro acadêmico, sem cobrança integrada.";
        r.status=Status.CONTRATADO; rentals.flush();
        c=contracts.saveAndFlush(c);
        if (r.bank!=null) {
            CreditContract credit=new CreditContract(); credit.contract=c; credit.bank=r.bank;
            credit.amount=r.creditAmount; credit.installments=r.installments; credit.acceptedAt=c.acceptedAt;
            credit.terms="CONTRATO DE CRÉDITO — Aluguel #"+c.id+" / Pedido #"+r.id+"\n"+
                "Banco concedente: "+r.bank.name+"\nContratante: "+r.client.name+" | CPF: "+r.client.cpf+
                "\nValor concedido: R$ "+r.creditAmount+" | Parcelas: "+r.installments+
                " | Juros: zero.\nSaldo fora do crédito: R$ "+r.total.subtract(r.creditAmount)+
                "\nAceito pelo cliente em "+c.acceptedAt+". Registro acadêmico, sem desembolso integrado.";
            creditContracts.saveAndFlush(credit);
        }
        event(actor,r,"CONTRATO_FORMALIZADO","Condições confirmadas pelo cliente.");
        return Views.Agreement.of(c);
    }
    private void apply(Account actor,Rental r,Requests.RentalInput input) {
        dates(input.startDate(),input.endDate());
        Vehicle v=vehicles.lockById(input.vehicleId()).orElseThrow(()->notFound("Veículo"));
        if (actor.role!=Role.CLIENTE && !v.company.id.equals(r.company.id))
            throw bad("Agentes só podem selecionar veículos da empresa responsável.");
        available(v.id,r.id,input.startDate(),input.endDate());
        r.vehicle=v; r.company=v.company; r.startDate=input.startDate(); r.endDate=input.endDate();
        r.total=v.dailyRate.multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(r.startDate,r.endDate)));
        if (r.total.compareTo(new BigDecimal("9999999999.99"))>0) throw bad("O valor total excede o limite permitido.");
        if (input.bankId()!=null) {
            r.bank=accounts.findById(input.bankId()).filter(a->a.role==Role.BANCO).orElseThrow(()->bad("Selecione um banco válido."));
            if (input.creditAmount()==null || input.installments()==null || input.creditAmount().compareTo(r.total)>0)
                throw bad("Informe valor de crédito até o total do aluguel e quantidade de parcelas.");
            r.creditAmount=input.creditAmount(); r.installments=input.installments();
        } else {
            if (input.creditAmount()!=null || input.installments()!=null) throw bad("Selecione um banco para solicitar crédito.");
            r.bank=null; r.creditAmount=null; r.installments=null;
        }
        r.status=Status.EM_ANALISE;
    }
    private Rental locked(Account actor,Long id,long version) {
        Rental r=rentals.lockById(id).orElseThrow(()->notFound("Pedido")); requireRead(actor,r);
        if (r.version!=version) throw fail(HttpStatus.CONFLICT,"O pedido mudou. Atualize os dados antes de continuar.");
        return r;
    }
    private void editable(Rental r) {
        if (r.status==Status.CANCELADO || r.status==Status.CONTRATADO)
            throw fail(HttpStatus.CONFLICT,"Pedidos cancelados ou contratados não podem ser alterados.");
    }
    private boolean canRead(Account a,Rental r) {
        return switch(a.role) {
            case CLIENTE -> r.client.id.equals(a.id);
            case EMPRESA -> r.company.id.equals(a.id);
            case BANCO -> r.bank!=null && r.bank.id.equals(a.id);
        };
    }
    private void requireRead(Account a,Rental r) { if (!canRead(a,r)) throw forbidden(); }
    private void requireRole(Account a,Role role) { if (a.role!=role) throw forbidden(); }
    private void available(Long vehicleId,Long exclude,LocalDate start,LocalDate end) {
        if (rentals.countOverlaps(vehicleId,exclude,start,end,ACTIVE)>0)
            throw fail(HttpStatus.CONFLICT,"Veículo indisponível nesse período. Escolha outras datas ou outro carro.");
    }
    private void dates(LocalDate start,LocalDate end) {
        if (start==null || end==null || start.isBefore(LocalDate.now()) || !end.isAfter(start))
            throw bad("A retirada deve ser hoje ou depois, e a devolução deve ser posterior à retirada.");
    }
    private void event(Account a,Rental r,String action,String note) {
        RentalEvent e=new RentalEvent(); e.rental=r; e.actorId=a.id; e.actorName=a.name;
        e.action=action; e.note=note; e.createdAt=Instant.now(); events.save(e);
    }
    private String conditions(Rental r) {
        return "Placa: "+r.vehicle.plate+" | Retirada: "+r.startDate+" | Devolução: "+r.endDate+" | Total: R$ "+r.total+
            (r.bank==null ? " | Sem crédito" : " | Banco: "+r.bank.name+" | Crédito: R$ "+r.creditAmount+" | Parcelas: "+r.installments);
    }
    private ResponseStatusException bad(String message) { return fail(HttpStatus.BAD_REQUEST,message); }
    private ResponseStatusException forbidden() { return fail(HttpStatus.FORBIDDEN,"Você não tem permissão para esta operação."); }
    private ResponseStatusException notFound(String resource) { return fail(HttpStatus.NOT_FOUND,resource+" não encontrado."); }
    private ResponseStatusException fail(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
