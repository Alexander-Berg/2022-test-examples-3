package ru.yandex.market.logistics.mqm.tms.housekeeping

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest

class RescheduleFailedPlanFactsExecutorTest: AbstractContextualTest() {

    @Autowired
    lateinit var executor: RescheduleFailedPlanFactsExecutor

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-12-31T07:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Упавшие план-факты возвращаются обратно в обработку")
    @DatabaseSetup("/tms/RescheduleFailedPlanFactsExecutor/setup.xml")
    @ExpectedDatabase(
        value = "/tms/RescheduleFailedPlanFactsExecutor/rescheduled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun rescheduleFailed() {
        executor.doJob(null)
    }

    @Test
    @DisplayName("Упавшие план-факты, не возвращаются в обработку, если превышены попытки")
    @DatabaseSetup("/tms/RescheduleFailedPlanFactsExecutor/setup_max_attempts.xml")
    @ExpectedDatabase(
        value = "/tms/RescheduleFailedPlanFactsExecutor/setup_max_attempts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun notRescheduleIfLimitExceeded() {
        executor.doJob(null)
    }
}
