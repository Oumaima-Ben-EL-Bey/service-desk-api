package de.oumaima.servicedesk.user;

import java.util.List;

public record MeResponse(
        Long id,
        String fullName,
        List<String> roles
) {
}
