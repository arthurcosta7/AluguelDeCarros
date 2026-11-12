package com.aluguelcarros.aluguel.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aluguelcarros.aluguel.domain.Model.CreditContract;
public interface CreditContractRepository extends JpaRepository<CreditContract, Long> {
 java.util.Optional<CreditContract> findByContractRentalId(Long rentalId);
}
