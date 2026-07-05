package de.oumaima.servicedesk.comment;

import java.time.OffsetDateTime;

public record CommentResponse(
         Long id,
         String body,
         Long ticketId,
         Long authorId,
         OffsetDateTime createdAt

) {
}
