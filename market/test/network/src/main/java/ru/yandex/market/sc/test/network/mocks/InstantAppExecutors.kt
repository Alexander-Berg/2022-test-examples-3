package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.network.AppExecutors
import java.util.concurrent.Executor

class InstantAppExecutors : AppExecutors(instant, instant, instant) {
    companion object {
        private val instant = Executor { it.run() }
    }
}
