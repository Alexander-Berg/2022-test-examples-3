package ru.yandex.market.pvz.tms.executor.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.report.Report;
import ru.yandex.market.pvz.core.domain.report.ReportRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.report.core.ReportService;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@Import({GeneratePaidOrderReportExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class GeneratePaidOrderReportExecutorTest {

    private final TestableClock clock;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final ReportRepository reportRepository;
    private final GeneratePaidOrderReportExecutor executor;

    // we need this mock because Jasper report generation doesn't work in Arcadia
    @MockBean
    private ReportService reportService;

    @Test
    void generatePaidOrderReportOnDefaultDays() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        createAndDeliver(pickupPoint, LocalDateTime.of(2021, 1, 31, 15, 30, 0));
        for (int i = 0; i < 7; i++) {
            createAndDeliver(pickupPoint, LocalDateTime.of(2021, 2, i + 1, 15, 30, 0));
        }

        assertGenerated(LocalDate.of(2021, 2, 1), 1);
        assertNotGenerated(LocalDate.of(2021, 2, 2), 1);
        assertGenerated(LocalDate.of(2021, 2, 3), 2);
        assertNotGenerated(LocalDate.of(2021, 2, 4), 2);
        Report lastReport = assertGenerated(LocalDate.of(2021, 2, 5), 3);
        assertNotGenerated(LocalDate.of(2021, 2, 6), 3);
        assertNotGenerated(LocalDate.of(2021, 2, 7), 3);

        assertThat(lastReport.getDateFrom()).isEqualTo(LocalDate.of(2021, 2, 3));
        assertThat(lastReport.getDateTo()).isEqualTo(LocalDate.of(2021, 2, 4));
        assertThat(lastReport.getName()).isEqualTo("Отчет об оплаченных заказах 03.02.2021 - 04.02.2021");
    }

    @Test
    void generateFirstReportInThreeDaysAfterFirstDeliveredOrder() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        assertNotGenerated(LocalDate.of(2021, 2, 1));
        createAndDeliver(pickupPoint, LocalDateTime.of(2021, 2, 1, 15, 30, 0));

        assertNotGenerated(LocalDate.of(2021, 2, 6));
        assertNotGenerated(LocalDate.of(2021, 2, 7));

        Report report = assertGenerated(LocalDate.of(2021, 2, 8), 1);
        assertThat(report.getDateFrom()).isEqualTo(LocalDate.of(2021, 2, 1));
        assertThat(report.getDateTo()).isEqualTo(LocalDate.of(2021, 2, 7));
        assertThat(report.getName()).isEqualTo("Отчет об оплаченных заказах 01.02.2021 - 07.02.2021");
    }

    private void assertNotGenerated(LocalDate when) {
        assertNotGenerated(when, 0);
    }

    private void assertNotGenerated(LocalDate when, int prevCount) {
        setDate(when);
        executor.doRealJob(null);
        assertThat(reportRepository.findAll()).hasSize(prevCount);
    }

    private Report assertGenerated(LocalDate when, int count) {
        setDate(when);
        executor.doRealJob(null);
        List<Report> reports = reportRepository.findAll(Sort.by("dateFrom"));
        assertThat(reports.size()).isEqualTo(count);
        return reports.get(count - 1);
    }

    private void setDate(LocalDate date) {
        LocalDateTime current = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 4, 0, 0);
        clock.setFixed(current.toInstant(DateTimeUtil.DEFAULT_ZONE_ID), clock.getZone());
    }

    @Test
    void tryToGeneratePaidOrderReportIfAlreadyExists() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        createAndDeliver(pickupPoint, LocalDateTime.of(2021, 2, 1, 15, 30, 0));

        LocalDateTime current = LocalDateTime.of(2021, 2, 3, 4, 0, 0);
        clock.setFixed(current.toInstant(zone), zone);

        executor.doRealJob(null);

        List<Report> reports = reportRepository.findAll();
        assertThat(reports.size()).isEqualTo(1);
        Report report = reports.get(0);
        assertThat(report.getDateFrom()).isEqualTo(LocalDate.of(2021, 2, 1));
        assertThat(report.getDateTo()).isEqualTo(LocalDate.of(2021, 2, 2));

        executor.doRealJob(null);

        reports = reportRepository.findAll();
        assertThat(reports.size()).isEqualTo(1);
    }

    @Test
    void noOrdersForPaidOrderReport() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        LocalDateTime current = LocalDateTime.of(2020, 8, 17, 4, 0, 0);
        clock.setFixed(current.toInstant(zone), zone);

        executor.doRealJob(null);

        List<Report> reports = reportRepository.findAll();
        assertThat(reports).isEmpty();
    }

    private void createAndDeliver(PickupPoint pickupPoint, LocalDateTime paymentDate) {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.UNKNOWN)
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .build())
                .build());
        order = orderFactory.receiveOrder(order.getId());

        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(paymentDate.toInstant(zone), zone);

        orderFactory.deliverOrderCompletely(order.getId(), OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);
    }
}
