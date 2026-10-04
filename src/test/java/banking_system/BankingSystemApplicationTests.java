package banking_system;

import banking_system.testsupport.MySQLTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class BankingSystemApplicationTests {

	@DynamicPropertySource
	static void registerMySQLProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MySQLTestContainer.INSTANCE::getJdbcUrl);
		registry.add("spring.datasource.username", MySQLTestContainer.INSTANCE::getUsername);
		registry.add("spring.datasource.password", MySQLTestContainer.INSTANCE::getPassword);
	}

	@Test
	void contextLoads() {
	}

}