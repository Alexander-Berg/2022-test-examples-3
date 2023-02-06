package ru.yandex.market.delivery.transport_manager.service.health.product;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DatabaseSetup(
    "/repository/movement_courier/movement_courier_sending_status_with_time.xml"
)
class MovementCourierHealthCheckerTest extends AbstractContextualTest {

    @Autowired
    private MovementCourierHealthChecker movementCourierHealthChecker;

    @Test
    public void checkOk() {
        clock.setFixed(Instant.parse("2021-12-01T18:00:00.00Z"), ZoneOffset.UTC);
        var result = movementCourierHealthChecker.checkMovementCouriersNewStatus();
        assertEquals("0;OK", result);
    }

    @Test
    public void checkWarn() {
        clock.setFixed(Instant.parse("2021-12-01T19:01:00.00Z"), ZoneOffset.UTC);
        var result = movementCourierHealthChecker.checkMovementCouriersNewStatus();
        var warnMessage = "1;Courier TMU1 id=4 is not sent for 1 hours";
        assertEquals(warnMessage, result);
    }

    @Test
    public void checkCrit() {
        clock.setFixed(Instant.parse("2021-12-01T20:01:00.00Z"), ZoneOffset.UTC);
        var result = movementCourierHealthChecker.checkMovementCouriersNewStatus();
        var critMessage = "2;Courier TMU1 id=4 is not sent for 2 hours";
        assertEquals(critMessage, result);
    }

    @Test
    public void checkAllCrit() {
        clock.setFixed(Instant.parse("2021-12-02T20:01:00.00Z"), ZoneOffset.UTC);
        var result = movementCourierHealthChecker.checkMovementCouriersNewStatus();
        var critMessage = "2;Courier TMU1 id=4 is not sent for 26 hours, "
            + "Courier TMU2 id=1 is not sent for 25 hours, "
            + "Couriers TMU3 id=2, TMU4 id=3 is not sent for 24 hours";
        assertEquals(critMessage, result);
    }
}
