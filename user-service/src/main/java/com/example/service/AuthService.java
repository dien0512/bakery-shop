package com.example.service;

import com.example.dto.event.NotificationEvent;
import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.AuthResponse;
import com.example.dto.response.UserResponse;
import com.example.entity.User;
import com.example.exception.BadRequestException;
import com.example.exception.ConflictException;
import com.example.exception.UnauthorizedException;
import com.example.mapper.UserMapper;
import com.example.repository.UserRepository;
import com.example.security.JwtService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject UserRepository userRepository;
    @Inject JwtService jwtService;
    @Inject UserMapper userMapper;
    @Inject NotificationProducerService notificationProducer; // ← CHỈ GIỮ CÁI NÀY

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Uniqueness checks
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt(12)))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();

        userRepository.persist(user);
        userRepository.flush();

        LOG.infof("New user registered: %s (%s)", user.getUsername(), user.getId());

        // Publish Kafka → notification-service gửi email chào mừng
        NotificationEvent event = NotificationEvent.builder()
                .type("USER_REGISTERED")
                .userId(user.getId())
                .recipientEmail(user.getEmail())
                .recipientPhone(user.getPhone())
                .channels(List.of("EMAIL"))
                .data(Map.of(
                        "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
                        "username", user.getUsername()
                ))
                .build();
        notificationProducer.sendUserRegistered(event);

        return buildAuthResponse(user);
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.isDeleted()) {
            throw new UnauthorizedException("Account has been deleted");
        }
        if (user.isBanned()) {
            throw new UnauthorizedException("Account has been banned. Please contact support");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive. Please contact support");
        }
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.persist(user);
        userRepository.flush();

        LOG.infof("User logged in: %s", user.getUsername());
        return buildAuthResponse(user);
    }

    // ─── Refresh token ────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(JsonWebToken refreshJwt) {
        if (!jwtService.isRefreshToken(refreshJwt)) {
            throw new BadRequestException("Token provided is not a refresh token");
        }

        String userId = refreshJwt.getSubject();
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found or inactive"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is not active");
        }

        return buildAuthResponse(user);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        UserResponse userResponse = userMapper.toResponse(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenLifespan())
                .user(userResponse)
                .build();
    }
}