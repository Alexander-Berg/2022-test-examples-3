package ru.yandex.market.billing.fulfillment.billing.storage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.order.model.ValueType;
import ru.yandex.market.billing.fulfillment.OrderType;
import ru.yandex.market.billing.fulfillment.SkuStockInfo;
import ru.yandex.market.billing.fulfillment.tariffs.FulfillmentTariff;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsIterator;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.imports.shopsku.ShopSkuInfo;
import ru.yandex.market.billing.imports.shopsku.SupplierShopSkuKey;
import ru.yandex.market.billing.imports.stocks.Stock;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.billing.order.model.BillingUnit;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.fulfillment.entities.base.DateTimeInterval;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.FfStorageBillingMultiplierTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Тесты для сервиса биллинга хранения {@link StorageBillingService}.
 */
@DbUnitDataSet(before = "StorageBillingServiceTest.categories.before.csv")
class StorageBillingServiceTest extends FunctionalTest {
    private static final LocalDate DATE_2019_01_30 = LocalDate.of(2019, Month.JANUARY, 30);
    private static final LocalDate DATE_2019_01_31 = LocalDate.of(2019, Month.JANUARY, 31);
    private static final LocalDate DATE_2020_08_07 = LocalDate.of(2020, Month.AUGUST, 7);
    private static final LocalDate DATE_2020_08_08 = LocalDate.of(2020, Month.AUGUST, 8);
    private static final LocalDate DATE_2021_03_25 = LocalDate.of(2021, Month.MARCH, 25);
    private static final LocalDate DATE_2021_10_02 = LocalDate.of(2021, Month.OCTOBER, 2);

    private static final LocalDate DATE_2021_09_12 = LocalDate.of(2021, Month.SEPTEMBER, 12);
    private static final LocalDate DATE_2021_09_15 = LocalDate.of(2021, Month.SEPTEMBER, 15);
    private static final LocalDate DATE_2021_09_22 = LocalDate.of(2021, Month.SEPTEMBER, 22);
    private static final LocalDate DATE_2021_11_22 = LocalDate.of(2021, Month.NOVEMBER, 22);
    private static final long SUPPLIER_ID = 33;

    @Autowired
    private StorageBillingService storageBillingService;

    @Autowired
    private StorageTariffFactory storageTariffFactory;

    @Autowired
    private TariffsService tariffsService;


    private static final List<FulfillmentTariff> DEFAULT_TARIFFS = List.of(
            createTariff(
                    125000L,
                    150L,
                    15000L,
                    40
            ),
            createTariff(
                    150L,
                    15000L,
                    40
            ),
            createTariff(
                    250L,
                    40000L,
                    500
            ),
            createTariff(
                    null,
                    null,
                    800
            )
    );

    private static final List<FulfillmentTariff> ONE_TARIFF = List.of(
            createTariff(
                    null,
                    null,
                    50
            )
    );

    private static final List<FulfillmentTariff> SMOKE_TEST_TARIFFS = List.of(
            createTariff(
                    125000L,
                    150L,
                    15000L,
                    40
            ),
            createTariff(
                    150L,
                    15000L,
                    40
            ),
            createTariff(
                    250L,
                    40000L,
                    500
            ),
            createTariff(
                    350L,
                    100000L,
                    500
            ),
            createTariff(
                    null,
                    null,
                    0
            )
    );

