package ru.yandex.market.mbi.orderservice.api

import kotlin.reflect.KProperty

/**
 * Делегат урлов в тестах. Чтобы каждый раз не писать метод с конкатенацией.
 */
internal class ApiUrl(
    private val url: String
) {
    private val delim: String = if (url.startsWith("/")) "" else "/"

    operator fun getValue(thisRef: FunctionalTest, property: KProperty<*>): String {
        return "${thisRef.baseUrl}$delim$url"
    }
}
