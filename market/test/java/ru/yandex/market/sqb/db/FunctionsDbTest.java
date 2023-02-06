package ru.yandex.market.sqb.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.test.db.misc.DbOperation;
import ru.yandex.market.sqb.test.db.misc.DbOperationSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.sqb.db.GeneratedQueryDbTest.DATASET_XML;
import static ru.yandex.market.sqb.db.GeneratedQueryDbTest.SCHEMA_SQL;
import static ru.yandex.market.sqb.service.config.ConfigurationReaderFactory.createClasspathReader;
import static ru.yandex.market.sqb.test.db.DbUnitUtils.runWithDb;
import static ru.yandex.market.sqb.test.db.DbUtils.runWithStatement;

/**
 * Тест для проверки стандартных и user-defined functions.
 *
 * @author Vladislav Bauer
 * @see #CUSTOM_FUNCTIONS_SQL
 */
class FunctionsDbTest {

    private static final String SEPARATOR = SystemUtils.LINE_SEPARATOR;

    private static final String CUSTOM_FUNCTIONS_SQL = "custom-functions.sql";
    private static final String STANDARD_FUNCTIONS_TEST_SQL = "functions-test.sql";

    private static final String FUNCTION_STRSUM_TEST_SQL = "function-strsum-test.sql";
    private static final String FUNCTION_JSON_VALUE_TEST_SQL = "function-json_value-test.sql";

    private static final String FUNCTION_STRSUM_EXPECTED = "1=0;2=0;";
    private static final String FUNCTION_JSON_VALUE_EXPECTED = "Hello, World!";

    @Test
    void testFunctions() throws Exception {
        runDbOperation(statement -> execSqlAndCheck(statement, STANDARD_FUNCTIONS_TEST_SQL));
    }

    @Test
    void testStrSumFunction() throws Exception {
        checkFunction(FUNCTION_STRSUM_TEST_SQL, FUNCTION_STRSUM_EXPECTED);
    }

    @Test
    void testJsonValueFunction() throws Exception {
        checkFunction(FUNCTION_JSON_VALUE_TEST_SQL, FUNCTION_JSON_VALUE_EXPECTED);
    }


    private void checkFunction(final String sqlScript, final String expectedValue) throws Exception {
        runDbOperation(statement -> {
            final ResultSet resultSet = execSqlAndCheck(statement, sqlScript);
            assertThat(resultSet.next(), equalTo(true));

            final String value = resultSet.getString(1);
            assertThat(value, equalTo(expectedValue));
            assertThat(resultSet.next(), equalTo(false));

            return resultSet;
        });
    }

    private ResultSet execSqlAndCheck(final Statement statement, final String sqlScript) throws SQLException {
        final String sql = createReader(sqlScript).get();
        final ResultSet resultSet = statement.executeQuery(sql);

        assertThat(resultSet, notNullValue());

        return resultSet;
    }

    private Supplier<String> createReader(final String fileName) {
        return createClasspathReader(getClass(), fileName);
    }

    private void runDbOperation(final DbOperation<?> dbOperation) throws Exception {
        final DbOperationSet dbOperationSet = () -> runWithStatement(dbOperation);
        final Supplier<String> dataSetXmlReader = createReader(DATASET_XML);
        final Supplier<String> schemaSqlReader =
                () -> Stream.of(createReader(SCHEMA_SQL), createReader(CUSTOM_FUNCTIONS_SQL))
                        .map(Supplier::get)
                        .collect(Collectors.joining(SEPARATOR));

        runWithDb(schemaSqlReader, dataSetXmlReader, dbOperationSet);
    }

}
