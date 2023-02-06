package ru.yandex.market.logistics.management.service.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.health.jobs.DynamicValidChecker;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
class DynamicValidCheckerTest extends AbstractContextualTest {

    @Autowired
    private DynamicValidChecker service;

    @Test
    void testEmptyDynamicLog() {
        String dynamicValidStatus = service.getDynamicValidStatus();
        softly.assertThat(dynamicValidStatus).as("Should return ok")
            .isEqualTo("0;OK");
    }

    // Несмотря на наличие WARN'ов Juggler не должен загорать лампочки, они будут только в админке
    @Test
    @Sql("/data/controller/health/dynamic_log_ok.sql")
    void testDynamicOk() {
        String dynamicValidStatus = service.getDynamicValidStatus();
        softly.assertThat(dynamicValidStatus).as("Should return ok")
            .isEqualTo("0;OK");
    }

    @Test
    @Sql("/data/controller/health/dynamic_log_warn.sql")
    void testDynamicWarn() {
        String dynamicValidStatus = service.getDynamicValidStatus();
        softly.assertThat(dynamicValidStatus).as("Should return ok")
            .isEqualTo("0;OK");
    }

    @Test
    @Sql("/data/controller/health/dynamic_log_failed.sql")
    void testDynamicFailed() {
        String dynamicValidStatus = service.getDynamicValidStatus();
        softly.assertThat(dynamicValidStatus).as("Should throw error with reasons")
            .startsWith("2;")
            .contains("FAILED_2")
            .contains("FAILED_3")
            .contains("FAILED_4")
            .doesNotContain("WARN")
            .doesNotContain("FAILED_1");
    }
}
