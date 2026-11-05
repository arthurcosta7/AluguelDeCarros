package com.aluguelcarros.aluguel.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aluguelcarros.aluguel.domain.Model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select v from Vehicle v where v.id = :id")
 java.util.Optional<Vehicle> lockById(Long id);
}
