package com.aluguelcarros.aluguel.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aluguelcarros.aluguel.domain.Model.*;
public interface EventRepository extends JpaRepository<RentalEvent, Long> {
 java.util.List<RentalEvent> findByRentalIdOrderByCreatedAtAscIdAsc(Long rentalId);
}
