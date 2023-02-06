package ru.yandex.market.liquibase.model;

import java.io.File;
import java.io.StringReader;
import java.sql.CallableStatement;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.config.DevIntegrationTest;
import ru.yandex.market.liquibase.misc.RQueryXmlParser;

/**
 * Интеграционный тест проверяющий корректность SQL-запросов отчетов mbi-admin на базе данных БТ.
 */
class RQueryXmlParserIntegrationTest extends DevIntegrationTest {
    private static final String REPORT_VALIDATION_QUERY = "" +
            "declare" +
            "   cursor_id integer;" +
            "begin" +
            "   cursor_id := dbms_sql.open_cursor;" +
            "   dbms_sql.parse(cursor_id, statement => ?, language_flag => dbms_sql.native);" +
            "   dbms_sql.close_cursor(cursor_id);" +
            "exception when others then" +
            "   if dbms_sql.is_open(cursor_id) then" +
            "       dbms_sql.close_cursor(cursor_id);" +
            "   end if;" +
            "   raise;" +
            "end;";

    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.liquibase.model.RQueryXmlParserFunctionalTest#testReportData")
    void testReport(String fileName, File file) throws Exception {
        RQuery reportQuery = RQueryXmlParser.parse(file);
        RQueryXmlParserFunctionalTest.skipTestIfForYql(reportQuery);

        String query = reportQuery.getQuery();
        jdbcTemplate.execute(REPORT_VALIDATION_QUERY, (CallableStatement cs) -> {
            cs.setClob(1, new StringReader(query));
            cs.execute();
            return null;
        });
    }

}
