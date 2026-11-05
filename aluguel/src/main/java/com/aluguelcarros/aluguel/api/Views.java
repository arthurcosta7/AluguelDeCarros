package com.aluguelcarros.aluguel.api;

import com.aluguelcarros.aluguel.domain.Model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class Views {
    private Views() {}
    public record Person(Long id, String name, Role role) {
        public static Person of(Account a) { return a==null ? null : new Person(a.id,a.name,a.role); }
    }
    public record User(Long id, String username, String name, Role role, String rg, String cpf,
                       String address, String profession, List<Requests.Job> employers) {
        public static User of(Account a) { return new User(a.id,a.username,a.name,a.role,a.rg,a.cpf,a.address,a.profession,
            a.employers.stream().map(e->new Requests.Job(e.name,e.income)).toList()); }
    }
    public record Car(Long id, String registration, int year, String brand, String model, String plate,
                      BigDecimal dailyRate, Person owner, Person company) {
        public static Car of(Vehicle v) { return new Car(v.id,v.registration,v.year,v.brand,v.model,v.plate,v.dailyRate,Person.of(v.owner),Person.of(v.company)); }
    }
    public record Booking(Long id, User client, Car vehicle, Person company, Person bank,
                          LocalDate startDate, LocalDate endDate, BigDecimal total, BigDecimal creditAmount,
                          Integer installments, Status status, Instant createdAt, long version) {
        public static Booking of(Rental r) { return new Booking(r.id,User.of(r.client),Car.of(r.vehicle),Person.of(r.company),Person.of(r.bank),r.startDate,r.endDate,r.total,r.creditAmount,r.installments,r.status,r.createdAt,r.version); }
    }
    public record Event(Long id, String actorName, String action, String note, Instant createdAt) {
        public static Event of(RentalEvent e) { return new Event(e.id,e.actorName,e.action,e.note,e.createdAt); }
    }
    public record Agreement(Long id, Long rentalId, Instant acceptedAt, String terms) {
        public static Agreement of(Contract c) { return new Agreement(c.id,c.rental.id,c.acceptedAt,c.terms); }
    }
    public record Detail(Booking rental, List<Event> events, Agreement contract) {}
}
