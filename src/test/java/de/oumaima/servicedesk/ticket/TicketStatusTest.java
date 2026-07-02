package de.oumaima.servicedesk.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTest {

    @Test
    void newCanAdvanceButCannotBeIllegallyMoved() {
        assertThat(TicketStatus.NEW.canTransitionTo(TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(TicketStatus.NEW.canTransitionTo(TicketStatus.NEW)).isFalse();
    }

    @Test
    void inProgressCanAdvanceButCannotBeMovedToNewNorToSelf() {
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.RESOLVED)).isTrue();
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.NEW)).isFalse();
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.IN_PROGRESS)).isFalse();

    }


    @Test
    void resolvedCanBeMovedToAllStatesButNewOrToSelf() {
        assertThat(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.CLOSED)).isTrue();
        assertThat(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.RESOLVED)).isFalse();
        assertThat(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.NEW)).isFalse();
    }

    @Test
    void closedCannotBemoved() {
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.RESOLVED)).isFalse();
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.IN_PROGRESS)).isFalse();
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.NEW)).isFalse();
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.CLOSED)).isFalse();

    }

}