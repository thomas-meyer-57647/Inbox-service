package de.innologic.inboxservice.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class FlywayIndexIT {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.4")
        .withDatabaseName("inbox")
        .withUsername("root")
        .withPassword("");

    @Test
    void flywayCreatesAttachmentRefCompositeIndex() throws Exception {
        Flyway flyway = Flyway.configure()
            .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
            .load();
        flyway.migrate();

        try (Connection connection = MARIADB.createConnection("");
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'inbox_attachment_ref' AND INDEX_NAME = 'idx_attachment_company_message'")) {
            statement.setString(1, MARIADB.getDatabaseName());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isGreaterThan(0);
            }
        }
    }
}
