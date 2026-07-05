package de.oumaima.servicedesk.comment;

import de.oumaima.servicedesk.ticket.Ticket;
import de.oumaima.servicedesk.ticket.TicketRepository;
import de.oumaima.servicedesk.ticket.TicketStatus;
import de.oumaima.servicedesk.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public Comment create(Long ticketId, String body, User author) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot comment on a closed ticket");
        }

        Comment comment = new Comment();
        comment.setBody(body);
        comment.setTicket(ticket);
        comment.setAuthor(author);
        return commentRepository.save(comment);
    }
}
