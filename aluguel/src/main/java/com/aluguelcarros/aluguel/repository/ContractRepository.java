package com.aluguelcarros.aluguel.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aluguelcarros.aluguel.domain.Model.*;
public interface ContractRepository extends JpaRepository<Contract, Long> {
 java.util.Optional<Contract> findByRentalId(Long rentalId);
}
