package dev.lotnest.habitflow.auth.service;

import dev.lotnest.habitflow.auth.exception.AuthException;
import dev.lotnest.habitflow.auth.model.User;
import dev.lotnest.habitflow.auth.payload.LoginRequest;
import dev.lotnest.habitflow.auth.payload.RegisterRequest;
import dev.lotnest.habitflow.auth.repository.UserRepository;
import dev.lotnest.habitflow.auth.security.JwtTokenUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new AuthException("Email already registered");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .build();

        userRepository.save(user);
    }

    public void loginWithCookies(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid credentials");
        }

        String accessToken = jwtTokenUtil.generateAccessToken(user.getId());
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getId());

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        Cookie accessCookie = createCookie(ACCESS_TOKEN_COOKIE, accessToken, jwtTokenUtil.getAccessTokenExpirationSeconds());
        Cookie refreshCookie = createCookie(REFRESH_TOKEN_COOKIE, refreshToken, jwtTokenUtil.getRefreshTokenExpirationSeconds());

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    public void logoutAndClearCookies(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request);

        if (refreshToken != null && jwtTokenUtil.validateToken(refreshToken)) {
            String userId = jwtTokenUtil.getUserIdFromJWT(refreshToken);
            userRepository.findById(userId).ifPresent(user -> {
                user.setRefreshToken(null);
                userRepository.save(user);
            });
        }

        clearCookie(ACCESS_TOKEN_COOKIE, response);
        clearCookie(REFRESH_TOKEN_COOKIE, response);
    }

    public boolean refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request);

        if (refreshToken == null || !jwtTokenUtil.validateToken(refreshToken)) {
            return false;
        }

        String userId = jwtTokenUtil.getUserIdFromJWT(refreshToken);
        User user = userRepository.findById(userId).orElse(null);

        if (user == null || !refreshToken.equals(user.getRefreshToken())) {
            return false;
        }

        String newAccessToken = jwtTokenUtil.generateAccessToken(userId);
        Cookie newAccessCookie = createCookie(ACCESS_TOKEN_COOKIE, newAccessToken, jwtTokenUtil.getAccessTokenExpirationSeconds());

        response.addCookie(newAccessCookie);
        return true;
    }

    private Cookie createCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        return cookie;
    }

    private void clearCookie(String name, HttpServletResponse response) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getCookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> StringUtils.equals(REFRESH_TOKEN_COOKIE, cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
