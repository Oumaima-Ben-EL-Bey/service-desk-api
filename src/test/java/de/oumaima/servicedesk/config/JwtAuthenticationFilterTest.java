package de.oumaima.servicedesk.config;

import de.oumaima.servicedesk.TestcontainersConfiguration;
import de.oumaima.servicedesk.user.JwtService;
import de.oumaima.servicedesk.user.User;
import de.oumaima.servicedesk.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class JwtAuthenticationFilterTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    @Test
    void validToken_passesSecurity() throws Exception {
        User user = new User();
        user.setEmail("filter-test@example.com");
        user.setFullName("Filter Test");
        user.setPasswordHash("irrelevant-for-token-auth");
        userRepository.save(user);

        String token = jwtService.generateToken("filter-test@example.com");

        mockMvc.perform(get("/tickets/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void noToken_returns401() throws Exception {
        User user = new User();
        user.setEmail("filter-test1@example.com");
        user.setFullName("Filter Test");
        user.setPasswordHash("irrelevant-for-token-auth");
        userRepository.save(user);

        String token = jwtService.generateToken("filter-test@example.com");

        mockMvc.perform(get("/tickets/999999")
                        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void badToken_returns401() throws Exception {
        User user = new User();
        user.setEmail("filter-test2@example.com");
        user.setFullName("Filter Test");
        user.setPasswordHash("irrelevant-for-token-auth");
        userRepository.save(user);

        String token = jwtService.generateToken("filter-test@example.com");

        mockMvc.perform(get("/tickets/999999")
                        .header("Authorization", "Bearer garbage.not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }
}
