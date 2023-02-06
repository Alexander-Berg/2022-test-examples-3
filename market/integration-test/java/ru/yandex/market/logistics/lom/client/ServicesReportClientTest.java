package ru.yandex.market.logistics.lom.client;

import java.math.BigDecimal;
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
import ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory;
import ru.yandex.market.logistics.lom.model.enums.BillingProductType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.filter.ServicesReportFilter;
import ru.yandex.market.logistics.lom.model.report.dto.ServicesReportDto;
import ru.yandex.market.logistics.lom.model.report.dto.ServicesReportOrderDto;
import ru.yandex.market.logistics.lom.model.report.dto.ServicesReportTransactionDto;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class ServicesReportClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Тест на получение отчета Услуги")
    void servicesReportTest() {
        mockFind(extractFileContent("response/report/services_response.json"));

        softly.assertThat(lomClient.getServicesReport(defaultFilter()))
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(List.of(
                ServicesReportDto.builder()
                    .order(defaultOrder())
                    .transactions(List.of(defaultTransaction()))
                    .build()
            ));
    }

    @Test
    @DisplayName("Тест на получение пустого отчета Услуги")
    void servicesReportEmptyTest() {
        mockFind("[]");

        softly.assertThat(lomClient.getServicesReport(defaultFilter()))
            .usingRecursiveFieldByFieldElementComparator()
            .isEmpty();
    }

    @Nonnull
    private ServicesReportOrderDto defaultOrder() {
        return ServicesReportOrderDto.builder()
            .assessedValue(BigDecimal.ZERO)
            .deliveryAtStartDate(ZonedDateTime.of(2019, 12, 12, 12, 0, 0, 0, ZoneId.of("UTC")))
            .deliveryDeliveredDate(ZonedDateTime.of(2019, 11, 11, 8, 11, 50, 0, ZoneId.of("UTC")))
            .deliveryLoadedDate(ZonedDateTime.of(2019, 12, 7, 12, 0, 0, 0, ZoneId.of("UTC")))
            .deliveryName("Partner 51")
            .warehouseFromAddress(DtoBuilderFactory.addressDtoBuilber().build())
            .warehouseToAddress(DtoBuilderFactory.addressDtoBuilber().settlement("Москва").build())
            .firstPartnerType(PartnerType.DELIVERY)
            .firstShipmentType(ShipmentType.IMPORT)
            .orderCost(BigDecimal.valueOf(11002))
            .orderId(2L)
            .orderStatus(OrderStatus.FINISHED)
            .pickupPointGeoId(213)
            .platformClientId(3L)
            .recipientFirstName("Partner 51")
            .recipientGeoId(213)
            .recipientLastName("Partner 51")
            .recipientLocality("Москва")
            .recipientMiddleName("Partner 51")
            .recipientRegion("Москва")
            .senderId(1L)
            .senderName("sender 1")
            .sortingCenterAtStartDate(ZonedDateTime.of(2019, 12, 8, 12, 0, 0, 0, ZoneId.of("UTC")))
            .sortingCenterReturnReturnedDate(ZonedDateTime
                .of(2019, 12, 13, 21, 0, 0, 0, ZoneId.of("UTC")))
            .sortingCenterReturnRffArrivedDate(ZonedDateTime
                .of(2019, 12, 12, 21, 0, 0, 0, ZoneId.of("UTC")))
            .sortingCenterReturnRffTransmittedDate(ZonedDateTime
                .of(2019, 12, 11, 21, 0, 0, 0, ZoneId.of("UTC")))
            .sortingCenterTransmittedDate(ZonedDateTime
                .of(2019, 12, 9, 12, 0, 0, 0, ZoneId.of("UTC")))
            .totalWeight(new BigDecimal("4.0000"))
            .build();
    }

    @Nonnull
    private ServicesReportTransactionDto defaultTransaction() {
        return ServicesReportTransactionDto.builder()
            .productServiceType(ShipmentOption.CASH_SERVICE)
            .productType(BillingProductType.SERVICE)
            .transactionAmount(BigDecimal.valueOf(-209))
            .transactionDate(ZonedDateTime.of(2019, 11, 15, 8, 11, 50, 0, ZoneId.of("UTC")))
            .transactionId(201L)
            .build();
    }

    @Nonnull
    private ServicesReportFilter defaultFilter() {
        return ServicesReportFilter.builder()
            .senderIds(Set.of(1L))
            .dateFrom(ZonedDateTime.of(2019, 12, 8, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE))
            .dateTo(ZonedDateTime.of(2019, 12, 12, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE))
            .platformClientId(3L)
            .build();
    }

    private void mockFind(String response) {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(content().json(extractFileContent("request/report/services_request.json")))
            .andExpect(requestTo(startsWith(uri + "/report/services-data")))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }
}
