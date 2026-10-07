package banking_system.testsupport;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class ApiFixtures {

    public static final String PASSWORD = "secret123";

    public record Session(String token, String userId, String email) {
        public String bearer() {
            return "Bearer " + token;
        }
    }

    public record AccountRef(String id, String number) {
    }

    private ApiFixtures() {
    }

    public static Session registerNewUser(MockMvc mvc) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@bank.se";
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Session(JsonFields.string(body, "token"), JsonFields.string(body, "userId"), email);
    }

    public static AccountRef createAccount(MockMvc mvc, Session session) throws Exception {
        String body = mvc.perform(post("/api/v1/accounts")
                        .param("ownerId", session.userId())
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new AccountRef(JsonFields.string(body, "id"), JsonFields.string(body, "accountNumber"));
    }

    public static void deposit(MockMvc mvc, Session session, AccountRef account, String amount) throws Exception {
        mvc.perform(post("/api/v1/accounts/{id}/deposit", account.id())
                        .param("amount", amount)
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk());
    }
}