package ru.yandex.market.fulfillment.stockstorage;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static ru.yandex.market.fulfillment.stockstorage.configuration.DateTimeTestConfig.REAL_JDBC_CLOCK;

public class JdbcClockTest extends AbstractContextualTest {

    @Autowired
    @Qualifier(REAL_JDBC_CLOCK)
    private Clock clock;

    @Test
    public void testJdbcClock() {
        softly.assertThat(clock.millis()).isGreaterThan(0);
        softly.assertThat(clock.instant()).isNotNull();
    }
}
