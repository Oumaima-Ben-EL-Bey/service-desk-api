package de.oumaima.servicedesk.ticket;

import de.oumaima.servicedesk.TestcontainersConfiguration;
import de.oumaima.servicedesk.team.TeamRepository;
import de.oumaima.servicedesk.user.JwtService;
import de.oumaima.servicedesk.user.RoleRepository;
import de.oumaima.servicedesk.user.User;
import de.oumaima.servicedesk.team.Team;
import de.oumaima.servicedesk.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Test
    void requesterListsOnlyTheirOwnTickets() throws Exception {
        User alice = saveUser("alice-list@example.com");
        User bob = saveUser("bob-list@example.com");
        Ticket aliceTicket = saveTicket("Alice ticket", alice, null);
        Ticket bobTicket = saveTicket("Bob ticket", bob, null);

        String token = jwtService.generateToken("alice-list@example.com");

        String body = mockMvc.perform(get("/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TicketResponse[] tickets = objectMapper.readValue(body, TicketResponse[].class);

        assertThat(tickets).extracting(TicketResponse::id)
                .contains(aliceTicket.getId())
                .doesNotContain(bobTicket.getId());
    }

    @Test
    void agentListsOnlyTheirOwnTeamTickets() throws Exception {
        Team teamNet = saveTeam("Network_List");
        Team teamHar = saveTeam("Hardware_List");
        User AgentNet = saveUserWithRole("Network-list@example.com","AGENT",teamNet);
        User AgentHar = saveUserWithRole("Hardware-list@example.com","AGENT",teamHar);
        User requester = saveUser("requester_netTicket@example.com");

        Ticket NetTicket = saveTicket("Network ticket", requester, teamNet);
        Ticket HarTicket = saveTicket("Hardware ticket", requester, teamHar);

        String token = jwtService.generateToken("Network-list@example.com");

        String body = mockMvc.perform(get("/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TicketResponse[] tickets = objectMapper.readValue(body, TicketResponse[].class);

        assertThat(tickets).extracting(TicketResponse::id)
                .contains(NetTicket.getId())
                .doesNotContain(HarTicket.getId());
    }

    @Test
    void adminListsAllTickets() throws Exception {
        Team teamNet = saveTeam("Network_List_ad");
        Team teamHar = saveTeam("Hardware_List_ad");
        User Admin = saveUserWithRole("admin-list@example.com","ADMIN", null);
        User requester = saveUser("requester_allTicket@example.com");

        Ticket NetTicket = saveTicket("Network ticket", requester, teamNet);
        Ticket HarTicket = saveTicket("Hardware ticket", requester, teamHar);

        String token = jwtService.generateToken("admin-list@example.com");

        String body = mockMvc.perform(get("/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TicketResponse[] tickets = objectMapper.readValue(body, TicketResponse[].class);

        assertThat(tickets).extracting(TicketResponse::id)
                .contains(NetTicket.getId())
                .contains(HarTicket.getId());
    }
    @Test
    void changeStatus_legalMove_returns200AndNewStatus() throws Exception {
        saveUserWithRole("status-admin1@example.com", "ADMIN", null);
        User requester = saveUser("status-req1@example.com");
        Ticket ticket = saveTicket("Move me", requester, null);   // saved as NEW

        String token = jwtService.generateToken("status-admin1@example.com");
        ChangeStatusRequest request = new ChangeStatusRequest(TicketStatus.IN_PROGRESS);

        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/tickets/" + ticket.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    }

    @Test
    void changeStatus_illegalMove_returns409() throws Exception {
        saveUserWithRole("status-admin2@example.com", "ADMIN", null);
        User requester = saveUser("status-req2@example.com");
        Ticket ticket = saveTicket("Closed one", requester, null);   // saved as NEW
        ticket.setStatus(TicketStatus.CLOSED);
        ticketRepository.save(ticket);

        String token = jwtService.generateToken("status-admin2@example.com");
        ChangeStatusRequest request = new ChangeStatusRequest(TicketStatus.IN_PROGRESS);

        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void changeStatus_NoTicket_returns404() throws Exception {
        saveUserWithRole("status-admin3@example.com", "ADMIN", null);


        String token = jwtService.generateToken("status-admin3@example.com");
        ChangeStatusRequest request = new ChangeStatusRequest(TicketStatus.IN_PROGRESS);

        mockMvc.perform(patch("/tickets/" + 99999 + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

    }

    @Test
    void requesterCannotChangeStatusOfOwnTicket() throws Exception {
        User requester = saveUser("status-req-forbidden@example.com");
        Ticket ticket = saveTicket("Mine", requester, null);

        String token = jwtService.generateToken("status-req-forbidden@example.com");
        ChangeStatusRequest request = new ChangeStatusRequest(TicketStatus.IN_PROGRESS);

        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
    @Test
    void agentCanChangeStatusInTheirTeam() throws Exception {
        Team teamNetStat = saveTeam("Network_Stat1");
        User requester = saveUser("status-requester1@example.com");
        Ticket ticket = saveTicket("Assigned", requester, teamNetStat);
        User agentNet = saveUserWithRole("agent_Net_team@example.com", "AGENT", teamNetStat);
        String token = jwtService.generateToken("agent_Net_team@example.com");
        ChangeStatusRequest request = new ChangeStatusRequest(TicketStatus.IN_PROGRESS);

        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void agentCannotChangeStatusInOtherTeam() throws Exception {
        Team teamNetStat = saveTeam("Network_Stat2");
        Team teamHarStat = saveTeam("Hardware_Stat");
        User requester = saveUser("status-requester2@example.com");
        Ticket ticket = saveTicket("Assigned", requester, teamNetStat);
        User agentHar = saveUserWithRole("agent_Har_team@example.com", "AGENT", teamHarStat);
        String token = jwtService.generateToken("agent_Har_team@example.com");
        ChangeStatusRequest request = new ChangeStatusRequest(TicketStatus.IN_PROGRESS);

        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    @Test
    void agentCanSeeTicketTheyRequestedInAnotherTeam() throws Exception {
        // agent belongs to team A...
        Team agentTeam = saveTeam("AgentOwnTeam");
        Team otherTeam = saveTeam("SomeOtherTeam");
        User agentRequester = saveUserWithRole("agent-req-other@example.com", "AGENT", agentTeam);
        // ...but the ticket is in team B, and THIS agent is its requester
        Ticket ticket = saveTicket("My own issue", agentRequester, otherTeam);

        String token = jwtService.generateToken("agent-req-other@example.com");
        mockMvc.perform(get("/tickets/" + ticket.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
    @Test
    void agentClaimsTicketInTheirTeam() throws Exception {
        Team team = saveTeam("Claim_Team");
        User agent = saveUserWithRole("claim-agent1@example.com", "AGENT", team);
        User requester = saveUser("claim-req1@example.com");
        Ticket ticket = saveTicket("Claim me", requester, team);

        String token = jwtService.generateToken("claim-agent1@example.com");

        String body = mockMvc.perform(post("/tickets/" + ticket.getId() + "/claim")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TicketResponse claimed = objectMapper.readValue(body, TicketResponse.class);
        assertThat(claimed.assigneeId()).isEqualTo(agent.getId());
    }

    @Test
    void agentCannotClaimTicketInOtherTeam() throws Exception {
        Team team1 = saveTeam("Claim_Team1");
        Team team2 = saveTeam("Claim_Team2");
        User agent = saveUserWithRole("claim-agent11@example.com", "AGENT", team1);
        User requester = saveUser("claim-req2@example.com");
        Ticket ticket = saveTicket("Claim me", requester, team2);

        String token = jwtService.generateToken("claim-agent11@example.com");

        mockMvc.perform(post("/tickets/" + ticket.getId() + "/claim")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

    }

    @Test
    void adminCannotClaim() throws Exception {
        Team team = saveTeam("Claim_Team_ad");
        User agent = saveUserWithRole("claim-admin@example.com", "ADMIN", null);
        User requester = saveUser("claim-req3@example.com");
        Ticket ticket = saveTicket("Claim me", requester, team);

        String token = jwtService.generateToken("claim-admin@example.com");

        mockMvc.perform(post("/tickets/" + ticket.getId() + "/claim")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

    }

    @Test
    void  requesterCannotClaim() throws Exception {
        Team team = saveTeam("Claim_Team_req");
        User requester = saveUser("claim-req4@example.com");
        Ticket ticket = saveTicket("Claim me", requester, team);

        String token = jwtService.generateToken("claim-req4@example.com");

        mockMvc.perform(post("/tickets/" + ticket.getId() + "/claim")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

    }

    @Test
    void  cannotClaimClosedTicket() throws Exception {
        Team team = saveTeam("Claim_Team4");
        User agent = saveUserWithRole("claim-agent4@example.com", "AGENT", team);
        User requester = saveUser("claim-req5@example.com");
        Ticket ticket = saveTicket("Closed one", requester, team);
        ticket.setStatus(TicketStatus.CLOSED);
        ticketRepository.save(ticket);
        String token = jwtService.generateToken("claim-agent4@example.com");

        mockMvc.perform(post("/tickets/" + ticket.getId() + "/claim")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

    }
    @Test
    void adminAssignsAgentToTicket() throws Exception {
        Team team = saveTeam("Assign_Team1");
        saveUserWithRole("assign-admin1@example.com", "ADMIN", null);
        User agent = saveUserWithRole("assign-agent1@example.com", "AGENT", team);
        User requester = saveUser("assign-req1@example.com");
        Ticket ticket = saveTicket("Assign me", requester, team);

        String token = jwtService.generateToken("assign-admin1@example.com");
        AssignRequest request = new AssignRequest(agent.getId());

        String body = mockMvc.perform(patch("/tickets/" + ticket.getId() + "/assignee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TicketResponse assigned = objectMapper.readValue(body, TicketResponse.class);
        assertThat(assigned.assigneeId()).isEqualTo(agent.getId());
    }

    @Test
    void agentCannotAssign() throws Exception {
        Team team = saveTeam("Assign_Team2");
        User agent = saveUserWithRole("assign-agent2@example.com", "AGENT", team);
        User requester = saveUser("assign-req2@example.com");
        Ticket ticket = saveTicket("Assign me", requester, team);

        String token = jwtService.generateToken("assign-agent2@example.com");
        AssignRequest request = new AssignRequest(agent.getId());

        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/assignee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());


    }

    @Test
    void cannotAssignClosedTicket() throws Exception {
        Team team = saveTeam("Assign_Team3");
        saveUserWithRole("assign-admin3@example.com", "ADMIN", null);
        User agent = saveUserWithRole("assign-agent3@example.com", "AGENT", team);
        User requester = saveUser("assign-req3@example.com");
        Ticket ticket = saveTicket("Assign me", requester, team);
        ticket.setStatus(TicketStatus.CLOSED);
        ticketRepository.save(ticket);
        String token = jwtService.generateToken("assign-admin3@example.com");
        AssignRequest request = new AssignRequest(agent.getId());

        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/assignee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

    }

    @Test
    void  assignToNonexistentUser() throws Exception {
        Team team = saveTeam("Assign_Team4");
        saveUserWithRole("assign-admin4@example.com", "ADMIN", null);
        User requester = saveUser("assign-req4@example.com");
        Ticket ticket = saveTicket("Assign me", requester, team);

        String token = jwtService.generateToken("assign-admin4@example.com");
        AssignRequest request = new AssignRequest(99999L);

        mockMvc.perform(patch("/tickets/" + ticket.getId() + "/assignee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

    }

    @Test
    void adminReassignsToAnotherAgent() throws Exception {
        Team team = saveTeam("Assign_Team5");
        saveUserWithRole("assign-admin5@example.com", "ADMIN", null);
        User agent1 = saveUserWithRole("assign-agent51@example.com", "AGENT", team);
        User agent2 = saveUserWithRole("assign-agent52@example.com", "AGENT", team);

        User requester = saveUser("assign-req5@example.com");
        Ticket ticket = saveTicket("Assign me", requester, team);
        ticket.setAssignee(agent1);
        ticketRepository.save(ticket);

        String token = jwtService.generateToken("assign-admin5@example.com");
        AssignRequest request = new AssignRequest(agent2.getId());

        String body = mockMvc.perform(patch("/tickets/" + ticket.getId() + "/assignee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TicketResponse assigned = objectMapper.readValue(body, TicketResponse.class);
        assertThat(assigned.assigneeId()).isEqualTo(agent2.getId());
    }

    @ParameterizedTest
    @EnumSource(TicketCategory.class)
    void ticketRoutesToTeamMatchingItsCategory(TicketCategory category) throws Exception {
        // unique user per run — the category makes the email distinct
        saveUser("route-" + category + "@example.com");
        String token = jwtService.generateToken("route-" + category + "@example.com");

        Team expectedTeam = teamRepository.findByCategory(category).orElseThrow();

        CreateTicketRequest request = new CreateTicketRequest(
                "Issue with " + category, "cannot connect", category);

        mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teamId").value(expectedTeam.getId()));
    }



}
