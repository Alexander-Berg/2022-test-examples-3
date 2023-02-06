package ru.yandex.market.pvz.core.domain.logs.pickup_point_scan;

import java.time.Instant;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.model.LogPickupPointScan;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.model.LogPickupPointScanDetails;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.model.LogPickupPointScanType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LogPickupPointScanCommandServiceTest {

    private final LogPickupPointScanCommandService logPickupPointScanCommandService;
    private final LogPickupPointScanRepository logPickupPointScanRepository;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestableClock clock;

    @Test
    void checkLog() {
        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneId.systemDefault());
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        LogPickupPointScan expected = LogPickupPointScan.builder()
                .pickupPointId(pickupPoint.getId())
                .logPickupPointScanType(LogPickupPointScanType.VERIFY_ORDER_BARCODE)
                .scannedAt(clock.instant())
                .details(LogPickupPointScanDetails.builder()
                        .orderId(1L)
                        .barcode("12344")
                        .build())
                .build();
        logPickupPointScanCommandService.logOrderScan(
                expected.getPickupPointId(),
                expected.getDetails(),
                expected.getLogPickupPointScanType()
        );
        LogPickupPointScan actual = logPickupPointScanRepository.findAll().get(0);
        assertThat(actual).isEqualToIgnoringGivenFields(expected, "id", "uid");
    }

}
