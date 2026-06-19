package de.oumaima.servicedesk.ticket;

import java.time.OffsetDateTime;

public record TicketResponse(

        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketCategory category,
        Long requesterId,
        Long assigneeId,
        Long teamId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
