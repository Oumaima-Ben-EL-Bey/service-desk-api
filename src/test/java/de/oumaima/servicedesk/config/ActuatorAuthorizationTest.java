package de.oumaima.servicedesk.config;

import de.oumaima.servicedesk.TestcontainersConfiguration;
import de.oumaima.servicedesk.user.*;
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
public class ActuatorAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private JwtService jwtService;

    private String tokenForUserWithRole(String email, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User();
        user.setEmail(email);
        user.setFullName("Test User");
        user.setPasswordHash("irrelevant-for-token-auth");
        user.addRole(role);
        userRepository.save(user);
        return jwtService.generateToken(email);
    }

    @Test
    void admin_passesActuatorRule() throws Exception {
        String token = tokenForUserWithRole("actuator-admin@example.com", "ADMIN");

        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
    @Test
    void requester_isForbiddenFromActuator() throws Exception {
        String token = tokenForUserWithRole("actuator-requester@example.com", "REQUESTER");

        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
