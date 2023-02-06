package ru.yandex.market.logistics.lom.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.model.dto.NamedEntity;
import ru.yandex.market.logistics.lom.model.dto.StatusDto;
import ru.yandex.market.logistics.lom.model.enums.BalanceOperationType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.filter.BalancePaymentReportFilter;
import ru.yandex.market.logistics.lom.model.report.dto.BalancePaymentDto;
import ru.yandex.market.logistics.lom.model.report.dto.BalancePaymentOrderDto;
import ru.yandex.market.logistics.lom.model.report.dto.BalancePaymentReportDto;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class BalancePaymentsReportTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Поиск данных для отчета Движение по ПП и АВ")
    void getBalancePaymentReport() {
        mockFind(extractFileContent("response/report/balance_payments.json"));

        List<BalancePaymentReportDto> result = lomClient.getBalancePaymentReport(defaultFilter());

        softly.assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(List.of(
                BalancePaymentReportDto.builder()
                    .order(
                        BalancePaymentOrderDto.builder()
                            .id(1L)
                            .senderId(1L)
                            .barcode("32-LO1")
                            .externalId("32")
                            .partner(NamedEntity.builder().name("Partner 48").id(48L).build())
                            .status(
                                StatusDto.builder()
                                    .datetime(
                                        LocalDateTime.of(2019, 12, 6, 15, 0, 0)
                                            .atZone(DateTimeUtils.MOSCOW_ZONE).toInstant()
                                    )
                                    .status(OrderStatus.FINISHED)
                                    .build()
                            )
                            .deliveredDate(ZonedDateTime.of(2019, 12, 6, 8, 11, 50, 0, ZoneId.of("UTC")))
                            .orderCost(BigDecimal.valueOf(200L))
                            .totalCost(BigDecimal.valueOf(11000L))
                            .cashServicePercent(BigDecimal.valueOf(5L))
                            .build()
                    )
                    .payments(List.of(
                        BalancePaymentDto.builder()
                            .basketId("balance_basket_1")
                            .operationType(BalanceOperationType.PAYMENT)
                            .build()
                    ))
                    .build()
            ));
    }

    @Test
    @DisplayName("Пустой список")
    void getEmptyBalancePaymentReport() {
        mockFind("[]");

        List<BalancePaymentReportDto> result = lomClient.getBalancePaymentReport(defaultFilter());

        softly.assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(List.of());
    }

    @Nonnull
    private BalancePaymentReportFilter defaultFilter() {
        return BalancePaymentReportFilter.builder()
            .senderIds(Set.of(1L))
            .dateFrom(ZonedDateTime.of(2019, 12, 8, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE))
            .dateTo(ZonedDateTime.of(2019, 12, 10, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE))
            .platformClientId(3L)
            .build();
    }

    private void mockFind(String response) {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(content().json(extractFileContent("request/report/balance_payments_request.json")))
            .andExpect(requestTo(startsWith(uri + "/report/balance-payments-data")))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }
}
