package ru.yandex.market.mapi.core

import org.junit.jupiter.api.BeforeEach
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder
import ru.yandex.market.mapi.core.util.mockMapiContext
import java.time.Instant

/**
 * @author Ilya Kislitsyn / ilyakis@ / 02.02.2022
 */
open class AbstractNonSpringTest {

    @BeforeEach
    open fun prepareTests() {
        MapiDateBuilder.BUILD_INSTANT_FUN = {
            // Часовой пояс мск +03:00
            // мока на 1647341610 - это 15 марта 2022 13:53
            // обрезает до 1647302400000 ms (в днях)
            Instant.ofEpochSecond(1647341610)
        }

        mockMapiContext { context ->
            context.ip = "mocked_ip"
            context.secHeaders = mapOf("sec_header" to "sec_value", "sec_header2" to "sec_value2")
        }
    }
}
