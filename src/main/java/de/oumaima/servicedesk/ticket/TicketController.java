package de.oumaima.servicedesk.ticket;

import de.oumaima.servicedesk.user.CustomUserDetails;
import de.oumaima.servicedesk.user.User;
import de.oumaima.servicedesk.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;

    public TicketController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        User requester = principal.getUser();

        Ticket ticket = TicketMapper.toEntity(request, requester);
        Ticket saved = ticketRepository.save(ticket);

        return ResponseEntity.status(HttpStatus.CREATED).body(TicketMapper.toResponse(saved));
    }
    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    return TicketMapper.toResponse(ticket);
    }
}
