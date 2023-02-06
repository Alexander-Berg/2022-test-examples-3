package ru.yandex.market.pvz.internal.domain.sla;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.sla.SlaOrderRepository;
import ru.yandex.market.pvz.core.domain.sla.SlaPickupPointRepository;
import ru.yandex.market.pvz.core.domain.sla.entity.SlaOrder;
import ru.yandex.market.pvz.core.domain.sla.entity.SlaPickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.sla.dto.SlaOrderReportDto;
import ru.yandex.market.pvz.internal.controller.pi.sla.dto.SlaPickupPointInfoDto;
import ru.yandex.market.pvz.internal.controller.pi.sla.dto.SlaPickupPointReportDto;

import static org.assertj.core.api.Assertions.assertThat;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SlaServiceTest {

    private static final String REPORT_MONTH = "2021-10";
    private static final String MONTH = "10";
    private static final String YEAR = "2021";

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final SlaOrderRepository slaOrderRepository;
    private final SlaPickupPointRepository slaPickupPointRepository;

    private final SlaService slaService;

    @Test
    void getLegalPartnerSlaInfo() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());

        var expected = SlaPickupPointInfoDto.builder()
                .id(pickupPoint.getId())
                .pvzMarketId(pickupPoint.getPvzMarketId())
                .name(pickupPoint.getName())
                .active(pickupPoint.getActive())
                .build();

        var legalPartnerSlaInfo = slaService.getLegalPartnerSlaInfo(legalPartner.getId());
        assertThat(legalPartnerSlaInfo.getLegalPartnerCreationTime()).isEqualTo(legalPartner.getCreatedAt());

        var pickupPointSlaInfos = legalPartnerSlaInfo.getApprovedPickupPoints();
        assertThat(pickupPointSlaInfos.size()).isEqualTo(1);
        assertThat(pickupPointSlaInfos.get(0)).isEqualTo(expected);
    }

    @Test
    void getSlaOrdersReport() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder
                .builder()
                .pickupPoint(pickupPoint)
                .build());
        createSlaOrder(order.getExternalId());

        var expected = SlaOrderReportDto.builder()
                .externalId(order.getExternalId())
                .build();

        var slaOrderReportDtos = slaService.getSlaOrdersReport(pickupPoint.getId(), 10, 2021);
        assertThat(slaOrderReportDtos.size()).isEqualTo(1);
        assertThat(slaOrderReportDtos.get(0)).isEqualTo(expected);
    }

    @Test
    void getSlaPickupPointsReport() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        var pickupPoint2 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder
                .builder()
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.forceReceive(order.getId(), LocalDate.of(2021, 10, 2));
        createSlaPickupPoint(pickupPoint, REPORT_MONTH);
        createSlaPickupPoint(pickupPoint2, REPORT_MONTH);

        var expected = SlaPickupPointReportDto.builder()
                .name(pickupPoint.getName())
                .build();

        var slaPickupPointReportDtos = slaService.getSlaPickupPointReport(legalPartner.getId(), MONTH, YEAR);
        assertThat(slaPickupPointReportDtos.size()).isEqualTo(2);
        assertThat(slaPickupPointReportDtos.get(0)).isEqualTo(expected);
        assertThat(slaPickupPointReportDtos.get(1)).isEqualTo(expected);
    }

    private void createSlaPickupPoint(PickupPoint pickupPoint, String reportMonth) {
        slaPickupPointRepository.save(SlaPickupPoint.builder()
                .pickupPoint(pickupPoint)
                .reportMonth(reportMonth)
                .build()
        );
    }

    private void createSlaOrder(String externalId) {
        slaOrderRepository.save(SlaOrder.builder()
                .externalId(externalId)
                .finalStatusDate(LocalDate.of(2021, 10, 19))
                .build());
    }
}
