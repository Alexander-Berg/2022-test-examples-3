package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.handler.QualityRuleHandler
import java.time.Instant

class ShootingSingleProcessorTest: AbstractContextualTest() {

    @Autowired
    @Qualifier("planFactHandler")
    private lateinit var planFactHandler: QualityRuleHandler

    @BeforeEach
    fun setUp() {
        clock.setFixed(START_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Успешное обновление")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/shooting/before/success_updating.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/shooting/after/success_updating.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successUpdating() {
        handlePlanFacts()
    }

    @DisplayName("Успешное остановка обработки")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/shooting/before/success_stop.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/shooting/after/success_stop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successStop() {
        handlePlanFacts()
    }

    private fun handlePlanFacts() {
        planFactHandler.handle(listOf(103L), Instant.parse("2021-01-01T12:00:00.00Z"))
    }

    companion object {
        private val START_TIME = Instant.parse("2022-01-28T05:00:00.00Z")
    }
}
