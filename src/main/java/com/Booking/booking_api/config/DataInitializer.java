package com.Booking.booking_api.config;

import com.Booking.booking_api.entity.User;
import com.Booking.booking_api.enums.Role;
import com.Booking.booking_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.enabled:true}") boolean seedEnabled,
            @Value("${app.seed.admin-username:admin}") String adminUsername,
            @Value("${app.seed.admin-password:Admin@123}") String adminPassword,
            @Value("${app.seed.user-username:user}") String userUsername,
            @Value("${app.seed.user-password:User@123}") String userPassword) {

        return args -> {
            if (!seedEnabled) {
                return;
            }

            createIfMissing(
                    userRepository,
                    passwordEncoder,
                    adminUsername,
                    adminPassword,
                    adminUsername + "@example.com",
                    Role.ADMIN
            );

            createIfMissing(
                    userRepository,
                    passwordEncoder,
                    userUsername,
                    userPassword,
                    userUsername + "@example.com",
                    Role.USER
            );
        };
    }

    private void createIfMissing(
            UserRepository repository,
            PasswordEncoder encoder,
            String username,
            String password,
            String email,
            Role role) {

        if (repository.findByUsername(username).isPresent()) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);

        repository.save(user);
    }
}
