package de.oumaima.servicedesk.ticket;

import de.oumaima.servicedesk.user.User;
import de.oumaima.servicedesk.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketController(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest request) {
        User requester = userRepository.findById(request.requesterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requester not found"));

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
