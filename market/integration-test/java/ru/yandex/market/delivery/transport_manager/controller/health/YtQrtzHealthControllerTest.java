package ru.yandex.market.delivery.transport_manager.controller.health;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitDatabaseConnectionQrtz"
)
class YtQrtzHealthControllerTest extends AbstractContextualTest {

    @Autowired
    YtHealthController ytHealthController;

    @Test
    @DatabaseSetup(value = "/repository/health/yt/qrtz/sync_failed.xml")
    void checkFailed() {
        softly.assertThat(ytHealthController.failedSync())
            .isEqualTo("2;1 errors in last 5 jobs of type getMovementConfiguration");
    }

    @Test
    @DatabaseSetup(value = "/repository/health/yt/qrtz/sync_ok.xml")
    void checkOK() {
        clock.setFixed(Instant.parse("2020-07-07T14:00:00.00Z"), ZoneOffset.UTC);
        softly.assertThat(ytHealthController.failedSync())
            .isEqualTo("0;OK");
    }
}
