package ru.yandex.market.pvz.core.domain.report.payload.manager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerRepository;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.report.PvzReportType;
import ru.yandex.market.pvz.core.domain.report.Report;
import ru.yandex.market.pvz.core.domain.report.ReportCommandService;
import ru.yandex.market.pvz.core.domain.report.ReportGenerator;
import ru.yandex.market.pvz.core.domain.report.ReportQueryService;
import ru.yandex.market.pvz.core.domain.report.payload.model.PaidOrderReportPayload;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PaidOrderReportPayloadManagerTest {

    private final TestableClock clock;
    private final TransactionTemplate transactionTemplate;

    private final TestOrderFactory orderFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final ReportCommandService reportCommandService;
    private final ReportGenerator reportGenerator;
    private final ReportQueryService reportQueryService;

    private final LegalPartnerRepository legalPartnerRepository;
    private final OrderRepository orderRepository;

    @SpyBean
    private PaidOrderReportPayloadManager payloadManager;

    @Test
    @Transactional
    void createReportForPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);

        String cardOrderExternalId = "2392932";
        String cashOrderExternalId = "2392933";
        createAndDeliverOrder(pickupPoint, cardOrderExternalId, OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);
        createAndDeliverOrder(pickupPoint, cashOrderExternalId, OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CASH);

        Order cardOrder = orderRepository.findByExternalIdAndPickupPointIdOrThrow(cardOrderExternalId,
                pickupPoint.getId());
        Order cashOrder = orderRepository.findByExternalIdAndPickupPointIdOrThrow(cashOrderExternalId,
                pickupPoint.getId());

        var sourceDataO = payloadManager.getSourceData(
                pickupPoint,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 15));
        assertThat(sourceDataO).isNotEmpty();

        PaidOrderReportPayload expected = new PaidOrderReportPayload(
                "Отчет об оплаченных заказах 09.08.2020 - 15.08.2020",
                "Отчет об оплаченных заказах 09.08.2020 - 15.08.2020.xlsx",
                PaidOrderReportPayload.Parameters.builder()
                        .totalSum(cardOrder.getBoughtTotalPrice().add(cardOrder.getBoughtTotalPrice()))
                        .build(),
                List.of(
                        new PaidOrderReportPayload.Item(
                                1,
                                pickupPoint.getLegalPartner().getOrganization().getFullName(),
                                pickupPoint.getName(),
                                pickupPoint.getPvzMarketId(),
                                cardOrder.getExternalId(),
                                clock.instant().atOffset(zone),
                                cardOrder.getBoughtTotalPrice(),
                                OrderPaymentType.CARD,
                                null,
                                null
                        ),
                        new PaidOrderReportPayload.Item(
                                2,
                                pickupPoint.getLegalPartner().getOrganization().getFullName(),
                                pickupPoint.getName(),
                                pickupPoint.getPvzMarketId(),
                                cashOrder.getExternalId(),
                                clock.instant().atOffset(zone),
                                cashOrder.getBoughtTotalPrice(),
                                OrderPaymentType.CASH,
                                null,
                                null
                        )
                )
        );

        var payload = payloadManager.create(sourceDataO.get());
        assertThat(payload).isEqualTo(expected);
    }

    @Test
    @Transactional
    void createReportForPickupPointWithPartialDeliveredOrder() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);
        clock.setFixed(paymentDate.toInstant(zone), zone);

        Order cardOrder = orderFactory.createSimpleFashionOrder(false, pickupPoint);
        orderFactory.receiveOrder(cardOrder.getId());
        orderFactory.partialDeliver(cardOrder.getId(), List.of(UIT_2_1));
        orderFactory.commitPartialDelivery(cardOrder.getId());
        orderFactory.commitDelivery(cardOrder.getId());

        var sourceDataO = payloadManager.getSourceData(
                pickupPoint,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 15));
        assertThat(sourceDataO).isNotEmpty();

        cardOrder = orderRepository.findByIdOrThrow(cardOrder.getId());
        PaidOrderReportPayload expected = new PaidOrderReportPayload(
                "Отчет об оплаченных заказах 09.08.2020 - 15.08.2020",
                "Отчет об оплаченных заказах 09.08.2020 - 15.08.2020.xlsx",
                PaidOrderReportPayload.Parameters.builder()
                        .totalSum(cardOrder.getBoughtTotalPrice())
                        .build(),
                List.of(
                        new PaidOrderReportPayload.Item(
                                1,
                                pickupPoint.getLegalPartner().getOrganization().getFullName(),
                                pickupPoint.getName(),
                                pickupPoint.getPvzMarketId(),
                                cardOrder.getExternalId(),
                                clock.instant().atOffset(zone),
                                cardOrder.getBoughtTotalPrice(),
                                OrderPaymentType.CARD,
                                null,
                                null
                        )
                )
        );

        var payload = payloadManager.create(sourceDataO.get());
        assertThat(payload).isEqualTo(expected);
    }

    @Test
    void notIncludePrepaidOrderForPickupPoint() {
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        createAndDeliverOrder(pickupPoint, "2392932", OrderPaymentType.PREPAID,
                OrderPaymentStatus.PAID, paymentDate, OrderDeliveryType.UNKNOWN, null);

        var sourceDataO = payloadManager.getSourceData(
                pickupPoint,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 15));
        assertThat(sourceDataO).isEmpty();
    }

    @Test
    void includeOutOfDateOrderForPickupPoint() {
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        createAndDeliverOrder(pickupPoint, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        transactionTemplate.execute(ts -> {
            var sourceDataO = payloadManager.getSourceData(
                    pickupPoint,
                    LocalDate.of(2020, 8, 15),
                    LocalDate.of(2020, 8, 21));
            assertThat(sourceDataO).isNotEmpty();
            var payload = payloadManager.create(sourceDataO.get());
            assertThat(payload.getDetailRows()).hasSize(1);
            return null;
        });
    }

    @Test
    void notIncludeOrderWithDifferentPickupPoint() {
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        createAndDeliverOrder(pickupPoint, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        PickupPoint anotherPickupPoint = pickupPointFactory.createPickupPoint();

        var sourceDataO = payloadManager.getSourceData(
                anotherPickupPoint,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 15));
        assertThat(sourceDataO).isEmpty();
    }

    @Test
    void notIncludeNextDayOrderForPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 15, 0, 30, 0);

        createAndDeliverOrder(pickupPoint, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        var sourceDataO = payloadManager.getSourceData(
                pickupPoint,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 14));
        assertThat(sourceDataO).isEmpty();
    }

    @Transactional
    @Test
    void includeThisDayOrderForPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 23, 30, 0);

        createAndDeliverOrder(pickupPoint, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        var sourceDataO = payloadManager.getSourceData(
                pickupPoint,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 14));
        assertThat(sourceDataO).isNotEmpty();

        var payload = payloadManager.create(sourceDataO.get());
        assertThat(payload.getDetailRows()).hasSize(1);
    }

    @Transactional
    @Test
    void includeFirstDayOrderForPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 9, 0, 30, 0);

        createAndDeliverOrder(pickupPoint, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        var sourceDataO = payloadManager.getSourceData(
                pickupPoint,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 14));
        assertThat(sourceDataO).isNotEmpty();

        var payload = payloadManager.create(sourceDataO.get());
        assertThat(payload.getDetailRows()).hasSize(1);
    }

    @Test
    @SneakyThrows
    void includeMoneyTransferDateAndPaymentOrderNumberIfMarkedAsPayedForPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 23, 30, 0);

        createAndDeliverOrder(pickupPoint, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        ArgumentCaptor<PaidOrderReportPayload> payloadCaptor = ArgumentCaptor.forClass(PaidOrderReportPayload.class);
        doReturn(new byte[0]).when(payloadManager).getBinaryContent(payloadCaptor.capture());

        Report report = reportGenerator.generate(
                pickupPoint, PvzReportType.PAID_ORDER,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 14)
        );

        String paymentOrderNumber = "123321";
        LocalDate moneyTransferDate = LocalDate.of(2020, 10, 15);

        transactionTemplate.execute(ts -> {
            var reportViewBeforePayed = reportQueryService.getView(report.getId());
            assertThat(reportViewBeforePayed.getLastPaymentDate()).isNull();
            assertThat(reportViewBeforePayed.getPaymentOrderNumbers()).isNotEmpty();
            assertThat(reportViewBeforePayed.getPaymentOrderNumbers().get(0)).isNull();
            return null;
        });

        reportCommandService.markPaid(report.getId(), clock.instant(), paymentOrderNumber, moneyTransferDate);
        assertThat(payloadCaptor.getAllValues()).hasSize(2);

        transactionTemplate.execute(ts -> {
            var reportViewAfterPayed = reportQueryService.getView(report.getId());
            assertThat(reportViewAfterPayed.getLastPaymentDate()).isEqualTo(moneyTransferDate);
            assertThat(reportViewAfterPayed.getPaymentOrderNumbers()).isNotEmpty();
            assertThat(reportViewAfterPayed.getPaymentOrderNumbers().get(0)).isEqualTo(paymentOrderNumber);
            return null;
        });
    }

    @Transactional
    @Test
    void createReportForLegalPartner() {
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

        String cardOrderExternalId = "2392932";
        String cashOrderExternalId = "2392933";
        createAndDeliverOrder(pickupPoint1, cardOrderExternalId, OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);
        createAndDeliverOrder(pickupPoint2, cashOrderExternalId, OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CASH);

        Order cardOrder = orderRepository.findByExternalIdAndPickupPointIdOrThrow(cardOrderExternalId,
                pickupPoint1.getId());
        Order cashOrder = orderRepository.findByExternalIdAndPickupPointIdOrThrow(cashOrderExternalId,
                pickupPoint2.getId());

        var sourceDataO = payloadManager.getSourceData(
                legalPartner,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 15));
        assertThat(sourceDataO).isNotEmpty();

        PaidOrderReportPayload expected = new PaidOrderReportPayload(
                "Отчет об оплаченных заказах 09.08.2020 - 15.08.2020",
                "Отчет об оплаченных заказах 09.08.2020 - 15.08.2020.xlsx",
                PaidOrderReportPayload.Parameters.builder()
                        .totalSum(cardOrder.getBoughtTotalPrice().add(cardOrder.getBoughtTotalPrice()))
                        .build(),
                List.of(
                        new PaidOrderReportPayload.Item(
                                1,
                                legalPartner.getOrganization().getFullName(),
                                pickupPoint1.getName(),
                                pickupPoint1.getPvzMarketId(),
                                cardOrder.getExternalId(),
                                clock.instant().atOffset(zone),
                                cardOrder.getBoughtTotalPrice(),
                                OrderPaymentType.CARD,
                                null,
                                null
                        ),
                        new PaidOrderReportPayload.Item(
                                2,
                                legalPartner.getOrganization().getFullName(),
                                pickupPoint2.getName(),
                                pickupPoint2.getPvzMarketId(),
                                cashOrder.getExternalId(),
                                clock.instant().atOffset(zone),
                                cashOrder.getBoughtTotalPrice(),
                                OrderPaymentType.CASH,
                                null,
                                null
                        )
                )
        );


        var payload = payloadManager.create(sourceDataO.get());
        assertThat(payload).isEqualTo(expected);
    }

    @Transactional
    @Test
    void notIncludePrepaidOrderForLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint1 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        PickupPoint pickupPoint2 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());

        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);

        createAndDeliverOrder(pickupPoint1, "2392932", OrderPaymentType.PREPAID,
                OrderPaymentStatus.PAID, paymentDate, OrderDeliveryType.UNKNOWN, null);
        createAndDeliverOrder(pickupPoint2, "2392933", OrderPaymentType.PREPAID,
                OrderPaymentStatus.PAID, paymentDate, OrderDeliveryType.UNKNOWN, null);

        var sourceDataO = payloadManager.getSourceData(
                legalPartner,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 15));
        assertThat(sourceDataO).isEmpty();
    }

    @Transactional
    @Test
    void notIncludeOutOfDateOrderForLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint1 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        PickupPoint pickupPoint2 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());

        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);

        createAndDeliverOrder(pickupPoint1, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);
        createAndDeliverOrder(pickupPoint2, "2392933", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        var sourceDataO = payloadManager.getSourceData(
                legalPartner,
                LocalDate.of(2020, 8, 15),
                LocalDate.of(2020, 8, 21));
        assertThat(sourceDataO).isNotEmpty();
        var payload = payloadManager.create(sourceDataO.get());
        assertThat(payload.getDetailRows()).hasSize(2);
    }

    @Transactional
    @Test
    void notIncludeOrderWithDifferentLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());

        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);

        createAndDeliverOrder(pickupPoint, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        LegalPartner anotherLegalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint anotherPickupPoint =
                pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                        .builder()
                        .legalPartner(anotherLegalPartner)
                        .build());

        createAndDeliverOrder(anotherPickupPoint, "2392933", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        var sourceDataO = payloadManager.getSourceData(
                legalPartner,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 15));
        assertThat(sourceDataO).isNotEmpty();

        var payload = payloadManager.create(sourceDataO.get());
        assertThat(payload.getDetailRows()).hasSize(1);
    }

    @Transactional
    @Test
    void notIncludeNextDayOrderForLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint1 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        PickupPoint pickupPoint2 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());

        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 15, 0, 30, 0);

        createAndDeliverOrder(pickupPoint1, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);
        createAndDeliverOrder(pickupPoint2, "2392933", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        var sourceDataO = payloadManager.getSourceData(
                legalPartner,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 14));
        assertThat(sourceDataO).isEmpty();
    }

    @Transactional
    @Test
    void includeThisDayOrderForLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint1 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        PickupPoint pickupPoint2 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 23, 30, 0);

        createAndDeliverOrder(pickupPoint1, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);
        createAndDeliverOrder(pickupPoint2, "2392933", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        var sourceDataO = payloadManager.getSourceData(
                legalPartner,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 14));
        assertThat(sourceDataO).isNotEmpty();

        var payload = payloadManager.create(sourceDataO.get());
        assertThat(payload.getDetailRows()).hasSize(2);
    }

    @Transactional
    @Test
    void includeFirstDayOrderForLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint1 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        PickupPoint pickupPoint2 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .legalPartner(legalPartner)
                .build());
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 9, 0, 30, 0);

        createAndDeliverOrder(pickupPoint1, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);
        createAndDeliverOrder(pickupPoint2, "2392933", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        var sourceDataO = payloadManager.getSourceData(
                legalPartner,
                LocalDate.of(2020, 8, 9),
                LocalDate.of(2020, 8, 14));
        assertThat(sourceDataO).isNotEmpty();

        var payload = payloadManager.create(sourceDataO.get());
        assertThat(payload.getDetailRows()).hasSize(2);
    }

    @Test
    @SneakyThrows
    void includeMoneyTransferDateAndPaymentOrderNumberIfMarkedAsPayedForLegalPartner() {
        ArgumentCaptor<PaidOrderReportPayload> payloadCaptor = ArgumentCaptor.forClass(PaidOrderReportPayload.class);
        doReturn(new byte[0]).when(payloadManager).getBinaryContent(payloadCaptor.capture());

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint1 =
                pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                        .builder()
                        .legalPartner(legalPartner)
                        .build());
        PickupPoint pickupPoint2 =
                pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                        .builder()
                        .legalPartner(legalPartner)
                        .build());
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 23, 30, 0);

        createAndDeliverOrder(pickupPoint1, "2392932", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);
        createAndDeliverOrder(pickupPoint2, "2392933", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        Long reportId = transactionTemplate.execute(ts -> {
            LegalPartner lp = legalPartnerRepository.findByIdOrThrow(legalPartner.getId());
            Report report = reportGenerator.generate(
                    lp, PvzReportType.PAID_ORDER,
                    LocalDate.of(2020, 8, 9),
                    LocalDate.of(2020, 8, 14)
            );
            return report.getId();
        });

        assertThat(reportId).isNotNull();

        transactionTemplate.execute(ts -> {
            var reportViewBeforePayed = reportQueryService.getView(reportId);
            assertThat(reportViewBeforePayed.getLastPaymentDate()).isNull();
            assertThat(reportViewBeforePayed.getPaymentOrderNumbers()).isNotEmpty();
            assertThat(reportViewBeforePayed.getPaymentOrderNumbers().get(0)).isNull();
            return null;
        });

        String paymentOrderNumber = "123321";
        LocalDate moneyTransferDate = LocalDate.of(2020, 10, 15);

        reportCommandService.markPaid(reportId, clock.instant(), paymentOrderNumber, moneyTransferDate);
        assertThat(payloadCaptor.getAllValues()).hasSize(2);

        transactionTemplate.execute(ts -> {
            var reportViewAfterPayed = reportQueryService.getView(reportId);
            assertThat(reportViewAfterPayed.getLastPaymentDate()).isEqualTo(moneyTransferDate);
            assertThat(reportViewAfterPayed.getPaymentOrderNumbers()).isNotEmpty();
            assertThat(reportViewAfterPayed.getPaymentOrderNumbers().get(0)).isEqualTo(paymentOrderNumber);
            return null;
        });
    }

    @Test
    @SneakyThrows
    void regenerateReportAfterNewDeliveredOrders() {
        ArgumentCaptor<PaidOrderReportPayload> payloadCaptor = ArgumentCaptor.forClass(PaidOrderReportPayload.class);
        doReturn(new byte[0]).when(payloadManager).getBinaryContent(payloadCaptor.capture());
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime paymentDate = LocalDateTime.of(2020, 8, 14, 15, 30, 0);

        var orderExternalIdBeforeRegeneration = "2392932";
        createAndDeliverOrder(pickupPoint, orderExternalIdBeforeRegeneration, OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        var generatedReportId = transactionTemplate.execute(ts -> {
            LegalPartner lp = legalPartnerRepository.findByIdOrThrow(legalPartner.getId());
            var generatedReport = reportGenerator.generate(
                    lp, PvzReportType.PAID_ORDER,
                    LocalDate.of(2020, 8, 9),
                    LocalDate.of(2020, 8, 14)
            );
            var ordersBeforeRegeneration = generatedReport.getOrders();

            assertThat(ordersBeforeRegeneration).isNotEmpty();
            assertThat(ordersBeforeRegeneration.size()).isEqualTo(1);
            assertThat(ordersBeforeRegeneration.get(0).getExternalId()).isEqualTo(orderExternalIdBeforeRegeneration);

            return generatedReport.getId();
        });

        createAndDeliverOrder(pickupPoint, "2392933", OrderPaymentType.UNKNOWN,
                OrderPaymentStatus.UNPAID, paymentDate, OrderDeliveryType.PAYMENT, OrderPaymentType.CASH);

        transactionTemplate.execute(ts -> {
            var ordersAfterRegeneration = reportGenerator.regenerate(
                    reportQueryService.get(generatedReportId)).getOrders();

            assertThat(ordersAfterRegeneration).isNotEmpty();
            assertThat(ordersAfterRegeneration.size()).isEqualTo(1);
            assertThat(ordersAfterRegeneration.get(0).getExternalId()).isEqualTo(orderExternalIdBeforeRegeneration);
            return null;
        });
    }

    private Order createAndDeliverOrder(PickupPoint pickupPoint,
                                        String externalId,
                                        OrderPaymentType sourcePaymentType,
                                        OrderPaymentStatus paymentStatus,
                                        LocalDateTime paymentDate,
                                        OrderDeliveryType deliveryType,
                                        OrderPaymentType paymentType) {
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(paymentDate.toInstant(zone), zone);
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId(externalId)
                        .paymentType(sourcePaymentType)
                        .paymentStatus(paymentStatus)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        order = orderFactory.receiveOrder(order.getId());
        order = orderFactory.deliverOrderCompletely(order.getId(), deliveryType, paymentType);
        return order;
    }
}
