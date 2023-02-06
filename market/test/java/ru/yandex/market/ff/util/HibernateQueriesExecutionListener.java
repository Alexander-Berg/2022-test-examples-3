package ru.yandex.market.ff.util;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.market.ff.util.query.count.JpaQueriesCount;
import ru.yandex.market.ff.util.query.count.QueriesCountInspector;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слушатель исполняемых JPA запросов
 *
 * @author kotovdv 11/08/2017.
 */
public class HibernateQueriesExecutionListener extends AbstractTestExecutionListener {

    public static final String DESCRIPTION = "Asserting JPA queries count";

    @Override
    public void beforeTestMethod(TestContext testContext) {
        if (testContext == null) {
            return;
        }
        Method testMethod = testContext.getTestMethod();

        JpaQueriesCount jpaQueriesCount = testMethod.getAnnotation(JpaQueriesCount.class);
        if (jpaQueriesCount != null) {
            QueriesCountInspector.reset();
        }
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        if (testContext == null) {
            return;
        }
        Method testMethod = testContext.getTestMethod();

        JpaQueriesCount jpaQueriesCount = testMethod.getAnnotation(JpaQueriesCount.class);
        if (jpaQueriesCount != null) {
            int actual = QueriesCountInspector.getCount();
            int expected = jpaQueriesCount.value();

            if (jpaQueriesCount.isThreshold()) {
                assertThat(actual)
                        .as(DESCRIPTION)
                        .withFailMessage(getErrorMessage(expected))
                        .isLessThanOrEqualTo(expected);
            } else {
                assertThat(actual)
                        .as(DESCRIPTION)
                        .withFailMessage(getErrorMessage(expected))
                        .isEqualTo(expected);
            }
        }
    }

    private static String getErrorMessage(int expectedCount) {
        return String.join("\n",
                "\n",
                "Expected query count: " + expectedCount,
                "Actual query count: " + QueriesCountInspector.getCount(),
                "",
                "Actual query list:",
                QueriesCountInspector.getQueries().stream()
                        .map(HibernateQueriesExecutionListener::buildQueryMessage)
                        .collect(Collectors.joining("\n"))
        );
    }

    private static String buildQueryMessage(QueriesCountInspector.QueryInfo query) {
        return query.getSource() + ":    " + query.getSql();
    }
}
