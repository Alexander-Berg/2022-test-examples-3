package ru.yandex.market.logistics.test.integration.jpa;

import java.lang.reflect.Method;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слушатель исполняемых JPA запросов
 */
@ParametersAreNonnullByDefault
public class HibernateQueriesExecutionListener extends AbstractTestExecutionListener {

    private static final String DESCRIPTION = "Asserting JPA queries count";

    @Override
    public void beforeTestMethod(TestContext testContext) {
        resetInspectors();
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        try {
            Method testMethod = testContext.getTestMethod();

            JpaQueriesCount jpaQueriesCount = testMethod.getAnnotation(JpaQueriesCount.class);
            if (jpaQueriesCount == null) {
                return;
            }

            Integer actual = QueriesContentInspector.getCount();
            if (QueriesContentInspector.getCount() == 0) {
                actual = MultiplyThreadsQueriesInspector.getCount();
            }

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
        } finally {
            resetInspectors();
        }
    }

    private void resetInspectors() {
        QueriesContentInspector.reset();
        MultiplyThreadsQueriesInspector.reset();
    }
}
