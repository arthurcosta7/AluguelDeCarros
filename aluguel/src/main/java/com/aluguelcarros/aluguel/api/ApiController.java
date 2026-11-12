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
    private final com.aluguelcarros.aluguel.config.PresentationAccess presentation;
    private final jakarta.servlet.http.HttpServletRequest request;
    public ApiController(RentalService service,com.aluguelcarros.aluguel.config.PresentationAccess presentation,jakarta.servlet.http.HttpServletRequest request) {
        this.service=service; this.presentation=presentation; this.request=request;
    }
    private com.aluguelcarros.aluguel.domain.Model.Account actor(Authentication auth) { return presentation.actor(auth.getName(),request.getSession(false)); }
    @PostMapping("/auth/presentation-role") public Views.User selectRole(Authentication auth,@Valid @RequestBody Requests.PresentationRole input) {
        return Views.User.of(presentation.select(auth.getName(),input.role(),request.getSession()),presentation.roles(auth.getName()));
    }
    @GetMapping("/auth/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public Views.User register(@Valid @RequestBody Requests.Registration input) { return service.register(input); }
    @GetMapping("/auth/me") public Views.User me(Authentication auth) { return Views.User.of(actor(auth),presentation.roles(auth.getName())); }
    @PutMapping("/clients/me") public Views.User updateProfile(Authentication auth,@Valid @RequestBody Requests.ProfileInput input) {
        service.updateProfile(actor(auth),input);
        return Views.User.of(actor(auth),presentation.roles(auth.getName()));
    }
    @DeleteMapping("/clients/me") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(Authentication auth,@Valid @RequestBody Requests.DeleteAccount input,
            jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response) {
        if (presentation.enabled(auth.getName())) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,"A conta de apresentação não pode ser excluída enquanto a alternância estiver habilitada.");
        service.deleteProfile(actor(auth),input.password());
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request,response,auth);
    }
    @GetMapping("/banks") public List<Views.Person> banks() { return service.banks(); }
    @GetMapping("/owners") public List<Views.Person> owners(Authentication auth) { return service.owners(actor(auth)); }
    @GetMapping("/vehicles") public List<Views.Car> cars(@RequestParam(required=false) LocalDate start,@RequestParam(required=false) LocalDate end) { return service.cars(start,end); }
    @PostMapping("/vehicles") @ResponseStatus(HttpStatus.CREATED)
    public Views.Car createCar(Authentication auth,@Valid @RequestBody Requests.VehicleInput input) { return service.saveCar(actor(auth),null,input); }
    @PutMapping("/vehicles/{id}") public Views.Car updateCar(Authentication auth,@PathVariable Long id,@Valid @RequestBody Requests.VehicleInput input) { return service.saveCar(actor(auth),id,input); }
    @GetMapping("/rentals") public List<Views.Booking> bookings(Authentication auth) { return service.bookings(actor(auth)); }
    @GetMapping("/rentals/{id}") public Views.Detail detail(Authentication auth,@PathVariable Long id) { return service.detail(actor(auth),id); }
    @PostMapping("/rentals") @ResponseStatus(HttpStatus.CREATED)
    public Views.Booking create(Authentication auth,@Valid @RequestBody Requests.RentalInput input) { return service.create(actor(auth),input); }
    @PutMapping("/rentals/{id}") public Views.Booking update(Authentication auth,@PathVariable Long id,@RequestParam long version,@Valid @RequestBody Requests.RentalInput input) { return service.update(actor(auth),id,version,input); }
    @PostMapping("/rentals/{id}/cancel") public Views.Booking cancel(Authentication auth,@PathVariable Long id,@RequestParam long version) { return service.cancel(actor(auth),id,version); }
    @PostMapping("/rentals/{id}/evaluate") public Views.Booking evaluate(Authentication auth,@PathVariable Long id,@RequestParam long version,@Valid @RequestBody Requests.Evaluation input) { return service.evaluate(actor(auth),id,version,input); }
    @PostMapping("/rentals/{id}/accept") public Views.Agreement accept(Authentication auth,@PathVariable Long id,@RequestParam long version) { return service.accept(actor(auth),id,version); }
}
