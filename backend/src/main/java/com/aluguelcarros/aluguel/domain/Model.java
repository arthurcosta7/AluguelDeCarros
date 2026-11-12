package com.aluguelcarros.aluguel.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class Model {
    private Model() {}
    public enum Role { CLIENTE, EMPRESA, BANCO }
    public enum Status { EM_ANALISE, AGUARDANDO_BANCO, APROVADO, REJEITADO, CANCELADO, CONTRATADO }

    @Entity(name="Account") @Table(name="accounts")
    public static class Account {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false, unique=true, length=40) public String username;
        @Column(name="password_hash", nullable=false) public String passwordHash;
        @Column(nullable=false, length=120) public String name;
        @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) public Role role;
        @Column(length=20) public String rg;
        @Column(unique=true, length=11) public String cpf;
        @Column(length=250) public String address;
        @Column(length=100) public String profession;
        @ElementCollection(fetch=FetchType.EAGER)
        @CollectionTable(name="employers", joinColumns=@JoinColumn(name="account_id"))
        public List<Employer> employers = new ArrayList<>();
    }
    @Embeddable
    public static class Employer {
        @Column(nullable=false, length=120) public String name;
        @Column(nullable=false, precision=12, scale=2) public BigDecimal income;
        public Employer() {}
        public Employer(String name, BigDecimal income) { this.name=name; this.income=income; }
    }
    @Entity(name="Vehicle") @Table(name="vehicles")
    public static class Vehicle {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false, unique=true, length=40) public String registration;
        @Column(nullable=false) public int year;
        @Column(nullable=false, length=60) public String brand;
        @Column(nullable=false, length=80) public String model;
        @Column(nullable=false, unique=true, length=7) public String plate;
        @Column(name="daily_rate", nullable=false, precision=12, scale=2) public BigDecimal dailyRate;
        @ManyToOne(optional=false) @JoinColumn(name="owner_id") public Account owner;
        @ManyToOne(optional=false) @JoinColumn(name="company_id") public Account company;
        @Version public long version;
    }
    @Entity(name="Rental") @Table(name="rentals")
    public static class Rental {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @ManyToOne(optional=false) @JoinColumn(name="client_id") public Account client;
        @ManyToOne(optional=false) @JoinColumn(name="vehicle_id") public Vehicle vehicle;
        @ManyToOne(optional=false) @JoinColumn(name="company_id") public Account company;
        @ManyToOne @JoinColumn(name="bank_id") public Account bank;
        @Column(name="start_date", nullable=false) public LocalDate startDate;
        @Column(name="end_date", nullable=false) public LocalDate endDate;
        @Column(nullable=false, precision=12, scale=2) public BigDecimal total;
        @Column(name="credit_amount", precision=12, scale=2) public BigDecimal creditAmount;
        public Integer installments;
        @Enumerated(EnumType.STRING) @Column(nullable=false, length=24) public Status status;
        @Column(name="created_at", nullable=false) public Instant createdAt;
        @Version public long version;
    }
    @Entity(name="Contract") @Table(name="contracts")
    public static class Contract {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @OneToOne(optional=false) @JoinColumn(name="rental_id", unique=true) public Rental rental;
        @Column(name="accepted_at", nullable=false) public Instant acceptedAt;
        @Column(nullable=false, columnDefinition="text") public String terms;
    }
    @Entity(name="CreditContract") @Table(name="credit_contracts")
    public static class CreditContract {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @OneToOne(optional=false) @JoinColumn(name="contract_id", unique=true) public Contract contract;
        @ManyToOne(optional=false) @JoinColumn(name="bank_id") public Account bank;
        @Column(nullable=false, precision=12, scale=2) public BigDecimal amount;
        @Column(nullable=false) public int installments;
        @Column(name="accepted_at", nullable=false) public Instant acceptedAt;
        @Column(nullable=false, columnDefinition="text") public String terms;
    }
    @Entity(name="RentalEvent") @Table(name="rental_events")
    public static class RentalEvent {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @ManyToOne(optional=false) @JoinColumn(name="rental_id") public Rental rental;
        @Column(name="actor_id", nullable=false) public Long actorId;
        @Column(name="actor_name", nullable=false, length=120) public String actorName;
        @Column(nullable=false, length=60) public String action;
        @Column(nullable=false, length=4000) public String note;
        @Column(name="created_at", nullable=false) public Instant createdAt;
    }
}
