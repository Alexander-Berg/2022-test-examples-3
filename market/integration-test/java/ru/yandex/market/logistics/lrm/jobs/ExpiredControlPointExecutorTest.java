package ru.yandex.market.logistics.lrm.jobs;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;

@DisplayName("Контроль сроков нахождения на контрольной точке")
class ExpiredControlPointExecutorTest extends AbstractIntegrationTest {
    private static final Instant NOW = Instant.parse("2022-05-25T09:00:00.00Z");

    @Autowired
    ExpiredControlPointExecutor expiredControlPointExecutor;

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/database/jobs/control-point/handle-expired/before/success.xml")
    @ExpectedDatabase(
        value = "/database/jobs/control-point/handle-expired/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
        expiredControlPointExecutor.execute(null);
    }
}
