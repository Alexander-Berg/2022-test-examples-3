package ru.yandex.market.markup3.mboc

import io.kotest.matchers.doubles.shouldBeGreaterThan
import org.junit.Test

class MbocTaskGeneratorTest {
    @Test
    fun `Test priority`() {
        val deadlineFurther = PriorityUtil.calculatePriority(10000, true)
        val deadlineNearer = PriorityUtil.calculatePriority(9999, false)
        deadlineNearer shouldBeGreaterThan deadlineFurther

        val critical = PriorityUtil.calculatePriority(1, true)
        val nonCritical = PriorityUtil.calculatePriority(1, false)
        critical shouldBeGreaterThan nonCritical
    }
}
