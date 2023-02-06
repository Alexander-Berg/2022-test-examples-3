package ru.yandex.market.core.fulfillment.calculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.billing.commission.DbSupplierCategoryFeeDao;
import ru.yandex.market.core.billing.commission.InternalFeeServiceType;
import ru.yandex.market.core.billing.commission.SupplierCategoryFee;
import ru.yandex.market.core.datacamp.DataCampUtil;
import ru.yandex.market.core.date.Period;
import ru.yandex.market.core.fulfillment.FulfillmentTariffDao;
import ru.yandex.market.core.fulfillment.OrderType;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.model.BillingUnit;
import ru.yandex.market.core.fulfillment.model.FulfillmentTariff;
import ru.yandex.market.core.fulfillment.model.TariffValue;
import ru.yandex.market.core.fulfillment.model.ValueType;
import ru.yandex.market.core.offer.mapping.OfferConversionService;
import ru.yandex.market.core.order.model.MbiBlueOrderType;
import ru.yandex.market.fulfillment.entities.base.DateTimeInterval;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.api.billing.client.model.CurrentAndNextMonthPayoutFrequencyDTO;
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.fulfillment.model.TariffValueMatcher.hasBillingUnit;
import static ru.yandex.market.core.fulfillment.model.TariffValueMatcher.hasValue;
import static ru.yandex.market.core.fulfillment.model.TariffValueMatcher.hasValueType;

/**
 * Тесты для {@link TarifficatorService}
 */
@DbUnitDataSet(before = "db/TariffCalculatorServiceTest.general.csv")
class TarifficatorServiceTest extends FunctionalTest {

    private static final LocalDate TARGET_DATE_2019_10_10 = LocalDate.of(2019, 10, 10);

    private static final long SUPPLIER_ID = 10L;
    private static final long BUSINESS_ID = 1002L;
    private static final int WAREHOUSE_ID = 146;
    private static final String SHOP_SKU = "ssku10";
    private static final long CATEGORY_ID = 1L;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private TarifficatorService tarifficatorService;

    @Autowired
    private DbSupplierCategoryFeeDao dbSupplierCategoryFeeDao;

    @Autowired
    private FulfillmentTariffDao fulfillmentTariffDao;

    @Autowired
    private MbiBillingClient mbiBillingClient;

