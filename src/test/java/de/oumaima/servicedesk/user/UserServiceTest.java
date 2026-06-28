package de.oumaima.servicedesk.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;
    @Mock
    private RoleRepository roleRepository;

    @Test
    void register_hashesPassword_andSavesUser() {
        // Arrange
        RegisterRequest request = new RegisterRequest("jane@example.com", "Jane Doe", "secret123");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-pw");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName("REQUESTER")).thenReturn(Optional.of(new Role()));
        // Act
        User result = userService.register(request);

        // Assert
        assertThat(result.getPasswordHash()).isEqualTo("hashed-pw");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void register_throwsWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("jane@example.com", "Jane Doe", "secret123");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.register(request)) .isInstanceOf(ResponseStatusException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_assignsRequesterRole() {
        RegisterRequest request = new RegisterRequest("jane@example.com", "Jane Doe", "secret123");
        Role requesterRole = new Role();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("REQUESTER")).thenReturn(Optional.of(requesterRole));
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-pw");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertThat(result.getRoles()).contains(requesterRole);
    }
}
