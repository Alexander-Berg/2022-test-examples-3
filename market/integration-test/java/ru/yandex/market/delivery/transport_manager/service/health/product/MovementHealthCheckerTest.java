package ru.yandex.market.delivery.transport_manager.service.health.product;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

@DatabaseSetup("/repository/movement/movement_confirmation.xml")
class MovementHealthCheckerTest extends AbstractContextualTest {

    @Autowired
    private MovementHealthChecker healthChecker;

    @Test
    void testOk() {
        clock.setFixed(Instant.parse("2021-07-13T15:00:00.00Z"), ZoneOffset.UTC);
        String result = healthChecker.checkMovementConfirmed();
        softly.assertThat(result).isEqualTo("0;OK");
    }

    @Test
    void testWarn() {
        clock.setFixed(Instant.parse("2021-07-13T19:00:00.00Z"), ZoneOffset.UTC);
        String result = healthChecker.checkMovementConfirmed();
        softly.assertThat(result).isEqualTo("1;Movement TMM3 has not been confirmed for 4 hours");
    }

    @Test
    void testCrit() {
        clock.setFixed(Instant.parse("2021-07-14T12:00:00.00Z"), ZoneOffset.UTC);
        String result = healthChecker.checkMovementConfirmed();
        softly.assertThat(result)
            .isEqualTo("2;Movement TMM3 has not been confirmed for 21 hours, " +
                "Movements TMM4, TMM5 has not been confirmed for 13 hours");
    }
}
