package de.oumaima.servicedesk.user;

public record UserResponse(
        Long id,
        String email,
        String fullName
) {
}
