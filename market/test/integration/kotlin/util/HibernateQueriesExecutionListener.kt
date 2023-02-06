package ru.yandex.market.logistics.calendaring.util

import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount
import java.lang.reflect.Method


class HibernateQueriesExecutionListener : AbstractTestExecutionListener() {

    override fun beforeTestMethod(testContext: TestContext?) {
        if (testContext == null) {
            return
        }
        val testMethod: Method = testContext.testMethod
        val jpaQueriesCount: JpaQueriesCount? = testMethod.getAnnotation(JpaQueriesCount::class.java)
        if (jpaQueriesCount != null) {
            QueriesCountInspector.reset()
        }
    }

    @Throws(Exception::class)
    override fun afterTestMethod(testContext: TestContext?) {
        if (testContext == null) {
            return
        }
        val testMethod: Method = testContext.testMethod
        val jpaQueriesCount: JpaQueriesCount? = testMethod.getAnnotation(JpaQueriesCount::class.java)
        if (jpaQueriesCount != null) {
            val actual: Int = QueriesCountInspector.getCount()
            val expected: Int = jpaQueriesCount.value
            if (jpaQueriesCount.isThreshold) {
                assertThat(actual)
                    .`as`("Asserting JPA queries count")
                    .withFailMessage(getErrorMessage(expected))
                    .isLessThanOrEqualTo(expected)
            } else {
                assertThat(actual)
                    .`as`("Asserting JPA queries count")
                    .withFailMessage(getErrorMessage(expected))
                    .isEqualTo(expected)
            }
        }
    }

    private fun getErrorMessage(expectedCount: Int): String? {
        return java.lang.String.join("\n",
            "\n",
            "Expected query count: $expectedCount",
            "Actual query count: " + QueriesCountInspector.getCount(),
            "",
            "Actual query list:",
            QueriesCountInspector.getQueries().joinToString("\n") { query ->
                query.source + ":    " + query.sql
            }

        )
    }
}
