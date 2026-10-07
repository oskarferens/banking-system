package banking_system.account.infrastructure.adapter.in.web;

import banking_system.testsupport.ApiFixtures;
import banking_system.testsupport.ApiFixtures.AccountRef;
import banking_system.testsupport.ApiFixtures.Session;
import banking_system.testsupport.JsonFields;
import banking_system.testsupport.MySQLTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountApiTest {

    @DynamicPropertySource
    static void registerMySQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySQLTestContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", MySQLTestContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", MySQLTestContainer.INSTANCE::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    private Session session;

    @BeforeEach
    void setUp() throws Exception {
        session = ApiFixtures.registerNewUser(mvc);
    }

    private String fetchAccount(AccountRef account) throws Exception {
        return mvc.perform(get("/api/v1/accounts/{id}", account.id())
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("creating an account returns an empty SEK account with the -500 SEK overdraft limit")
    void createAccountReturnsEmptySekAccount() throws Exception {
        String body = mvc.perform(post("/api/v1/accounts")
                        .param("ownerId", session.userId())
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonFields.string(body, "accountNumber")).matches("^SE\\d{22}$");
        assertThat(JsonFields.string(body, "ownerId")).isEqualTo(session.userId());
        assertThat(JsonFields.string(body, "currency")).isEqualTo("SEK");
        assertThat(JsonFields.decimal(body, "balance")).isEqualByComparingTo("0.00");
        assertThat(JsonFields.decimal(body, "overdraftLimit")).isEqualByComparingTo("-500.00");
    }

    @Test
    @DisplayName("an account can be fetched again by its id")
    void getAccountReturnsStoredAccount() throws Exception {
        AccountRef account = ApiFixtures.createAccount(mvc, session);

        String body = fetchAccount(account);

        assertThat(JsonFields.string(body, "id")).isEqualTo(account.id());
        assertThat(JsonFields.string(body, "accountNumber")).isEqualTo(account.number());
    }

    @Test
    @DisplayName("fetching an unknown account is a 404")
    void getUnknownAccountIsNotFound() throws Exception {
        String body = mvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID().toString())
                        .header("Authorization", session.bearer()))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("Account not found");
    }

    @Test
    @DisplayName("a deposit raises the balance and the new balance is persisted")
    void depositIncreasesAndPersistsBalance() throws Exception {
        AccountRef account = ApiFixtures.createAccount(mvc, session);

        String body = mvc.perform(post("/api/v1/accounts/{id}/deposit", account.id())
                        .param("amount", "1000")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonFields.decimal(body, "balance")).isEqualByComparingTo("1000.00");
        assertThat(JsonFields.decimal(fetchAccount(account), "balance")).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("a withdrawal lowers the balance")
    void withdrawDecreasesBalance() throws Exception {
        AccountRef account = ApiFixtures.createAccount(mvc, session);
        ApiFixtures.deposit(mvc, session, account, "500");

        String body = mvc.perform(post("/api/v1/accounts/{id}/withdraw", account.id())
                        .param("amount", "200")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonFields.decimal(body, "balance")).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("a withdrawal exactly down to the overdraft limit is allowed")
    void withdrawExactlyToOverdraftLimitIsAllowed() throws Exception {
        AccountRef account = ApiFixtures.createAccount(mvc, session);

        String body = mvc.perform(post("/api/v1/accounts/{id}/withdraw", account.id())
                        .param("amount", "500")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonFields.decimal(body, "balance")).isEqualByComparingTo("-500.00");
    }

    @Test
    @DisplayName("a withdrawal beyond the overdraft limit is a 400 and leaves the balance untouched")
    void withdrawBeyondOverdraftIsBadRequest() throws Exception {
        AccountRef account = ApiFixtures.createAccount(mvc, session);

        String body = mvc.perform(post("/api/v1/accounts/{id}/withdraw", account.id())
                        .param("amount", "500.01")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("overdraft");
        assertThat(JsonFields.decimal(fetchAccount(account), "balance")).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a deposit of zero is a 400")
    void depositOfZeroIsBadRequest() throws Exception {
        AccountRef account = ApiFixtures.createAccount(mvc, session);

        String body = mvc.perform(post("/api/v1/accounts/{id}/deposit", account.id())
                        .param("amount", "0")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("positive");
    }

    @Test
    @DisplayName("a missing required parameter is a 400, not a 500")
    void missingParameterIsBadRequest() throws Exception {
        String body = mvc.perform(post("/api/v1/accounts")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("ownerId");
    }

    @Test
    @DisplayName("a non-numeric amount is a 400, not a 500")
    void nonNumericAmountIsBadRequest() throws Exception {
        AccountRef account = ApiFixtures.createAccount(mvc, session);

        String body = mvc.perform(post("/api/v1/accounts/{id}/deposit", account.id())
                        .param("amount", "abc")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("amount");
    }

    @Test
    @DisplayName("an unknown path is a 404, not a 500")
    void unknownPathIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/no-such-endpoint")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an unsupported HTTP method is a 405, not a 500")
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
        AccountRef account = ApiFixtures.createAccount(mvc, session);

        mvc.perform(delete("/api/v1/accounts/{id}", account.id())
                        .header("Authorization", session.bearer()))
                .andExpect(status().isMethodNotAllowed());
    }
}