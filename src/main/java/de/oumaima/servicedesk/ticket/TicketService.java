package de.oumaima.servicedesk.ticket;

import de.oumaima.servicedesk.team.TeamRepository;
import de.oumaima.servicedesk.user.User;
import de.oumaima.servicedesk.user.UserRepository;
import io.micrometer.core.instrument.Counter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.micrometer.core.instrument.MeterRegistry;
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final Counter ticketsCreated;
    private final Counter ticketsResolved;


    public TicketService(TicketRepository ticketRepository, UserRepository userRepository, TeamRepository teamRepository, MeterRegistry meterRegistry) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.ticketsCreated = Counter.builder("tickets.created")
                .description("Total tickets created")
                .register(meterRegistry);
        this.ticketsResolved = Counter.builder("tickets.resolved")
                .description("Total tickets resolved")
                .register(meterRegistry);
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
        Ticket saved = ticketRepository.save(ticket);
        if (target == TicketStatus.RESOLVED) {
            ticketsResolved.increment();
        }
        return saved;
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

        Ticket saved = ticketRepository.save(ticket);
        ticketsCreated.increment();
        return saved;
    }
}