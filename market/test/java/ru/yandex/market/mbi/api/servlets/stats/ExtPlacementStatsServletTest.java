package ru.yandex.market.mbi.api.servlets.stats;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.api.servlets.ExtPlacementStatsServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link ExtPlacementStatsServlet}.
 *
 * @author vbudnev
 */
@DisplayName("Выдача сервантлета для /external-placement")
class ExtPlacementStatsServletTest extends FunctionalTest {

    private static final long DISTR_7 = 7L;
    private static final String PARAM_DATE_2018_01_01 = "2018-01-01";
    private static final String PARAM_DATE_2018_12_12 = "2018-12-12";

    @Autowired
    private RestTemplate restTemplate;

    @DisplayName("Если показов меньше чем кликов, то синхронизируем")
    @Test
    @DbUnitDataSet(before = "db/cpc_stats.shows_lt_clicks.before.csv")
    void test_cpcStats_showsLtClick() {
        final String url = buildUrlWithHeaders(PARAM_DATE_2018_01_01, PARAM_DATE_2018_12_12, DISTR_7).toString();
        loadAndCompare(url, "cpc_stats.shows_lt_clicks.csv");
    }

    @DisplayName("Группировка по дням")
    @Test
    @DbUnitDataSet(before = "db/cpc_stats.group_by_day.before.csv")
    void test_cpcStats_groupBy() {
        final String url = buildUrlWithHeaders(PARAM_DATE_2018_01_01, PARAM_DATE_2018_12_12, DISTR_7).toString();
        loadAndCompare(url, "cpc_stats.group_by_day.csv");
    }

    private URIBuilder buildUrlWithHeaders(String dateFrom, String dateTo, long distrType) {
        return new URIBuilder()
                .setScheme("http")
                .setHost("localhost")
                .setPort(port)
                .setPath("/external-placement")
                .addParameter("header", "1")
                .setParameter("from_date", dateFrom)
                .setParameter("to_date", dateTo)
                .setParameter("distr_type", Long.toString(distrType));
    }

    private void loadAndCompare(String url, String fileName) {
        final String response = restTemplate.getForObject(url, String.class);
        final List<String> responseAsStrings;
        if (StringUtils.isEmpty(response)) {
            responseAsStrings = Collections.emptyList();
        } else {
            responseAsStrings = Arrays.stream(response.split("\n"))
                    .map(stringLine -> {
                        stringLine = ExtPlacementStatsServletTestLoader.reformate(stringLine, false);
                        if (stringLine.length() > 0 && stringLine.charAt(stringLine.length() - 1) == ';') {
                            return stringLine.substring(0, stringLine.length() - 1);
                        }
                        return stringLine;
                    }).collect(Collectors.toList());
        }

        assertThat(responseAsStrings)
                .containsExactly(
                        StringTestUtil.getString(
                                ExtPlacementStatsServletTestLoader.class,
                                fileName
                        ).split("\n"));
    }

}
