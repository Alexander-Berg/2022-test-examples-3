package ru.yandex.market.partner.mvc.controller.delivery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.TimeIntervalDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.filter.WithdrawReportFilter;
import ru.yandex.market.logistics.lom.model.report.dto.WithdrawReportDto;
import ru.yandex.market.logistics.lom.model.report.dto.WithdrawShipmentReportDto;
import ru.yandex.market.logistics.lom.model.report.dto.WithdrawTransactionReportDto;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;
import ru.yandex.market.partner.mvc.controller.BaseReportControllerTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DaasWithdrawsReportTest extends BaseReportControllerTest {

    private static final long CONTRACT_1 = 142L;
    private static final long SHOP_1 = 214L;
    private static final long MARKET_ID = 10002L;
    private static final long SENDER_1 = 1L;
    private static final ZonedDateTime TEST_DATE = ZonedDateTime.of(2019, 1, 15, 12, 0, 0, 0, ZoneId.of("UTC"));
    public static final WithdrawReportDto WITHDRAW_DATA_1 = WithdrawReportDto.builder()
            .shipment(defaultWithdraw().build())
            .transactions(ImmutableList.of(
                    defaultTransaction().build()
            ))
            .build();
    /**
     * Отгрузка, у которой неизвестен партнер и склад.
     * Несколько корректировок, включая корректировки в прошедшем периоде.
     */
    public static final WithdrawReportDto WITHDRAW_DATA_2 = WithdrawReportDto.builder()
            .shipment(
                    defaultWithdraw()
                            .applicationExternalId("ext2")
                            .applicationId(2L)
                            .warehouseFrom(333L)
                            .partnerId(49L)
                            .interval(TimeIntervalDto.builder().from(LocalTime.of(14, 0)).to(LocalTime.of(18, 0)).build())
                            .build()
            )
            .transactions(ImmutableList.of(
                    defaultTransaction().build(),
                    defaultTransaction().amount(BigDecimal.valueOf(-115L)).created(TEST_DATE.minusDays(22)).build(),
                    defaultTransaction().amount(BigDecimal.valueOf(295L)).created(TEST_DATE.plusDays(2)).build(),
                    defaultTransaction().amount(BigDecimal.valueOf(-300L)).created(TEST_DATE.plusDays(2)).build()
            ))
            .build();
    private static final ShopWithSendersDto SHOP_WITH_SENDERS_1 = ShopWithSendersDto.builder()
            .id(SHOP_1)
            .balanceContractId(CONTRACT_1)
            .marketId(MARKET_ID)
            .name("Магазин Музей")
            .senders(List.of(NamedEntity.builder().id(SENDER_1).name("Кое-кто").build()))
            .build();
    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private LMSClient lmsClient;

    @Nonnull
    private static Stream<Arguments> badRequestSource() {
        return Stream.of(
                Arguments.of(
                        "Невалидный интервал дат",
                        "dateFrom=2019-01-10&dateTo=2019-01-02&datasource_id=774",
                        "json/daas-report-to-is-before-from.json"
                ),
                Arguments.of(
                        "Большой интервал запроса",
                        "dateFrom=2019-01-01&dateTo=2019-02-02&datasource_id=774",
                        "json/daas-report-invalid-dates-range.json"
                ),
                Arguments.of("Без dateFrom", "dateTo=2019-02-02&datasource_id=774", "json/daas-report-no-date-from.json"),
                Arguments.of("Без dateTo", "dateFrom=2019-02-02&datasource_id=774", "json/daas-report-no-date-to.json")
        );
    }

    @Nonnull
    private static WithdrawShipmentReportDto.WithdrawShipmentReportDtoBuilder defaultWithdraw() {
        return WithdrawShipmentReportDto.builder()
                .partnerType(PartnerType.DELIVERY)
                .partnerId(48L)
                .interval(TimeIntervalDto.builder().from(LocalTime.of(10, 0)).to(LocalTime.of(12, 0)).build())
                .cost(BigDecimal.valueOf(295L))
                .applicationId(1L)
                .applicationExternalId("ext1")
                .shipmentDate(TEST_DATE.toLocalDate())
                .warehouseFrom(332L);
    }

    @Nonnull
    private static WithdrawTransactionReportDto.WithdrawTransactionReportDtoBuilder defaultTransaction() {
        return WithdrawTransactionReportDto.builder()
                .isRevertTx(false)
                .created(TEST_DATE)
                .amount(BigDecimal.valueOf(-295L))
                .transactionId(22L);
    }

    /**
     * Тестирование генерации отчета "Заборы".
     */
    @Test
    void generateReportTest() {
        when(nesuClient.searchShopWithSenders(ShopWithSendersFilter.builder().shopIds(Set.of(SHOP_1)).build()))
                .thenReturn(List.of(SHOP_WITH_SENDERS_1));
        when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(48L, 49L)).build()))
                .thenReturn(List.of(PartnerResponse.newBuilder().id(48L).readableName("CDEK").build()));
        when(lmsClient
                .getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder().ids(Sets.newHashSet(332L, 333L)).build())))
                .thenReturn(ImmutableList.of(
                        LogisticsPointResponse.newBuilder().id(332L).address(defaultAddress()).build()
                ));

        when(lomClient.getWithdrawReport(defaultLomFilter()))
                .thenReturn(List.of(WITHDRAW_DATA_1, WITHDRAW_DATA_2));

        Sheet sheet = queryForSheet("/daas/reports/withdraws?dateFrom=2019-01-01&dateTo=2019-01-31&datasource_id=214");
        rowAssert(sheet.getRow(5), "ext1-1", "CDEK", "15.01.2019 / 10:00 - 12:00",
                "address string 332", 295.0D, 295.0D, "", 0.0D, "январь 2019");
        rowAssert(sheet.getRow(6), "ext2-2", "Неизвестно", "24.12.2018 / 14:00 - 18:00",
                "Неизвестно", 300, 0, "2, 3, 4", 300.0D, "декабрь 2018");
    }

    private void rowAssert(
            Row row,
            String externalId,
            String partnerName,
            String dateTime,
            String address,
            double servicesTotal,
            double withdrawCost,
            String correctionNumber,
            double correctionSum,
            String serviceDate
    ) {
        assertThat(row.getCell(0).getStringCellValue(), is(externalId));
        assertThat(row.getCell(1).getStringCellValue(), is(partnerName));
        assertThat(row.getCell(2).getStringCellValue(), is(dateTime));
        assertThat(row.getCell(3).getStringCellValue(), is(address));
        assertThat(row.getCell(4).getNumericCellValue(), is(servicesTotal));
        assertThat(row.getCell(5).getNumericCellValue(), is(withdrawCost));
        assertThat(row.getCell(6).getStringCellValue(), is(correctionNumber));
        assertThat(row.getCell(7).getNumericCellValue(), is(correctionSum));
        assertThat(row.getCell(8).getStringCellValue(), is(serviceDate));
    }

    /**
     * Тестирование генерации отчета "Заборы". Пустой отчет.
     */
    @Test
    void emptyReport() {
        when(nesuClient.searchShopWithSenders(ShopWithSendersFilter.builder().shopIds(Set.of(SHOP_1)).build()))
                .thenReturn(List.of());

        Sheet sheet = queryForSheet("/daas/reports/withdraws?dateFrom=2019-01-01&dateTo=2019-02-01&datasource_id=214");
        Assertions.assertEquals(4, sheet.getLastRowNum());

        verifyZeroInteractions(lomClient);
        verifyZeroInteractions(lmsClient);
    }

    /**
     * Тестирование генерации отчета "Заборы". Пустой отчет с существующим сендером.
     */
    @Test
    void emptyReportWithSender() {
        when(nesuClient.searchShopWithSenders(ShopWithSendersFilter.builder().shopIds(Set.of(SHOP_1)).build()))
                .thenReturn(List.of(SHOP_WITH_SENDERS_1));

        when(lomClient.getWithdrawReport(defaultLomFilter()))
                .thenReturn(List.of());

        Sheet sheet = queryForSheet("/daas/reports/withdraws?dateFrom=2019-01-01&dateTo=2019-02-01&datasource_id=214");
        Assertions.assertEquals(4, sheet.getLastRowNum());

        verifyZeroInteractions(lmsClient);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("badRequestSource")
    public void badRequestAssert(@SuppressWarnings("unused") String caseName, String params, String jsonPath) {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/daas/reports/withdraws?" + params)
        );
        assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(), jsonPath);
    }

    @Nonnull
    private WithdrawReportFilter defaultLomFilter() {
        return WithdrawReportFilter.builder()
                .platformClientId(3L)
                .dateFrom(LocalDate.of(2019, 1, 1))
                .dateTo(LocalDate.of(2019, 1, 31))
                .marketIds(Set.of(10002L))
                .build();
    }

    @Nonnull
    private Address defaultAddress() {
        return Address.newBuilder()
                .locationId(213)
                .settlement("settlement 332")
                .postCode("332332")
                .latitude(BigDecimal.ZERO)
                .longitude(BigDecimal.TEN)
                .street("Street 332")
                .house("house 332")
                .housing("")
                .building("15")
                .apartment("3")
                .comment("comment 332")
                .region("Moscow")
                .addressString("address string 332")
                .shortAddressString("addr str 332")
                .build();
    }
}
