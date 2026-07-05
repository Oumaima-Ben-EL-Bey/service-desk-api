package de.oumaima.servicedesk.team;

import de.oumaima.servicedesk.ticket.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team,Long> {

    Optional<Team> findByCategory(TicketCategory category);
}
