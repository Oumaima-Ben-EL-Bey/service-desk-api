package de.oumaima.servicedesk.user;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public CustomUserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmailWithRoles(username).orElseThrow(()-> new UsernameNotFoundException("No user with email: " + username));
    return new CustomUserDetails(user);
    }


}
