package ru.yandex.market.liquibase.model;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.core.database.FunctionalTest;
import ru.yandex.market.liquibase.misc.RQueryXmlParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Интеграционный тест проверяющий корректность SQL-запросов отчетов mbi-admin.
 */
class RQueryXmlParserFunctionalTest extends FunctionalTest {
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Map<String, String> SEEN_REPORT_NAMES = new ConcurrentHashMap<>(128);
    private static final String CHECKED_REPORTS_MD5 = "3d45db71eb13dd4bf1e5fe4a2a8fce77";

    @Deprecated(forRemoval = true, since = "should be fixed for PG or dropped entirely")
    private static final Set<String> IGNORED = Set.of(
            "balanceOfShopOff",
            "cpaLimitations",
            "customShopsFromParam",
            "dailyClicksMinusRollbackFullInfo",
            "deliveryShipments",
            "direct:moneyByPof",
            "emailsByShops",
            "industrialManagers",
            "largestRecentUploadLeaps",
            "market_categories",
            "money:balanceRemains",
            "money:dailyCorrections",
            "money:fishkiOtkrutkiByShops",
            "money:moneyStatsNew",
            "money:regionalManagerMonthMoney",
            "money:yearByYear",
            "newRegionalShops",
            "oldCorrections",
            "onlyUkrainaShops",
            "otkrutkiByManager",
            "overdraftControlExclusions",
            "ownRegionStats",
            "prepayStatuses",
            "promoShops",
            "regionHistory",
            "shopClicksByCategory",
            "shopCutoffsStat",
            "shopDeliveryInfo",
            "shopFeeCharges",
            "shopForDelivery",
            "shopsFromRegion",
            "shopsWithParam",
            "sklikClickByRegion",
            "statClicks",
            "supplierInfo",
            "totalClicksByPeriod_Shop_HyperId",
            "wizardsMoney",
            "workingOrRecentlyCutShopsUni",
            "" // tail
    );

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    static Stream<Arguments> testReportData() throws IOException {
        var resolver = ResourcePatternUtils.getResourcePatternResolver(null);
        var resources = resolver.getResources("shops_web/report_query/xml/*");
        return Arrays.stream(resources)
                .map(r -> {
                    try {
                        return Arguments.of(r.getFilename(), r.getFile());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    static void skipTestIfForYql(RQuery reportQuery) {
        assumeThat(reportQuery.getJdbcTemplate()).isNotEqualToIgnoringCase("yqlJdbcTemplate");
    }

    /**
     * @see ru.yandex.market.admin.ui.model.report.ReportQueryParamType
     * @see ru.yandex.market.admin.ui.client.reports.UniReportPropertiesPanel.ButtonClickListener#convertParamValue
     * @see ru.yandex.common.report.tabular.AbstractReportFactory#fillResult
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testReportData")
    void testReport(String fileName, File file) throws Exception {
        var reportQuery = RQueryXmlParser.parse(file);
        var fileName2 = SEEN_REPORT_NAMES.put(reportQuery.getName(), fileName);
        assertThat(fileName2)
                .as("Reports [%s] and [%s] have same name [%s]", fileName, fileName2, reportQuery.getName())
                .isNull();
        skipTestIfForYql(reportQuery);
        assumeThat(IGNORED).doesNotContain(reportQuery.getName());

        var query = reportQuery.getQuery();
        var queryParams = new MapSqlParameterSource();
        for (var param : reportQuery.getInputParameters()) {
            queryParams.addValue(param.getName(), valueFor(param), Types.VARCHAR);
        }
        jdbcTemplate.query(query, queryParams, ResultSet::next);
    }

    @Nullable
    static String valueFor(RQueryParam param) {
        switch (param.getName()) {
            case "dt":
            case "dt_start":
                // обязательно непустые параметры
                return LocalDate.now().format(DT_FORMAT);
            case "fdt":
                return LocalDate.now().minusDays(7).format(DT_FORMAT);
            default:
                return null;
        }
    }

    /**
     * @see ru.yandex.market.liquibase.model.RQueryXmlParserIntegrationTest
     */
    @AfterAll
    static void tearDown() {
        var checkedReports = SEEN_REPORT_NAMES.keySet().stream().sorted().collect(Collectors.joining());
        var md5 = DigestUtils.md5Hex(checkedReports);
        assertThat(md5)
                .as("после изменения доступных репортов прогони" +
                        " интеграционный тест RQueryXmlParserIntegrationTest, лежащий в mbi-core," +
                        " и после того как убедишься что всё работает и в этом тесте и в том," +
                        " поменяй константу с md5 в этом тесте на новую")
                .isEqualTo(CHECKED_REPORTS_MD5);
    }
}
