package ru.yandex.market.sqb.db;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.model.common.HasName;
import ru.yandex.market.sqb.test.ConfigurationReaderUtils;
import ru.yandex.market.sqb.test.YamlUtils;
import ru.yandex.market.sqb.test.db.DbQueryConfigChecker.ParamResultInfo;
import ru.yandex.market.sqb.test.db.DbQueryConfigChecker.QueryResultInfo;
import ru.yandex.market.sqb.test.db.DbUnitUtils;
import ru.yandex.market.sqb.test.db.misc.DbOperationSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.sqb.service.config.ConfigurationReaderFactory.createClasspathReader;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.createReader;
import static ru.yandex.market.sqb.test.db.DbQueryConfigChecker.checkAll;
import static ru.yandex.market.sqb.test.db.DbQueryConfigChecker.checkParameters;
import static ru.yandex.market.sqb.test.db.DbQueryConfigChecker.checkRows;

/**
 * Тесты для проверки интеграции с БД.
 *
 * @author Vladislav Bauer
 */
class GeneratedQueryDbTest {

    static final String SCHEMA_SQL = "schema.sql";
    static final String DATASET_XML = "dataset.xml";

    private static final String RESULT_YAML = "result.yaml";
    private static final String UNIQUE_KEY = "FEED_ID";


    @Test
    void testPositive() throws Exception {
        runDbOperation(() -> {
            final QueryResultInfo resultInfo = checkAll(createReader(ConfigurationReaderUtils.FILE_POSITIVE));
            final List<ParamResultInfo> badParams = resultInfo.getBadParams();

            checkParameters(badParams);

            final List<Map<String, Object>> actualRows = resultInfo.getRows();
            final List<Map<String, Object>> expectedRows = YamlUtils.read(resource(RESULT_YAML));

            final boolean checkOrder = QueryResultInfo.isOrdered(resultInfo);
            checkRows(actualRows, expectedRows, UNIQUE_KEY, checkOrder);
        });
    }

    @Test
    void testNegative() throws Exception {
        runDbOperation(() -> {
            final QueryResultInfo resultInfo = checkAll(createReader(ConfigurationReaderUtils.FILE_INVALID_SQL));
            final List<ParamResultInfo> badParams = resultInfo.getBadParams();
            final List<String> expected = Arrays.asList("URL", "CONSTANT");

            assertThat(badParams, hasSize(expected.size()));
            assertThat(HasName.getNames(badParams), equalTo(expected));
        });
    }


    private void runDbOperation(final DbOperationSet dbOperationSet) throws Exception {
        DbUnitUtils.runWithDb(resource(SCHEMA_SQL), resource(DATASET_XML), dbOperationSet);
    }

    private Supplier<String> resource(final String schemaSql) {
        return createClasspathReader(GeneratedQueryDbTest.class, schemaSql);
    }

}
