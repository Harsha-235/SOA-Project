package com.archivalia.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.archivalia.auth.dto.LoginRequest;
import com.archivalia.auth.dto.LoginResponse;
import com.archivalia.auth.entity.User;
import com.archivalia.auth.repository.UserRepository;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private AuthService authService;

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void login_shouldWorkForValidUser() {

        User user = User.builder()
                .userId(24L)
                .name("Prasanna Savithru")
                .email("2400030070cse1@gmail.com")
                .password(passwordEncoder.encode("password123"))
                .mobileNumber("9133754566")
                .emailVerified(true)
                .mobileVerified(true)
                .role("USER")
                .accountStatus("ACTIVE")
                .nameChangeCount(2)
                .build();

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
        .thenReturn("test-jwt-token");

        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("password123");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("test-jwt-token");
        assertThat(response.getUserId()).isEqualTo(24L);
        assertThat(response.getRole()).isEqualTo("USER");

        verify(userRepository).findByEmail(user.getEmail());
    }

    @Test
    void login_shouldRejectWrongPassword() {

        User user = User.builder()
                .userId(24L)
                .email("2400030070cse1@gmail.com")
                .password(passwordEncoder.encode("correctPassword"))
                .mobileNumber("9133754566")
                .emailVerified(true)
                .mobileVerified(true)
                .role("USER")
                .accountStatus("ACTIVE")
                .nameChangeCount(2)
                .build();

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("wrongPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_shouldRejectSuspendedUser() {

        User user = User.builder()
                .userId(24L)
                .email("2400030070cse1@gmail.com")
                .password(passwordEncoder.encode("password123"))
                .mobileNumber("9133754566")
                .emailVerified(true)
                .mobileVerified(true)
                .role("USER")
                .accountStatus("SUSPENDED")
                .nameChangeCount(2)
                .build();

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account is not active");    }
}