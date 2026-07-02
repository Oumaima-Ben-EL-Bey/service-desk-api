package de.oumaima.servicedesk.ticket;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public Ticket changeStatus(Long ticketId, TicketStatus target) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (!ticket.getStatus().canTransitionTo(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Illegal transition: " + ticket.getStatus() + " → " + target);
        }

        ticket.setStatus(target);
        return ticketRepository.save(ticket);
    }
}