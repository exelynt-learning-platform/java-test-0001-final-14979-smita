package com.Booking.booking_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "timestamp", LocalDateTime.now().toString(),
                                    "status", 401,
                                    "error", "Unauthorized",
                                    "message", "Authentication is required"
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "timestamp", LocalDateTime.now().toString(),
                                    "status", 403,
                                    "error", "Forbidden",
                                    "message", "You do not have permission to access this resource"
                            ));
                        }))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // USER + ADMIN can only READ resources.
                        .requestMatchers(HttpMethod.GET, "/resources/**")
                        .hasAnyRole("USER", "ADMIN")

                        // ADMIN has full resource CRUD.
                        .requestMatchers(
                                HttpMethod.POST, "/resources/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT, "/resources/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE, "/resources/**")
                        .hasRole("ADMIN")

                        // USER + ADMIN can view reservations.
                        .requestMatchers(
                                HttpMethod.GET, "/reservations/**")
                        .hasAnyRole("USER", "ADMIN")

                        // USER can create; ADMIN can also create.
                        .requestMatchers(
                                HttpMethod.POST, "/reservations/**")
                        .hasAnyRole("USER", "ADMIN")

                        // Only ADMIN has update/delete reservation permissions.
                        .requestMatchers(
                                HttpMethod.PUT, "/reservations/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE, "/reservations/**")
                        .hasRole("ADMIN")

                        // User management is ADMIN-only.
                        .requestMatchers("/users/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
