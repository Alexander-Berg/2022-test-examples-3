package ru.yandex.market.logistics.lom.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.TimeIntervalDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.filter.WithdrawReportFilter;
import ru.yandex.market.logistics.lom.model.report.dto.WithdrawReportDto;
import ru.yandex.market.logistics.lom.model.report.dto.WithdrawShipmentReportDto;
import ru.yandex.market.logistics.lom.model.report.dto.WithdrawTransactionReportDto;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class WithdrawReportClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Успешный запрос данных для отчета Заборы")
    void getWithdrawReport() {
        mockFind(extractFileContent("response/report/withdraws_response.json"));

        List<WithdrawReportDto> result = lomClient.getWithdrawReport(defaultFilter());

        softly.assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(List.of(
                WithdrawReportDto.builder()
                    .shipment(
                        WithdrawShipmentReportDto.builder()
                            .applicationExternalId("app_ext_id")
                            .applicationId(1L)
                            .cost(BigDecimal.valueOf(295))
                            .interval(
                                TimeIntervalDto.builder()
                                    .from(LocalTime.of(10, 0))
                                    .to(LocalTime.of(12, 0))
                                    .build()
                            )
                            .partnerId(48L)
                            .partnerType(PartnerType.SORTING_CENTER)
                            .shipmentDate(LocalDate.of(2019, 12, 11))
                            .warehouseFrom(42L)
                            .build()
                    )
                    .transactions(
                        List.of(
                            WithdrawTransactionReportDto.builder()
                                .isRevertTx(false)
                                .amount(BigDecimal.valueOf(-1000))
                                .transactionId(201L)
                                .created(ZonedDateTime.of(2019, 12, 11, 9, 0, 0, 0, ZoneId.of("UTC")))
                                .build()
                        )
                    )

                    .build()
            ));
    }

    @Test
    @DisplayName("Пустой список")
    void getEmptyWithdrawReport() {
        mockFind("[]");

        List<WithdrawReportDto> result = lomClient.getWithdrawReport(defaultFilter());

        softly.assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(List.of());
    }

    @Nonnull
    private WithdrawReportFilter defaultFilter() {
        return WithdrawReportFilter.builder()
            .marketIds(Set.of(1L))
            .dateFrom(LocalDate.of(2019, 12, 10))
            .dateTo(LocalDate.of(2019, 12, 12))
            .platformClientId(3L)
            .build();
    }

    private void mockFind(String response) {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(content().json(extractFileContent("request/report/withdraws_request.json")))
            .andExpect(requestTo(startsWith(uri + "/report/withdraws-data")))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }
}
