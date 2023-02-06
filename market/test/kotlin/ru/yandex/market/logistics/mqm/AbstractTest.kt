package ru.yandex.market.logistics.mqm

import org.junit.jupiter.params.ParameterizedTest
import ru.yandex.market.common.util.DateTimeUtils
import java.time.Instant
import java.time.LocalDateTime

open class AbstractTest {
    companion object {
        const val TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + ParameterizedTest.INDEX_PLACEHOLDER + "] {0}"
    }

    protected fun String.toInstant(): Instant =
        LocalDateTime.parse(this)
            .atZone(DateTimeUtils.MOSCOW_ZONE)
            .toInstant()
}
