package ru.yandex.market.logistics.cte.base;

import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.annotation.ExpectedDatabases;
import com.google.common.collect.ImmutableSet;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class DbUnitTestExecutionListener extends AbstractTestExecutionListener {
    private static final ImmutableSet<String> IGNORE = ImmutableSet.<String>builder()
            .add("databasechangelog", "databasechangeloglock")
            .build();

    private final DataSetProcessor dataSetProcessor = new DataSetProcessor(IGNORE);

    @Override
    public void beforeTestMethod(final TestContext testContext) throws Exception {
        Stream.of(testContext.getTestMethod(), testContext.getTestClass())
                .flatMap(o -> Stream.of(
                        o.getAnnotation(DatabaseSetup.class),
                        o.getAnnotation(DatabaseSetups.class),
                        o.getAnnotation(ExpectedDatabase.class)
                ))
                .filter(Objects::nonNull)
                .findAny()
                .ifPresent(annotation -> cleanDatabase(testContext));
    }

    private void cleanDatabase(final TestContext testContext) {
        DataSource dataSource = testContext.getApplicationContext()
                .getBean("dataSource", DataSource.class);

        try {
            dataSetProcessor.truncateAllTables(dataSource);
            dataSetProcessor.resetAllSequences(dataSource);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
