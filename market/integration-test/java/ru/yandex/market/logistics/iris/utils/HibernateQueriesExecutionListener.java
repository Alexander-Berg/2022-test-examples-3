package ru.yandex.market.logistics.iris.utils;

import java.lang.reflect.Method;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.market.logistics.iris.utils.query.JpaQueriesCount;
import ru.yandex.market.logistics.iris.utils.query.QueriesCountInspector;

import static org.assertj.core.api.Assertions.assertThat;

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
            Integer actual = QueriesCountInspector.getCount().orElse(0);
            int expected = jpaQueriesCount.value();

            if (jpaQueriesCount.isThreshold()) {
                assertThat(actual)
                        .as(DESCRIPTION)
                        .isLessThanOrEqualTo(expected);
            } else {
                assertThat(actual)
                        .as(DESCRIPTION)
                        .isEqualTo(expected);
            }
        }
    }
}
