package com.aluguelcarros.aluguel.config;

import com.aluguelcarros.aluguel.domain.Model.*;
import com.aluguelcarros.aluguel.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name="app.demo.enabled",havingValue="true")
public class DemoData implements CommandLineRunner {
    private final AccountRepository accounts; private final VehicleRepository vehicles; private final PasswordEncoder encoder;
    private final String password;
    public DemoData(AccountRepository accounts,VehicleRepository vehicles,PasswordEncoder encoder,@Value("${app.demo.password}") String password) {
        this.accounts=accounts; this.vehicles=vehicles; this.encoder=encoder; this.password=password;
    }
    @Override @Transactional public void run(String... args) {
        if (password.length()<8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("Defina DEMO_PASSWORD com pelo menos 8 caracteres e no máximo 72 bytes.");
        Account company=create("empresa","Rota Locadora",Role.EMPRESA);
        create("banco","Banco Horizonte",Role.BANCO);
        create("cliente","Cliente Demonstração",Role.CLIENTE);
        if (vehicles.count()==0) {
            car("000001","Fiat","Argo","ABC1D23",2024,"120.00",company);
            car("000002","Volkswagen","Polo","EFG4H56",2025,"150.00",company);
            car("000003","Toyota","Corolla","IJK7L89",2025,"240.00",company);
        }
    }
    private Account create(String username,String name,Role role) {
        return accounts.findByUsername(username).orElseGet(()-> {
            Account a=new Account(); a.username=username; a.name=name; a.role=role; a.passwordHash=encoder.encode(password);
            if (role==Role.CLIENTE) {
                a.rg="DEMO123"; a.cpf="00000000000"; a.address="Rua de demonstração, 100"; a.profession="Estudante";
                a.employers.add(new Employer("Empresa de demonstração",new BigDecimal("3000.00")));
            }
            return accounts.save(a);
        });
    }
    private void car(String registration,String brand,String model,String plate,int year,String rate,Account company) {
        Vehicle v=new Vehicle(); v.registration=registration; v.brand=brand; v.model=model; v.plate=plate;
        v.year=year; v.dailyRate=new BigDecimal(rate); v.company=company; v.owner=company; vehicles.save(v);
    }
}
