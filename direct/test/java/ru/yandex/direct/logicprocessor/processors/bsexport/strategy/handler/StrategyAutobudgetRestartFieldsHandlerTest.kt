package ru.yandex.direct.logicprocessor.processors.bsexport.strategy.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.strategy.Strategy
import ru.yandex.direct.autobudget.restart.model.PackageStrategyDto
import ru.yandex.direct.autobudget.restart.repository.PackageStrategyAutobudgetRestartRepository
import ru.yandex.direct.autobudget.restart.repository.RestartTimes
import ru.yandex.direct.autobudget.restart.repository.StrategyRestartData
import ru.yandex.direct.autobudget.restart.service.StrategyState
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyName
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.logicprocessor.processors.bsexport.strategy.handler.StrategyAutobudgetRestartFieldsHandler.Companion.buildAutobudgetRestart
import java.time.LocalDateTime

class StrategyAutobudgetRestartFieldsHandlerTest {

    private fun mockAutobudgetRestartRepository(
        restarts: List<StrategyRestartData>
    ): PackageStrategyAutobudgetRestartRepository =
        mock {
            on { getAutobudgetRestartData(any(), any()) } doReturn restarts
        }

    private fun restartData(
        strategyId: Long,
        times: RestartTimes,
        state: StrategyState
    ): StrategyRestartData =
        StrategyRestartData(
            strategyId,
            PackageStrategyDto(
                false,
                StrategyName.DEFAULT_,
                false
            ),
            times,
            state
        )

    private fun property(boolean: Boolean): PpcProperty<Boolean> =
        mock {
            on { getOrDefault(any()) } doReturn (boolean)
        }

    private fun ppcPropertiesSupport(boolean: Boolean = false): PpcPropertiesSupport {
        val property = property(boolean)
        return mock {
            on { get(any<PpcPropertyName<*>>()) } doReturn (property)
        }
    }

    private fun handler(
        ppcPropertiesSupport: PpcPropertiesSupport = ppcPropertiesSupport(),
        strategyAutobudgetRestartRepository: PackageStrategyAutobudgetRestartRepository = mockAutobudgetRestartRepository(
            listOf()
        )
    ) = StrategyAutobudgetRestartFieldsHandler(
        strategyAutobudgetRestartRepository,
        ppcPropertiesSupport
    )

    private fun defaultStrategy(id: Long) = DefaultManualStrategy().withId(id)
    private fun defaultExpectedStrategy() = Strategy.newBuilder().build()

    @Test
    fun `do nothing on property disabled`() {
        val strategyAutobudgetRestartRepository = mockAutobudgetRestartRepository(listOf())
        StrategyHandlerAssertions.assertProtoFilledCorrectly(
            handler(strategyAutobudgetRestartRepository = strategyAutobudgetRestartRepository),
            strategy = defaultStrategy(1L),
            expectedProto = defaultExpectedStrategy()
        )
        verifyZeroInteractions(strategyAutobudgetRestartRepository)
    }

    @Test
    fun `no restart times`() {
        StrategyHandlerAssertions.assertProtoFilledCorrectly(
            handler(ppcPropertiesSupport(true)),
            strategy = defaultStrategy(1L),
            expectedProto = defaultExpectedStrategy()
        )
    }

    @Test
    fun `correctly set restart times`() {
        val strategyId = 1L
        val restartTimes =
            RestartTimes(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                "some_reason"
            )
        val state = StrategyState(LocalDateTime.now().plusDays(1))
        val restartData = restartData(
            strategyId,
            restartTimes,
            state
        )
        val strategyAutobudgetRestartRepository = mockAutobudgetRestartRepository(listOf(restartData))
        val expectedProto = defaultExpectedStrategy()
            .toBuilder()
            .setAutobudgetRestart(buildAutobudgetRestart(restartTimes, state))
            .build()
        StrategyHandlerAssertions.assertProtoFilledCorrectly(
            handler(ppcPropertiesSupport(true), strategyAutobudgetRestartRepository),
            strategy = defaultStrategy(strategyId),
            expectedProto = expectedProto
        )
    }
}
