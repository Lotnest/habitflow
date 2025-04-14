package dev.lotnest.habitflow.auth.security;

import dev.lotnest.habitflow.auth.model.User;
import dev.lotnest.habitflow.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    @SneakyThrows
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) {

        String jwt = extractTokenFromCookies(request);

        if (jwt != null && jwtTokenUtil.validateToken(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String userId = jwtTokenUtil.getUserIdFromJWT(jwt);

            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null, null
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("jwt"))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
