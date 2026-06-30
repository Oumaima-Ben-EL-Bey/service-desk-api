package de.oumaima.servicedesk.ticket;

import de.oumaima.servicedesk.user.CustomUserDetails;
import de.oumaima.servicedesk.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TicketSecurity {
    private final TicketRepository ticketRepository;

    public TicketSecurity(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public boolean canAccess(Long id, CustomUserDetails principal) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        User user = principal.getUser();
        if (hasRole(user, "ADMIN")) {
            return true;
        }
        if (hasRole(user, "AGENT")) {
            return ticket.getTeam() != null
                    && user.getTeam() != null
                    && ticket.getTeam().getId().equals(user.getTeam().getId());
        }
        return ticket.getRequester().getId().equals(user.getId());
    }
    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
}
