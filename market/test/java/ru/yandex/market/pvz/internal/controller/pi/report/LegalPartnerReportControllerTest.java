package ru.yandex.market.pvz.internal.controller.pi.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.order.OrderCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.report.PvzReportType;
import ru.yandex.market.pvz.core.domain.report.Report;
import ru.yandex.market.pvz.core.domain.report.ReportGenerator;
import ru.yandex.market.pvz.core.domain.report.ReportSourceData;
import ru.yandex.market.pvz.core.domain.report.payload.creator.ReportPayloadCreatorSelector;
import ru.yandex.market.pvz.core.domain.report.payload.manager.MockPayloadManager;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.CreatePickupPointBuilder;
import static ru.yandex.market.tpl.common.util.StringFormatter.formatVars;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerReportControllerTest extends BaseShallowTest {

    @MockBean
    private ReportPayloadCreatorSelector selector;

    @Spy
    private MockPayloadManager payloadManager;

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;
    private final ReportGenerator reportGenerator;
    private final TestableClock clock;

    @BeforeEach
    void selectManager() {
        doReturn(payloadManager).when(selector).select(PvzReportType.PAID_ORDER);
        doReturn(payloadManager).when(selector).select(PvzReportType.EXECUTED_ORDERS_REPORT);
        doReturn(Optional.of(ReportSourceData.builder().orders(List.of()).build()))
                .when(payloadManager).getSourceData(any(LegalPartner.class), any(), any());
        doReturn(Optional.of(ReportSourceData.builder().orders(List.of()).build()))
                .when(payloadManager).getSourceData(any(LegalPartner.class), any(), any(), any());
    }

    @Test
    void getPartnerReports() throws Exception {
        LocalDate dateFrom = LocalDate.of(2021, 3, 9);
        LocalDate dateTo = LocalDate.of(2021, 3, 15);

        var legalPartner1 = legalPartnerFactory.createLegalPartner();
        var legalPartner2 = legalPartnerFactory.createLegalPartner();

        Report report1 = reportGenerator.generate(legalPartner1, PvzReportType.EXECUTED_ORDERS_REPORT, dateFrom,
                dateTo);
        Report report2 = reportGenerator.generate(
                legalPartner1, PvzReportType.EXECUTED_ORDERS_REPORT, dateFrom.plusDays(7), dateTo.plusDays(7));
        Report report3 = reportGenerator.generate(
                legalPartner1, PvzReportType.EXECUTED_ORDERS_REPORT, dateFrom.plusDays(21), dateTo.plusDays(21));
        Report report4 = reportGenerator.generate(
                legalPartner1, PvzReportType.PAID_ORDER, dateFrom.plusDays(7), dateTo.plusDays(7));
        Report report5 = reportGenerator.generate(
                legalPartner2, PvzReportType.EXECUTED_ORDERS_REPORT, dateFrom.plusDays(7), dateTo.plusDays(7));

        mockMvc.perform(
                get("/v1/pi/partners/" + legalPartner1.getPartnerId() + "/reports")
                        .param("dateFrom", dateFrom.minusDays(1).toString())
                        .param("dateTo", dateTo.plusDays(8).toString())
                        .param("tabFilter", "MONTH_CLOSING"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("report/response_get_reports.json"),
                        report1.getId(),
                        report2.getId())));
    }

    @Test
    @SneakyThrows
    void testEditPaymentWithDate() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());

        Order order = createAndDeliver(pickupPoint, LocalDateTime.of(2022, 1, 15, 15, 30, 0));

        Report report = reportGenerator.generate(
                legalPartner.getId(),
                PvzReportType.PAID_ORDER,
                LocalDate.of(2022, 1, 14),
                LocalDate.of(2022, 1, 16));
        orderCommandService.bindOrdersToPaidReport(List.of(order), report);

        mockMvc.perform(
                patch("/v1/pi/partners/" + legalPartner.getPartnerId() + "/reports/" + report.getId() + "/mark-paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getPaymentDataDto("123", "2022-01-16")))
                .andExpect(status().is2xxSuccessful());


        mockMvc.perform(
                patch("/v1/pi/partners/" + legalPartner.getPartnerId() + "/reports/" + report.getId() + "/edit-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getPaymentDataDto("321", "2022-01-17")))
                .andExpect(status().is2xxSuccessful());

        OrderParams orderParams = orderQueryService.get(order.getId());
        assertThat(orderParams.getPaymentOrderNumber()).isEqualTo("321");
        assertThat(orderParams.getPaymentOrderDate()).isEqualTo("2022-01-17");
    }

    @Test
    @SneakyThrows
    void testEditPaymentWithoutDateBackwardsCompatibility() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());

        Order order = createAndDeliver(pickupPoint, LocalDateTime.of(2022, 1, 15, 15, 30, 0));

        Report report = reportGenerator.generate(
                legalPartner.getId(),
                PvzReportType.PAID_ORDER,
                LocalDate.of(2022, 1, 14),
                LocalDate.of(2022, 1, 16));
        orderCommandService.bindOrdersToPaidReport(List.of(order), report);

        mockMvc.perform(
                patch("/v1/pi/partners/" + legalPartner.getPartnerId() + "/reports/" + report.getId() + "/mark-paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getPaymentDataDto("123", "2022-01-16")))
                .andExpect(status().is2xxSuccessful());


        mockMvc.perform(
                patch("/v1/pi/partners/" + legalPartner.getPartnerId() + "/reports/" + report.getId() + "/edit-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getEditPaymentOldDto("321")))
                .andExpect(status().is2xxSuccessful());

        OrderParams orderParams = orderQueryService.get(order.getId());
        assertThat(orderParams.getPaymentOrderNumber()).isEqualTo("321");
        assertThat(orderParams.getPaymentOrderDate()).isEqualTo("2022-01-16");
    }

    private Order createAndDeliver(PickupPoint pickupPoint, LocalDateTime paymentDate) {
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

        return orderFactory.deliverOrderCompletely(order.getId(), OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);
    }

    private String getPaymentDataDto(String paymentNumber, String paymentDate) {
        return formatVars(getFileContent("report/request_mark_paid_or_edit_payment.json"), Map.of(
                "paymentNumber", paymentNumber,
                "paymentDate", paymentDate
        ));
    }

    private String getEditPaymentOldDto(String paymentNumber) {
        return formatVars(getFileContent("report/request_edit_payment_old.json"), Map.of(
                "paymentNumber", paymentNumber
        ));
    }

}
