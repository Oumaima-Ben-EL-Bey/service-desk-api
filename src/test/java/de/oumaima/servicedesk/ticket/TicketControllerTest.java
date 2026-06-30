package de.oumaima.servicedesk.ticket;

import de.oumaima.servicedesk.TestcontainersConfiguration;
import de.oumaima.servicedesk.team.TeamRepository;
import de.oumaima.servicedesk.user.JwtService;
import de.oumaima.servicedesk.user.RoleRepository;
import de.oumaima.servicedesk.user.User;
import de.oumaima.servicedesk.team.Team;
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
public class TicketControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TeamRepository teamRepository;

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
    void postThenGet_returnsTheCreatedTicket() throws Exception {
        User requester = new User();
        requester.setEmail("alice@example.com");
        requester.setFullName("Alice");
        requester.setPasswordHash("test-hash");
        requester = userRepository.save(requester);

        String token = jwtService.generateToken("alice@example.com");

        CreateTicketRequest request = new CreateTicketRequest(
                "VPN not connecting",
                "Cannot reach the VPN from home",
                TicketCategory.NETWORK);

        String body = mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("VPN not connecting"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();

        TicketResponse created = objectMapper.readValue(body, TicketResponse.class);
        assertThat(created.requesterId()).isEqualTo(requester.getId());

        mockMvc.perform(get("/tickets/" + created.id())
                            .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("VPN not connecting"))
                .andExpect(jsonPath("$.status").value("NEW"));

    }

    @Test
    void requesterCannotGetAnotherUsersTicket() throws Exception {

        User owner = saveUser("owner@example.com");
        saveUser("intruder@example.com");

        Ticket ticket = saveTicket("VPN issue", owner, null);
        String intruderTocken = jwtService.generateToken("intruder@example.com");

        mockMvc.perform(get("/tickets/" + ticket.getId())
                .header("Authorization", "Bearer " + intruderTocken))
                .andExpect(status().isForbidden());


    }

    @Test
    void agentCanGetTicketInTheirTeam() throws Exception {
        Team network = saveTeam("Network1");
        User agent = saveUserWithRole("agent-net@example.com", "AGENT", network);
        User requester = saveUser("req-net@example.com");
        Ticket ticket = saveTicket("VPN issue", requester, network);

        String token = jwtService.generateToken("agent-net@example.com");

        mockMvc.perform(get("/tickets/" + ticket.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void agentForbiddenFromTicketInOtherTeams() throws Exception {
        Team network = saveTeam("Network");
        User agent = saveUserWithRole("agent-net1@example.com", "AGENT", network);
        User requester = saveUser("req-h@example.com");
        Team otherNetwork = saveTeam("Hardware");
        Ticket ticket = saveTicket("Cable issue", requester, otherNetwork);

        String token = jwtService.generateToken("agent-net1@example.com");

        mockMvc.perform(get("/tickets/" + ticket.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden() );
    }

    @Test
    void adminCanGetAnyTicket() throws Exception {
        User admin = saveUserWithRole("admin@example.com", "ADMIN", null);
        User requester = saveUser("req-admin@example.com");
        Ticket ticket = saveTicket("Anything", requester, null);

        String token = jwtService.generateToken("admin@example.com");

        mockMvc.perform(get("/tickets/" + ticket.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void userRequestsNonexistentTicket() throws Exception {
        User requester = saveUser("requester_noticket@example.com");
        String token = jwtService.generateToken("requester_noticket@example.com");

        mockMvc.perform(get("/tickets/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

}
