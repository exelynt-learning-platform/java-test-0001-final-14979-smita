package com.Booking.booking_api;

import com.Booking.booking_api.entity.Resource;
import com.Booking.booking_api.entity.User;
import com.Booking.booking_api.enums.Role;
import com.Booking.booking_api.repository.ResourceRepository;
import com.Booking.booking_api.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private String otherUserToken;
    private Long userId;
    private Long otherUserId;

    @BeforeEach
    void setUp() throws Exception {
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        User admin = createUser("admin", "admin@example.com", "admin123", Role.ADMIN);
        User user = createUser("user", "user@example.com", "user123", Role.USER);
        User otherUser = createUser("other", "other@example.com", "other123", Role.USER);

        userId = user.getId();
        otherUserId = otherUser.getId();

        adminToken = obtainToken("admin", "admin123");
        userToken = obtainToken("user", "user123");
        otherUserToken = obtainToken("other", "other123");
    }

    @Test
    void userCanReadResourcesButCannotModifyThem() throws Exception {
        Resource resource = createResource();

        mockMvc.perform(get("/resources")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Meeting Room"));

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Another Room",
                                  "type": "ROOM",
                                  "description": "Test",
                                  "available": true
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/resources/" + resource.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Changed",
                                  "type": "ROOM",
                                  "description": "Test",
                                  "available": true
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/resources/" + resource.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void userOwnsReservationFromJwtAndCannotAccessAnotherUsersReservation() throws Exception {
        Resource resource = createResource();

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": %d,
                                  "userId": %d,
                                  "startTime": "2026-09-20T10:00:00",
                                  "endTime": "2026-09-20T12:00:00",
                                  "price": 500.00,
                                  "status": "CONFIRMED"
                                }
                                """.formatted(resource.getId(), otherUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(response);
        long reservationId = created.get("id").asLong();

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("user"));

        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        // USER is not allowed to update or delete reservations.
        mockMvc.perform(put("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": %d,
                                  "startTime": "2026-09-20T13:00:00",
                                  "endTime": "2026-09-20T14:00:00",
                                  "price": 700.00
                                }
                                """.formatted(resource.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSeeAllReservationsAndUseFiltersPaginationAndSorting() throws Exception {
        Resource resource = createResource();

        createReservationAsAdmin(resource.getId(), userId, "2026-09-21T10:00:00", "2026-09-21T11:00:00", 300, "CONFIRMED");
        createReservationAsAdmin(resource.getId(), otherUserId, "2026-09-21T12:00:00", "2026-09-21T13:00:00", 800, "PENDING");

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "CONFIRMED")
                        .param("minPrice", "200")
                        .param("maxPrice", "500")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(300.0))
                .andExpect(jsonPath("$.content[0].username").value("user"));
    }

    private User createUser(
            String username,
            String email,
            String password,
            Role role) {

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Resource createResource() {
        Resource resource = new Resource();
        resource.setName("Meeting Room");
        resource.setType("ROOM");
        resource.setDescription("Board room");
        resource.setAvailable(true);
        return resourceRepository.save(resource);
    }

    private void createReservationAsAdmin(
            Long resourceId,
            Long userId,
            String start,
            String end,
            double price,
            String status) throws Exception {

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": %d,
                                  "userId": %d,
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "price": %.2f,
                                  "status": "%s"
                                }
                                """.formatted(resourceId, userId, start, end, price, status)))
                .andExpect(status().isCreated());
    }

    private String obtainToken(String username, String password) throws Exception {
        String loginBody = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
}
