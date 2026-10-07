package banking_system.security.infrastructure.adapter.in.web;

import banking_system.testsupport.ApiFixtures;
import banking_system.testsupport.ApiFixtures.Session;
import banking_system.testsupport.JsonFields;
import banking_system.testsupport.MySQLTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @DynamicPropertySource
    static void registerMySQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySQLTestContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", MySQLTestContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", MySQLTestContainer.INSTANCE::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String newEmail() {
        return "auth-" + UUID.randomUUID() + "@bank.se";
    }

    private String register(String email, int expectedStatus) throws Exception {
        return mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, ApiFixtures.PASSWORD)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("register returns a token, a user id and the registered email")
    void registerReturnsTokenUserIdAndEmail() throws Exception {
        String email = newEmail();

        String body = register(email, 200);

        assertThat(JsonFields.string(body, "email")).isEqualTo(email);
        assertThat(JsonFields.string(body, "token")).isNotBlank();
        assertThat(JsonFields.string(body, "userId")).isNotBlank();
    }

    @Test
    @DisplayName("registering the same email twice is rejected with 400")
    void duplicateEmailIsRejected() throws Exception {
        String email = newEmail();
        register(email, 200);

        String body = register(email, 400);

        assertThat(body).contains("already exists");
    }

    @Test
    @DisplayName("an invalid email and a too short password are rejected with field-level messages")
    void invalidBodyIsRejectedWithFieldMessages() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("not-an-email", "123")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("email").contains("password");
        assertThat(body).contains("\"status\":400");
    }

    @Test
    @DisplayName("a malformed JSON body is a 400, not a 500")
    void malformedJsonIsBadRequest() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("Malformed");
    }

    @Test
    @DisplayName("login with the right credentials returns a token for the same user")
    void loginWithCorrectCredentialsReturnsToken() throws Exception {
        Session session = ApiFixtures.registerNewUser(mvc);

        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(session.email(), ApiFixtures.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonFields.string(body, "token")).isNotBlank();
        assertThat(JsonFields.string(body, "userId")).isEqualTo(session.userId());
    }

    @Test
    @DisplayName("login with a wrong password is a 401")
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        Session session = ApiFixtures.registerNewUser(mvc);

        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(session.email(), "definitely-wrong")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("Email or password incorrect!");
    }

    @Test
    @DisplayName("login for an unknown user is a 401")
    void loginForUnknownUserIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(newEmail(), ApiFixtures.PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a protected endpoint rejects a request without a token")
    void protectedEndpointRejectsMissingToken() throws Exception {
        // The exact status code (401 or 403) depends on Spring Security's default entry point,
        // so we verify what is guaranteed - the request is rejected.
        mvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID().toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("a protected endpoint rejects a garbage token")
    void protectedEndpointRejectsGarbageToken() throws Exception {
        mvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("a token from register lets the request through to the controller")
    void registerTokenGrantsAccess() throws Exception {
        Session session = ApiFixtures.registerNewUser(mvc);

        // 404 (rather than a security related 4xx) proves that authorization succeeded
        // and only the requested resource is missing.
        mvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID().toString())
                        .header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }
}