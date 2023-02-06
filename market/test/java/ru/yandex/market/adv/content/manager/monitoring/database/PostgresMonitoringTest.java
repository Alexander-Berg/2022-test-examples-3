package ru.yandex.market.adv.content.manager.monitoring.database;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.adv.monitoring.postgres.PostgresMonitoring;
import ru.yandex.market.application.monitoring.ComplexMonitoring;

/**
 * Date: 25.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class PostgresMonitoringTest extends AbstractContentManagerTest {

    @Autowired
    private PostgresMonitoring postgresMonitoring;
    @Autowired
    private ComplexMonitoring complexMonitoring;

    @DisplayName("Работа мониторинга Postgres завершилась статусом OK")
    @Test
    void createPostgresMonitoring_postgresAlive_ok() {
        postgresMonitoring.createPostgresMonitoring();

        Assertions.assertThat(complexMonitoring.getResult("PostgreSqlStatus"))
                .hasToString("0;OK");
    }
}
