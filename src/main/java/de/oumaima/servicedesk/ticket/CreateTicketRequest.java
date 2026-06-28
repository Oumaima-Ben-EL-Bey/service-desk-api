package de.oumaima.servicedesk.ticket;

public record CreateTicketRequest(
        String title,
        String description,
        TicketCategory category


) {

}