    private OffsetDateTime createOffsetDateTime(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private FulfillmentTariff createTariff(
            LocalDate from,
            LocalDate to,
            BillingServiceType serviceType,
            Long priceTo,
            Long dimensionsTo,
            Long weightTo,
            int value,
            ValueType valueType,
            BillingUnit billingUnit,
            OrderType orderType) {
        return new FulfillmentTariff(
                new DateTimeInterval(
                        createOffsetDateTime(from),
                        createOffsetDateTime(to)
                ),
                serviceType,
                priceTo,
                dimensionsTo,
                weightTo,
                value,
                null,
                null,
                valueType,
                billingUnit,
                orderType,
                null,
                null,
                null
        );
    }

    private Instant createInstant(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Pair<SupplierCategoryFee, MbiBlueOrderType> createSupplierCategoryFee(
            long hyperId,
            Long supplierId,
            int value,
            LocalDate from,
            LocalDate to,
            MbiBlueOrderType orderType) {
        return new Pair<>(new SupplierCategoryFee(
                hyperId,
                supplierId,
                value,
                new ru.yandex.market.core.date.Period(createInstant(from), createInstant(to)),
                BillingServiceType.FEE
        ), orderType);
    }

    private Pair<SupplierCategoryFee, MbiBlueOrderType> createCancelledOrderFee(
            Long supplierId,
            int value,
            MbiBlueOrderType orderType) {
        return new Pair<>(new SupplierCategoryFee(
                InternalFeeServiceType.CANCELLED_ORDER_FEE_CATEGORY_VALUE,
                supplierId,
                value,
                new ru.yandex.market.core.date.Period(createInstant(
                        LocalDate.of(2018, 11, 1)),
                        createInstant(LocalDate.MAX)
                ),
                BillingServiceType.CANCELLED_ORDER_FEE
        ), orderType);
    }

    private void mock(
            List<Pair<SupplierCategoryFee, MbiBlueOrderType>> feeList,
            List<Pair<SupplierCategoryFee, MbiBlueOrderType>> cancelledOrderFeeList,
            List<FulfillmentTariff> tariffList,
            List<Pair<SupplierCategoryFee, MbiBlueOrderType>> minFeeList) {
        Mockito.doAnswer(getFees(feeList)).when(dbSupplierCategoryFeeDao).getFee(Mockito.any(), Mockito.any());
        Mockito.doAnswer(getFees(feeList)).when(dbSupplierCategoryFeeDao).getCashOnly(Mockito.any(), Mockito.any());
        Mockito.doAnswer(getFees(cancelledOrderFeeList)).when(dbSupplierCategoryFeeDao)
                .getCancelledOrderFee(Mockito.any(), Mockito.any());
        Mockito.doReturn(tariffList).when(fulfillmentTariffDao).getOrderedTariffs(Mockito.any());
        Mockito.doAnswer(getFees(minFeeList)).when(dbSupplierCategoryFeeDao).getMinFee(Mockito.any(), Mockito.any());
    }

    private Answer<List<SupplierCategoryFee>> getFees(List<Pair<SupplierCategoryFee, MbiBlueOrderType>> feeList) {
        return invocation -> {
            Instant date = createInstant(invocation.getArgument(0));
            MbiBlueOrderType orderType = invocation.getArgument(1);
            return feeList.stream().filter(fee -> {
                Period period = fee.getFirst().getPeriod();
                Instant from = period.getFrom();
                Instant to = period.getTo();
                return fee.getSecond() == orderType && from.compareTo(date) <= 0 && date.compareTo(to) < 0;
            }).map(fee -> fee.getFirst()).collect(Collectors.toList());
        };
    }

    private void mock(List<Pair<SupplierCategoryFee, MbiBlueOrderType>> list,
                      List<Pair<SupplierCategoryFee, MbiBlueOrderType>> cancelledOrderFeeList) {
        mock(list, cancelledOrderFeeList, List.of(), List.of());
    }

    private void mockTariff(List<FulfillmentTariff> list) {
        mock(List.of(), List.of(), list, List.of());
    }

    private void mock() {
        mock(List.of(), List.of(), List.of(), List.of());
    }

    @BeforeEach
    void setUp() {
        tarifficatorService.invalidateCache();

        CurrentAndNextMonthPayoutFrequencyDTO frequency = new CurrentAndNextMonthPayoutFrequencyDTO();
        frequency.setContractId(1L);
        frequency.setCurrentMonthFrequency(PayoutFrequencyDTO.DAILY);
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(anyList())).thenReturn(List.of(frequency));
    }

    @Test
    @DisplayName("Тест на получение тарифа по размещению товарных предложений")
    @DbUnitDataSet(before = "db/TariffCalculatorServiceTest.getFee.csv")
    void testGetFee() {
        mock(List.of(
                createSupplierCategoryFee(
                        1,
                        10L,
                        50,
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX.minusDays(1),
                        MbiBlueOrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        1,
                        null,
                        150,
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX.minusDays(1),
                        MbiBlueOrderType.DROP_SHIP
                ),
                createSupplierCategoryFee(
                        1,
                        10L,
                        999,
                        LocalDate.MAX.minusDays(1),
                        LocalDate.MAX,
                        MbiBlueOrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        1,
                        10L,
                        888,
                        LocalDate.MAX.minusDays(1),
                        LocalDate.MAX,
                        MbiBlueOrderType.DROP_SHIP
                )
        ),
                List.of(
                        createCancelledOrderFee(
                                10L,
                                3500,
                                MbiBlueOrderType.FULFILLMENT
                        ),
                        createCancelledOrderFee(
                                null,
                                3700,
                                MbiBlueOrderType.DROP_SHIP
                        )
                ));

        TariffValue tariff = tarifficatorService.getFee(
                SUPPLIER_ID,
                CATEGORY_ID,
                MbiBlueOrderType.FULFILLMENT
        );
        assertThat(tariff, allOf(
                hasValue(50),
                hasValueType(ValueType.RELATIVE)
        ));
    }

    @Test
    @DisplayName("Тест на получение тарифа по размещению товарных предложений из родительской категории")
    @DbUnitDataSet(before = "db/TariffCalculatorServiceTest.getFeeFromParent.csv")
    void testGetFeeFromParent() {
        mock(List.of(
                createSupplierCategoryFee(
                        90401,
                        10L,
                        200,
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX,
                        MbiBlueOrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        90402,
                        null,
                        400,
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX,
                        MbiBlueOrderType.DROP_SHIP
                )
        ),
                List.of(
                        createCancelledOrderFee(
                                10L,
                                3500,
                                MbiBlueOrderType.FULFILLMENT
                                ),
                        createCancelledOrderFee(
                                null,
                                3700,
                                MbiBlueOrderType.DROP_SHIP
                                )
                ));

        TariffValue tariff = tarifficatorService.getFee(
                SUPPLIER_ID,
                CATEGORY_ID,
                MbiBlueOrderType.DROP_SHIP
        );

        assertThat(tariff, allOf(
                hasValue(400),
                hasValueType(ValueType.RELATIVE)
        ));
    }

    @Test
    @DisplayName("Тест, что получение тарифа по размещению товарных предложений вернет null, если тарифов не найдется")
    @DbUnitDataSet(before = "db/TariffCalculatorServiceTest.categories.csv")
    void testGetShopSkuFailed() {
        mock();

        TariffValue tariff = tarifficatorService.getFee(
                SUPPLIER_ID,
                CATEGORY_ID,
                MbiBlueOrderType.FULFILLMENT
        );
        assertThat(tariff, nullValue());
    }

    @Test
    @DisplayName("Тест на получение тарифа по агентскому вознаграждению")
    @DbUnitDataSet(before = "db/TarifficatorServiceText.agencyCommission.csv")
    void testGetAgencyCommission() {
        var tariffs = tarifficatorService.getAgencyCommissions(SUPPLIER_ID);
        assertThat(tariffs, hasSize(1));
        assertThat(tariffs.get(0), allOf(
                hasValue(175),
                hasValueType(ValueType.RELATIVE)
        ));
    }

    @Test
    @DisplayName("Тест на получение тарифа по складской обработке для ФФ поставщика")
    void testGetFfProcessingTariff() {
        mockTariff(List.of(
                createTariff(
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX.minusDays(1),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        100,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX.minusDays(1),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        200,
                        ValueType.RELATIVE,
                        BillingUnit.SUPPLY_PALLET,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.MAX.minusDays(1),
                        LocalDate.MAX,
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        999,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                )
        ));

        var tariffs = tarifficatorService.getFfProcessing(
                tarifficatorService.getShopSkuInfo(SUPPLIER_ID, SHOP_SKU),
                null,
                TARGET_DATE_2019_10_10,
                OrderType.FULFILLMENT,
                SUPPLIER_ID
        );
        assertThat(tariffs, hasSize(2));
        assertThat(tariffs, contains(
                allOf(hasValue(100), hasBillingUnit(BillingUnit.ITEM), hasValueType(ValueType.ABSOLUTE)),
                allOf(hasValue(200), hasBillingUnit(BillingUnit.SUPPLY_PALLET), hasValueType(ValueType.RELATIVE))
        ));
    }

    @Test
    @DisplayName("Тест на получение тарифа по складской обработке для ФФ+кросдок поставщика")
    void testGetFfProcessingCrossdockTariff() {
        mockTariff(List.of(
                createTariff(
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX.minusDays(1),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        200,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX.minusDays(1),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        100,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.CROSSDOCK
                ),
                createTariff(
                        LocalDate.MAX.minusDays(1),
                        LocalDate.MAX,
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        399,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.MAX.minusDays(1),
                        LocalDate.MAX,
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        499,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.CROSSDOCK
                )
        ));

        var tariffs = tarifficatorService.getFfProcessing(
                tarifficatorService.getShopSkuInfo(SUPPLIER_ID, SHOP_SKU),
                null,
                TARGET_DATE_2019_10_10,
                OrderType.CROSSDOCK,
                SUPPLIER_ID
        );
        assertThat(tariffs, hasSize(1));
        assertThat(tariffs.get(0), allOf(
                hasValue(100),
                hasValueType(ValueType.ABSOLUTE)
        ));
    }

    @Test
    @DisplayName("Тест на получение тарифа по известной цене по складской обработке для ФФ поставщика")
    void testGetFfProcessingWithPrice() {
        mockTariff(List.of(
                createTariff(
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX.minusDays(1),
                        BillingServiceType.FF_PROCESSING,
                        100L,
                        150L,
                        15000L,
                        100,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX.minusDays(1),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        200,
                        ValueType.RELATIVE,
                        BillingUnit.SUPPLY_PALLET,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.MAX.minusDays(1),
                        LocalDate.MAX,
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        999,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                )
        ));

        var tariffs = tarifficatorService.getFfProcessing(
                tarifficatorService.getShopSkuInfo(SUPPLIER_ID, SHOP_SKU),
                new BigDecimal(150),
                TARGET_DATE_2019_10_10,
                OrderType.FULFILLMENT,
                SUPPLIER_ID
        );
        assertThat(tariffs, hasSize(1));
        assertThat(tariffs, contains(
                allOf(hasValue(200), hasBillingUnit(BillingUnit.SUPPLY_PALLET), hasValueType(ValueType.RELATIVE))
        ));
    }

    @Test
    @DisplayName("Тест на получение тарифа по хранению")
    void testGetFfStorageBillingTariff() {
        mockTariff(List.of(
                createTariff(
                    LocalDate.of(2018, 1, 1),
                    LocalDate.MAX.minusDays(1),
                    BillingServiceType.FF_STORAGE_BILLING,
                    null,
                    150L,
                    15000L,
                    200,
                    ValueType.ABSOLUTE,
                    BillingUnit.SUPPLY_PALLET,
                    OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.MAX.minusDays(1),
                        LocalDate.MAX,
                        BillingServiceType.FF_STORAGE_BILLING,
                        null,
                        150L,
                        15000L,
                        999,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                )
        ));

        var tariff = tarifficatorService.getFfStorageBillingTariffMeta(
                tarifficatorService.getShopSkuInfo(SUPPLIER_ID, SHOP_SKU),
                TARGET_DATE_2019_10_10,
                SUPPLIER_ID
        );
        assertThat(tariff, allOf(
                hasValue(200),
                hasValueType(ValueType.ABSOLUTE)
        ));
    }

    @Test
    @DisplayName("Тест на получение тарифа по вывозу")
    void testGetFfWithdrawTariff() {
        mockTariff(List.of(
                createTariff(
                        LocalDate.of(2018, 1, 1),
                        LocalDate.MAX.minusDays(1),
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        250L,
                        15000L,
                        300,
                        ValueType.ABSOLUTE,
                        BillingUnit.SUPPLY_PALLET,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.MAX.minusDays(1),
                        LocalDate.MAX,
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        250L,
                        15000L,
                        999,
                        ValueType.ABSOLUTE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                )
        ));

        var tariff = tarifficatorService.getFfWithdraw(
                tarifficatorService.getShopSkuInfo(SUPPLIER_ID, SHOP_SKU),
                TARGET_DATE_2019_10_10
        );
        assertThat(tariff, allOf(
                hasValue(300),
                hasValueType(ValueType.ABSOLUTE)
        ));
    }

    @Test
    @DisplayName("Тест на получение тарифа по излишкам")
    void testGetFfSurplusSupplyTariff() {
        var tariff = tarifficatorService.getFfSurplusSupply(
                TARGET_DATE_2019_10_10
        );
        assertThat(tariff, allOf(
                hasValue(15000),
                hasValueType(ValueType.ABSOLUTE)
        ));
    }

    @Test
    @DisplayName("Тест на получение null, если тариф withdraw не будет найден")
    void testGetFFTariffNotFound() {
        mock();

        TariffValue ffWithdraw = tarifficatorService.getFfWithdraw(
                tarifficatorService.getShopSkuInfo(SUPPLIER_ID, SHOP_SKU),
                TARGET_DATE_2019_10_10
        );
        assertThat(ffWithdraw, nullValue());
    }

    @Test
    @DisplayName("Тест на получение категорийного пути")
    @DbUnitDataSet(before = "db/TariffCalculatorServiceTest.getCategoryPath.csv")
    void testGetCategoryPath() {
        String categoryPath = tarifficatorService.getCategoryPath(4L);
        assertThat(categoryPath, is("Все товары/Авто/Аудио и видеотехника/Автомагнитолы"));
    }

    @Test
    @DisplayName("Метод возвращает цену из поля basicPrice по офферу партнера без Единого каталога")
    @DbUnitDataSet(before = "db/TarifficatorServiceTest.getPriceStatusNo.before.csv")
    void testPriceExistsForNonUnitedCatalogPartner() {
        SyncGetOffer.GetOfferResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetOfferResponse.class,
                "json/DataCampResponse.testPriceExistsForNonUnitedCatalogPartner.json",
                this.getClass()
        );
        when(dataCampShopClient.getOffer(SUPPLIER_ID, SHOP_SKU, WAREHOUSE_ID, null))
                .thenReturn(mockedResponse);

        BigDecimal result = Objects.requireNonNull(tarifficatorService.getPrice(SUPPLIER_ID, SHOP_SKU));
        assertEquals(150L, result.longValue());
    }

