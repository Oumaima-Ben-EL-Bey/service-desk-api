package de.oumaima.servicedesk.ticket;

import java.util.Set;

public enum TicketStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    private Set<TicketStatus> allowedNextStates;

    static {
        NEW.allowedNextStates = Set.of(IN_PROGRESS, RESOLVED, CLOSED);
        IN_PROGRESS.allowedNextStates = Set.of( RESOLVED, CLOSED);
        RESOLVED.allowedNextStates = Set.of(IN_PROGRESS,CLOSED);
        CLOSED.allowedNextStates = Set.of();
    }
    public boolean canTransitionTo(TicketStatus target) {
        return allowedNextStates.contains(target);
    }
}
