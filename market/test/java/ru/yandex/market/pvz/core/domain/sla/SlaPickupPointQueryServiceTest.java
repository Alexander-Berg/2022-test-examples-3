package ru.yandex.market.pvz.core.domain.sla;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.sla.entity.SlaPickupPoint;
import ru.yandex.market.pvz.core.domain.sla.params.SlaPickupPointParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SlaPickupPointQueryServiceTest {

    private static final String MONTH = "01";
    private static final String YEAR = "2021";
    private static final String REPORT_MONTH = "2021-01";

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final SlaPickupPointQueryService slaPickupPointQueryService;
    private final SlaPickupPointRepository slaPickupPointRepository;

    @Test
    void getLegalPartnerPickupPointsSla() {
        var partner = legalPartnerFactory.createLegalPartner();
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(partner)
                .build());
        var pickupPoint2 =
                pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(partner)
                        .build());
        createOrder(pickupPoint);
        createOrder(pickupPoint2);
        createSlaPickupPoint(pickupPoint, REPORT_MONTH);
        createSlaPickupPoint(pickupPoint2, REPORT_MONTH);

        var slas = slaPickupPointQueryService.getSlaForPickupPointsWithOrders(partner.getId(), MONTH, YEAR,
                List.of(pickupPoint.getId(), pickupPoint2.getId()));

        assertThat(StreamEx.of(slas).map(SlaPickupPointParams::getPickupPointId).toList())
                .containsExactlyInAnyOrderElementsOf(List.of(pickupPoint.getId(), pickupPoint2.getId()));
    }

    private void createSlaPickupPoint(PickupPoint pickupPoint, String reportMonth) {
        slaPickupPointRepository.save(SlaPickupPoint.builder()
                .pickupPoint(pickupPoint)
                .reportMonth(reportMonth)
                .build());
    }

    private void createOrder(PickupPoint pickupPoint) {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder
                .builder()
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.forceReceive(order.getId(), LocalDate.of(2021, 1, 2));
    }

}
