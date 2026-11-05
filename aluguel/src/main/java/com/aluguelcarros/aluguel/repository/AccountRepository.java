package com.aluguelcarros.aluguel.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aluguelcarros.aluguel.domain.Model.*;
import java.util.Optional;
public interface AccountRepository extends JpaRepository<Account, Long> {
 Optional<Account> findByUsername(String username);
 boolean existsByUsername(String username);
 boolean existsByCpf(String cpf);
 java.util.List<Account> findByRole(Role role);
}
