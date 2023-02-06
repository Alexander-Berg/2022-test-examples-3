package ru.yandex.market.abo.core;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.report.Const;
import ru.yandex.common.report.tabular.ReportBuilder;
import ru.yandex.common.report.tabular.db.DbReportFactory;
import ru.yandex.common.util.parameters.ParametersSource;
import ru.yandex.common.util.parameters.ParametersSourceImpl;
import ru.yandex.market.abo.util.db.DbUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author kukabara
 */
public class ReportQueryTest extends EmptyTest {
    @Autowired
    private JdbcTemplate pgJdbcTemplate;
    @Autowired
    private DbReportFactory dbReportFactory;

    @Test
    public void testAllQueryAreValid() {
        List<Integer> exceptIds = Arrays.asList(6);
        List<String> names = pgJdbcTemplate.queryForList("SELECT name FROM report_query " +
                "WHERE query NOT LIKE '%arbitrage.%' " +
                (!exceptIds.isEmpty() ? " AND id NOT IN " + DbUtils.getInSection(exceptIds) : "") +
                " ORDER BY id", String.class);
        for (String name : names) {
            try {
                ParametersSource paramSource = new ParametersSourceImpl();
                paramSource.setParam(Const.QUERY_NAME_PARAM, name);
                ReportBuilder reportBuilder = dbReportFactory.createReportBuilder(paramSource);
                assertNotNull(reportBuilder.createReport(), "Null report " + name);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Invalid report_query " + name + ": " + e.getMessage());
            }
        }
    }
}
