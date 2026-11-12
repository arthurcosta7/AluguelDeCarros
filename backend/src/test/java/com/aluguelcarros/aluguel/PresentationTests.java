package com.aluguelcarros.aluguel;

import com.aluguelcarros.aluguel.config.PresentationAccess;
import com.aluguelcarros.aluguel.domain.Model.*;
import com.aluguelcarros.aluguel.repository.AccountRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"app.presentation.username=apresentador","spring.datasource.url=jdbc:h2:mem:presentation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR"})
@AutoConfigureMockMvc
class PresentationTests {
 @Autowired MockMvc mvc;
 @Autowired AccountRepository accounts;
 @Autowired PasswordEncoder encoder;
 @Autowired PresentationAccess presentation;
 @BeforeEach void prepare() {
  if (accounts.findByUsername("apresentador").isEmpty()) {
   Account a=new Account();a.username="apresentador";a.name="Apresentador";a.role=Role.CLIENTE;a.passwordHash=encoder.encode("senha1234"); accounts.saveAndFlush(a);
  }
  presentation.run();
 }
 @Test void switchesSessionWithoutChangingOriginalAccountOrOtherSessions() throws Exception {
  var result=mvc.perform(post("/api/auth/login").with(csrf()).param("username","apresentador").param("password","senha1234")).andExpect(status().isNoContent()).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);
  for (String role:new String[]{"EMPRESA","BANCO","CLIENTE"}) {
   mvc.perform(post("/api/auth/presentation-role").session(session).with(csrf()).contentType("application/json").content("{\"role\":\""+role+"\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role)).andExpect(jsonPath("$.presentationRoles.length()").value(3));
   mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.role").value(role));
  }
  mvc.perform(get("/api/auth/me").with(user("apresentador"))).andExpect(jsonPath("$.role").value("CLIENTE"));
  assertThat(accounts.findByUsername("apresentador").orElseThrow().role).isEqualTo(Role.CLIENTE);
  assertThat(encoder.matches("senha1234",accounts.findByUsername("apresentador").orElseThrow().passwordHash)).isTrue();
  mvc.perform(delete("/api/clients/me").session(session).with(csrf()).contentType("application/json").content("{\"password\":\"senha1234\"}")).andExpect(status().isConflict());
 }
 @Test void blocksUnconfiguredAccountsAndRequiresCsrf() throws Exception {
  mvc.perform(post("/api/auth/presentation-role").with(user("outro")).with(csrf()).contentType("application/json").content("{\"role\":\"BANCO\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/presentation-role").with(user("apresentador")).contentType("application/json").content("{\"role\":\"BANCO\"}")).andExpect(status().isForbidden());
 }
}
