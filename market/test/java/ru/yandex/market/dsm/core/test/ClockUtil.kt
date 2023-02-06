package ru.yandex.market.dsm.core.test

import lombok.experimental.UtilityClass
import org.mockito.Mockito
import ru.yandex.market.tpl.common.util.DateTimeUtil
import java.time.Clock
import java.time.LocalDateTime

@UtilityClass
object ClockUtil {
    // monday
    fun defaultDateTime(): LocalDateTime {
        return LocalDateTime.of(1990, 1, 1, 0, 0, 0)
    }

    fun initFixed(clock: Clock, dateTime: LocalDateTime = defaultDateTime()): Clock {
        val zoneOffset = DateTimeUtil.DEFAULT_ZONE_ID
        Mockito.lenient().doReturn(dateTime.toInstant(zoneOffset))
            .`when`(clock).instant()
        Mockito.lenient().doReturn(zoneOffset).`when`(clock).zone
        return clock
    }

    fun reset(clock: Clock): Clock {
        Mockito.doCallRealMethod().`when`(clock).instant()
        Mockito.doCallRealMethod().`when`(clock).zone
        return clock
    }
}
