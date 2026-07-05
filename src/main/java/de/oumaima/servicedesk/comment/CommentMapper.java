package de.oumaima.servicedesk.comment;

public class CommentMapper {

    public static CommentResponse toResponse(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getBody(),
                c.getTicket().getId(),
                c.getAuthor().getId(),
                c.getCreatedAt()
        );
    }
}
