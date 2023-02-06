package ru.yandex.market.pvz.tms.executor.report.producer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.report.PvzReportType;
import ru.yandex.market.pvz.core.domain.report.Report;
import ru.yandex.market.pvz.core.domain.report.ReportCommandService;
import ru.yandex.market.pvz.core.domain.report.ReportGenerator;
import ru.yandex.market.pvz.core.domain.report.properties.OebsMatchingStatus;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.tms.executor.report.producer.model.PaidOrderYtModel;
import ru.yandex.market.pvz.tms.test.TransactionlessEmbeddedDbTmsTest;
import ru.yandex.market.tpl.report.core.ReportService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.getDefaultTotalPrice;

@TransactionlessEmbeddedDbTmsTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PaidOrdersUploadDataProducerTest {

    private final TestableClock clock;
    private final TestOrderFactory orderFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final ReportCommandService reportCommandService;
    private final ReportGenerator reportGenerator;

    private final PaidOrdersUploadDataProducer uploadDataProducer;

    @MockBean
    private ReportService reportService;

    @Test
    void testUploadDataProducedCorrectly() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint1 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        PickupPoint pickupPoint2 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint1.getTimeOffset());
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);
        clock.setFixed(paymentDate.toInstant(zone), zone);
        Order cardOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("2392932")
                        .paymentType(OrderPaymentType.UNKNOWN)
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .build())
                .pickupPoint(pickupPoint1)
                .build());
        cardOrder = orderFactory.receiveOrder(cardOrder.getId());
        cardOrder = orderFactory.deliverOrderCompletely(
                cardOrder.getId(),
                OrderDeliveryType.PAYMENT,
                OrderPaymentType.CARD
        );

        Order cashOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("2392933")
                        .paymentType(OrderPaymentType.UNKNOWN)
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .build())
                .pickupPoint(pickupPoint2)
                .build());
        cashOrder = orderFactory.receiveOrder(cashOrder.getId());
        cashOrder = orderFactory.deliverOrderCompletely(
                cashOrder.getId(),
                OrderDeliveryType.PAYMENT,
                OrderPaymentType.CASH
        );

        Report report = reportGenerator.generate(
                legalPartner.getId(), uploadDataProducer.getReportType(),
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 15)
        );
        List<PaidOrderYtModel> models = uploadDataProducer.produce(report.getId());

        assertThat(models).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(List.of(
                        PaidOrderYtModel.builder()
                                .partnerName(legalPartner.getOrganization().getFullName())
                                .pickupPointName(pickupPoint1.getName())
                                .externalOrderId(cardOrder.getExternalId())
                                .paymentDate("14.08.2020")
                                .sum(getDefaultTotalPrice().doubleValue())
                                .paymentType(OrderPaymentType.CARD.getDescription())
                                .moneyTransferDate(null)
                                .paymentOrderNumber(null)
                                .oebsMatchingStatus(OebsMatchingStatus.NOT_FOUND.name())
                                .pvzMarketId(pickupPoint1.getPvzMarketId())
                                .build(),

                        PaidOrderYtModel.builder()
                                .partnerName(legalPartner.getOrganization().getFullName())
                                .pickupPointName(pickupPoint2.getName())
                                .externalOrderId(cashOrder.getExternalId())
                                .paymentDate("14.08.2020")
                                .sum(getDefaultTotalPrice().doubleValue())
                                .paymentType(OrderPaymentType.CASH.getDescription())
                                .moneyTransferDate(null)
                                .paymentOrderNumber(null)
                                .oebsMatchingStatus(OebsMatchingStatus.NOT_FOUND.name())
                                .pvzMarketId(pickupPoint2.getPvzMarketId())
                                .build()
                ));
    }

    @Test
    @SneakyThrows
    void checkDataReuploadedIfReportIsMarkedAsPayed() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime paymentDate = OffsetDateTime.of(
                LocalDateTime.of(2020, 8, 14, 23, 30, 0),
                zone);

        clock.setFixed(paymentDate.toInstant(), zone);
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("2392932")
                        .paymentType(OrderPaymentType.UNKNOWN)
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        order = orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrderCompletely(order.getId(), OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        Report report = reportGenerator.generate(
                legalPartner.getId(), PvzReportType.PAID_ORDER,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 14)
        );

        String paymentOrderNumber = "123321";
        LocalDate moneyTransferDate = LocalDate.of(2020, 10, 15);

        PaidOrderYtModel modelBeforePaid = uploadDataProducer.produce(report.getId()).get(0);
        reportCommandService.markPaid(report.getId(), clock.instant(), paymentOrderNumber, moneyTransferDate);
        PaidOrderYtModel modelAfterPaid = uploadDataProducer.produce(report.getId()).get(0);

        assertThat(modelBeforePaid.getMoneyTransferDate()).isNull();
        assertThat(modelBeforePaid.getPaymentOrderNumber()).isNull();

        assertThat(modelAfterPaid.getMoneyTransferDate()).isEqualTo("15.10.2020");
        assertThat(modelAfterPaid.getPaymentOrderNumber()).isEqualTo(paymentOrderNumber);
    }

}
