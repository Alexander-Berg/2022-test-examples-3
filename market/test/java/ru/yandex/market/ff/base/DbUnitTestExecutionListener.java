package ru.yandex.market.ff.base;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableSet;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Обработчик тестов для очистки базы перед выполнением теста.
 *
 * @author avetokhin 11/01/18.
 */
public class DbUnitTestExecutionListener extends AbstractTestExecutionListener {
    private static final ImmutableSet<String> IGNORE = ImmutableSet.<String>builder()
            .add("document_template", "databasechangelog", "databasechangeloglock", "request_type", "request_status",
                    "vat_rate", "request_subtype")
            .build();

    private final DataSetProcessor dataSetProcessor = new DataSetProcessor(IGNORE);

    @Override
    public void beforeTestMethod(final TestContext testContext) throws Exception {
        final DatabaseSetup[] methodDataSet = testContext.getTestMethod().getAnnotationsByType(DatabaseSetup.class);
        final DatabaseSetup[] classDataSet = testContext.getTestClass().getAnnotationsByType(DatabaseSetup.class);
        if (methodDataSet.length != 0 || classDataSet.length != 0) {
            cleanDatabase(testContext);
        }
    }

    private void cleanDatabase(final TestContext testContext) throws SQLException {
        final DataSource dataSource = testContext.getApplicationContext()
                .getBean("dataSource", DataSource.class);

        dataSetProcessor.truncateAllTables(dataSource);
        dataSetProcessor.resetAllSequences(dataSource);
    }

}
