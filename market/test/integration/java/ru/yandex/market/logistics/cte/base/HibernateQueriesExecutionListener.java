package ru.yandex.market.logistics.cte.base;

import java.lang.reflect.Method;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;

public class HibernateQueriesExecutionListener extends AbstractTestExecutionListener {

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

            assertThat(actual)
                    .as("Asserting JPA queries count")
                    .isEqualTo(expected);
        }
    }
}
