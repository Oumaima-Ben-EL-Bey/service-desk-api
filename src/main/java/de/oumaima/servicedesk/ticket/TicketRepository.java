package de.oumaima.servicedesk.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByRequesterId(Long requesterId);
    List<Ticket> findByTeamId(Long teamId);
}
