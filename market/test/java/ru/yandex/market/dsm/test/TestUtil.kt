package ru.yandex.market.dsm.test

import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters

class TestUtil {
    companion object {
        val OBJECT_GENERATOR = run {
            val parameters = EasyRandomParameters()
            parameters.overrideDefaultInitialization(true)
            EasyRandom(parameters)
        }
    }
}
