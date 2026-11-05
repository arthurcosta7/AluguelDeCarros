package com.aluguelcarros.aluguel.config;

import com.aluguelcarros.aluguel.repository.AccountRepository;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(AccountRepository accounts) {
        return username -> accounts.findByUsername(username.toLowerCase(java.util.Locale.ROOT))
            .map(a -> User.withUsername(a.username).password(a.passwordHash).roles(a.role.name()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha inválidos."));
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/error").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/auth/register", "/api/auth/login").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                .successHandler((req,res,auth) -> res.setStatus(204))
                .failureHandler((req,res,ex) -> error(res,401,"Usuário ou senha inválidos.")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,ex) -> error(res,401,"Entre na sua conta para continuar."))
                .accessDeniedHandler((req,res,ex) -> error(res,403,"Acesso negado ou sessão expirada. Atualize a página.")))
            .requestCache(cache -> cache.disable())
            .build();
    }
    private static void error(jakarta.servlet.http.HttpServletResponse res,int status,String message) throws java.io.IOException {
        res.setStatus(status); res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\":\""+message+"\"}");
    }
}