    @Test
    @DisplayName("Если в оффере партнера без Единого каталога нет поля basicPrice, метод вернет null")
    @DbUnitDataSet(before = "db/TarifficatorServiceTest.getPriceStatusNo.before.csv")
    void testNoPriceForNonUnitedCatalogPartner() {
        when(dataCampShopClient.getOffer(SUPPLIER_ID, SHOP_SKU, WAREHOUSE_ID, null))
                .thenReturn(SyncGetOffer.GetOfferResponse.newBuilder().build());

        assertNull(tarifficatorService.getPrice(SUPPLIER_ID, SHOP_SKU));
    }

    @Test
    @DisplayName("Для партнера из ЕОХа берем basic-цену, как у для старых партнеров")
    @DbUnitDataSet(before = {
            "db/TarifficatorServiceTest.getPriceBusiness.before.csv",
            "db/TarifficatorServiceTest.getPriceStatusSuccess.before.csv"
    })
    void testServicePriceByWarehouseForUnitedCatalogPartner() {
        SyncGetOffer.GetUnitedOffersResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "json/DataCampResponse.testServicePriceByWarehouseForUnitedCatalogPartner.json",
                this.getClass()
        );
        when(dataCampShopClient.getBusinessUnitedOffer(BUSINESS_ID, List.of(SHOP_SKU), SUPPLIER_ID))
                .thenReturn(mockedResponse);

