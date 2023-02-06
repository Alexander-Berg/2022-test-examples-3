package ru.yandex.market.pricingmgmt

import org.mockito.ArgumentCaptor
import org.mockito.Mockito

object TestUtils {

    // Использовать вместо Mockito.any() для заглушки not-null параметров
    fun <T> any(type: Class<T>): T = Mockito.any(type)

    fun <T> notNull(): T = Mockito.notNull()

    fun readResourceFile(file: String): String = requireNotNull(javaClass.getResource(file)).readText()

    // Использовать вместо ArgumentCaptor.capture() для заглушки not-null параметров
    fun <T> ArgumentCaptor<T>.captureNotNull(): T {
        return this.capture()
    }
}
