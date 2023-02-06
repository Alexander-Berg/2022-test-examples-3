package ru.yandex.market.mbi.partner_stat.repository.stat;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partner_stat.config.ClickHouseTestConfig;
import ru.yandex.market.mbi.partner_stat.repository.ClickhouseFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link StatDictionaryClickHouseDao}
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(
        dataSource = ClickHouseTestConfig.DATA_SOURCE,
        type = DataSetType.SINGLE_CSV,
        before = "stat.before.csv"
)
public class StatDictionaryClickHouseDaoTest extends ClickhouseFunctionalTest {

    @Autowired
    private StatDictionaryClickHouseDao statDictionaryClickHouseDao;

    static Stream<Arguments> brandsArgs() {
        return Stream.of(
                Arguments.of("Тест получения брендов",
                        1L,
                        775,
                        List.of(105),
                        "[(13,EA)]"),
                Arguments.of("Тест получения брендов",
                        1L,
                        775,
                        List.of(103),
                        "[(12,Samsung)]")
        );
    }

    static Stream<Arguments> regionsArgs() {
        return Stream.of(
                Arguments.of("Тест получения регионов", 1L, 774, "[(6,Калуга), (7,Кострома)]")
        );
    }

    static Stream<Arguments> brandsFromFiltersArgs() {
        return Stream.of(
                Arguments.of("Тест получения брендов",
                        774,
                        "[(11,Apple), (12,Samsung)]")
        );
    }

    static Stream<Arguments> regionsFromFiltersArgs() {
        return Stream.of(
                Arguments.of("Тест получения регионов", 774, "[(6,Калуга), (7,Кострома)]")
        );
    }

    static Stream<Arguments> categoriesFromFiltersArgs() {
        return Stream.of(
                Arguments.of("Тест получения категорий",
                        774,
                        "[(101,Бытовая техника), (104,Игровые приставки), (102,Компьютеры), (103,Ноутбуки)]")
        );
    }

    @MethodSource("brandsFromFiltersArgs")
    @ParameterizedTest
    public void testGetBrandsFromFilters(String description,
                                         long partnerId,
                                         String summary) {
        assertEquals(summary, statDictionaryClickHouseDao.getBrandsFromFilters(partnerId).toString());
    }

    @MethodSource("regionsFromFiltersArgs")
    @ParameterizedTest
    public void testGetRegionsFromFilters(String description,
                                          long partnerId,
                                          String summary) {
        assertEquals(summary, statDictionaryClickHouseDao.getRegionsFromFilters(partnerId).toString());
    }

    @MethodSource("brandsArgs")
    @ParameterizedTest
    public void testGetBrands(String description,
                              long businessId,
                              long partnerId,
                              List<Long> categories,
                              String summary) {
        assertEquals(summary, statDictionaryClickHouseDao.getBrands(businessId, partnerId, categories).toString());
    }

    @MethodSource("regionsArgs")
    @ParameterizedTest
    public void testGetRegions(String description,
                               long businessId,
                               long partnerId,
                               String summary) {
        assertEquals(summary, statDictionaryClickHouseDao.getRegions(businessId, partnerId).toString());
    }
}
