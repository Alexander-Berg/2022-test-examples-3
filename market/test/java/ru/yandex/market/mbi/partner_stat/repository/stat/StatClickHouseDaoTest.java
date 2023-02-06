package ru.yandex.market.mbi.partner_stat.repository.stat;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.partner_stat.config.ClickHouseTestConfig;
import ru.yandex.market.mbi.partner_stat.repository.ClickhouseFunctionalTest;
import ru.yandex.market.mbi.partner_stat.service.stat.model.DetalizationType;
import ru.yandex.market.mbi.partner_stat.service.stat.model.StatFilter;
import ru.yandex.market.mbi.partner_stat.service.stat.model.TimeSeriesType;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailGroupingType;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailPage;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailPageFilter;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailSort;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailTotal;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailTotalFilter;
import ru.yandex.market.mbi.partner_stat.service.stat.model.summary.SummaryFilter;
import ru.yandex.market.mbi.partner_stat.service.stat.model.summary.SummaryPage;
import ru.yandex.misc.time.MoscowTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link StatClickHouseDao}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(
        dataSource = ClickHouseTestConfig.DATA_SOURCE,
        type = DataSetType.SINGLE_CSV,
        before = "stat.before.csv"
)
public class StatClickHouseDaoTest extends ClickhouseFunctionalTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private StatClickHouseDao statClickHouseDao;

    static StatFilter filter1() {
        return new StatFilter(
                List.of(1L),
                List.of(774L),
                DetalizationType.MONTH,
                localDateToInstant(2015, 6, 15),
                localDateToInstant(2022, 12, 31),
                List.of(6L, 7L),
                List.of(11L, 12L),
                List.of(101L, 103L),
                List.of("ssku1",
                        "ssku2",
                        "ssku4",
                        "ssku5",
                        "ssku7",
                        "ssku8")
        );
    }

    static StatFilter filter2() {
        return new StatFilter(
                List.of(1L),
                List.of(774L),
                DetalizationType.YEAR,
                localDateToInstant(2015, 9, 15),
                localDateToInstant(2022, 12, 31),
                List.of(6L),
                List.of(),
                List.of(101L),
                List.of("ssku1",
                        "ssku4",
                        "ssku5",
                        "ssku7",
                        "ssku8")
        );
    }

    static StatFilter filter3() {
        return new StatFilter(
                List.of(1L),
                List.of(774L),
                DetalizationType.DAY,
                localDateToInstant(2015, 2, 15),
                localDateToInstant(2022, 12, 31),
                List.of(6L),
                List.of(11L),
                List.of(),
                List.of("ssku4",
                        "ssku5")
        );
    }

    static StatFilter filter4() {
        return new StatFilter(
                List.of(1L),
                List.of(774L),
                DetalizationType.WEEK,
                localDateToInstant(2015, 7, 15),
                localDateToInstant(2022, 12, 31),
                List.of(),
                List.of(11L),
                List.of(101L),
                List.of()
        );
    }

    static Stream<Arguments> timeSeriesSummaryArgs() {
        return Stream.of(
                Arguments.of(
                        "Тест суммы показов фильтра 1",
                        TimeSeriesType.SHOWS,
                        filter1(),
                        "(31313,31313)"
                ),
                Arguments.of(
                        "Тест суммы показов фильтра 2",
                        TimeSeriesType.SHOWS,
                        filter2(),
                        "(4343,4343)"
                ),
                Arguments.of(
                        "Тест суммы продаж фильтра 3",
                        TimeSeriesType.ITEMS_DELIVERED,
                        filter3(),
                        "(87,87)"
                )
        );
    }

    static Stream<Arguments> timeSeriesArgs() {
        return Stream.of(
                Arguments.of(
                        "Тест показов фильтра 1",
                        TimeSeriesType.SHOWS,
                        filter1(),
                        "[(5343, 1548968400),(4343, 1556658000),(5943, 1567285200)," +
                                "(4348, 1609448400),(5348, 1612126800),(5988, 1619816400)]"
                ),
                Arguments.of(
                        "Тест корзины фильтра 2",
                        TimeSeriesType.CHECKOUTS,
                        filter2(),
                        "[(1351, 1546290000)]"
                ),
                Arguments.of(
                        "Тест продаж фильтра 3",
                        TimeSeriesType.ITEMS_DELIVERED,
                        filter3(),
                        "[(87, 1567717200)]"
                ),
                Arguments.of(
                        "Тест продаж фильтра 4",
                        TimeSeriesType.ITEMS_DELIVERED,
                        filter4(),
                        "[(17, 1557090000),(17, 1609707600)]"
                )

        );
    }

    static Stream<Arguments> detailTotalArgs() {
        return Stream.of(
                Arguments.of("Тест детализации товаров",
                        new DetailTotalFilter(
                                detailStatFilter(),
                                "ssku",
                                DetailGroupingType.OFFERS
                        ), "data/getDetailTotal-1.json"),
                Arguments.of("Тест детализации категорий",
                        new DetailTotalFilter(
                                detailStatFilter(),
                                "О",
                                DetailGroupingType.CATEGORIES
                        ), "data/getDetailTotal-2.json"),
                Arguments.of("Тест детализации брендов",
                        new DetailTotalFilter(
                                detailStatFilter(),
                                "1",
                                DetailGroupingType.BRANDS
                        ), "data/getDetailTotal-3.json")
        );
    }

    static Stream<Arguments> detailPageArgs() {
        return Stream.of(
                Arguments.of("Тест детализации товаров",
                        new DetailPageFilter(
                                3,
                                3,
                                detailStatFilter(),
                                DetailGroupingType.OFFERS,
                                "р",
                                new DetailSort(DetailSort.SortColumn.ITEMS_DELIVERED, DetailSort.SortDirection.DESC)
                        ), "data/getDetailPage-1.json"),
                Arguments.of("Тест детализации категорий - поиск по названию категории",
                        new DetailPageFilter(
                                0,
                                200,
                                detailStatFilter(),
                                DetailGroupingType.CATEGORIES,
                                "о",
                                new DetailSort(DetailSort.SortColumn.SALES, DetailSort.SortDirection.ASC)
                        ), "data/getDetailPage-2.json"),
                Arguments.of("Тест детализации категорий - поиск по id категории",
                        new DetailPageFilter(
                                0,
                                200,
                                detailStatFilter(),
                                DetailGroupingType.CATEGORIES,
                                "03",
                                new DetailSort(DetailSort.SortColumn.SALES, DetailSort.SortDirection.ASC)
                        ), "data/getDetailPage-3.json"),
                Arguments.of("Тест детализации брендов - поиск по названию бренда",
                        new DetailPageFilter(
                                0,
                                200,
                                detailStatFilter(),
                                DetailGroupingType.BRANDS,
                                "a",
                                null
                        ), "data/getDetailPage-4.json"),
                Arguments.of("Тест детализации брендов - поиск по id бренда",
                        new DetailPageFilter(
                                0,
                                200,
                                detailStatFilter(),
                                DetailGroupingType.BRANDS,
                                "2",
                                null
                        ), "data/getDetailPage-5.json"),
                Arguments.of("Тест детализации товаров (сортировка по цене)",
                        new DetailPageFilter(
                                0,
                                3,
                                detailStatFilter(),
                                DetailGroupingType.OFFERS,
                                "Приставка",
                                new DetailSort(DetailSort.SortColumn.PRICE, DetailSort.SortDirection.DESC)
                        ), "data/getDetailPage-6.json")
        );
    }

    static Stream<Arguments> summaryPageArgs() {
        return Stream.of(
                Arguments.of("Тест тотального отчета",
                        new SummaryFilter(
                                1L,
                                774L,
                                DetalizationType.MONTH,
                                LocalDate.of(2019, 1, 1),
                                LocalDate.of(2022, 2, 1)
                        ), "summary/getSummaryPage-1.json")
        );
    }

    static StatFilter detailStatFilter() {
        return new StatFilter(
                List.of(1L),
                List.of(774L),
                DetalizationType.DAY,
                localDateToInstant(2000, 7, 15),
                localDateToInstant(2030, 12, 31),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    @MethodSource("timeSeriesArgs")
    @ParameterizedTest
    public void testGetTimeSeries(String description,
                                  TimeSeriesType timeSeriesType,
                                  StatFilter statFilter,
                                  String timeSeries) {
        assertEquals(timeSeries,
                statClickHouseDao.getTimeSeries(timeSeriesType, statFilter).toString());
    }

    @MethodSource("timeSeriesSummaryArgs")
    @ParameterizedTest
    public void testGetTimeSeriesSummary(String description,
                                         TimeSeriesType timeSeriesType,
                                         StatFilter statFilter,
                                         String summary) {
        assertEquals(summary,
                statClickHouseDao.getTimeSeriesSummary(timeSeriesType, statFilter).toString());
    }

    @MethodSource("detailTotalArgs")
    @ParameterizedTest
    public void testGetDetailTotal(String description,
                                   DetailTotalFilter statFilter,
                                   String pathToExpectedData) throws IOException {
        var expected = getExpectedData(pathToExpectedData, DetailTotal.class);
        assertEquals(expected, statClickHouseDao.getDetailTotal(statFilter));
    }

    @MethodSource("detailPageArgs")
    @ParameterizedTest
    public void testGetDetailPage(String description,
                                  DetailPageFilter statFilter,
                                  String pathToExpectedData) throws IOException {
        var expected = getExpectedData(pathToExpectedData, DetailPage.class);
        assertEquals(expected, statClickHouseDao.getDetailPage(statFilter));
    }

    @MethodSource("summaryPageArgs")
    @ParameterizedTest
    public void testGetSummaryPage(String description,
                                   SummaryFilter filter,
                                   String pathToExpectedData) throws IOException {
        var actual = statClickHouseDao.getSummaryPage(
                filter.getBusinessId(),
                filter.getPartnerId(),
                filter.getDateFrom(),
                filter.getDateTo(),
                filter.getDetalization()
        );
        var expected = getExpectedData(pathToExpectedData, SummaryPage.class);
        assertEquals(expected, actual);
    }

    private static Instant localDateToInstant(int year, int month, int day) {
        return LocalDate.of(year, month, day)
                .atStartOfDay(MoscowTime.TZ.toTimeZone().toZoneId())
                .toInstant();
    }

    private <T> T getExpectedData(String fileName, Class<T> clazz) throws IOException {
        return MAPPER.readValue(StringTestUtil.getString(getClass(), fileName), clazz);
    }
}
