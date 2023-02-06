package ru.yandex.market.banner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.banner.model.BannerDisplayType;
import ru.yandex.market.core.banner.model.BannerType;

/**
 * Тесты для {@link ImportBannerDao}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ImportBannerDaoTest extends FunctionalTest {

    @Autowired
    private ImportBannerDao importBannerDao;

    static Stream<Arguments> testCheckShopSqlData() {
        return Stream.of(
                Arguments.of(
                        "Невалидный sql",
                        "bad sql",
                        List.of("bad SQL grammar")
                ),
                Arguments.of(
                        "sql вернул 2 столбца",
                        "select 1, 2 from dual",
                        List.of("Shop sql contains invalid column number")
                ),
                Arguments.of(
                        "sql вернул столбец-строку",
                        "select 'abc' as shop_id from dual",
                        List.of("Data conversion error converting", "Bad value for type long")
                )
        );
    }

    @Test
    @DisplayName("Удаление баннеров по списку id")
    @DbUnitDataSet(before = "csv/ImportBannerDao.testDeleteBanners.before.csv", after = "csv/ImportBannerDao.testDeleteBanners.after.csv")
    void testDeleteBanners() {
        importBannerDao.deleteBanners(List.of("banner101", "banner102"));
    }

    @Test
    @DisplayName("Получение существующих баннеров")
    @DbUnitDataSet(before = "csv/ImportBannerDao.testGetBannersIds.before.csv")
    void testGetBannersIds() {
        final List<String> expected = List.of("banner101", "banner102", "banner103", "banner104", "banner105");
        final List<String> actual = importBannerDao.getBannersIds();
        MatcherAssert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @ParameterizedTest
    @MethodSource("testCheckShopSqlData")
    @DisplayName("Валидация sql для получения списка партнеров")
    @DbUnitDataSet(before = "csv/ImportBannerDao.testCheckShopSql.before.csv")
    void testCheckShopSql(final String name, final String sql, final List<String> expectedErrors) {
        final Exception error = Assertions.assertThrows(
                Exception.class,
                () -> importBannerDao.insertBanner("banner_id", BannerType.GENERAL, sql, BannerDisplayType.NONE)
        );
        final String actual = error.getMessage();
        Assertions.assertFalse(expectedErrors.stream()
                        .filter(actual::contains)
                        .collect(Collectors.toList())
                        .isEmpty(),
                "Actual: " + actual);
    }

    @Test
    @DisplayName("Вставка обычного баннера по sql")
    @DbUnitDataSet(before = "csv/ImportBannerDao.testInsertGeneralBannerBySql.before.csv", after = "csv/ImportBannerDao.testInsertGeneralBannerBySql.after.csv")
    void testInsertGeneralBannerBySql() {
        importBannerDao.insertBanner("banner106", BannerType.GENERAL, "select id as shop_id from shops_web.partner",
                BannerDisplayType.NONE);
    }

    @Test
    @DisplayName("Вставка обычного баннера по списку")
    @DbUnitDataSet(before = "csv/ImportBannerDao.testInsertGeneralBannerBySql.before.csv", after = "csv/ImportBannerDao.testInsertGeneralBannerBySql.after.csv")
    void testInsertGeneralBannerByList() {
        importBannerDao.insertBanner("banner106", BannerType.GENERAL, List.of(1001L, 1002L),
                BannerDisplayType.NONE);
    }

    @Test
    @DisplayName("Вставка обычного баннера для партнера с убер-баннером")
    @DbUnitDataSet(before = "csv/ImportBannerDao.testInsertGeneralBannerBySql.before.csv", after = "csv/ImportBannerDao.testInsertGeneralBannerBySql.after.csv")
    void testInsertGeneralBannerForUber() {
        importBannerDao.insertBanner("banner106", BannerType.GENERAL, List.of(124L, 1001L, 1002L),
                BannerDisplayType.NONE);
    }

    @Test
    @DisplayName("Вставка убер баннера по sql")
    @DbUnitDataSet(before = "csv/ImportBannerDao.testInsertGeneralBannerBySql.before.csv",
            after = "csv/ImportBannerDao.testInsertUberBannerBySql.after.csv")
    void testInsertUberBannerBySql() {
        importBannerDao.insertBanner("banner106", BannerType.UBER, "select id as shop_id from shops_web.partner",
                BannerDisplayType.NONE);
    }

    @Test
    @DisplayName("Вставка обычных алерт-баннеров с отображением на сводке бизнеса")
    @DbUnitDataSet(before = "csv/ImportBannerDao.testInsertGeneralBannerBySql.before.csv",
            after = "csv/ImportBannerDao.testInsertBusinessBannersByList.after.csv")
    void testInsertBusinessBannersByList() {
        importBannerDao.insertBanner("bus_banner", BannerType.GENERAL, List.of(123L, 124L, 1001L, 1002L),
                BannerDisplayType.ALERT);
    }

    @Test
    @DisplayName("Вставка обычных рекламных баннеров с отображением на сводке бизнеса по sql")
    @DbUnitDataSet(before = "csv/ImportBannerDao.testInsertGeneralBannerBySql.before.csv", after = "csv/ImportBannerDao.testInsertBusinessBannersBySql.after.csv")
    void testInsertBusinessBannersBySql() {
        importBannerDao.insertBanner("bus_banner", BannerType.GENERAL, "select id as shop_id from shops_web.partner",
                BannerDisplayType.ADVERTISING);
    }
}
