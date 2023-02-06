package ru.yandex.market.pvz.internal.controller.pi.report;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.domain.pickup_point.InMemoryPickupPointService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointSimpleParams;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;
import ru.yandex.market.pvz.internal.controller.pi.report.dto.ReportPageDto;
import ru.yandex.market.pvz.internal.controller.pi.report.spec.ReportFilterParams;
import ru.yandex.market.pvz.internal.controller.pi.report.spec.ReportMatchingStatusDto;
import ru.yandex.market.pvz.internal.controller.pi.report.spec.ReportTabFilter;
import ru.yandex.market.pvz.internal.domain.report.ReportPartnerService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReportControllerTest extends BaseShallowTest {

    @MockBean
    private ReportPartnerService reportPartnerService;

    @MockBean
    private InMemoryPickupPointService inMemoryPickupPointService;

    @Test
    @SneakyThrows
    void getReports() {
        doReturn(PickupPointSimpleParams.builder().pvzMarketId(1L).build())
                .when(inMemoryPickupPointService).getByPvzMarketIdOrThrow(anyLong());

        ReportFilterParams params = ReportFilterParams.builder()
                .dateFrom(LocalDate.of(2020, 1, 1))
                .dateTo(LocalDate.of(2020, 1, 30))
                .tabFilter(ReportTabFilter.PAID_ORDERS).build();
        when(reportPartnerService.getReports(eq(null), anyLong(), Mockito.eq(params), any()))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(3);

                    var reports = List.of(
                            ReportPageDto.builder()
                                    .id(1L)
                                    .name("Отчет 1")
                                    .isPaid(false)
                                    .paymentStatus(ReportMatchingStatusDto.SUM_MISMATCH).build()
                    );

                    return new PageImpl<>(reports, pageable, 1);
                });

        mockMvc.perform(
                get("/v1/pi/pickup-points/1/reports")
                        .param("dateFrom", params.getDateFrom().toString())
                        .param("dateTo", params.getDateTo().toString())
                        .param("tabFilter", params.getTabFilter().toString()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("" +
                        "{\n" +
                        "  \"content\": [\n" +
                        "    {\n" +
                        "      \"id\": 1,\n" +
                        "      \"name\": \"Отчет 1\",\n" +
                        "      \"isPaid\": false,\n" +
                        "      \"paymentStatus\": \"SUM_MISMATCH\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"pageable\": {\n" +
                        "    \"sort\": {\n" +
                        "      \"unsorted\": false,\n" +
                        "      \"sorted\": true,\n" +
                        "      \"empty\": false\n" +
                        "    },\n" +
                        "    \"pageSize\": 20,\n" +
                        "    \"offset\": 0,\n" +
                        "    \"pageNumber\": 0,\n" +
                        "    \"paged\": true,\n" +
                        "    \"unpaged\": false\n" +
                        "  },\n" +
                        "  \"totalPages\": 1,\n" +
                        "  \"totalElements\": 1,\n" +
                        "  \"last\": true,\n" +
                        "  \"first\": true,\n" +
                        "  \"numberOfElements\": 1,\n" +
                        "  \"number\": 0,\n" +
                        "  \"size\": 20,\n" +
                        "  \"sort\": {\n" +
                        "    \"unsorted\": false,\n" +
                        "    \"sorted\": true,\n" +
                        "    \"empty\": false\n" +
                        "  },\n" +
                        "  \"empty\": false\n" +
                        "}"
                ));
    }

    @Test
    void markPaidNotNumericOrderPaymentNumber() throws Exception {
        mockMvc.perform(
                patch("/v1/pi/pickup-points/1/reports/1/mark-paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("report/request_mark_paid_not_numeric_order_payment_number.json")))
                .andExpect(status().is4xxClientError());
    }
}