    private static OffsetDateTime createOffsetDateTime(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static FulfillmentTariff createTariff(
            Long priceTo,
            Long dimensionsTo,
            Long weightTo,
            int value) {
        return new FulfillmentTariff(
                new DateTimeInterval(
                        createOffsetDateTime(LocalDate.of(2018, 11, 1)),
                        createOffsetDateTime(LocalDate.MAX)
                ),
                BillingServiceType.FF_STORAGE_BILLING,
                priceTo,
                dimensionsTo,
                weightTo,
                value,
                null,
                null,
                ValueType.ABSOLUTE,
                BillingUnit.ITEM,
                OrderType.FULFILLMENT,
                null,
                null, null);
    }

    private static FulfillmentTariff createTariff(
            Long dimensionsTo,
            Long weightTo,
            int value) {
        return createTariff(null, dimensionsTo, weightTo, value);
    }


    private static Stream<Arguments> getWeightAndDimensions() {
        return Stream.of(
                Arguments.of(
                        "Should be a low tariff",
                        49, 50, 50, BigDecimal.valueOf(14), 40
                ),
                Arguments.of(
                        "Should be a high tariff (length >= 150)",
                        150, 1, 1, BigDecimal.valueOf(14), 500
                ),
                Arguments.of(
                        "Should be a high tariff (width >= 150)",
                        1, 150, 1, BigDecimal.valueOf(14), 500
                ),
                Arguments.of(
                        "Should be a high tariff (height >= 150)",
                        1, 1, 150, BigDecimal.valueOf(14), 500
                ),
                Arguments.of(
                        "Should be a highest tariff (length + width + height = 250 >= 150)",
                        100, 100, 50, BigDecimal.valueOf(14), 800
                ),
                Arguments.of(
                        "Should be a low tariff (weight <= 15)",
                        30, 30, 30, BigDecimal.valueOf(14), 40
                ),
                Arguments.of(
                        "Should be a highest tariff (dimension > 251 && weight < 40kg)",
                        100, 100, 51, BigDecimal.valueOf(39), 800
                ),
                Arguments.of(
                        "Should be a highest tariff (dimension < 251 && weight > 40kg)",
                        100, 100, 38, BigDecimal.valueOf(41), 800
                ),
                Arguments.of(
                        "Should be a highest tariff (length + width + height = 250 && weight = 40)",
                        100, 100, 50, BigDecimal.valueOf(40), 800
                ),
                Arguments.of(
                        "Huge dimensions and weight has no exceptions - billed with highest tariff",
                        Integer.MAX_VALUE, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 2,
                        new BigDecimal("2000000000.01"), 800
                )
        );
    }

    @Test
    @DisplayName("Ресчёт биллинга хранения")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.before.csv",
            after = "StorageBillingServiceTest.after.csv"
    )
    void test_storageBilling() {
        mock(DEFAULT_TARIFFS, storageBillingTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Обиливание при перемещении товарамежду складами")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.transfer.before.csv",
            after = "StorageBillingServiceTest.transfer.after.csv"
    )
    void test_storageBillingWarehouseTransfer() {
        mock(DEFAULT_TARIFFS, storageBillingTariffs());
        storageBillingService.process(DATE_2021_10_02);
    }

    @Test
    @DisplayName("Обиливание хранения после бесплатного периода")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.billAfterFreePeriod.before.csv",
            after = "StorageBillingServiceTest.billAfterFreePeriod.after.csv"
    )
    void test_storageBillingAfterFree() {
        mock(DEFAULT_TARIFFS, storageBillingTariffs());
        //последний день платного храненияя перед перемещением
        storageBillingService.process(DATE_2021_09_12);
        //день когда в перемещении
        storageBillingService.process(DATE_2021_09_15);
        //день когда прибыл на склад и начало бесплатного хренения
        storageBillingService.process(DATE_2021_09_22);
        //день платного хранения
        storageBillingService.process(DATE_2021_11_22);
    }

