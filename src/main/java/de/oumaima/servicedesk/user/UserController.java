package de.oumaima.servicedesk.user;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();
        return new MeResponse(user.getId(), user.getFullName(), roles);
    }
}
