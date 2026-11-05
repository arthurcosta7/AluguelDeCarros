package com.aluguelcarros.aluguel.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aluguelcarros.aluguel.domain.Model.*;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface RentalRepository extends JpaRepository<Rental, Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Rental r where r.id = :id")
 java.util.Optional<Rental> lockById(Long id);
 @Query("select count(r) from Rental r where r.vehicle.id = :vehicleId and (:excludeId is null or r.id <> :excludeId) and r.status in :active and r.startDate < :end and r.endDate > :start")
 long countOverlaps(Long vehicleId, Long excludeId, LocalDate start, LocalDate end, java.util.Collection<Status> active);
}
