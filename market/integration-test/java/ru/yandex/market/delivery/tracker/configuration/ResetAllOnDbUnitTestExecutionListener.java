package ru.yandex.market.delivery.tracker.configuration;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableSet;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class ResetAllOnDbUnitTestExecutionListener extends AbstractTestExecutionListener {
    private static final ImmutableSet<String> IGNORE = ImmutableSet.<String>builder()
        .add("document_template",
            "databasechangelog",
            "databasechangeloglock",
            "job_monitoring_config",
            "qrtz_triggers")
        .build();

    private final DataSetProcessor dataSetProcessor = new DataSetProcessor(IGNORE);

    @Override
    public void beforeTestMethod(final TestContext testContext) throws Exception {
        final DatabaseSetup methodDataSet = testContext.getTestMethod().getAnnotation(DatabaseSetup.class);
        final DatabaseSetup classDataSet = testContext.getTestClass().getAnnotation(DatabaseSetup.class);
        if (methodDataSet != null || classDataSet != null) {
            cleanDatabase(testContext);
        }
    }

    private void cleanDatabase(final TestContext testContext) throws SQLException {
        final DataSource dataSource = testContext.getApplicationContext()
            .getBean("dataSource", DataSource.class);

        dataSetProcessor.truncateAllTables(dataSource);
    }
}