        DataCampOffer.Offer offer = DataCampUtil.mergeServiceOfferWithBasicOffer(mockedResponse.getOffers(0), SUPPLIER_ID);
        Optional<BigDecimal> result = OfferConversionService.toPrice(offer);
        Assertions.assertTrue(result.isPresent());
        assertEquals(150L, result.get().longValue());
    }

    @Test
    @DisplayName("Если в оффере партнера с Единым каталогом вернулась сервисная часть, " +
            "но в его цене пустое поле priceByWarehouse и в базовом оффере нет цены, метод вернет null")
    @DbUnitDataSet(before = {
            "db/TarifficatorServiceTest.getPriceBusiness.before.csv",
            "db/TarifficatorServiceTest.getPriceStatusSuccess.before.csv"
    })
    void testNoPriceByWarehouseForUnitedCatalogPartner() {
        SyncGetOffer.GetUnitedOffersResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "json/DataCampResponse.testNoPriceByWarehouseForUnitedCatalogPartner.json",
                this.getClass()
        );
        when(dataCampShopClient.getBusinessUnitedOffer(BUSINESS_ID, List.of(SHOP_SKU), SUPPLIER_ID))
                .thenReturn(mockedResponse);

        DataCampOffer.Offer offer = DataCampUtil.mergeServiceOfferWithBasicOffer(mockedResponse.getOffers(0), SUPPLIER_ID);
        assertFalse(OfferConversionService.toPrice(offer).isPresent());
    }
}
