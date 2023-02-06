package ru.yandex.market.logistics.management.util;

import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.market.logistics.management.configuration.PostgresDatabaseCleaner;

/**
 * This listener "cleans" test databases by invoking {@link PostgresDatabaseCleaner}, which runs
 * {@code RESET IDENTITY CASCADE} for every database present in the test context.
 *
 * This effectively circumvents issues in both @Sql and @DatabaseSetup where the test runner fails to reset the
 * database's identity generator.
 *
 * See https://github.com/springtestdbunit/spring-test-dbunit/issues/131 for an instance of this issue.
 */
public class CleanDbTestExecutionListener extends AbstractTestExecutionListener implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(AbstractTestExecutionListener.class);

    @Override
    public void beforeTestMethod(@Nonnull TestContext testContext) {
        clean(testContext, testContext.getTestMethod(), testContext.getTestClass());
    }

    private void clean(TestContext testContext, AnnotatedElement... targets) {
        if (Stream.of(targets)
            .flatMap(t -> Stream.of(
                    AnnotationUtils.findAnnotation(t, CleanDatabase.class),
                    AnnotationUtils.findAnnotation(t, Sql.class)))
            .anyMatch(Objects::nonNull)) {
            getDatabaseCleaner(testContext).ifPresent(PostgresDatabaseCleaner::truncate);
        }
    }

    private Optional<PostgresDatabaseCleaner> getDatabaseCleaner(TestContext testContext) {
        try {
            return Optional.of(
                testContext
                    .getApplicationContext()
                    .getBean(PostgresDatabaseCleaner.class)
            );
        } catch (Exception e) {
            log.error("Error occurred during DatabaseCleaner retrieval ", e);
            return Optional.empty();
        }
    }

    public int getOrder() {
        return 0;
    }
}
