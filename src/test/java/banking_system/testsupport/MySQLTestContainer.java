package banking_system.testsupport;

import org.testcontainers.mysql.MySQLContainer;

public final class MySQLTestContainer {

    public static final MySQLContainer INSTANCE = new MySQLContainer("mysql:8.0.46");

    static {
        INSTANCE.start();
    }

    private MySQLTestContainer() {
    }
}