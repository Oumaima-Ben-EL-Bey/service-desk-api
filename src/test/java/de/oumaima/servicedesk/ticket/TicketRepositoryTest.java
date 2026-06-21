package de.oumaima.servicedesk.ticket;


import de.oumaima.servicedesk.TestcontainersConfiguration;
import de.oumaima.servicedesk.user.User;
import de.oumaima.servicedesk.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
public class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private Ticket persistTicket(String title, User requester) {
        Ticket t = new Ticket();
        t.setTitle(title);
        t.setDescription("...");
        t.setStatus(TicketStatus.NEW);
        t.setCategory(TicketCategory.NETWORK);
        t.setRequester(requester);
        return ticketRepository.save(t);
    }

    @Test
    void findByRequester_returnsListOfTickets_byRequester() {

        User alice = new User();
        alice.setEmail("alice@example.com");
        alice.setFullName("Alice");
        alice.setPasswordHash("test-hash");
        userRepository.save(alice);

        User bob = new User();
        bob.setEmail("bob@example.com");
        bob.setFullName("Bob");
        bob.setPasswordHash("test-hash");
        userRepository.save(bob);

        persistTicket("Alice's first ticket", alice);
        persistTicket("Alice's second ticket", alice);
        persistTicket("Bob's first ticket", bob);


        List<Ticket> result = ticketRepository.findByRequesterId(alice.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(t -> t.getRequester().getId()).containsOnly(alice.getId());

    }



}
