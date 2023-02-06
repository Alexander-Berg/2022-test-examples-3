package ru.yandex.market.logistics.management.service.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.health.jobs.MarketIdUpdateChecker;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
class MarketIdUpdateCheckerTest extends AbstractContextualTest {

    @Autowired
    private MarketIdUpdateChecker marketIdUpdateChecker;

    @Test
    void testEmptyTable() {
        String status = marketIdUpdateChecker.getMarketIdUpdated();
        softly.assertThat(status).as("Should return ok")
            .isEqualTo("0;OK");
    }

    @Test
    @Sql("/data/controller/health/market_id_updated_ok.sql")
    void testOk() {
        String status = marketIdUpdateChecker.getMarketIdUpdated();
        softly.assertThat(status).as("Should return ok")
            .isEqualTo("0;OK");
    }

    @Test
    @Sql("/data/controller/health/market_id_updated_fail.sql")
    void testFailed() {
        String status = marketIdUpdateChecker.getMarketIdUpdated();
        softly.assertThat(status).as("Should fail")
            .matches("2;Cannot update market_id for partners with id=(\\[1, 3\\]|\\[3, 1\\])");
    }
}
