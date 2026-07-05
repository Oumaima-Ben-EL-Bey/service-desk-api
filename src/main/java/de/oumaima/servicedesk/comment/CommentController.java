package de.oumaima.servicedesk.comment;


import de.oumaima.servicedesk.user.CustomUserDetails;
import de.oumaima.servicedesk.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final CommentRepository  commentRepository;

    public CommentController(CommentService commentService, CommentRepository commentRepository) {
        this.commentService = commentService;
        this.commentRepository = commentRepository;
    }

    @PostMapping
    @PreAuthorize("@ticketSecurity.canAccess(#ticketId, principal)")
    public ResponseEntity<CommentResponse> create(@PathVariable Long ticketId, @RequestBody CreateCommentRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        User author = principal.getUser();
        Comment saved = commentService.create(ticketId, request.body(), author);



        return ResponseEntity.status(HttpStatus.CREATED).body(CommentMapper.toResponse(saved));
    }

    @GetMapping
    @PreAuthorize("@ticketSecurity.canAccess(#ticketId, principal)")
    public List<CommentResponse> list(@PathVariable Long ticketId ) {


        List<Comment> comments = commentRepository.findByTicketId(ticketId);


        return comments.stream().map(CommentMapper::toResponse).toList();
    }
}
