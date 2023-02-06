package ru.yandex.market.abo.core.pinger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.core.cutoff.CutoffManager
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType
import ru.yandex.market.abo.core.pinger.model.MpSchedule
import ru.yandex.market.abo.core.pinger.model.MpScheduleState
import ru.yandex.market.abo.core.pinger.service.MpScheduleService
import ru.yandex.market.abo.cpa.MbiApiService
import java.time.LocalDateTime

internal class GoodsPingMpScheduleStateChangerTest {

    private val mbiApiService: MbiApiService = mock()
    private val exceptionalShopsService: ExceptionalShopsService = mock()
    private val mpScheduleService: MpScheduleService = mock()
    private val goodsPingMpStatProvider: GoodsPingMpStatProvider = mock()
    private val cutoffManager: CutoffManager = mock()

    private val goodsPingMpScheduleStateChanger =
        GoodsPingMpScheduleStateChanger(
            mbiApiService, exceptionalShopsService,
            mpScheduleService, goodsPingMpStatProvider, cutoffManager
        )

    @ParameterizedTest
    @MethodSource("source for changing state")
    fun changeState(initialState: MpScheduleState, hasCutoff: Boolean, stat: MpStat, expectedState: MpScheduleState) {
        whenever(cutoffManager.openAboCutoff(anyLong(), any())).thenReturn(true)
        doNothing().whenever(cutoffManager).closeAboCutoff(anyLong(), any(), anyLong(), any(), any())
        val schedule = MpSchedule(PARTNER_ID, GEN, initialState, LocalDateTime.now())
        val finalState = goodsPingMpScheduleStateChanger.changeState(schedule, hasCutoff, stat)?.state
            ?: schedule.state
        assertEquals(expectedState, finalState)
    }


    companion object {
        private const val PARTNER_ID = 1L
        private val GEN = MpGeneratorType.GOODS_PING

        @JvmStatic
        fun `source for changing state`(): Iterable<Arguments> = listOf(
            Arguments.of(MpScheduleState.PING, false, MpStat(0, 0, false), MpScheduleState.PING),
            Arguments.of(MpScheduleState.PING, false, MpStat(1, 1, true), MpScheduleState.PING),
            Arguments.of(MpScheduleState.PING, false, MpStat(1, 0, false), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.PING, false, MpStat(2, 1, true), MpScheduleState.PING),
            Arguments.of(MpScheduleState.PING, false, MpStat(2, 1, false), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.PING, false, MpStat(4, 1, true), MpScheduleState.PING),
            Arguments.of(MpScheduleState.PING, false, MpStat(4, 1, false), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.PING, false, MpStat(4, 0, false), MpScheduleState.CONTROL_PING),

            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(0, 0, false), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(1, 1, true), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(1, 0, false), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(2, 1, true), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(2, 1, false), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(4, 1, true), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(4, 1, false), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(4, 0, false), MpScheduleState.CONTROL_PING),
            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(4, 3, true), MpScheduleState.FREQUENT_PING),
            Arguments.of(MpScheduleState.FREQUENT_PING, false, MpStat(4, 4, true), MpScheduleState.PING),

            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(0, 0, false), MpScheduleState.CONTROL_PING),
            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(1, 1, true), MpScheduleState.PING),
            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(1, 0, false), MpScheduleState.CONTROL_PING),
            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(2, 1, true), MpScheduleState.PING),
            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(2, 1, false), MpScheduleState.CONTROL_PING),
            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(4, 1, true), MpScheduleState.PING),
            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(4, 1, false), MpScheduleState.CONTROL_PING),
            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(4, 0, false), MpScheduleState.CONTROL_PING),
            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(4, 3, true), MpScheduleState.PING),
            Arguments.of(MpScheduleState.CONTROL_PING, true, MpStat(4, 4, true), MpScheduleState.PING),
        )
    }
}
