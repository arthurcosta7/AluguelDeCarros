package com.aluguelcarros.aluguel;

import com.aluguelcarros.aluguel.api.Requests;
import com.aluguelcarros.aluguel.domain.Model.*;
import com.aluguelcarros.aluguel.repository.*;
import com.aluguelcarros.aluguel.service.RentalService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest
@AutoConfigureMockMvc
class AluguelApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired VehicleRepository vehicles;
    @Autowired RentalRepository rentals;
    @Autowired EventRepository events;
    @Autowired ContractRepository contracts;
    @Autowired PasswordEncoder encoder;
    @Autowired RentalService service;
    final JsonMapper json=JsonMapper.builder().build();
    Account client, company, bank, otherCompany;
    Vehicle car;
    final LocalDate start=LocalDate.now().plusDays(5);
    final LocalDate end=start.plusDays(3);

    @BeforeEach void setup() {
        contracts.deleteAll(); events.deleteAll(); rentals.deleteAll(); vehicles.deleteAll(); accounts.deleteAll();
        client=account("cliente",Role.CLIENTE); company=account("empresa",Role.EMPRESA);
        bank=account("banco",Role.BANCO); otherCompany=account("outra",Role.EMPRESA);
        car=new Vehicle(); car.registration="001"; car.plate="ABC1D23"; car.brand="Fiat"; car.model="Argo";
        car.year=2024; car.dailyRate=new BigDecimal("100.00"); car.owner=company; car.company=company;
        car=vehicles.saveAndFlush(car);
    }
    Account account(String username,Role role) {
        Account a=new Account(); a.username=username; a.name=username; a.role=role; a.passwordHash=encoder.encode("senha1234");
        if (role==Role.CLIENTE) { a.cpf="12345678901"; a.rg="12345"; a.address="Rua A"; a.profession="Estudante"; a.employers.add(new Employer("Empresa A",new BigDecimal("3000.00"))); }
        return accounts.saveAndFlush(a);
    }
    Map<String,Object> input() {
        return new HashMap<>(Map.of("vehicleId",car.id,"startDate",start.toString(),"endDate",end.toString()));
    }
    Map<String,Object> creditInput() {
        Map<String,Object> data=input(); data.put("bankId",bank.id); data.put("creditAmount",200); data.put("installments",2); return data;
    }
    MvcResult create(Map<String,Object> input) throws Exception {
        return mvc.perform(post("/api/rentals").with(user("cliente")).with(csrf()).contentType("application/json").content(json.writeValueAsString(input)))
            .andExpect(status().isCreated()).andReturn();
    }
    long number(MvcResult result,String path) throws Exception { return ((Number)JsonPath.read(result.getResponse().getContentAsString(),path)).longValue(); }
    void evaluate(long id,long version,String username,boolean approve) throws Exception {
        mvc.perform(post("/api/rentals/"+id+"/evaluate?version="+version).with(user(username)).with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("approve",approve,"note","Documentação e renda verificadas.")))).andExpect(status().isOk());
    }
    long version(long id) throws Exception {
        return number(mvc.perform(get("/api/rentals/"+id).with(user("cliente"))).andExpect(status().isOk()).andReturn(),"$.rental.version");
    }
    @Test void loginUsesSessionAndMutationsRequireCsrf() throws Exception {
        mvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","cliente").param("password","errada")).andExpect(status().isUnauthorized());
        MvcResult login=mvc.perform(post("/api/auth/login").with(csrf()).param("username","cliente").param("password","senha1234"))
            .andExpect(status().isNoContent()).andReturn();
        MockHttpSession session=(MockHttpSession)login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CLIENTE"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }
    @Test void realCsrfTokensSupportFrontendLoginAndRotateAfterAuthentication() throws Exception {
        MvcResult csrf=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        String token=JsonPath.read(csrf.getResponse().getContentAsString(),"$.token");
        MockHttpSession session=(MockHttpSession)csrf.getRequest().getSession(false);
        mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN",token)
            .param("username","cliente").param("password","senha1234")).andExpect(status().isNoContent());
        mvc.perform(post("/api/rentals").session(session).header("X-CSRF-TOKEN",token)
            .contentType("application/json").content(json.writeValueAsString(input()))).andExpect(status().isForbidden());
        MvcResult fresh=mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        String freshToken=JsonPath.read(fresh.getResponse().getContentAsString(),"$.token");
        mvc.perform(post("/api/rentals").session(session).header("X-CSRF-TOKEN",freshToken)
            .contentType("application/json").content(json.writeValueAsString(input()))).andExpect(status().isCreated());
    }
    @Test void fleetRegistrationSupportsBankOwnershipAndProtectsActiveVehicles() throws Exception {
        Map<String,Object> data=Map.of("registration","002","year",2025,"brand","Toyota","model","Corolla","plate","DEF4G56","dailyRate",200,"ownerId",bank.id);
        mvc.perform(post("/api/vehicles").with(user("cliente")).with(csrf()).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isForbidden());
        MvcResult saved=mvc.perform(post("/api/vehicles").with(user("empresa")).with(csrf()).contentType("application/json").content(json.writeValueAsString(data)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.owner.role").value("BANCO")).andReturn();
        long newCar=number(saved,"$.id");
        mvc.perform(put("/api/vehicles/"+newCar).with(user("outra")).with(csrf()).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isForbidden());
        create(input());
        mvc.perform(put("/api/vehicles/"+car.id).with(user("empresa")).with(csrf()).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isConflict());
    }
    @Test void registrationChecksDocumentsIncomeAndDuplicateUser() throws Exception {
        Map<String,Object> data=new HashMap<>(Map.of("username","novo","password","senha1234","name","Novo Cliente","cpf","23456789012","rg","MG123","address","Rua B","profession","Professor","employers",List.of(Map.of("name","Escola","income",2500))));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(json.writeValueAsString(data)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CLIENTE")).andExpect(jsonPath("$.employers[0].income").value(2500));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isConflict());
        data.put("employers",Collections.nCopies(4,Map.of("name","Empresa","income",1000)));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isBadRequest());
        data.put("employers",List.of()); data.put("cpf","123");
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isBadRequest());
    }
    @Test void approvedRentalBecomesImmutableContractWithSnapshot() throws Exception {
        MvcResult created=create(input()); long id=number(created,"$.id"); long v=number(created,"$.version");
        assertThat(number(created,"$.total")).isEqualTo(300);
        mvc.perform(post("/api/rentals/"+id+"/accept?version="+v).with(user("cliente")).with(csrf())).andExpect(status().isConflict());
        evaluate(id,v,"empresa",true); long approved=version(id);
        mvc.perform(post("/api/rentals/"+id+"/accept?version="+v).with(user("cliente")).with(csrf())).andExpect(status().isConflict());
        mvc.perform(post("/api/rentals/"+id+"/accept?version="+approved).with(user("cliente")).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.terms").value(org.hamcrest.Matchers.containsString("ABC1D23")));
        mvc.perform(get("/api/rentals/"+id).with(user("empresa"))).andExpect(jsonPath("$.rental.status").value("CONTRATADO"))
            .andExpect(jsonPath("$.events.length()").value(3)).andExpect(jsonPath("$.contract.id").exists());
        mvc.perform(post("/api/rentals/"+id+"/cancel?version="+version(id)).with(user("cliente")).with(csrf())).andExpect(status().isConflict());
        assertThat(contracts.count()).isEqualTo(1);
        Account changed=accounts.findById(client.id).orElseThrow(); changed.name="Nome alterado"; accounts.saveAndFlush(changed);
        assertThat(contracts.findAll().getFirst().terms).contains("Contratante: cliente").doesNotContain("Nome alterado");
    }
    @Test void creditRequiresCompanyAndBankApprovalAndChangesResetAnalysis() throws Exception {
        long id=number(create(creditInput()),"$.id");
        mvc.perform(post("/api/rentals/"+id+"/evaluate?version=0").with(user("banco")).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("approve",true,"note","ok"))))
            .andExpect(status().isConflict());
        evaluate(id,version(id),"empresa",true);
        mvc.perform(get("/api/rentals/"+id).with(user("banco"))).andExpect(jsonPath("$.rental.status").value("AGUARDANDO_BANCO"));
        Map<String,Object> changed=creditInput(); changed.put("endDate",end.plusDays(1).toString());
        mvc.perform(put("/api/rentals/"+id+"?version="+version(id)).with(user("banco")).with(csrf()).contentType("application/json").content(json.writeValueAsString(changed)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANALISE"));
        evaluate(id,version(id),"empresa",true); evaluate(id,version(id),"banco",true);
        mvc.perform(post("/api/rentals/"+id+"/accept?version="+version(id)).with(user("cliente")).with(csrf())).andExpect(status().isOk())
            .andExpect(jsonPath("$.terms").value(org.hamcrest.Matchers.containsString("Banco concedente: banco")));
    }
    @Test void authorizationRestrictsClientsAndUnrelatedAgents() throws Exception {
        long id=number(create(input()),"$.id");
        mvc.perform(get("/api/rentals/"+id).with(user("outra"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/rentals/"+id).with(user("banco"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/rentals").with(user("outra"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/owners").with(user("cliente"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/rentals/"+id+"/evaluate?version=0").with(user("cliente")).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("approve",true,"note","ok"))))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/rentals/"+id+"/cancel?version=0").with(user("empresa")).with(csrf())).andExpect(status().isForbidden());
        Map<String,Object> credit=creditInput(); credit.put("creditAmount",301);
        mvc.perform(post("/api/rentals").with(user("cliente")).with(csrf()).contentType("application/json").content(json.writeValueAsString(credit)))
            .andExpect(status().isConflict());
    }
    @Test void overlapsAreBlockedButAdjacentPeriodsAndCancelledPeriodsAreAvailable() throws Exception {
        long id=number(create(input()),"$.id");
        mvc.perform(post("/api/rentals").with(user("cliente")).with(csrf()).contentType("application/json").content(json.writeValueAsString(input())))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/vehicles?start="+start+"&end="+end).with(user("cliente"))).andExpect(jsonPath("$.length()").value(0));
        Map<String,Object> adjacent=input(); adjacent.put("startDate",end.toString()); adjacent.put("endDate",end.plusDays(1).toString()); create(adjacent);
        mvc.perform(post("/api/rentals/"+id+"/cancel?version="+version(id)).with(user("cliente")).with(csrf())).andExpect(status().isOk());
        create(input());
        mvc.perform(get("/api/vehicles?start="+start).with(user("cliente"))).andExpect(status().isBadRequest());
    }
    @Test void invalidDatesAndCreditAreRejectedAndRejectionCanBeResubmitted() throws Exception {
        Map<String,Object> invalid=input(); invalid.put("endDate",start.toString());
        mvc.perform(post("/api/rentals").with(user("cliente")).with(csrf()).contentType("application/json").content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
        invalid=creditInput(); invalid.put("creditAmount",301);
        mvc.perform(post("/api/rentals").with(user("cliente")).with(csrf()).contentType("application/json").content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
        long id=number(create(input()),"$.id"); evaluate(id,version(id),"empresa",false);
        mvc.perform(get("/api/vehicles?start="+start+"&end="+end).with(user("cliente"))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put("/api/rentals/"+id+"?version="+version(id)).with(user("cliente")).with(csrf()).contentType("application/json").content(json.writeValueAsString(input())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANALISE"));
    }
    @Test void concurrentReservationsOnlyAllowOneWinner() throws Exception {
        var input=new Requests.RentalInput(car.id,start,end,null,null,null,null);
        CountDownLatch ready=new CountDownLatch(2), go=new CountDownLatch(1);
        try (var executor=Executors.newFixedThreadPool(2)) {
            Callable<Integer> attempt=()-> {
                ready.countDown(); go.await(5,TimeUnit.SECONDS);
                try { service.create(client,input); return 201; }
                catch (ResponseStatusException e) { return e.getStatusCode().value(); }
            };
            var first=executor.submit(attempt); var second=executor.submit(attempt);
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue(); go.countDown();
            assertThat(List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
            assertThat(rentals.count()).isEqualTo(1);
        }
    }
}
