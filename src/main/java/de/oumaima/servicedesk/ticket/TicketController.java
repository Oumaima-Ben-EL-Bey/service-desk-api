package de.oumaima.servicedesk.ticket;

import de.oumaima.servicedesk.user.CustomUserDetails;
import de.oumaima.servicedesk.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    public TicketController(TicketRepository ticketRepository, TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        Ticket saved = ticketService.create(request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketMapper.toResponse(saved));
    }

    @PreAuthorize("@ticketSecurity.canAccess(#id, principal)")
    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    return TicketMapper.toResponse(ticket);
    }

    @GetMapping
    public List<TicketResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();

        List<Ticket> tickets;
        if (user.hasRole("ADMIN")) {
            tickets = ticketRepository.findAll();
        } else if (user.hasRole("AGENT")) {
            tickets = user.getTeam() == null
                    ? List.of()
                    : ticketRepository.findByTeamId(user.getTeam().getId());
        } else {
            tickets = ticketRepository.findByRequesterId(user.getId());
        }

        return tickets.stream().map(TicketMapper::toResponse).toList();
    }

    @PreAuthorize("@ticketSecurity.canChangeStatus(#id, principal)")
    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id,
                                       @RequestBody ChangeStatusRequest request) {
        Ticket updated = ticketService.changeStatus(id, request.status());
        return TicketMapper.toResponse(updated);
    }

    @PreAuthorize("@ticketSecurity.canClaim(#id, principal)")
    @PostMapping("/{id}/claim")
    public TicketResponse claim(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails principal) {
        Ticket updated = ticketService.claim(id, principal.getUser());
        return TicketMapper.toResponse(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/assignee")
    public TicketResponse assign(@PathVariable Long id,
                                 @RequestBody AssignRequest request) {
        Ticket updated = ticketService.assign(id, request.assigneeId());
        return TicketMapper.toResponse(updated);
    }
}
