package ru.yandex.market.delivery.transport_manager.service.health.product;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

class XDocTransportHealthCheckerTest extends AbstractContextualTest {
    @Autowired
    private XDocTransportHealthChecker xDocTransportHealthChecker;

    @Test
    @DatabaseSetup("/repository/health/transportation/xdoc_transport_booking_slot_failed.xml")
    void hasXDocTransportCancelledBecauseOfSlotBookingFailed() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 12, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(xDocTransportHealthChecker.ensureNoXDocTransportationsCancelledBySlotBookingFailed())
            .isEqualTo(
                "2;1 XDOC_TRANSPORT transportations were CANCELLED because of slot booking failed: 11");
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/xdoc_transport_utilization_ok.xml")
    void ensureXDocTransportPlanPalletCountOk() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 12, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(xDocTransportHealthChecker.ensureXDocTransportPlanPalletCountTooSmall())
            .matches(
                "0;OK");
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/xdoc_transport_low_utilization.xml")
    void ensureXDocTransportPlanPalletCountTooSmall() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 12, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(xDocTransportHealthChecker.ensureXDocTransportPlanPalletCountTooSmall())
            .matches(
                "2;Too small XDock car utilization plan: transportationId=11: utilization=0[.,]50 \\(1/2\\)");
    }
}
