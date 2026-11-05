package com.aluguelcarros.aluguel.api;

import com.aluguelcarros.aluguel.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final RentalService service;
    public ApiController(RentalService service) { this.service=service; }
    @GetMapping("/auth/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public Views.User register(@Valid @RequestBody Requests.Registration input) { return service.register(input); }
    @GetMapping("/auth/me") public Views.User me(Authentication auth) { return Views.User.of(service.account(auth.getName())); }
    @GetMapping("/banks") public List<Views.Person> banks() { return service.banks(); }
    @GetMapping("/owners") public List<Views.Person> owners(Authentication auth) { return service.owners(service.account(auth.getName())); }
    @GetMapping("/vehicles") public List<Views.Car> cars(@RequestParam(required=false) LocalDate start,@RequestParam(required=false) LocalDate end) { return service.cars(start,end); }
    @PostMapping("/vehicles") @ResponseStatus(HttpStatus.CREATED)
    public Views.Car createCar(Authentication auth,@Valid @RequestBody Requests.VehicleInput input) { return service.saveCar(service.account(auth.getName()),null,input); }
    @PutMapping("/vehicles/{id}") public Views.Car updateCar(Authentication auth,@PathVariable Long id,@Valid @RequestBody Requests.VehicleInput input) { return service.saveCar(service.account(auth.getName()),id,input); }
    @GetMapping("/rentals") public List<Views.Booking> bookings(Authentication auth) { return service.bookings(service.account(auth.getName())); }
    @GetMapping("/rentals/{id}") public Views.Detail detail(Authentication auth,@PathVariable Long id) { return service.detail(service.account(auth.getName()),id); }
    @PostMapping("/rentals") @ResponseStatus(HttpStatus.CREATED)
    public Views.Booking create(Authentication auth,@Valid @RequestBody Requests.RentalInput input) { return service.create(service.account(auth.getName()),input); }
    @PutMapping("/rentals/{id}") public Views.Booking update(Authentication auth,@PathVariable Long id,@RequestParam long version,@Valid @RequestBody Requests.RentalInput input) { return service.update(service.account(auth.getName()),id,version,input); }
    @PostMapping("/rentals/{id}/cancel") public Views.Booking cancel(Authentication auth,@PathVariable Long id,@RequestParam long version) { return service.cancel(service.account(auth.getName()),id,version); }
    @PostMapping("/rentals/{id}/evaluate") public Views.Booking evaluate(Authentication auth,@PathVariable Long id,@RequestParam long version,@Valid @RequestBody Requests.Evaluation input) { return service.evaluate(service.account(auth.getName()),id,version,input); }
    @PostMapping("/rentals/{id}/accept") public Views.Agreement accept(Authentication auth,@PathVariable Long id,@RequestParam long version) { return service.accept(service.account(auth.getName()),id,version); }
}
