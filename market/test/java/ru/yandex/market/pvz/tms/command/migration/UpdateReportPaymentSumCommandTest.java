package ru.yandex.market.pvz.tms.command.migration;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.report.PvzReportType;
import ru.yandex.market.pvz.core.domain.report.ReportGenerator;
import ru.yandex.market.pvz.core.domain.report.properties.ReportPropertiesRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.report.core.ReportService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.getDefaultTotalPrice;
import static ru.yandex.market.pvz.tms.command.migration.UpdateReportPaymentSumCommand.COMMAND_NAME;

@TransactionlessEmbeddedDbTest
@Import({
        UpdateReportPaymentSumCommand.class
})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UpdateReportPaymentSumCommandTest {

    private final TestableClock clock;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final ReportGenerator reportGenerator;

    private final JdbcTemplate jdbcTemplate;
    private final ReportPropertiesRepository reportPropertiesRepository;

    private final UpdateReportPaymentSumCommand command;

    @MockBean
    private Terminal terminal;

    @MockBean
    private PrintWriter printWriter;

    @MockBean
    private ReportService reportService;

    @Test
    void migrate() {
        when(terminal.getWriter()).thenReturn(printWriter);
        LocalDate from = LocalDate.of(2021, 10, 8);
        LocalDate to = LocalDate.of(2021, 10, 28);
        LocalDate deliveredAt = LocalDate.of(2021, 10, 15);

        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(deliveredAt)
                        .paymentType(OrderPaymentType.CARD)
                        .build())
                .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(deliveredAt.atStartOfDay().toInstant(zone), zone);
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrderCompletely(order.getId(), OrderDeliveryType.PASSPORT, OrderPaymentType.CARD);

        var report = reportGenerator.generate(pickupPoint, PvzReportType.PAID_ORDER, from, to);

        var reportProperties = reportPropertiesRepository.findByIdOrThrow(report.getId());
        assertThat(reportProperties.getPaymentSum()).isEqualByComparingTo(getDefaultTotalPrice());

        jdbcTemplate.update("UPDATE report_property SET payment_sum = NULL");
        reportProperties = reportPropertiesRepository.findByIdOrThrow(report.getId());
        assertThat(reportProperties.getPaymentSum()).isNull();

        command.executeCommand(
                new CommandInvocation(COMMAND_NAME, new String[]{}, Collections.emptyMap()), terminal);

        reportProperties = reportPropertiesRepository.findByIdOrThrow(report.getId());
        assertThat(reportProperties.getPaymentSum()).isEqualByComparingTo(getDefaultTotalPrice());
    }
}
