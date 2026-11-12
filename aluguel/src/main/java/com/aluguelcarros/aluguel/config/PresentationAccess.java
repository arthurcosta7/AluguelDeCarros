package com.aluguelcarros.aluguel.config;

import com.aluguelcarros.aluguel.domain.Model.*;
import com.aluguelcarros.aluguel.repository.AccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** Alternância explícita para a conta de apresentação configurada pelo operador. */
@Component
public class PresentationAccess implements CommandLineRunner {
    private static final String SESSION_ROLE="rota.presentation.role";
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final String username;
    public PresentationAccess(AccountRepository accounts,PasswordEncoder encoder,
            @Value("${app.presentation.username:}") String username) {
        this.accounts=accounts; this.encoder=encoder; this.username=username.trim().toLowerCase(Locale.ROOT);
    }
    public boolean enabled(String principal) { return !username.isEmpty() && username.equals(principal); }
    public List<Role> roles(String principal) { return enabled(principal) ? List.of(Role.CLIENTE,Role.EMPRESA,Role.BANCO) : List.of(); }
    private String target(Role role) { return role==Role.CLIENTE ? username : username+"."+role.name().toLowerCase(Locale.ROOT); }
    @Override @Transactional
    public void run(String... args) {
        if (username.isEmpty()) return;
        Account base=accounts.findByUsername(username).orElse(null);
        if (base==null) return;
        if (base.role!=Role.CLIENTE || username.length()>32) throw new IllegalStateException("Use uma conta de cliente com usuário de até 32 caracteres para apresentação.");
        for (Role role:List.of(Role.EMPRESA,Role.BANCO)) {
            Account actor=accounts.findByUsername(target(role)).orElseGet(()->{
                Account a=new Account(); a.username=target(role); a.name=(base.name+" · "+(role==Role.EMPRESA ? "Empresa" : "Banco"));
                if (a.name.length()>120) a.name=a.name.substring(0,120);
                a.role=role; a.passwordHash=encoder.encode(UUID.randomUUID().toString());
                return accounts.saveAndFlush(a);
            });
            if (actor.role!=role) throw new IllegalStateException("Usuário de apresentação vinculado a um perfil incompatível.");
        }
    }
    public Account actor(String principal,HttpSession session) {
        Role role=enabled(principal) && session!=null && session.getAttribute(SESSION_ROLE) instanceof Role selected ? selected : null;
        return accounts.findByUsername(role==null ? principal : target(role))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sessão inválida."));
    }
    public Account select(String principal,Role role,HttpSession session) {
        if (!enabled(principal)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Alternância não habilitada para sua conta.");
        Account actor=accounts.findByUsername(target(role)).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Perfil de apresentação não preparado. Reinicie o backend."));
        if (actor.role!=role) throw new ResponseStatusException(HttpStatus.CONFLICT,"Perfil incompatível.");
        session.setAttribute(SESSION_ROLE,role);
        return actor;
    }
}
