package de.oumaima.servicedesk.ticket;


import de.oumaima.servicedesk.user.User;

public class TicketMapper {

    public static TicketResponse toResponse(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getCategory(),
                t.getRequester().getId(),
                t.getAssignee() == null ? null : t.getAssignee().getId(),
                t.getTeam() == null ? null : t.getTeam().getId(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    public static Ticket toEntity(CreateTicketRequest req, User requester) {
        Ticket ticket = new Ticket();

        ticket.setTitle(req.title());
        ticket.setDescription(req.description());
        ticket.setCategory(req.category());
        ticket.setRequester(requester);
        ticket.setStatus(TicketStatus.NEW);

        return ticket;
    }
}
