package ru.yandex.market.billing.distribution.share;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.distribution.imports.dao.DistributionPartnerDao;
import ru.yandex.market.billing.pg.dao.PgCategoryDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.distribution.share.DistributionTariffName;
import ru.yandex.market.core.report.model.Color;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.billing.distribution.share.DistributionTariffRateAndNameMatcher.hasTariffName;
import static ru.yandex.market.billing.distribution.share.DistributionTariffRateAndNameMatcher.hasTariffRate;

/**
 * Тесты для {@link DistributionCategoryTariffRates}.
 *
 * @author vbudnev
 */
class DistributionCategoryTariffRatesTest extends FunctionalTest {

    private static final LocalDate DATE_2019_01_10 = LocalDate.of(2019, Month.JANUARY, 10);

    private static final long YSTATION_MSKU = 100307940933L;
    private static final long CIG_STICK_MSKU = 100636703252L;

    private static final long ADMITAD_SPECIAL_CLID = 2342107;
    private static final long EPN_SPECIAL_CLID = 2356130;

    private static final long UNKNOWN_CLID = -1;
    private static final LocalDate DATE_2020_06_22 = LocalDate.of(2020, 6, 22);
    private static final LocalDate DATE_2020_06_09 = LocalDate.of(2020, 6, 9);
    private static final LocalDate DATE_2020_09_30 = LocalDate.of(2020, 9, 30);

    @Autowired
    private PgCategoryDao pgCategoryDao;

    @Autowired
    private DistributionCategoryTariffDao tariffDao;

    @Autowired
    private DistributionPartnerDao distributionPartnerDao;

    private DistributionCategoryTariffRates service;

    static Stream<Arguments> matchesArguments() {
        return Stream.of(
                Arguments.of("Перегрузка родительской категории", new BigDecimal("0.02"), 101L),
                Arguments.of("Дефолтный тариф для рутовой категории", new BigDecimal("0.15"), 31L),
                Arguments.of("Тариф не попадает под интервал => берется дефолтный", new BigDecimal("0.15"), 999L),
                Arguments.of("Перегрузка родительской категории", new BigDecimal("0.30"), 400L),
                Arguments.of("Перегрузка родительской категории", new BigDecimal("0.30"), 450L)
        );
    }

    static Stream<Arguments> cigSticksTariffArgs() {
        return Stream.of(
                Arguments.of("До начала действия тарифа", LocalDate.of(2019, 12, 19), new BigDecimal("0.25")),
                Arguments.of("Первый день дейстивя тарифа", LocalDate.of(2019, 12, 20), BigDecimal.ZERO),
                Arguments.of("Какой-то день действия тарифа", LocalDate.of(2020, 5, 5), BigDecimal.ZERO)
        );
    }

