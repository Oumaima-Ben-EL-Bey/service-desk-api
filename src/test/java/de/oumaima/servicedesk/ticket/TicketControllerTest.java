package de.oumaima.servicedesk.ticket;

import de.oumaima.servicedesk.TestcontainersConfiguration;
import de.oumaima.servicedesk.user.JwtService;
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
public class TicketControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;


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
}
