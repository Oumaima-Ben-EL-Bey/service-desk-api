package de.oumaima.servicedesk.comment;


import de.oumaima.servicedesk.TestcontainersConfiguration;
import de.oumaima.servicedesk.team.Team;
import de.oumaima.servicedesk.team.TeamRepository;
import de.oumaima.servicedesk.ticket.Ticket;
import de.oumaima.servicedesk.ticket.TicketCategory;
import de.oumaima.servicedesk.ticket.TicketRepository;
import de.oumaima.servicedesk.ticket.TicketStatus;
import de.oumaima.servicedesk.user.JwtService;
import de.oumaima.servicedesk.user.RoleRepository;
import de.oumaima.servicedesk.user.User;
import de.oumaima.servicedesk.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private CommentService commentService;


    private Comment saveComment(Ticket ticket, String body, User author) {


        return commentService.create(ticket.getId(), body, author);

    }
    private User saveUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(email);
        u.setPasswordHash("test-hash");
        return userRepository.save(u);
    }

    private Ticket saveTicket(String title, User requester, Team team) {
        Ticket t = new Ticket();
        t.setTitle(title);
        t.setDescription("desc");
        t.setCategory(TicketCategory.NETWORK);
        t.setStatus(TicketStatus.NEW);
        t.setRequester(requester);
        t.setTeam(team);          // null = unrouted ticket
        return ticketRepository.save(t);
    }
    private Team saveTeam(String name) {
        Team t = new Team();
        t.setName(name);
        return teamRepository.save(t);
    }

    private User saveUserWithRole(String email, String roleName, Team team) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(email);
        u.setPasswordHash("test-hash");
        u.setTeam(team);
        u.addRole(roleRepository.findByName(roleName).orElseThrow());
        return userRepository.save(u);
    }

    @Test
    void requesterCommentsOnOwnTicket_thenThreadContainsIt() throws Exception {
        User requester = saveUser("comment-req1@example.com");
        Ticket ticket = saveTicket("Need help", requester, null);

        String token = jwtService.generateToken("comment-req1@example.com");
        CreateCommentRequest request = new CreateCommentRequest("Any update on this?");

        // POST the comment → 201, body echoes back with the author flattened to an id
        String body = mockMvc.perform(post("/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Any update on this?"))
                .andExpect(jsonPath("$.authorId").value(requester.getId()))
                .andReturn().getResponse().getContentAsString();

        CommentResponse created = objectMapper.readValue(body, CommentResponse.class);

        // GET the thread → the comment is really persisted, not just echoed
        String listBody = mockMvc.perform(get("/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        CommentResponse[] thread = objectMapper.readValue(listBody, CommentResponse[].class);
        assertThat(thread).extracting(CommentResponse::id).contains(created.id());
    }

    @Test
    void agentCommentsOnTicketInTheirTeam() throws Exception {
        Team team = saveTeam("team2-comment");
        User agent = saveUserWithRole("agent2_comment@example.com", "AGENT",team);
        User requester = saveUser("comment-req2@example.com");
        Ticket ticket = saveTicket("Need help", requester, team);

        String token = jwtService.generateToken("agent2_comment@example.com");
        CreateCommentRequest request = new CreateCommentRequest("Agent's comment");
       mockMvc.perform(post("/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Agent's comment"))
                .andExpect(jsonPath("$.authorId").value(agent.getId()));

    }

    @Test
    void requesterCommentsOnTicketNotTheirOwn() throws Exception {
        User requester = saveUser("comment-req3@example.com");
        User requesterOwner = saveUser("owner-req@example.com");
        Ticket ticket = saveTicket("Need help", requesterOwner, null);

        String token = jwtService.generateToken("comment-req3@example.com");
        CreateCommentRequest request = new CreateCommentRequest("misplaced comment");

        mockMvc.perform(post("/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCommentsOnticketInOtherTeam() throws Exception {
        Team teamOther = saveTeam("otherteam-comment");
        Team teamOwn = saveTeam("ownteam-comment");

        User agent = saveUserWithRole("agent4_comment@example.com", "AGENT",teamOwn);
        User requester = saveUser("comment-req4@example.com");
        Ticket ticket = saveTicket("Need help", requester, teamOther);

        String token = jwtService.generateToken("agent4_comment@example.com");
        CreateCommentRequest request = new CreateCommentRequest("misplaced comment");

        mockMvc.perform(post("/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void commentOnClosedTicket() throws Exception{

        User requester = saveUser("comment-req5@example.com");
        Ticket ticket = saveTicket("Need help", requester, null);
        ticket.setStatus(TicketStatus.CLOSED);
        ticketRepository.save(ticket);

        String token = jwtService.generateToken("comment-req5@example.com");
        CreateCommentRequest request = new CreateCommentRequest("comment on closed ticket");

        mockMvc.perform(post("/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }


    @Test
    void commentOnNnExistentTicket() throws Exception{

        User requester = saveUser("comment-req6@example.com");


        String token = jwtService.generateToken("comment-req6@example.com");
        CreateCommentRequest request = new CreateCommentRequest("comment on closed ticket");

        mockMvc.perform(post("/tickets/" + 99999 + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requesterListsCommentsOnOwnTicket() throws Exception {
        User requester = saveUser("comment-req7@example.com");
        Team team = saveTeam("Team6");
        User agent = saveUserWithRole("comment-agent7@example.com", "AGENT", team);
        Ticket ticket = saveTicket("Need help", requester, team);
        Comment comment1 = saveComment(ticket, "first comment",agent);
        Comment comment2 = saveComment(ticket, "second comment",requester);

        String token = jwtService.generateToken("comment-req7@example.com");

        String listBody = mockMvc.perform(get("/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        CommentResponse[] thread = objectMapper.readValue(listBody, CommentResponse[].class);
        assertThat(thread).extracting(CommentResponse::id).contains(comment1.getId());
        assertThat(thread).extracting(CommentResponse::id).contains(comment2.getId());

    }
    @Test
    void requesterListsCommentsOnTicketNotTheirOwn() throws Exception {
        User requester = saveUser("comment-req8@example.com");
        User requesterOwner = saveUser("owner-req8@example.com");
        Ticket ticket = saveTicket("Need help", requesterOwner, null);
        Comment comment = saveComment(ticket, "my comment", requesterOwner);
        String token = jwtService.generateToken("comment-req8@example.com");

        mockMvc.perform(get("/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

    }
}



