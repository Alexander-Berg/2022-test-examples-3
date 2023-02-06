package ru.yandex.market.pvz.tms.executor.oebs_receipt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerRepository;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceipt;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.report.PvzReportType;
import ru.yandex.market.pvz.core.domain.report.Report;
import ru.yandex.market.pvz.core.domain.report.ReportCommandService;
import ru.yandex.market.pvz.core.domain.report.ReportGenerator;
import ru.yandex.market.pvz.core.domain.report.properties.OebsMatchingStatus;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.report.core.ReportService;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTmsTest
@Import({MatchWithOebsReceiptExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class MatchWithOebsReceiptExecutorTest {

    private static final String OEBS_NUMBER = "111";
    private static final String PAYENT_ORDER_NUMBER = "123";
    private static final String VIRTUAL_ACCOUNT_NUMBER = "ACCOUNT_123";
    private static final LocalDate PAYMENT_DATE = LocalDate.of(2020, 2, 5);

    private static final LocalDate FROM = LocalDate.of(2020, 1, 1);
    private static final LocalDate TO = LocalDate.of(2020, 1, 31);

    private final MatchWithOebsReceiptExecutor executor;

    private final LegalPartnerRepository legalPartnerRepository;
    private final OebsReceiptRepository oebsReceiptRepository;
    private final ReportCommandService reportCommandService;
    private final ReportGenerator reportGenerator;

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private Report report;
    private Order order;
    private PickupPoint pickupPoint;

    @MockBean
    private ReportService reportService;

    @BeforeEach
    void setup() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        partner.setVirtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER);
        legalPartnerRepository.saveAndFlush(partner);

        pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(partner)
                .build());

        order = prepareOrder();

        report = reportGenerator.generate(pickupPoint, PvzReportType.PAID_ORDER, FROM, TO);
        report = reportCommandService.markPaid(report.getId(), Instant.now(), PAYENT_ORDER_NUMBER, PAYMENT_DATE);
    }

    private Order prepareOrder() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .build())
                .build());

        LocalDate deliveryDate = FROM.plusDays(1);
        order.setDeliveredAt(OffsetDateTime.of(deliveryDate, LocalTime.MIDNIGHT, ZoneOffset.UTC));
        order.setBillingAt(OffsetDateTime.of(deliveryDate, LocalTime.MIDNIGHT, ZoneOffset.UTC));
        order.setStatusOnly(PvzOrderStatus.DELIVERED_TO_RECIPIENT);
        return orderFactory.updateOrder(order);
    }

    @Test
    void testMatchWithCorrectReceipt() {
        oebsReceiptRepository.saveAndFlush(OebsReceipt.builder()
                .oebsNumber(OEBS_NUMBER)
                .sum(order.getBoughtTotalPrice())
                .paymentOrderNumber(PAYENT_ORDER_NUMBER)
                .paymentOrderDate(PAYMENT_DATE)
                .virtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER)
                .build());

        executor.doRealJob(null);

        assertThat(order.getOebsMatchingStatus()).isEqualTo(OebsMatchingStatus.OK);
        assertThat(order.getOebsReceipt().getOebsNumber()).isEqualTo(OEBS_NUMBER);
    }

    @Test
    void testMatchIfPayedMore() {
        oebsReceiptRepository.saveAndFlush(OebsReceipt.builder()
                .oebsNumber(OEBS_NUMBER)
                .sum(order.getBoughtTotalPrice().add(BigDecimal.ONE))
                .paymentOrderNumber(PAYENT_ORDER_NUMBER)
                .paymentOrderDate(PAYMENT_DATE)
                .virtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER)
                .build());

        executor.doRealJob(null);

        assertThat(order.getOebsMatchingStatus()).isEqualTo(OebsMatchingStatus.OK);
        assertThat(order.getOebsReceipt().getOebsNumber()).isEqualTo(OEBS_NUMBER);
    }

    @Test
    void testNoMatchIfPayedLess() {
        oebsReceiptRepository.saveAndFlush(OebsReceipt.builder()
                .oebsNumber(OEBS_NUMBER)
                .sum(order.getBoughtTotalPrice().subtract(BigDecimal.ONE))
                .paymentOrderNumber(PAYENT_ORDER_NUMBER)
                .paymentOrderDate(PAYMENT_DATE)
                .virtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER)
                .build());

        executor.doRealJob(null);

        assertThat(order.getOebsMatchingStatus()).isEqualTo(OebsMatchingStatus.SUM_MISMATCH);
    }

    @Test
    void testMatchOnOtherVirtualAccountNumber() {
        oebsReceiptRepository.saveAndFlush(OebsReceipt.builder()
                .oebsNumber(OEBS_NUMBER)
                .sum(order.getBoughtTotalPrice())
                .paymentOrderNumber(PAYENT_ORDER_NUMBER)
                .paymentOrderDate(PAYMENT_DATE)
                .virtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER + "_other")
                .build());

        executor.doRealJob(null);

        assertThat(order.getOebsMatchingStatus()).isNull();
        assertThat(order.getOebsReceipt()).isNull();
    }

    @Test
    void testNoUpdateOnOtherPaymentOrderNumber() {
        oebsReceiptRepository.saveAndFlush(OebsReceipt.builder()
                .oebsNumber(OEBS_NUMBER)
                .sum(order.getBoughtTotalPrice())
                .paymentOrderNumber(PAYENT_ORDER_NUMBER + "9")
                .paymentOrderDate(PAYMENT_DATE)
                .virtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER)
                .build());

        executor.doRealJob(null);

        assertThat(order.getOebsMatchingStatus()).isNull();
        assertThat(order.getOebsReceipt()).isNull();
    }

    @Test
    void testMatchSumIfMultipleOrders() {
        Order order2 = prepareOrder();

        reportGenerator.regenerate(report);
        reportCommandService.markPaid(report.getId(), Instant.now(), PAYENT_ORDER_NUMBER, PAYMENT_DATE, true);

        oebsReceiptRepository.saveAndFlush(OebsReceipt.builder()
                .oebsNumber(OEBS_NUMBER)
                .sum(order.getBoughtTotalPrice().add(order2.getBoughtTotalPrice()))
                .paymentOrderNumber(PAYENT_ORDER_NUMBER)
                .paymentOrderDate(PAYMENT_DATE)
                .virtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER)
                .build());

        executor.doRealJob(null);

        assertThat(order.getOebsMatchingStatus()).isEqualTo(OebsMatchingStatus.OK);
        assertThat(order.getOebsReceipt().getOebsNumber()).isEqualTo(OEBS_NUMBER);

        assertThat(order2.getOebsMatchingStatus()).isNull();
        assertThat(order2.getOebsReceipt()).isNull();
    }

}
