package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import java.time.Instant

internal class SchedulingPlanFactGroupExecutorTest: AbstractContextualTest() {

    @Autowired
    private lateinit var schedulingPlanFactGroupExecutor: SchedulingPlanFactGroupExecutor

    @BeforeEach
    private fun setUp(){
        clock.setFixed(Instant.parse("2021-10-06T12:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }
    @Test
    @DatabaseSetup("/tms/SchedulingPlanFactGroupExecutor/before/successScheduling.xml")
    @ExpectedDatabase(
        value = "/tms/SchedulingPlanFactGroupExecutor/after/successScheduling.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное планирование created и active группы")
    fun successScheduling() {
        schedulingPlanFactGroupExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/SchedulingPlanFactGroupExecutor/before/doNotScheduleUnwanted.xml")
    @ExpectedDatabase(
        value = "/tms/SchedulingPlanFactGroupExecutor/after/doNotScheduleUnwanted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не планировать ненужные группы")
    fun doNotScheduleUnwanted() {
        schedulingPlanFactGroupExecutor.doJob(null)
    }
}

