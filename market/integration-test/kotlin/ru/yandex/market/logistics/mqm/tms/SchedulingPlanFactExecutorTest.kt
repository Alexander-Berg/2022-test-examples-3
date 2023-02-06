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

@DisplayName("Тест джобы планирования обработки план-фактов")
class SchedulingPlanFactExecutorTest: AbstractContextualTest() {

    @Autowired
    private lateinit var schedulingPlanFactExecutor: SchedulingPlanFactExecutor

    @BeforeEach
    private fun setUp(){
        clock.setFixed(Instant.parse("2021-10-06T15:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DatabaseSetup("/tms/SchedulingPlanFactExecutor/before/successScheduling.xml")
    @ExpectedDatabase(
        value = "/tms/SchedulingPlanFactExecutor/after/successScheduling.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное планирование created и active план-фактов")
    fun successScheduling() {
        schedulingPlanFactExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/SchedulingPlanFactExecutor/before/doNotScheduleUnwanted.xml")
    @ExpectedDatabase(
        value = "/tms/SchedulingPlanFactExecutor/after/doNotScheduleUnwanted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не планировать ненужные план-факты")
    fun doNotScheduleUnwanted() {
        schedulingPlanFactExecutor.doJob(null)
    }
}
