package ru.yandex.market.mbi.api.servlets.stats;

import java.time.LocalDate;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.servlets.ExtPlacementCpaStatsServletV2;

/**
 * Тесты для {@link ExtPlacementCpaStatsServletV2}.
 *
 * @author magomed
 */
@DisplayName("Выдача сервантлета для /external-placement-cpa-v2")
@DbUnitDataSet(before = {
        "db/orders.before.csv",
        "db/cpa_stats.distributions_colors.csv"
})
class ExtPlacementCpaStatsServletV2Test extends ExtPlacementStatsServletTestLoader {
    private static final long DISTR_7 = 7L;
    private static final String PARAM_DATE_2018_01_01 = "2018-01-01";
    private static final String PARAM_DATE_2018_12_12 = "2018-12-12";
    private static final String PARAM_DATE_2018_11_16 = "2018-11-16";
    private static final String PARAM_DATE_2018_11_19 = "2018-11-19";
    private static final String PARAM_DATE_2019_11_19 = "2019-11-19";

    @DisplayName("Выдача в формате CSV без заголовков")
    @Test
    @DbUnitDataSet(before = "db/cpa_stats_v2.no_header.before.csv")
    void testGeneralCsv() {
        final String url = buildUrl(PARAM_DATE_2018_01_01, PARAM_DATE_2018_12_12, DISTR_7).toString();
        loadAndCompare(url, "cpa_stats_no_header.csv");
    }

    @DisplayName("Фильтрация по времени - интервал")
    @Test
    @DbUnitDataSet(before = "db/cpa_stats_v2.date_filter.before.csv")
    void testCpaStatsDateFilter() {
        loadAndCompare(
                buildUrlWithHeaders(PARAM_DATE_2018_11_16, PARAM_DATE_2018_11_19, DISTR_7),
                "cpa_stats.date_filter.csv"
        );
    }

    @DisplayName("Ограничиваем точность десятичного значения для суммы")
    @Test
    @DbUnitDataSet(before = "db/cpa_stats_v2.limit_decimal_precision.before.csv")
    void testCpaStatsLimitDecimalPrecision() {
        loadAndCompare(
                buildUrlWithHeaders(PARAM_DATE_2018_11_16, PARAM_DATE_2018_11_19, DISTR_7),
                "cpa_stats.limit_decimal_precision.csv"
        );
    }

    @DisplayName("Проверяем границу дейстивия vat 18 vs 20")
    @Test
    @DbUnitDataSet(before = "db/cpa_stats_v2.vat_switch_check.before.csv")
    void testCpaStatsVatSwitchCheck() {
        loadAndCompare(
                buildUrlWithHeaders(PARAM_DATE_2018_01_01, PARAM_DATE_2019_11_19, DISTR_7),
                "cpa_stats.vat_switch_check.csv"
        );
    }

    @DisplayName("Фильтрация по времени - hold period")
    @Test
    @DbUnitDataSet(before = "db/cpa_stats_v2.hold_period.before.csv")
    void testCpaStatsHoldPeriod() {
        final String today = LocalDate.now().toString();
        loadAndCompare(
                buildUrlWithHeaders(PARAM_DATE_2018_01_01, today, DISTR_7),
                "cpa_stats.hold_period.csv"
        );
    }

    @DisplayName("Агрегация по date,clid,vid")
    @Test
    @DbUnitDataSet(before = "db/cpa_stats_v2.aggregation.before.csv")
    void testCpaStatsAggregation() {
        loadAndCompare(
                buildUrlWithHeaders(PARAM_DATE_2018_01_01, PARAM_DATE_2018_12_12, DISTR_7),
                "cpa_stats.aggregation.csv"
        );
    }

    @DisplayName("Не сматченные в click info")
    @Test
    @DbUnitDataSet(before = "db/cpa_stats_v2.not_matched_by_clickinfo.before.csv")
    void testNotMatchedClickInfo() {
        loadAndCompare(
                buildUrlWithHeaders(PARAM_DATE_2018_01_01, PARAM_DATE_2018_12_12, DISTR_7),
                "cpa_stats.not_matched_by_clickinfo.csv"
        );
    }

    @DisplayName("Различные vid")
    @Test
    @DbUnitDataSet(before = "db/cpa_stats_v2.vid.before.csv")
    void testCpaStatsVid() {
        loadAndCompare(
                buildUrlWithHeaders(PARAM_DATE_2018_01_01, PARAM_DATE_2018_12_12, DISTR_7),
                "cpa_stats.vid.csv"
        );
    }

    @DisplayName("Различные clid")
    @Test
    @DbUnitDataSet(before = "db/cpa_stats_v2.clid.before.csv")
    void testCpaStatsClid() {
        loadAndCompare(
                buildUrlWithHeaders(PARAM_DATE_2018_01_01, PARAM_DATE_2018_12_12, DISTR_7),
                "cpa_stats.clid.csv"
        );
    }

    @Test
    @DisplayName("Тест на то, что данные не отдаются в баланс, если возвратов больше чем самих айтемов")
    @DbUnitDataSet(before = "db/cpa_stats_v2.big_returns.before.csv")
    void testBigReturnsCount() {
        loadAndCompare(
                buildUrlWithHeaders(PARAM_DATE_2018_01_01, PARAM_DATE_2018_12_12, DISTR_7),
                "cpa_stats.big_returns.csv"
        );
    }

    private String buildUrlWithHeaders(String dateFrom, String dateTo, long distrType) {
        return buildUrl(dateFrom, dateTo, distrType)
                .addParameter("header", "1")
                .toString();
    }

    private URIBuilder buildUrl(String dateFrom, String dateTo, long distrType) {
        return buildUrl(dateFrom, dateTo)
                .setParameter("distr_type", Long.toString(distrType));
    }

    private URIBuilder buildUrl(String dateFrom, String dateTo) {
        return new URIBuilder()
                .setScheme("http")
                .setHost("localhost")
                .setPort(port)
                .setPath("/external-placement-cpa")
                .setParameter("from_date", dateFrom)
                .setParameter("to_date", dateTo);
    }
}
