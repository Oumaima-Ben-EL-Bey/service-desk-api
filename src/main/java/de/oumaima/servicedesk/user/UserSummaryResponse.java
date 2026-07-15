package de.oumaima.servicedesk.user;

import java.util.List;

public record UserSummaryResponse(
        Long id,
        String fullName,
        List<String> roles,
        String team
) {
}
