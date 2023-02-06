package ru.yandex.market.partner.mvc.controller.delivery;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.StatusDto;
import ru.yandex.market.logistics.lom.model.enums.BillingProductType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.filter.ServicesReportFilter;
import ru.yandex.market.logistics.lom.model.report.dto.ServicesReportDto;
import ru.yandex.market.logistics.lom.model.report.dto.ServicesReportOrderDto;
import ru.yandex.market.logistics.lom.model.report.dto.ServicesReportTransactionDto;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;
import ru.yandex.market.partner.mvc.controller.BaseReportControllerTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.market.personal_market.PersonalRetrieveResponse;
import ru.yandex.market.personal_market.client.model.CommonType;
import ru.yandex.market.personal_market.client.model.CommonTypeEnum;
import ru.yandex.market.personal_market.client.model.FullName;
import ru.yandex.market.personal_market.client.model.MultiTypeRetrieveResponseItem;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DaasServicesReportTest extends BaseReportControllerTest {

    @Autowired
    private PersonalMarketService personalMarketService;

    private static final long CONTRACT_1 = 142L;
    private static final long CONTRACT_2 = 143L;

    private static final long SHOP_1 = 214L;
    private static final long SHOP_2 = 215L;

    private static final long MARKET_ID = 10002L;

    private static final long SENDER_1 = 1L;
    private static final long SENDER_2 = 2L;
    private static final long SENDER_3 = 3L;
    private static final ZonedDateTime TEST_DATE = ZonedDateTime.of(2019, 1, 15, 12, 0, 0, 0, ZoneId.of("UTC"));

    private static final ShopWithSendersDto SHOP_WITH_SENDERS_1 = ShopWithSendersDto.builder()
            .id(SHOP_1)
            .balanceContractId(CONTRACT_1)
            .marketId(MARKET_ID)
            .name("Магазин Музей")
            .senders(List.of(NamedEntity.builder().id(SENDER_1).name("Кое-кто").build()))
            .build();

    private static final ShopWithSendersDto SHOP_WITH_SENDERS_2 = ShopWithSendersDto.builder()
            .id(SHOP_2)
            .balanceContractId(CONTRACT_2)
            .marketId(MARKET_ID)
            .name("Библиотека")
            .senders(List.of(
                    NamedEntity.builder().id(SENDER_2).name("Библиотекарь").build(),
                    NamedEntity.builder().id(SENDER_3).name("Библиотекарша").build()
            ))
            .build();

    private static final ServicesReportDto LOM_REPORT_DATA_1 = ServicesReportDto.builder()
            .order(defaultOrder().warehouseToAddress(null).build())
            .transactions(List.of(
                    defaultTx().build(),
                    defaultTx().productServiceType(ShipmentOption.DELIVERY)
                            .transactionDate(TEST_DATE.minusDays(14).minusHours(13)) //2018-12-31T23:00Z[UTC]
                            .transactionAmount(BigDecimal.valueOf(27))
                            .build(),
                    defaultTx().productServiceType(ShipmentOption.REPACK).transactionAmount(BigDecimal.valueOf(16)).build(),
                    defaultTx().productServiceType(ShipmentOption.SORT).transactionAmount(BigDecimal.valueOf(43)).build(),
                    defaultTx().productServiceType(ShipmentOption.WAIT_20).transactionAmount(BigDecimal.valueOf(24)).build(),
                    defaultTx().productServiceType(ShipmentOption.STORAGE).transactionAmount(BigDecimal.valueOf(19)).build(),
                    defaultTx().productServiceType(ShipmentOption.RETURN)
                            .transactionAmount(BigDecimal.valueOf(55))
                            .transactionDate(TEST_DATE.plusDays(23))
                            .build(),
                    defaultTx().productServiceType(ShipmentOption.RETURN_SORT)
                            .transactionAmount(BigDecimal.valueOf(28))
                            .transactionDate(TEST_DATE.plusDays(33))
                            .build()
            ))
            .build();

    /**
     * Забор СЦ, курьерская доставка.
     */
    private static final ServicesReportDto LOM_REPORT_DATA_2 = ServicesReportDto.builder()
            .order(
                    defaultOrder()
                            .orderId(23L)
                            .orderExternalId("ext02")
                            .barcode("ext02-LO23")
                            .senderId(2L)
                            .firstPartnerType(PartnerType.SORTING_CENTER)
                            .firstShipmentType(ShipmentType.WITHDRAW)
                            .pickupPointGeoId(null)
                            .recipientLocality("Не Москва")
                            .recipientLastName("Петров")
                            .personalFullNameId(null)
                            .build()
            )
            .transactions(List.of(
                    defaultTx().build(),
                    defaultTx().productServiceType(ShipmentOption.DELIVERY).transactionAmount(BigDecimal.valueOf(27)).build(),
                    defaultTx().productServiceType(ShipmentOption.REPACK).transactionAmount(BigDecimal.valueOf(16)).build(),
                    defaultTx().productServiceType(ShipmentOption.SORT).transactionAmount(BigDecimal.valueOf(43)).build(),
                    defaultTx().productServiceType(ShipmentOption.WAIT_20).transactionAmount(BigDecimal.valueOf(24)).build(),
                    defaultTx().productServiceType(ShipmentOption.STORAGE).transactionAmount(BigDecimal.valueOf(19)).build(),
                    defaultTx().productServiceType(ShipmentOption.RETURN)
                            .transactionAmount(BigDecimal.valueOf(55))
                            .transactionDate(TEST_DATE.plusDays(23))
                            .build(),
                    defaultTx().productServiceType(ShipmentOption.RETURN_SORT)
                            .transactionAmount(BigDecimal.valueOf(28))
                            .transactionDate(TEST_DATE.plusDays(33))
                            .build()
            ))
            .build();

    /**
     * Без дат и транзакций.
     */
    private static final ServicesReportDto LOM_REPORT_DATA_3 = ServicesReportDto.builder()
            .order(
                    defaultOrder()
                            .orderId(199L)
                            .orderCost(BigDecimal.valueOf(142L))
                            .senderId(3L)
                            .status(
                                    StatusDto.builder()
                                            .segmentStatus(SegmentStatus.INFO_RECEIVED)
                                            .partnerType(PartnerType.DELIVERY)
                                            .build()
                            )
                            .deliveryAtStartDate(null)
                            .deliveryDeliveredDate(null)
                            .deliveryLoadedDate(TEST_DATE)
                            .sortingCenterAtStartDate(null)
                            .sortingCenterReturnReturnedDate(null)
                            .sortingCenterReturnRffArrivedDate(null)
                            .sortingCenterReturnRffTransmittedDate(null)
                            .sortingCenterTransmittedDate(null)
                            .build()
            )
            .transactions(List.of())
            .build();

    /**
     * С корректировками.
     */
    private static final ServicesReportDto LOM_REPORT_DATA_4 = ServicesReportDto.builder()
            .order(defaultOrder().orderId(3324234L).senderId(3L).deliveryName("Partner 52").build())
            .transactions(List.of(
                    // исходная транзакция
                    defaultTx().transactionDate(TEST_DATE.plusDays(-35)).build(),
                    // до периода отчета
                    defaultTx().transactionDate(TEST_DATE.plusDays(-33)).transactionAmount(BigDecimal.valueOf(100L)).build(),
                    // в период отчета
                    defaultTx().transactionDate(TEST_DATE.plusDays(1)).transactionAmount(BigDecimal.valueOf(101L)).build(),
                    defaultTx().transactionDate(TEST_DATE.plusDays(2)).transactionAmount(BigDecimal.valueOf(102L)).build(),
                    defaultTx().transactionDate(TEST_DATE.plusDays(16)).transactionAmount(BigDecimal.valueOf(103L)).build(),
                    // после периода отчета
                    defaultTx().transactionDate(TEST_DATE.plusDays(24)).transactionAmount(BigDecimal.valueOf(104L)).build()
            ))
            .build();

    /**
     * Объединение услуг CHECK и WAIT_20.
     */
    private static final ServicesReportDto LOM_REPORT_DATA_5 = ServicesReportDto.builder()
            .order(defaultOrder().build())
            .transactions(List.of(
                    defaultTx().build(),
                    defaultTx().productServiceType(ShipmentOption.WAIT_20).transactionAmount(BigDecimal.valueOf(24)).build(),
                    defaultTx().productServiceType(ShipmentOption.CHECK).transactionAmount(BigDecimal.valueOf(42)).build()
            ))
            .build();

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private LomClient lomClient;

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
                Arguments.of("Без dateFrom", "dateTo=2019-02-02&datasource_id=774", "json/daas-report-no-date-from" +
                        ".json"),
                Arguments.of("Без dateTo", "dateFrom=2019-02-02&datasource_id=774", "json/daas-report-no-date-to.json")
        );
    }

    @Nonnull
    private static ServicesReportOrderDto.ServicesReportOrderDtoBuilder defaultOrder() {
        return ServicesReportOrderDto.builder()
                .orderExternalId("ext01")
                .barcode("ext01-LO2")
                .assessedValue(BigDecimal.valueOf(42L))
                .status(
                        StatusDto.builder()
                                .segmentStatus(SegmentStatus.OUT)
                                .partnerType(PartnerType.DELIVERY)
                                .datetime(TEST_DATE.toInstant())
                                .build()
                )
                .deliveryAtStartDate(TEST_DATE.plusDays(1))
                .deliveryDeliveredDate(TEST_DATE.plusDays(2))
                .deliveryLoadedDate(TEST_DATE.plusDays(3))
                .deliveryName("Partner 51")
                .firstPartnerType(PartnerType.DELIVERY)
                .firstShipmentType(ShipmentType.IMPORT)
                .orderCost(BigDecimal.valueOf(11002))
                .orderId(2L)
                .orderStatus(OrderStatus.FINISHED)
                .pickupPointGeoId(213)
                .platformClientId(3L)
                .warehouseFromAddress(getAddressDtoBuilder().build())
                .warehouseToAddress(
                        getAddressDtoBuilder()
                                .subRegion(null)
                                .housing(" ")
                                .room("   ")
                                .build()
                )
                .recipientFirstName("И")
                .recipientGeoId(213)
                .recipientLastName("Иванов")
                .recipientLocality("Москва")
                .recipientMiddleName("И")
                .recipientRegion("Москва")
                .personalFullNameId("personalFullNameId")
                .senderId(1L)
                .senderName("sender 1")
                .sortingCenterAtStartDate(TEST_DATE.plusDays(10))
                .sortingCenterReturnReturnedDate(TEST_DATE.plusDays(11))
                .sortingCenterReturnRffArrivedDate(TEST_DATE.plusDays(12))
                .sortingCenterReturnRffTransmittedDate(TEST_DATE.plusDays(13))
                .sortingCenterTransmittedDate(TEST_DATE.plusDays(14))
                .totalWeight(new BigDecimal("4.0000"));
    }

    @Nonnull
    private static AddressDto.AddressDtoBuilder getAddressDtoBuilder() {
        return AddressDto.builder()
                .country("Россия")
                .federalDistrict("Сибирский федеральный округ")
                .region("Новосибирская область")
                .locality("Новосибирск")
                .subRegion("Городской округ Новосибирск")
                .settlement("Новосибирск")
                .street("Николаева")
                .house("11")
                .building("1")
                .housing("2")
                .room("100")
                .zipCode("123321")
                .porch("3")
                .floor(4)
                .metro("Речной вокзал")
                .latitude(new BigDecimal("55.49"))
                .longitude(new BigDecimal("38.17"))
                .geoId(65)
                .intercom("123");
    }

    @Nonnull
    private static ServicesReportTransactionDto.ServicesReportTransactionDtoBuilder defaultTx() {
        return ServicesReportTransactionDto.builder()
                .productServiceType(ShipmentOption.INSURANCE)
                .productType(BillingProductType.SERVICE)
                .transactionAmount(BigDecimal.valueOf(-209))
                .transactionDate(TEST_DATE)
                .transactionId(201L);
    }

    /**
     * Тестирование генерации отчета "Услуги".
     */
    @Test
    void generateReportTest() {
        var personalName = new MultiTypeRetrieveResponseItem();
        personalName.setType(CommonTypeEnum.FULL_NAME);
        personalName.setId("personalFullNameId");

        var value = new CommonType();
        var name = new FullName();
        name.setForename("И");
        name.setSurname("Иванов");
        name.setPatronymic("И");
        value.setFullName(name);
        personalName.setValue(value);

        when(personalMarketService.retrieve(any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new PersonalRetrieveResponse(
                                List.of(
                                        personalName
                                )
                        )));

        when(nesuClient.searchShopWithSenders(ShopWithSendersFilter.builder().shopIds(Set.of(SHOP_1)).build()))
                .thenReturn(List.of(SHOP_WITH_SENDERS_1));

        when(nesuClient.searchShopWithSenders(ShopWithSendersFilter.builder().marketIds(Set.of(MARKET_ID)).build()))
                .thenReturn(List.of(SHOP_WITH_SENDERS_1, SHOP_WITH_SENDERS_2));

        when(lomClient.getServicesReport(
                ServicesReportFilter.builder()
                        .platformClientId(3L)
                        .dateFrom(ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, DateTimes.MOSCOW_TIME_ZONE))
                        .dateTo(ZonedDateTime.of(2019, 1, 31, 23, 59, 59, 999999999, DateTimes.MOSCOW_TIME_ZONE))
                        .senderIds(Set.of(SENDER_1, SENDER_2, SENDER_3))
                        .build()
        )).thenReturn(List
                .of(LOM_REPORT_DATA_1, LOM_REPORT_DATA_2, LOM_REPORT_DATA_3, LOM_REPORT_DATA_4, LOM_REPORT_DATA_5));

        Sheet sheet = queryForSheet("/daas/reports/services?dateFrom=2019-01-01&dateTo=2019-01-31&datasource_id=214");

        rowAssert(sheet.getRow(5), List.of("Кое-кто", "Магазин Музей", "Иванов И И", "ext01-LO2", "ext01",
                "Самопривоз - СД", "Partner 51", "Доставлен", "18.01.2019",
                "25.01.2019", "29.01.2019", "16.01.2019", "17.01.2019",
                "123321, Новосибирская область, Городской округ Новосибирск, Новосибирск, Николаева, 11, 1, 2, 100",
                "",
                "Москва", "28.01.2019", "27.01.2019",
                "26.01.2019", 11002.0D, 42.0D, 4.0D,
                -27.0D, "", 0D, "январь 2019", -16.0D,
                "", 0D, "январь 2019", -43.0D, "",
                0D, "январь 2019", -24.0D, "", 0D,
                "январь 2019", -19.0D, "", 0D,
                "январь 2019", 0D, "", 0D, "-",
                0D, "", 0D, "-",
                209.0D, "", 0D, "январь 2019", 80.0D
        ));

        rowAssert(sheet.getRow(6), List.of("Библиотекарь", "Библиотека", "Петров И И", "ext02-LO23", "ext02",
                "Забор - СЦ", "Partner 51", "Доставлен", "18.01.2019",
                "25.01.2019", "29.01.2019", "16.01.2019", "17.01.2019",
                "123321, Новосибирская область, Городской округ Новосибирск, Новосибирск, Николаева, 11, 1, 2, 100",
                "123321, Новосибирская область, Новосибирск, Николаева, 11, 1",
                "Не Москва", "28.01.2019", "27.01.2019",
                "26.01.2019", 11002.0D, 42.0D, 4.0D,
                -27.0D, "", 0D, "январь 2019", -16.0D,
                "", 0D, "январь 2019", -43.0D, "",
                0D, "январь 2019", -24.0D, "", 0D,
                "январь 2019", -19.0D, "", 0D,
                "январь 2019", 0D, "", 0D, "-",
                0D, "", 0D, "-",
                209.0D, "", 0D, "январь 2019", 80.0D
        ));

        rowAssert(sheet.getRow(7), List.of("Библиотекарша", "Библиотека", "Иванов И И", "ext01-LO2", "ext01",
                "Самопривоз - СД", "Partner 51", "Неизвестен", "15.01.2019",
                "", "", "", "",
                "123321, Новосибирская область, Городской округ Новосибирск, Новосибирск, Николаева, 11, 1, 2, 100",
                "123321, Новосибирская область, Новосибирск, Николаева, 11, 1",
                "Москва", "", "",
                "", 142.0D, 42.0D, 4.0D,
                0D, "", 0D, "-", 0D,
                "", 0D, "-", 0D, "",
                0D, "-", 0D, "", 0D,
                "-", 0D, "", 0D,
                "-", 0D, "", 0D, "-",
                0D, "", 0D, "-", 0D,
                "", 0D, "-", 0D
        ));

        rowAssert(sheet.getRow(8), List.of("Библиотекарша", "Библиотека", "Иванов И И", "ext01-LO2", "ext01",
                "Самопривоз - СД", "Partner 52", "Доставлен", "18.01.2019",
                "25.01.2019", "29.01.2019", "16.01.2019", "17.01.2019",
                "123321, Новосибирская область, Городской округ Новосибирск, Новосибирск, Николаева, 11, 1, 2, 100",
                "123321, Новосибирская область, Новосибирск, Николаева, 11, 1",
                "Москва", "28.01.2019", "27.01.2019",
                "26.01.2019", 11002.0D, 42.0D, 4.0D,
                0D, "", 0D, "-", 0D,
                "", 0D, "-", 0D, "",
                0D, "-", 0D, "", 0D, "-",
                0D, "", 0D, "-",
                0D, "", 0D, "-", 0D,
                "", 0D, "-", 0D, "2, 3, 4",
                -306.0D, "декабрь 2018", -306.0D
        ));

        rowAssert(sheet.getRow(9), List.of("Кое-кто", "Магазин Музей", "Иванов И И", "ext01-LO2", "ext01",
                "Самопривоз - СД", "Partner 51", "Доставлен", "18.01.2019",
                "25.01.2019", "29.01.2019", "16.01.2019", "17.01.2019",
                "123321, Новосибирская область, Городской округ Новосибирск, Новосибирск, Николаева, 11, 1, 2, 100",
                "123321, Новосибирская область, Новосибирск, Николаева, 11, 1",
                "Москва", "28.01.2019", "27.01.2019",
                "26.01.2019", 11002.0D, 42.0D, 4.0D,
                0D, "", 0D, "-", 0D,
                "", 0D, "-", 0D, "",
                0D, "-", -66.0D, "", 0D,
                "январь 2019", 0D, "", 0D, "-",
                0D, "", 0D, "-", 0D,
                "", 0D, "-", 209.0D, "",
                0D, "январь 2019", 143.0D
        ));
    }

    private void rowAssert(Row row, List<Object> cellValues) {
        int cellNumber = 0;
        for (Object cellValue : cellValues) {
            Cell cell = row.getCell(cellNumber);

            CellType cellType = cell.getCellType();
            if (cellType == CellType.STRING) {
                assertThat(cell.getStringCellValue(), is(cellValue));
            } else if (cellType == CellType.NUMERIC) {
                assertThat(cell.getNumericCellValue(), is(cellValue));
            } else if (cellType == CellType.BLANK) {
                assertTrue(
                        Objects.equals(cell.getStringCellValue(), cellValue)
                                || Objects.equals(cell.getNumericCellValue(), cellValue)
                );
            } else {
                throw new RuntimeException(String.format(
                        "Cell number %d has unsupported type %s",
                        cellNumber,
                        cellType
                ));
            }

            cellNumber++;
        }
    }

    /**
     * Тестирование генерации отчета "Услуги". Пустой отчет.
     */
    @Test
    void generateEmptyReportTest() {
        when(nesuClient.searchShopWithSenders(ShopWithSendersFilter.builder().shopIds(Set.of(SHOP_1)).build()))
                .thenReturn(List.of());

        Sheet sheet = queryForSheet("/daas/reports/services?dateFrom=2019-01-01&dateTo=2019-02-01&datasource_id=214");
        Assertions.assertEquals(4, sheet.getLastRowNum());

        verifyZeroInteractions(lomClient);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("badRequestSource")
    public void badRequestAssert(@SuppressWarnings("unused") String caseName, String params, String jsonPath) {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/daas/reports/services?" + params)
        );
        assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(), jsonPath);
    }
}