    static Stream<Arguments> yStationTariffArgs() {
        return Stream.of(
                Arguments.of("До начала действия тарифа", LocalDate.of(2019, 12, 6), new BigDecimal("0.25")),
                Arguments.of("Первый день дейстивя тарифа", LocalDate.of(2019, 12, 7), new BigDecimal("0.1")),
                Arguments.of("Последний день действия тарифа", LocalDate.of(2019, 12, 25), new BigDecimal("0.1")),
                Arguments.of("После окончания действия тарифа", LocalDate.of(2019, 12, 26), new BigDecimal("0.25")),
                Arguments.of("Первый день дейстивя тарифа, 2020-04-13", LocalDate.of(2020, 4, 13), new BigDecimal("0.045")),
                Arguments.of("Последний день действия тарифа, 2020-04-19", LocalDate.of(2020, 4, 19), new BigDecimal("0.045")),
                Arguments.of("После окончания действия тарифа, 2020-04-20", LocalDate.of(2020, 4, 20), new BigDecimal("0.25"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matchesArguments")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.before.csv"
    })
    void test_matches(String description, BigDecimal tariff, long categoryId) {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());
        assertThat(
                service.getCategoryTariff(
                        categoryId,
                        DATE_2019_01_10,
                        0L,
                        false,
                        false,
                        List.of(),
                        false).getTariffRate(),
                comparesEqualTo(tariff)
        );
    }

    /**
     * MBI-36683 для клида 2342107 тариф должен быть 25% и 3.4% вместо 15% и 2% соответственно.
     */
    @Test
    @DisplayName("Тест на переопределение тарифа для определенного клида адмитада")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.before.csv"
    })
    void test_matches_special_clid() {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());
        assertThat(
                service.getCategoryTariff(101L, DATE_2019_01_10, ADMITAD_SPECIAL_CLID, false, false, List.of(), false).getTariffRate(),
                comparesEqualTo(new BigDecimal("0.034"))
        );
        assertThat(
                service.getCategoryTariff(31L, DATE_2019_01_10, ADMITAD_SPECIAL_CLID, false, false, List.of(), false).getTariffRate(),
                comparesEqualTo(new BigDecimal("0.25"))
        );
        assertThat(
                service.getCategoryTariff(999L, DATE_2019_01_10, ADMITAD_SPECIAL_CLID, false, false, List.of(), false).getTariffRate(),
                comparesEqualTo(new BigDecimal("0.25"))
        );
    }

    /**
     * MBI-39281 для клида 2356130 тариф должен быть 25% и 3.4% вместо 15% и 2% соответственно.
     */
    @Test
    @DisplayName("Тест на переопределение тарифа для определенного клида epn")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.before.csv"
    })
    void test_matches_epn_special_clid() {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());
        assertThat(
                service.getCategoryTariff(101L, DATE_2019_01_10, EPN_SPECIAL_CLID, false, false, List.of(), false).getTariffRate(),
                comparesEqualTo(new BigDecimal("0.034"))
        );
        assertThat(
                service.getCategoryTariff(31L, DATE_2019_01_10, EPN_SPECIAL_CLID, false, false, List.of(), false).getTariffRate(),
                comparesEqualTo(new BigDecimal("0.25"))
        );
        assertThat(
                service.getCategoryTariff(999L, DATE_2019_01_10, EPN_SPECIAL_CLID, false, false, List.of(), false).getTariffRate(),
                comparesEqualTo(new BigDecimal("0.25"))
        );
    }

    /**
     * MBI-42197 на Яндекс.Станции и Яндекс.Станции мини тариф должен быть 10% для всех.
     * на время акции [07.12, 25.12]
     */
    @ParameterizedTest(name = "Переопределение тарифа для Y Станций: {0}")
    @MethodSource(value = "yStationTariffArgs")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.before.csv"
    })
    void test_getTariff_yStation(String description, LocalDate orderCreationTime, BigDecimal expectedTariff) {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());
        assertThat(service.getTariff(90001L,
                orderCreationTime.atStartOfDay(),
                YSTATION_MSKU,
                DATE_2019_01_10,
                ADMITAD_SPECIAL_CLID,
                false,
                false,
                List.of(),
                false
                ).getTariffRate(),
                comparesEqualTo(expectedTariff)
        );
    }

    /**
     * MBI-46137 Добавил обнуление тарифов для кастомных тарифов ястанции в случае фрода.
     */
    @ParameterizedTest(name = "Переопределение тарифа для Y Станций: {0}")
    @MethodSource(value = "yStationTariffArgs")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.before.csv"
    })
    void test_getTariff_yStation_fraud(String description, LocalDate orderCreationTime, BigDecimal expectedTariff) {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());
        assertThat(service.getTariff(90001L,
                orderCreationTime.atStartOfDay(),
                YSTATION_MSKU,
                DATE_2019_01_10,
                ADMITAD_SPECIAL_CLID,
                false,
                true,
                List.of(),
                false
                ).getTariffRate(),
                comparesEqualTo(BigDecimal.ZERO)
        );
    }

    /**
     * Для стиков стиков 0 тариф с 2019-12-20 и до бесконечности.
     */
    @ParameterizedTest(name = "Переопределение тарифа для стиков: {0}")
    @MethodSource(value = "cigSticksTariffArgs")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.before.csv"
    })
    void test_getTariff_citSticks(String description, LocalDate orderCreationTime, BigDecimal expectedTariff) {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());
        assertThat(service.getTariff(90001L,
                orderCreationTime.atStartOfDay(),
                CIG_STICK_MSKU,
                DATE_2019_01_10,
                ADMITAD_SPECIAL_CLID,
                false,
                false,
                List.of(),
                false).getTariffRate(),
                comparesEqualTo(expectedTariff)
        );
    }

    @Test
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.before.csv"
    })
    @Disabled
    void test_categoryWithoutRootCategory() {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.WHITE, newClidInfoCache());
        //для данной категории нет рутовой категории
        long categoryId = 101L;
        assertThat(
                service.getCategoryTariff(categoryId, DATE_2019_01_10, 0L, false, false, List.of(), false),
                allOf(
                        hasTariffName(DistributionTariffName.UNKNOWN),
                        hasTariffRate(BigDecimal.ZERO)
                )
        );
    }

    @Test
    @DisplayName("Тест на пересечение времен тарифов для категорий")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.crossing.date.range.before.csv"
    })
    void test_crossingDateRange() {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());
        //для данной категории существует несколько тарифов на дату 2019-01-01
        long categoryId = 1L;
        LocalDate crossingLocalDate = LocalDate.of(2019, 1, 1);
        CategoryTariffFoundException exception = assertThrows(
                CategoryTariffFoundException.class,
                () -> service.getCategoryTariff(categoryId, crossingLocalDate, 0L, false, false, List.of(), false)
        );
        assertEquals("For category 1 found more than one tariffs for date 2019-01-01", exception.getMessage());
    }

    @Test
    @DisplayName("Тест на кеш в DistributionCategoryTariffRates")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.one.category.more.periods.csv"
    })
    void test_withDifferenceDateForOneCategory() {
        service = spy(new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache()));
        final long categoryId = 1L;
        final LocalDate secondDate = LocalDate.of(2019, Month.MAY, 31);

        //апдейтим кеш
        BigDecimal tariff = service.getCategoryTariff(categoryId, DATE_2019_01_10, 0L, false, false, List.of(), false).getTariffRate();
        assertThat(tariff, comparesEqualTo(new BigDecimal("0.20")));

        //тариф мы не может брать из кеша, потому как даты не подходят
        BigDecimal newTariff = service.getCategoryTariff(categoryId, secondDate, 0L, false, false, List.of(), false).getTariffRate();
        assertThat(newTariff, comparesEqualTo(new BigDecimal("0.10")));

        verify(service, times(4)).calculateTariff(eq(categoryId), any(), any(), any());

        //тут уже берем из кеша
        BigDecimal cachedTariff = service.getCategoryTariff(categoryId, secondDate, 0L, false, false, List.of(), false).getTariffRate();
        assertThat(cachedTariff, comparesEqualTo(new BigDecimal("0.10")));

        verify(service, times(4)).calculateTariff(eq(categoryId), any(), any(), any());
    }

    @Test
    @DisplayName("Тест на кеш в DistributionCategoryTariffRates при перегрузке тарифа")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.one.category.period.override.csv"
    })
    void test_withDifferenceAndPeriodDateForOneCategory() {
        service = spy(new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache()));
        final long categoryId = 1L;
        final LocalDate secondDate = LocalDate.of(2018, Month.MAY, 31);

        //апдейтим кеш
        BigDecimal tariff = service.getCategoryTariff(categoryId, secondDate, 0L, false, false, List.of(), false).getTariffRate();
        assertThat(tariff, comparesEqualTo(new BigDecimal("0.10")));

        //тариф мы не может брать из кеша, потому как даты не подходят
        BigDecimal newTariff = service.getCategoryTariff(categoryId, DATE_2019_01_10, 0L, false, false, List.of(), false).getTariffRate();
        assertThat(newTariff, comparesEqualTo(new BigDecimal("0.20")));

        verify(service, times(3)).calculateTariff(eq(categoryId), any(), any(), any());

        //тут уже берем из кеша
        tariff = service.getCategoryTariff(categoryId, secondDate, 0L, false, false, List.of(), false).getTariffRate();
        assertThat(tariff, comparesEqualTo(new BigDecimal("0.10")));
        newTariff = service.getCategoryTariff(categoryId, DATE_2019_01_10, 0L, false, false, List.of(), false).getTariffRate();
        assertThat(newTariff, comparesEqualTo(new BigDecimal("0.20")));


        verify(service, times(3)).calculateTariff(eq(categoryId), any(), any(), any());
    }

    @Test
    @DisplayName("Тест на получение имени тарифа")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.before.csv"
    })
    void test_getTariffName() {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());

        assertThat(
                service.getCategoryTariff(198119L, DATE_2020_06_22, UNKNOWN_CLID, false, false, List.of(), false),
                allOf(
                        hasTariffName(DistributionTariffName.CEHAC),
                        hasTariffRate(new BigDecimal("0.02"))
                )
        );

        assertThat(
                service.getCategoryTariff(90401L, DATE_2020_06_22, UNKNOWN_CLID, false, false, List.of(), false),
                allOf(
                        hasTariffName(DistributionTariffName.ALL),
                        hasTariffRate(new BigDecimal("0.15"))
                )
        );

        //278345 = ->Все товары->Спорт и отдых->Роликовые коньки->Роликовые коньки
        assertThat(
                service.getCategoryTariff(278345, DATE_2020_06_22, UNKNOWN_CLID, false, false, List.of(), false),
                allOf(
                        hasTariffName(DistributionTariffName.DIY),
                        hasTariffRate(new BigDecimal("0.1"))
                )
        );
    }

    @Test
    @DisplayName("Тест на получение имени тарифа для стиков")
    void test_getTariffNameForSticks() {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());

        assertEquals(
                DistributionTariffName.STICKS,
                service.getTariff(-1L, DATE_2020_06_09.atStartOfDay(), CIG_STICK_MSKU, DATE_2020_06_09, UNKNOWN_CLID, false, false, List.of(), false).getTariffName()

        );
    }

    @Test
    @DisplayName("Тест на белый (DSBS) тариф")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.white.before.csv"
    })
    void test_getWhiteTariff() {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.WHITE, newClidInfoCache());

        assertThat(
                service.getCategoryTariff(198119, DATE_2020_09_30, UNKNOWN_CLID, false, false, List.of(), false),
                allOf(
                        hasTariffName(DistributionTariffName.CEHAC),
                        hasTariffRate(new BigDecimal("0.018"))
                )
        );
    }


    @Test
    @DisplayName("Тест на вовзращение дефолтного тарифа")
    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/categories.rates.white.before.csv"
    })
    void test_unknownTariff() {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.WHITE, newClidInfoCache());

        assertThat(
                service.getCategoryTariff(11111, DATE_2020_09_30, UNKNOWN_CLID, false, false, List.of(), false),
                allOf(
                        hasTariffName(DistributionTariffName.UNKNOWN),
                        hasTariffRate(BigDecimal.ZERO)
                )
        );
    }

    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/difference.tariff.rates.based.on.segment.before.csv"
    })
    @ParameterizedTest(name = "{3}")
    @MethodSource("getTariffDependenceOfSegmentData")
    void testGetTariffDependenceOfSegment(
            long clid,
            long categoryId,
            BigDecimal expectedRate,
            String displayName
    ) {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());
        DistributionTariffRateAndName categoryTariff = service.getCategoryTariff(
                categoryId,
                LocalDate.of(2021, Month.MAY, 1),
                clid,
                false,
                false,
                List.of(),
                false
        );
        assertThat(categoryTariff, hasTariffRate(expectedRate));
    }

    @DbUnitDataSet(before = {
            "db/categories.before.csv",
            "db/difference.tariff.rates.based.on.segment.extended.before.csv"
    })
    @ParameterizedTest(name = "{3}")
    @MethodSource("getTariffDependenceOfSegmentDataExtended")
    void testGetTariffDependenceOfSegmentExtended(
            long clid,
            long categoryId,
            BigDecimal expectedRate,
            String displayName
    ) {
        service = new DistributionCategoryTariffRates(pgCategoryDao, tariffDao, Color.BLUE, newClidInfoCache());
        DistributionTariffRateAndName categoryTariff = service.getCategoryTariff(
                categoryId,
                LocalDate.of(2022, Month.MARCH, 1),
                clid,
                false,
                false,
                List.of(),
                false
        );
        assertThat(categoryTariff, hasTariffRate(expectedRate));
    }

    private static Stream<Arguments> getTariffDependenceOfSegmentData() {
        return Stream.of(
                Arguments.of(1L, 1L, new BigDecimal("3"), "кастомный тариф для 1 клида"),
                Arguments.of(2L, 2L, new BigDecimal("1"), "нет кастомного тарифа => берем клозер (потому что такой клид) тариф"),
                Arguments.of(3L, 3L, new BigDecimal("2"), "нет кастомного тарифа => берем маркетинг (потому что такой клид) тариф"),
                Arguments.of(4L, 3L, new BigDecimal("1"), "нет кастомного тарифа и так же нет segment => берем клозер тариф")
        );
    }

    private static Stream<Arguments> getTariffDependenceOfSegmentDataExtended() {
        return Stream.of(
                Arguments.of(1L, 8475840L, new BigDecimal("3"), "кастомный тариф для 1 клида"),
                Arguments.of(2L, 8475840L, new BigDecimal("1"), "closer-promo-cashbacks"),
                Arguments.of(3L, 8475840L, new BigDecimal("2"), "marketing-bloggers"),
                Arguments.of(4L, 8475840L, new BigDecimal("1.5"), "closer-others")
        );
    }

    private ClidInfoCache newClidInfoCache() {
        return new ClidInfoCache(distributionPartnerDao);
    }

}
