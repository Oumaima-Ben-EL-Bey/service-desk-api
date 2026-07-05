package de.oumaima.servicedesk.ticket;

import de.oumaima.servicedesk.team.TeamRepository;
import de.oumaima.servicedesk.user.User;
import de.oumaima.servicedesk.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;


    public TicketService(TicketRepository ticketRepository, UserRepository userRepository, TeamRepository teamRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
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
    @Transactional
    public Ticket claim(Long ticketId, User assignee) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot assign a closed ticket");
        }
        ticket.setAssignee(assignee);


        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket assign(Long ticketId, Long assigneeId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot assign a closed ticket");
        }
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ticket.setAssignee(assignee);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket create(CreateTicketRequest request, User requester) {
        Ticket ticket = TicketMapper.toEntity(request, requester);

        teamRepository.findByCategory(ticket.getCategory())
                .ifPresent(ticket::setTeam);

        return ticketRepository.save(ticket);
    }
}