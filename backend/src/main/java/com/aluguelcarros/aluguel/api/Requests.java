package com.aluguelcarros.aluguel.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Requests {
    private Requests() {}
    public record Job(@NotBlank @Size(max=120) String name,
                      @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal income) {}
    public record Registration(
        @NotBlank @Pattern(regexp="[a-zA-Z0-9_.-]{3,40}") String username,
        @NotBlank @Size(min=8, max=64) String password,
        @NotBlank @Size(max=120) String name,
        @NotBlank @Size(max=20) String rg,
        @NotBlank @Pattern(regexp="[0-9]{11}") String cpf,
        @NotBlank @Size(max=250) String address,
        @NotBlank @Size(max=100) String profession,
        @NotNull @Size(max=3) List<@NotNull @Valid Job> employers) {}
    public record ProfileInput(
        @NotBlank @Size(max=120) String name,
        @NotBlank @Size(max=20) String rg,
        @NotBlank @Pattern(regexp="[0-9]{11}") String cpf,
        @NotBlank @Size(max=250) String address,
        @NotBlank @Size(max=100) String profession,
        @NotNull @Size(max=3) List<@NotNull @Valid Job> employers) {}
    public record PresentationRole(@NotNull com.aluguelcarros.aluguel.domain.Model.Role role) {}
    public record DeleteAccount(@NotBlank @Size(max=64) String password) {}
    public record VehicleInput(
        @NotBlank @Size(max=40) String registration,
        @Min(1900) @Max(2100) int year,
        @NotBlank @Size(max=60) String brand,
        @NotBlank @Size(max=80) String model,
        @NotBlank @Pattern(regexp="[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}") String plate,
        @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal dailyRate,
        @NotNull Long ownerId) {}
    public record RentalInput(
        @NotNull Long vehicleId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Long bankId,
        @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal creditAmount,
        @Min(1) @Max(60) Integer installments,
        @Size(max=1000) String note) {}
    public record Evaluation(@NotNull Boolean approve, @NotBlank @Size(max=1000) String note) {}
}
