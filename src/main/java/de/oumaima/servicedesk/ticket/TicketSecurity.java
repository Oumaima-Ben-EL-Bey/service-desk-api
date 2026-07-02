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



    private boolean isAdminOrTeamAgent(Ticket ticket, User user) {
        if (user.hasRole("ADMIN")) {
            return true;
        }
        if (user.hasRole("AGENT")) {
            return ticket.getTeam() != null
                    && user.getTeam() != null
                    && ticket.getTeam().getId().equals(user.getTeam().getId());
        }
        return false;
    }
    public boolean canAccess(Long id, CustomUserDetails principal) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        User user = principal.getUser();
        if(isAdminOrTeamAgent(ticket, user)) {
            return true;
        }
        return ticket.getRequester().getId().equals(user.getId());
    }



    public boolean canChangeStatus(Long id, CustomUserDetails principal) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        User user = principal.getUser();

        return isAdminOrTeamAgent(ticket, user);
    }

    public boolean canClaim(Long id, CustomUserDetails principal) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        User user = principal.getUser();
        return user.hasRole("AGENT")
                && ticket.getTeam() != null
                && user.getTeam() != null
                && ticket.getTeam().getId().equals(user.getTeam().getId());
    }



}
