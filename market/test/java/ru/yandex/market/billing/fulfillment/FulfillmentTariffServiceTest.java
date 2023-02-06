package ru.yandex.market.billing.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.FulfillmentTariffDao;
import ru.yandex.market.core.fulfillment.FulfillmentTariffService;
import ru.yandex.market.core.fulfillment.OrderType;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.model.BillingUnit;
import ru.yandex.market.core.fulfillment.model.FulfillmentTariff;
import ru.yandex.market.core.fulfillment.model.TariffValue;
import ru.yandex.market.core.fulfillment.model.ValueType;
import ru.yandex.market.core.fulfillment.tariff.TariffsIterator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.fulfillment.entities.base.DateTimeInterval;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.FulfillmentTariffsJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.fulfillment.model.TariffValueMatcher.hasBillingUnit;
import static ru.yandex.market.core.fulfillment.model.TariffValueMatcher.hasValue;
import static ru.yandex.market.core.fulfillment.model.TariffValueMatcher.hasValueType;

@DbUnitDataSet(before = "FulfillmentTariffServiceTest.before.csv")
class FulfillmentTariffServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2015_03_01 = LocalDate.of(2015, 3, 1);
    private static final LocalDate DATE_2017_01_01 = LocalDate.of(2017, 1, 1);
    private static final LocalDate DATE_2018_05_10 = LocalDate.of(2018, 5, 10);
    private static final LocalDate DATE_2018_05_22 = LocalDate.of(2018, 5, 22);
    private static final LocalDate DATE_2018_02_01 = LocalDate.of(2018, 2, 1);
    private static final LocalDate DATE_2018_02_05 = LocalDate.of(2018, 2, 5);
    private static final LocalDate DATE_2018_02_06 = LocalDate.of(2018, 2, 6);
    private static final Long SUPPLIER_ID_1 = 1L;
    private static final Long SUPPLIER_ID_2 = 2L;
    private static final Integer NO_PRICE = null;
    private static final Integer PRICE_70 = 70;
    private static final Integer PRICE_100 = 100;
    private static final Integer PRICE_150000 = 150000;
    private static final TariffValue NO_TARIFF = null;
    private static final Long NO_DIMENSIONS = null;
    private static final Long DIMENSIONS_50 = 50L;
    private static final Long DIMENSIONS_100 = 100L;
    private static final Long DIMENSIONS_130 = 130L;
    private static final Long DIMENSIONS_170 = 170L;
    private static final Long NO_WEIGHT = null;
    private static final Long WEIGHT_100 = 100L;
    private static final Long WEIGHT_1050 = 1050L;
    private static final Long WEIGHT_2050 = 2050L;
    private static final Long WEIGHT_10050 = 10050L;

    @Autowired
    private FulfillmentTariffDao dao;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private TariffsService tariffsService;

    @Autowired
    private EnvironmentService environmentService;

    private FulfillmentTariffService service;

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
            OrderType orderType,
            Long supplierId) {
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
                supplierId,
                null,
                null
        );
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


    private FulfillmentTariff createTariff(
            LocalDate from,
            LocalDate to,
            BillingServiceType serviceType,
            Long priceTo,
            Long dimensionsTo,
            Long weightTo,
            int value) {
        return createTariff(from, to, serviceType, priceTo, dimensionsTo, weightTo, value,
                ValueType.ABSOLUTE, BillingUnit.ITEM, OrderType.FULFILLMENT);
    }

    private void mock(List<FulfillmentTariff> list) {
        Mockito.when(dao.getOrderedTariffs(Mockito.any())).thenReturn(list);
    }

    private static Stream<Arguments> ffProcessingTariffArgs() {
        return testDataSet()
                .stream()
                .map(dw -> Arguments.of(
                                dw.price,
                                dw.dimensions,
                                dw.weight,
                                buildExpected(
                                        BillingServiceType.FF_PROCESSING,
                                        dw.price,
                                        dw.dimensions,
                                        dw.weight
                                )
                        )
                );
    }

    private static Stream<Arguments> ffStorageTariffArgs() {
        return testDataSet()
                .stream()
                .map(dw -> Arguments.of(
                                dw.price,
                                dw.dimensions,
                                dw.weight,
                                buildExpected(
                                        BillingServiceType.FF_STORAGE,
                                        dw.price,
                                        dw.dimensions,
                                        dw.weight
                                )
                        )
                );
    }

    private static Stream<Arguments> ffWithdrawTariffArgs() {
        return Stream.of(
                Arguments.of(
                        "Какой-то тариф",
                        DATE_2018_05_10,
                        PRICE_100,
                        DIMENSIONS_50,
                        WEIGHT_100,
                        new TariffValue(2100, ValueType.ABSOLUTE, BillingUnit.ITEM)
                ),
                Arguments.of(
                        "Дата не попадает ни в один интервал",
                        DATE_2017_01_01,
                        PRICE_100,
                        DIMENSIONS_50,
                        WEIGHT_100,
                        NO_TARIFF
                ),
                Arguments.of(
                        "Попадает в первый тарифный блок при отсутствии значения цены",
                        DATE_2018_05_22,
                        NO_PRICE,
                        DIMENSIONS_50,
                        WEIGHT_100,
                        new TariffValue(1101, ValueType.ABSOLUTE, BillingUnit.ITEM)
                ),
                Arguments.of(
                        "Попадает в первый тарифный блок по явному значению цены",
                        DATE_2018_05_22,
                        PRICE_70,
                        DIMENSIONS_50,
                        WEIGHT_100,
                        new TariffValue(1101, ValueType.ABSOLUTE, BillingUnit.ITEM)
                ),
                Arguments.of(
                        "В рамках идентичной price матчится первый по dimensions",
                        DATE_2018_05_22,
                        PRICE_100,
                        DIMENSIONS_50,
                        WEIGHT_100,
                        new TariffValue(1102, ValueType.ABSOLUTE, BillingUnit.ITEM)
                ),
                Arguments.of(
                        "В рамках идентичной price матчится второй блок по dimensions",
                        DATE_2018_05_22,
                        PRICE_100,
                        DIMENSIONS_100,
                        WEIGHT_100,
                        new TariffValue(1104, ValueType.ABSOLUTE, BillingUnit.ITEM)
                ),
                Arguments.of(
                        "В рамках идентичной price и dimensions матчится первый по weight",
                        DATE_2018_05_22,
                        PRICE_100,
                        DIMENSIONS_130,
                        WEIGHT_1050,
                        new TariffValue(1104, ValueType.ABSOLUTE, BillingUnit.ITEM)
                ),
                Arguments.of(
                        "В рамках идентичной price и dimensions матчится второй по weight",
                        DATE_2018_05_22,
                        PRICE_100,
                        DIMENSIONS_130,
                        WEIGHT_2050,
                        new TariffValue(1105, ValueType.ABSOLUTE, BillingUnit.ITEM)
                ),
                Arguments.of(
                        "Не найдено ни одного подходящего тарифа (из-за веса)",
                        DATE_2018_05_22,
                        PRICE_100,
                        DIMENSIONS_100,
                        WEIGHT_10050,
                        NO_TARIFF
                )
        );
    }

    private static List<PWDWrapper> testDataSet() {
        final List<PWDWrapper> pwdParams = new ArrayList<>();
        for (int dimensions = 100; dimensions <= 400; dimensions += 50) {
            for (int weight = 10000; weight <= 25000; weight += 5000) {
                for (int price = 1250; price <= 1500; price += 250) {
                    pwdParams.add(new PWDWrapper(price, weight, dimensions));
                }
                pwdParams.add(new PWDWrapper(null, weight, dimensions));
            }
        }
        return pwdParams;
    }

    @Nullable
    private static TariffValue buildExpected(BillingServiceType type, Integer price, long dimensions, long weight) {
        switch (type) {

            case FF_STORAGE:
                if (dimensions < 150 && weight < 15000) {
                    return new TariffValue(60, ValueType.ABSOLUTE, BillingUnit.ITEM);
                } else {
                    return new TariffValue(4200, ValueType.ABSOLUTE, BillingUnit.CUBIC_METER);
                }

            case FF_PROCESSING:
                if ((dimensions < 150 && weight < 15000 && price != null && price < 1250)
                        || (dimensions < 150 && weight < 15000 && price == null)
                ) {
                    return new TariffValue(800, ValueType.RELATIVE, BillingUnit.ITEM);
                } else if (dimensions < 150 && weight < 15000) {
                    return new TariffValue(10000, ValueType.ABSOLUTE, BillingUnit.ITEM);
                } else if (dimensions < 250 && weight < 20000) {
                    return new TariffValue(35000, ValueType.ABSOLUTE, BillingUnit.ITEM);
                } else if (dimensions < 350 && weight < 25000) {
                    return new TariffValue(40000, ValueType.ABSOLUTE, BillingUnit.ITEM);
                }

            default:
                return null;
        }
    }

    /**
     * Чтобы проще идентифицировать тест кейс в переборе.
     */
    private static String testCaseIdent(@Nullable Integer price, long dimensions, long weight) {
        return "price=" + price + " dimensions=" + dimensions + " weight=" + weight;
    }

    @BeforeEach
    void beforeEach() {
        mock(List.of(
                createTariff(
                        LocalDate.of(2018, 2, 5),
                        LocalDate.of(2018, 5, 22),
                        BillingServiceType.FF_PROCESSING,
                        1250L,
                        150L,
                        15000L,
                        800,
                        ValueType.RELATIVE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.of(2018, 2, 5),
                        LocalDate.of(2018, 5, 22),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        150L,
                        15000L,
                        10000
                ),
                createTariff(
                        LocalDate.of(2018, 2, 5),
                        LocalDate.of(2018, 5, 22),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        250L,
                        20000L,
                        35000
                ),
                createTariff(
                        LocalDate.of(2018, 2, 5),
                        LocalDate.of(2018, 5, 22),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        350L,
                        25000L,
                        40000
                ),
                createTariff(
                        LocalDate.of(2018, 2, 6),
                        LocalDate.MAX,
                        BillingServiceType.FF_STORAGE,
                        null,
                        150L,
                        15000L,
                        60
                ),
                createTariff(
                        LocalDate.of(2018, 2, 6),
                        LocalDate.MAX,
                        BillingServiceType.FF_STORAGE,
                        null,
                        null,
                        null,
                        4200,
                        ValueType.ABSOLUTE,
                        BillingUnit.CUBIC_METER,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.of(2018, 1, 1),
                        LocalDate.of(2018, 1, 10),
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        null,
                        null,
                        1800
                ),
                createTariff(
                        LocalDate.of(2018, 1, 10),
                        LocalDate.of(2018, 2, 5),
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        null,
                        null,
                        1900
                ),
                createTariff(
                        LocalDate.of(2018, 2, 5),
                        LocalDate.of(2018, 5, 22),
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        null,
                        null,
                        2100
                ),
                createTariff(
                        LocalDate.of(2018, 5, 22),
                        LocalDate.MAX,
                        BillingServiceType.FF_WITHDRAW,
                        100L,
                        100L,
                        1100L,
                        1101
                ),
                createTariff(
                        LocalDate.of(2018, 5, 22),
                        LocalDate.MAX,
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        100L,
                        1000L,
                        1102
                ),
                createTariff(
                        LocalDate.of(2018, 5, 22),
                        LocalDate.MAX,
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        100L,
                        1100L,
                        1103
                ),
                createTariff(
                        LocalDate.of(2018, 5, 22),
                        LocalDate.MAX,
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        150L,
                        2000L,
                        1104
                ),
                createTariff(
                        LocalDate.of(2018, 5, 22),
                        LocalDate.MAX,
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        150L,
                        4000L,
                        1105
                ),
                createTariff(
                        LocalDate.of(2018, 5, 22),
                        LocalDate.MAX,
                        BillingServiceType.FF_WITHDRAW,
                        null,
                        180L,
                        2000L,
                        1106
                ),
                createTariff(
                        LocalDate.of(2015, 1, 1),
                        LocalDate.of(2015, 6, 1),
                        BillingServiceType.FF_PROCESSING,
                        100L,
                        101L,
                        101L,
                        1
                ),
                createTariff(
                        LocalDate.of(2015, 1, 1),
                        LocalDate.of(2015, 6, 1),
                        BillingServiceType.FF_PROCESSING,
                        200L,
                        101L,
                        101L,
                        2,
                        ValueType.RELATIVE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.of(2015, 1, 1),
                        LocalDate.of(2015, 6, 1),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        101L,
                        101L,
                        3,
                        ValueType.RELATIVE,
                        BillingUnit.CUBIC_METER,
                        OrderType.FULFILLMENT
                ),
                createTariff(
                        LocalDate.of(2015, 1, 1),
                        LocalDate.of(2015, 6, 1),
                        BillingServiceType.FF_PROCESSING,
                        null,
                        102L,
                        101L,
                        4
                ),
                createTariff(
                        LocalDate.of(2018, 2, 6),
                        LocalDate.MAX,
                        BillingServiceType.FF_PROCESSING,
                        1250L,
                        150L,
                        15000L,
                        2800,
                        ValueType.RELATIVE,
                        BillingUnit.ITEM,
                        OrderType.FULFILLMENT,
                        1L
                )
        ));
        service = new FulfillmentTariffService(dao, DATE_2018_05_22);
    }

    @DisplayName("Тарифы для FF_PROCESSING")
    @MethodSource("ffProcessingTariffArgs")
    @ParameterizedTest(name = "p={0} d={1} w={2}")
    void test_ffProcessing(Integer price,
                           Long dimensions,
                           Long weight,
                           TariffValue expectedTariff
    ) {
        checkExpectedExplicit(DATE_2018_05_10,
                BillingServiceType.FF_PROCESSING,
                null,
                price,
                dimensions,
                weight,
                expectedTariff
        );
    }

    @DisplayName("Тарифы для FF_STORAGE")
    @MethodSource("ffStorageTariffArgs")
    @ParameterizedTest(name = "p={0} d={1} w={2}")
    void test_ffStorage(Integer price,
                        Long dimensions,
                        Long weight,
                        TariffValue expectedTariff
    ) {
        checkExpectedExplicit(DATE_2018_05_10,
                BillingServiceType.FF_STORAGE,
                null,
                price,
                dimensions,
                weight,
                expectedTariff
        );
    }

    @DisplayName(value = "Тарифы для FF_WITHDRAW")
    @MethodSource("ffWithdrawTariffArgs")
    @ParameterizedTest(name = "{0} [date={1} p={2} d={3} w={4}]")
    void test_ffWithdraw(
            String descr,
            LocalDate targetDate,
            @Nullable Integer price,
            Long dimensions,
            Long width,
            TariffValue expectedTariff
    ) {
        checkExpectedExplicit(
                targetDate,
                BillingServiceType.FF_WITHDRAW,
                null,
                price,
                dimensions,
                width,
                expectedTariff
        );
    }

    @DisplayName(value = "Пустой тариф для неизвестных, типов сервиса")
    @ParameterizedTest
    @EnumSource(value = BillingServiceType.class, mode = EnumSource.Mode.EXCLUDE, names = {"FF_PROCESSING",
            "FF_STORAGE", "FF_WITHDRAW"})
    void test_getTariff_emptyTariff_when_unexpectedServiceType(BillingServiceType serviceType) {
        testDataSet().forEach(
                dw -> checkEmptyOptional(DATE_2018_05_10, serviceType, null, dw.price, dw.dimensions, dw.weight)
        );
    }

    @DisplayName("Проверяем тариф до, во время и после даты смены тарифа")
    @Test
    void testTwoPeriodsWithDifferentTariffValues() {
        Optional<TariffValue> tariff =
                service.getTariff(DATE_2018_02_01, BillingServiceType.FF_WITHDRAW, OrderType.FULFILLMENT, null,
                        NO_PRICE, NO_DIMENSIONS, NO_WEIGHT, null, null).findFirst();
        assertTrue(tariff.isPresent());
        assertThat(tariff.get().getValue(), is(1900));
        assertThat(tariff.get().getValueType(), is(ValueType.ABSOLUTE));
        assertThat(tariff.get().getBillingUnit(), is(BillingUnit.ITEM));

        tariff = service.getTariff(DATE_2018_02_05, BillingServiceType.FF_WITHDRAW, OrderType.FULFILLMENT, null,
                NO_PRICE, NO_DIMENSIONS, NO_WEIGHT, null, null).findFirst();
        assertTrue(tariff.isPresent());
        assertThat(tariff.get().getValue(), is(2100));
        assertThat(tariff.get().getValueType(), is(ValueType.ABSOLUTE));
        assertThat(tariff.get().getBillingUnit(), is(BillingUnit.ITEM));

        tariff = service.getTariff(DATE_2018_02_06, BillingServiceType.FF_WITHDRAW, OrderType.FULFILLMENT, null,
                NO_PRICE, NO_DIMENSIONS, NO_WEIGHT, null, null).findFirst();
        assertTrue(tariff.isPresent());
        assertThat(tariff.get().getValue(), is(2100));
        assertThat(tariff.get().getValueType(), is(ValueType.ABSOLUTE));
        assertThat(tariff.get().getBillingUnit(), is(BillingUnit.ITEM));
    }

    @Test
    @DisplayName("Проверка, что находится несколько тарифов с одинаковыми ВГХ")
    void testManyFfProcessingTariffs() {
        List<TariffValue> rates = service.getAllSuitableTariffsOrdered(
                DATE_2015_03_01,
                BillingServiceType.FF_PROCESSING,
                OrderType.FULFILLMENT,
                null,
                DIMENSIONS_100,
                WEIGHT_100
        ).collect(Collectors.toList());
        assertThat(rates, hasSize(3));
        assertThat(rates, contains(
                allOf(hasBillingUnit(BillingUnit.ITEM), hasValue(1), hasValueType(ValueType.ABSOLUTE)),
                allOf(hasBillingUnit(BillingUnit.ITEM), hasValue(2), hasValueType(ValueType.RELATIVE)),
                allOf(hasBillingUnit(BillingUnit.CUBIC_METER), hasValue(3), hasValueType(ValueType.RELATIVE))
        ));

    }

    @DisplayName("Для заданного сервиса и указанной даты должен матчится только один действующий период")
    @Test
    void test_getTariff_throws_when_moreThenOneTariffMatches() {
        final FulfillmentTariffDao mockedDao = Mockito.mock(FulfillmentTariffDao.class);

        when(mockedDao.getOrderedTariffs(any(LocalDate.class)))
                .thenReturn(
                        ImmutableList.of(
                                new FulfillmentTariff(
                                        DateTimeInterval.fromFormattedValue(
                                                "2018-05-16T10:00:00+03:00/2018-05-28T10:00:00+03:00"
                                        ),
                                        BillingServiceType.FF_WITHDRAW,
                                        100L,
                                        100L,
                                        100L,
                                        100,
                                        null,
                                        null,
                                        ValueType.ABSOLUTE,
                                        BillingUnit.ITEM,
                                        OrderType.FULFILLMENT,
                                        1L,
                                        null,
                                        null
                                ),
                                new FulfillmentTariff(
                                        DateTimeInterval.fromFormattedValue(
                                                "2018-05-18T10:00:00+03:00/2018-05-24T10:00:00+03:00"
                                        ),
                                        BillingServiceType.FF_WITHDRAW,
                                        100L,
                                        1000L,
                                        1000L,
                                        200,
                                        null,
                                        null,
                                        ValueType.ABSOLUTE,
                                        BillingUnit.ITEM,
                                        OrderType.FULFILLMENT,
                                        1L,
                                        null,
                                        null
                                )
                        )
                );

        final FulfillmentTariffService service = new FulfillmentTariffService(mockedDao, DATE_2018_05_22);

        final IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.getTariff(DATE_2018_05_22, BillingServiceType.FF_WITHDRAW, OrderType.FULFILLMENT, 1L,
                        null, null)
        );

        assertEquals("There are more then one tariff matching: " +
                        "serviceType=FF_WITHDRAW " +
                        "targetDate=2018-05-22 " +
                        "matched intervals=[" +
                        "DateTimeInterval{from=2018-05-18T10:00+03:00, to=2018-05-24T10:00+03:00}, " +
                        "DateTimeInterval{from=2018-05-16T10:00+03:00, to=2018-05-28T10:00+03:00}" +
                        "]",
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Находит заданный тарифф для поставщика")
    void testGetActualTariffForSupplier() {
        Optional<TariffValue> tariff =
                service.getTariff(DATE_2018_02_06, BillingServiceType.FF_PROCESSING, OrderType.FULFILLMENT,
                        SUPPLIER_ID_1, PRICE_70, DIMENSIONS_50, WEIGHT_100, null, null).findFirst();
        assertTrue(tariff.isPresent());
        assertThat(tariff.get().getValue(), is(2800));
        assertThat(tariff.get().getValueType(), is(ValueType.RELATIVE));
        assertThat(tariff.get().getBillingUnit(), is(BillingUnit.ITEM));
    }

    @Test
    @DisplayName("Не находит заданный тарифф для поставщика и выбирает общий тарифф")
    void testGetGeneralTariffForSupplier() {
        Optional<TariffValue> tariff =
                service.getTariff(DATE_2018_02_05, BillingServiceType.FF_PROCESSING, OrderType.FULFILLMENT,
                        SUPPLIER_ID_1, PRICE_70, DIMENSIONS_50, WEIGHT_100, null, null).findFirst();
        assertTrue(tariff.isPresent());
        assertThat(tariff.get().getValue(), is(800));
        assertThat(tariff.get().getValueType(), is(ValueType.RELATIVE));
        assertThat(tariff.get().getBillingUnit(), is(BillingUnit.ITEM));
    }

    @Test
    @DisplayName("Проверяем обработку результатов вызова тарифницы")
    void testFulfillmentExternalTariffs() {
        environmentService.setValue("mbi.billing.use.external.tariffs.fulfillment", "true");
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");
            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }
                return getFulfillmentTariffs().stream()
                        .filter(tariff -> {
                            LocalDate to = tariff.getDateTo();
                            LocalDate from = tariff.getDateFrom();
                            return targetDate.compareTo(from) >= 0
                                    && (to == null || targetDate.compareTo(to) < 0);
                        })
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(any(TariffFindQuery.class));

        dao = new FulfillmentTariffDao(tariffsService);

        service = new FulfillmentTariffService(dao, DATE_2018_05_22);

        Optional<TariffValue> tariff = service.getTariff(
                DATE_2018_02_05,
                BillingServiceType.FF_PROCESSING,
                OrderType.CROSSDOCK,
                SUPPLIER_ID_1,
                PRICE_70,
                DIMENSIONS_170,
                WEIGHT_10050,
                null,
                null).findFirst();

        assertTrue(tariff.isPresent());
        assertThat(tariff.get().getValue(), is(40000));
        assertThat(tariff.get().getValueType(), is(ValueType.ABSOLUTE));
        assertThat(tariff.get().getBillingUnit(), is(BillingUnit.ITEM));

        tariff = service.getTariff(
                DATE_2018_02_05,
                BillingServiceType.FF_PROCESSING,
                OrderType.FULFILLMENT,
                SUPPLIER_ID_2,
                PRICE_150000,
                DIMENSIONS_50,
                WEIGHT_100,
                null,
                null).findFirst();

        assertTrue(tariff.isPresent());
        assertThat(tariff.get().getValue(), is(3000));
        assertThat(tariff.get().getValueType(), is(ValueType.ABSOLUTE));
        assertThat(tariff.get().getBillingUnit(), is(BillingUnit.ITEM));
    }

    private static List<TariffDTO> getFulfillmentTariffs() {
        return List.of(
                createTariff(1L, 1L,
                        LocalDate.of(2018, 1, 1),
                        null,
                        ModelType.FULFILLMENT_BY_YANDEX_PLUS,
                        List.of(
                                createMeta(
                                        new BigDecimal("10.00"),
                                        125000,
                                        150,
                                        15000,
                                        null,
                                        1,
                                        null,
                                        null),
                                createMeta(
                                        new BigDecimal("125.00"),
                                        null,
                                        150,
                                        15000,
                                        null,
                                        2,
                                        null,
                                        null),
                                createMeta(
                                        new BigDecimal("400.00"),
                                        null,
                                        250,
                                        40000,
                                        null,
                                        3,
                                        null,
                                        null),
                                createMeta(
                                        new BigDecimal("450.00"),
                                        null,
                                        null,
                                        null,
                                        null,
                                        4,
                                        null,
                                        null))),
                createTariff(2L, null,
                        LocalDate.of(2018, 1, 1),
                        null,
                        ModelType.FULFILLMENT_BY_YANDEX,
                        List.of(
                                createMeta(
                                        new BigDecimal("30.00"),
                                        125000,
                                        150,
                                        15000,
                                        null,
                                        1,
                                        null,
                                        null),
                                createMeta(
                                        new BigDecimal("30.00"),
                                        null,
                                        150,
                                        15000,
                                        null,
                                        2,
                                        null,
                                        null),
                                createMeta(
                                        new BigDecimal("150.00"),
                                        null,
                                        250,
                                        40000,
                                        null,
                                        3,
                                        null,
                                        null),
                                createMeta(
                                        new BigDecimal("150.00"),
                                        null,
                                        null,
                                        null,
                                        null,
                                        4,
                                        null,
                                        null)))
        );
    }

    private static TariffDTO createTariff(
            long id,
            Long supplierId,
            LocalDate from,
            LocalDate to,
            ModelType modelType,
            List<Object> meta
    ) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setIsActive(true);
        tariff.setServiceType(ServiceTypeEnum.FF_PROCESSING);
        tariff.setDateFrom(from);
        tariff.setDateTo(to);
        tariff.setPartner(new Partner().id(supplierId).type(PartnerType.SUPPLIER));
        tariff.setModelType(modelType);
        tariff.setMeta(meta);
        return tariff;
    }

    private static CommonJsonSchema createMeta(
            BigDecimal amount,
            Integer priceTo,
            Integer dimensionsTo,
            Integer weightTo,
            Integer minValue,
            Integer ordinal,
            Integer areaFrom,
            Integer areaTo) {
        return new FulfillmentTariffsJsonSchema()
                .priceTo(priceTo)
                .dimensionsTo(dimensionsTo)
                .weightTo(weightTo)
                .minValue(minValue)
                .ordinal(ordinal)
                .areaFrom(areaFrom)
                .areaFrom(areaTo)
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.ITEM);

    }


    private void checkExpectedExplicit(
            LocalDate targetDate,
            BillingServiceType type,
            Long supplierId,
            Integer price,
            long dimensions,
            long weight,
            TariffValue expectedTariffValue
    ) {
        final Optional<TariffValue> actual = service.getTariff(targetDate, type, OrderType.FULFILLMENT, supplierId,
                price, dimensions, weight, null, null).findFirst();
        final String msg = testCaseIdent(price, dimensions, weight);

        if (expectedTariffValue == null) {
            assertFalse("Unexpected tariff for " + msg, actual.isPresent());
        } else {
            assertTrue("Missing tariff for " + msg, actual.isPresent());
            assertEquals("Tariff value mismatch for " + msg, expectedTariffValue.getValue(), actual.get().getValue());
            assertEquals("Tariff value type mismatch for " + msg, expectedTariffValue.getValueType(),
                    actual.get().getValueType());
            assertEquals("Tariff units mismatch for " + msg, expectedTariffValue.getBillingUnit(),
                    actual.get().getBillingUnit());
        }

    }

    private void checkEmptyOptional(LocalDate targetDate,
                                    BillingServiceType type,
                                    Long supplierId,
                                    Integer price,
                                    long dimensions,
                                    long weight) {
        final Optional<TariffValue> actualTariffOpt = service.getTariff(targetDate, type, OrderType.FULFILLMENT,
                supplierId, price, dimensions, weight, null, null).findFirst();
        Assertions.assertFalse(actualTariffOpt.isPresent(), testCaseIdent(price, dimensions, weight));
    }

    private static class PWDWrapper {
        @Nullable
        Integer price;
        long weight;
        long dimensions;

        PWDWrapper(@Nullable Integer price, long weight, long dimensions) {
            this.price = price;
            this.weight = weight;
            this.dimensions = dimensions;
        }
    }

}