    @Test
    @DisplayName("Проверка фильтров поставок по статусам в расчете биллинга хранения")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.status_filters.before.csv",
            after = "StorageBillingServiceTest.status_filters.after.csv"
    )
    void test_storageBilling_statusFilters() {
        mock(DEFAULT_TARIFFS, storageBillingWithNullPartnerIdTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Кроссдок поставки не отфильтровываются")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.crossdock_supplies.before.csv",
            after = "StorageBillingServiceTest.crossdock_supplies.after.csv"
    )
    void test_storageBilling_crossDockSupplies() {
        mock(ONE_TARIFF, storageBillingWithNullPartnerIdTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Остатки по стокам только для ФФ складов")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.warehouse_filter.before.csv",
            after = "StorageBillingServiceTest.warehouse_filter.after.csv"
    )
    void test_storageBilling_filterWh() {
        mock(ONE_TARIFF, storageBillingWithNullPartnerIdTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Переобиливание поставок, у которых уже есть записи биллинга хранения")
    @DbUnitDataSet(
            before = {"StorageBillingServiceTest.before.csv", "StorageBillingServiceTest.existBilling.before.csv"},
            after = "StorageBillingServiceTest.existBilling.after.csv"
    )
    void test_rebuildStorageBilling() {
        mock(DEFAULT_TARIFFS, storageBillingTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Переобиливание поставок, у которых уже есть записи биллинга хранения")
    @DbUnitDataSet(
            before = {"StorageBillingServiceTest" +
                    ".test_rebuildStorageBillingMultipleFulfillmentSupplyItemsForOneShopSku.before.csv"},
            after = "StorageBillingServiceTest.test_rebuildStorageBillingMultipleFulfillmentSupplyItemsForOneShopSku" +
                    ".after.csv"
    )
    void test_rebuildStorageBillingMultipleFulfillmentSupplyItemsForOneShopSku() {
        mock(DEFAULT_TARIFFS, storageBillingWithNullPartnerIdTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Переобиливание поставок, только для некоторых поставщиков")
    @DbUnitDataSet(
            before = {"StorageBillingServiceTest.before.csv", "StorageBillingServiceTest.rebuildSupplier.before.csv"},
            after = "StorageBillingServiceTest.rebuildSupplier.after.csv"
    )
    void test_rebuildSupplierStorageBilling() {
        mock(DEFAULT_TARIFFS, storageBillingTariffs());
        storageBillingService.calculateStorageBilling(DATE_2019_01_30, Collections.singleton(SUPPLIER_ID));
    }

    @Test
    @DisplayName("Переобиливание поставок с одинаковым ключом")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.equalsKey.before.csv",
            after = "StorageBillingServiceTest.equalsKey.after.csv"
    )
    void test_equalsKeyStorageBilling() {
        mock(DEFAULT_TARIFFS, storageBillingWithNullPartnerIdTariffs());
        storageBillingService.calculateStorageBilling(DATE_2019_01_30, Collections.singleton(55L));
    }

    @Test
    @DisplayName("Переобиливание поставок с измененной поставкой")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.changeSupply.before.csv",
            after = "StorageBillingServiceTest.changeSupply.after.csv"
    )
    void test_ChangeSupplyStorageBilling() {
        mock(DEFAULT_TARIFFS, storageBillingWithNullPartnerIdTariffs());
        storageBillingService.calculateStorageBilling(DATE_2019_01_30, Collections.singleton(55L));
    }

    @ParameterizedTest
    @MethodSource("getWeightAndDimensions")
    @DisplayName("Проверка тарифов билинга хранения")
    @DbUnitDataSet(before = "StorageBillingServiceTest.before.csv")
    void test_getSkuTariff(
            String message,
            int length,
            int width,
            int height,
            BigDecimal weight,
            int expectedTariff
    ) {
        mock(DEFAULT_TARIFFS, storageBillingTariffs());
        int actualTariff = storageBillingService.getSkuTariff(
                new SkuStockInfo(
                        getDummyStock(),
                        getShopSkuInfo(length, width, height, weight)
                ),
                DATE_2019_01_30,
                storageTariffFactory.buildStorageTariffService(DATE_2019_01_30)
        );

        Assertions.assertEquals(
                expectedTariff,
                actualTariff,
                message
        );
    }

    @Test
    @DisplayName("Падать если отсутствуют данные о стоках")
    @DbUnitDataSet(before = "StorageBillingServiceTest.before.csv")
    void test_failIfNoStocks() {
        RuntimeException thrown = Assertions.assertThrows(
                RuntimeException.class,
                () -> storageBillingService.process(DATE_2019_01_31)
        );
        Assertions.assertEquals("No stocks for 2019-01-31", thrown.getMessage());
    }

    @Test
    @DisplayName("Падать, если отсутствуют данные о поставках")
    @DbUnitDataSet(before = "StorageBillingServiceTest.test_failIfNoImportedDailySupplies.before.csv")
    void test_failIfNoImportedDailySupplies() {
        RuntimeException thrown = Assertions.assertThrows(
                RuntimeException.class,
                () -> storageBillingService.process(DATE_2019_01_31)
        );
        Assertions.assertEquals("Daily supplies for 2019-01-31 were not imported", thrown.getMessage());
    }

    @Test
    @DisplayName("Падать, если отсутствуют данные о стоках")
    @DbUnitDataSet(before = "StorageBillingServiceTest.test_failIfNoImportedDailyStocks.before.csv")
    void test_failIfNoImportedDailyStocks() {
        RuntimeException thrown = Assertions.assertThrows(
                RuntimeException.class,
                () -> storageBillingService.process(DATE_2019_01_31)
        );
        Assertions.assertEquals("Daily stocks for 2019-01-31 were not imported", thrown.getMessage());
    }

    @Test
    @DisplayName("Smoketest на расчет бесплатного интервала хранения")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.freeStorageTariff.before.csv",
            after = "StorageBillingServiceTest.freeStorageTariff.after.csv"
    )
    void test_freeStorage() {
        mock(SMOKE_TEST_TARIFFS, freeStorageWithNullPartnerIdTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Smoketest на расчет коэффициента тарифа хранения")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.multiplierTariff.before.csv",
            after = "StorageBillingServiceTest.multiplierTariff.after.csv"
    )
    void test_multiplierTariff() {
        mock(SMOKE_TEST_TARIFFS, storageBillingMultiplierTariff());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Тест на то, что используется поле count вместо factCount, " +
            "когда поставка находится в статусе IN_PROGRESS")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.use_count.before.csv",
            after = "StorageBillingServiceTest.use_count.after.csv"
    )
    void test_useCountInsteadOfFactCount() {
        mock(ONE_TARIFF, storageBillingWithNullPartnerIdTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Тест на учет брака в определенные даты")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.test_defect.before.csv",
            after = "StorageBillingServiceTest.test_defect.after.csv"
    )
    void test_defects() {
        mock(ONE_TARIFF, storageBillingWithNullPartnerIdTariffs());
        storageBillingService.process(DATE_2020_08_07);
        storageBillingService.process(DATE_2020_08_08);
    }

    @Test
    @DisplayName("Ресчёт биллинга хранения для поставщиков с промо тарифами")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.promo.before.csv",
            after = "StorageBillingServiceTest.promo.after.csv"
    )
    void test_storageBillingWithPromo() {
        mock(DEFAULT_TARIFFS, storageBillingWithPromoTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    @Test
    @DisplayName("Не учитывать утилизацию в биллинге хранения")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.billableSupplies.before.csv",
            after = "StorageBillingServiceTest.billableSupplies.after.csv"
    )
    void test_storageBilling_billableSupplies() {
        mock(DEFAULT_TARIFFS, storageBillingWithNullPartnerIdTariffs());
        storageBillingService.process(DATE_2021_03_25);
    }

    @Test
    @DisplayName("Проверка того, что нулевые строчки(available = 0, defect = 0, expired = 0) из SHOPS_WEB.STOCKS_NEW" +
            "не учитываются при биллинге стоков и по ним не выставляется счет")
    @DbUnitDataSet(
            before = "StorageBillingServiceTest.nullSuppliesNonBilling.before.csv",
            after = "StorageBillingServiceTest.nullSuppliesNonBilling.after.csv"
    )
    void test_storageBilling_nullSuppliesNonBilling() {
        mockTariffs(storageBillingWithNullPartnerIdTariffs());
        storageBillingService.process(DATE_2019_01_30);
    }

    private ShopSkuInfo getShopSkuInfo(int length, int width, int height, BigDecimal weight) {
        return new ShopSkuInfo()
                .withSupplierId(1)
                .withShopSku("someSku")
                .withLength(length)
                .withWidth(width)
                .withHeight(height)
                .withWeight(weight);
    }

    private Stock<SupplierShopSkuKey> getDummyStock() {
        return Stock.Builder.newBuilder()
                .setKey(new SupplierShopSkuKey(1, "dummy"))
                .setDateTime(LocalDateTime.now())
                .setDefect(0)
                .setWarehouseId(0)
                .setExpired(0)
                .setFreeze(0)
                .setAvailable(0)
                .setQuarantine(0)
                .build();
    }


    private void mockTariffs(List<TariffDTO> tariffDTOList) {
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }

                return tariffDTOList
                        .stream()
                        .filter(tariff -> tariff.getServiceType() == ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER)
                        .filter(tariff -> {
                            LocalDate to = tariff.getDateTo();
                            LocalDate from = tariff.getDateFrom();
                            return targetDate.compareTo(from) >= 0
                                    && (to == null || targetDate.compareTo(to) < 0);
                        })
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(any(TariffFindQuery.class));
    }

    private void mockOrderedTariffs(List<FulfillmentTariff> list) {
        Mockito.when(tariffsService.getOrderedTariffs(Mockito.any())).thenReturn(list);
    }


    private void mock(List<FulfillmentTariff> list, List<TariffDTO> tariffDTOList) {
        mockTariffs(tariffDTOList);
        mockOrderedTariffs(list);
    }

    private TariffDTO createTariff(long id, Long partnerId, LocalDate from, LocalDate to, List<Object> meta) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setPartner(new Partner().id(partnerId).type(PartnerType.SUPPLIER));
        tariff.setIsActive(true);
        tariff.setServiceType(ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER);
        tariff.setDateFrom(from);
        tariff.setDateTo(to);
        tariff.setMeta(meta);
        return tariff;
    }

    private CommonJsonSchema createMeta(
            Long categoryId,
            Long daysOnStockFrom,
            Long daysOnStockTo,
            BigDecimal multiplier
    ) {
        return new FfStorageBillingMultiplierTariffJsonSchema()
                .categoryId(categoryId)
                .daysOnStockFrom(daysOnStockFrom)
                .daysOnStockTo(daysOnStockTo)
                .multiplier(multiplier);
    }

    private List<TariffDTO> getMultiplierTariffsSupplierCheck() {
        return List.of(
                createTariff(1L, null, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), List.of(
                        createMeta(90401L, 0L, null, new BigDecimal("1.1"))
                )),
                createTariff(2L, 1L, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), List.of(
                        createMeta(90401L, 0L, 100L, new BigDecimal("2.1"))
                )),
                createTariff(3L, 2L, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), List.of(
                        createMeta(3L, 0L, 100L, new BigDecimal("3.1"))
                )),
                createTariff(4L, 4L, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), List.of(
                        createMeta(90401L, 100L, null, new BigDecimal("4.1"))
                ))
        );
    }

    private List<TariffDTO> getMultiplierTariffsHierarchyCheck() {
        return List.of(
                createTariff(1L, null, LocalDate.parse("2000-01-01"), LocalDate.parse("2100-01-01"), List.of(
                        createMeta(90401L, 0L, 100L, new BigDecimal("1.1")),
                        createMeta(90401L, 100L, 200L, new BigDecimal("2.1")),
                        createMeta(3L, 100L, 120L, new BigDecimal("2.2")),
                        createMeta(33L, 100L, 150L, new BigDecimal("2.3"))
                )),
                createTariff(2L, 1L, LocalDate.parse("2000-01-01"), LocalDate.parse("2017-01-01"), List.of(
                        createMeta(3L, 0L, 100L, new BigDecimal("1.2"))
                )),
                createTariff(3L, 2L, LocalDate.parse("2000-01-01"), LocalDate.parse("2019-01-01"), List.of(
                        createMeta(33L, 0L, 100L, new BigDecimal("1.3"))
                ))
        );
    }

    private List<TariffDTO> getMultiplierTariffsLadderCheck() {
        return List.of(
                //одна категория, один период, разные интервалы дней
                createTariff(1L, null, LocalDate.parse("2018-01-01"), LocalDate.parse("2018-05-15"), List.of(
                        createMeta(22L, 0L, 50L, new BigDecimal("1.1")),
                        createMeta(22L, 50L, 100L, new BigDecimal("1.2")),
                        createMeta(22L, 100L, null, new BigDecimal("1.3"))
                )),
                //одна категория разные периоды с одним интервалом дней
                createTariff(2L, null, LocalDate.parse("2019-01-01"), LocalDate.parse("2019-05-15"), List.of(
                        createMeta(22L, 200L, 250L, new BigDecimal("2.1"))
                )),
                createTariff(3L, null, LocalDate.parse("2019-05-15"), LocalDate.parse("2019-10-15"), List.of(
                        createMeta(22L, 200L, 250L, new BigDecimal("2.2"))
                )),
                createTariff(4L, null, LocalDate.parse("2019-10-15"), null, List.of(
                        createMeta(22L, 200L, 250L, new BigDecimal("2.3"))
                )),
                //одна категория смешанные периоды и смешанные интервалы
                createTariff(5L, null, LocalDate.parse("2017-01-01"), LocalDate.parse("2017-05-15"), List.of(
                        createMeta(22L, 30L, 50L, new BigDecimal("3"))
                )),
                createTariff(6L, null, LocalDate.parse("2017-05-15"), LocalDate.parse("2017-10-15"), List.of(
                        createMeta(22L, 50L, 75L, new BigDecimal("3.1")),
                        createMeta(22L, 100L, 150L, new BigDecimal("3.2"))
                )),
                createTariff(7L, null, LocalDate.parse("2017-10-15"), LocalDate.parse("2017-12-15"), List.of(
                        createMeta(22L, 50L, 150L, new BigDecimal("3.3"))
                ))
        );
    }

    private List<TariffDTO> storageBillingTariffs() {
        return List.of(
                createTariff(1L, null, LocalDate.parse("2018-01-01"), LocalDate.MAX, List.of(
                        createMeta(90401L, 0L, 62L, new BigDecimal("0")),
                        createMeta(90401L, 62L, Long.MAX_VALUE, new BigDecimal("1"))
                )),
                createTariff(2L, 44L, LocalDate.parse("2018-01-01"), LocalDate.MAX, List.of(
                        createMeta(90401L, 62L, Long.MAX_VALUE, new BigDecimal("1.5"))
                ))
        );
    }

    private List<TariffDTO> storageBillingWithPromoTariffs() {
        return List.of(
                createTariff(1L, null, LocalDate.parse("2018-01-01"), LocalDate.MAX, List.of(
                        createMeta(90401L, 0L, 62L, new BigDecimal("0")),
                        createMeta(90401L, 62L, Long.MAX_VALUE, new BigDecimal("1"))
                )),
                createTariff(2L, 44L, LocalDate.parse("2018-01-01"), LocalDate.MAX, List.of(
                        createMeta(90401L, 62L, Long.MAX_VALUE, new BigDecimal("1.5"))
                )),
                createTariff(2L, 45L, LocalDate.parse("2018-01-01"), LocalDate.MAX, List.of(
                        createMeta(90401L, 62L, Long.MAX_VALUE, new BigDecimal("1.5"))
                )),
                createTariff(2L, 46L, LocalDate.parse("2018-01-01"), LocalDate.MAX, List.of(
                        createMeta(90401L, 62L, Long.MAX_VALUE, new BigDecimal("1.5"))
                ))
        );
    }

    private List<TariffDTO> storageBillingMultiplierTariff() {
        return List.of(
                createTariff(1L, null, LocalDate.parse("2018-01-01"), LocalDate.MAX, List.of(
                        createMeta(90401L, 0L, 11L, new BigDecimal("0")),
                        createMeta(300L, 0L, 1001L, new BigDecimal("0")),
                        createMeta(90401L, 11L, Long.MAX_VALUE, new BigDecimal("3"))
                ))
        );
    }


    private List<TariffDTO> storageBillingWithNullPartnerIdTariffs() {
        return List.of(
                createTariff(1L, null, LocalDate.parse("2018-01-01"), LocalDate.MAX, List.of(
                        createMeta(90401L, 0L, 62L, new BigDecimal("0")),
                        createMeta(90401L, 62L, Long.MAX_VALUE, new BigDecimal("1"))
                ))
        );
    }

    private List<TariffDTO> freeStorageWithNullPartnerIdTariffs() {
        return List.of(
                createTariff(1L, null, LocalDate.parse("2018-01-01"), LocalDate.MAX, List.of(
                        createMeta(90401L, 0L, 11L, new BigDecimal("0")),
                        createMeta(90401L, 11L, Long.MAX_VALUE, new BigDecimal("1"))
                ))
        );
    }

}
