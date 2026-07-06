package de.oumaima.servicedesk.config;

import de.oumaima.servicedesk.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class CorrelationIdFilterTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void generatesCorrelationIdWhenNoneProvided() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void reusesIncomingCorrelationId() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-Id", "fixed-test-id-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "fixed-test-id-123"));
    }
}
