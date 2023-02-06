package ru.yandex.market.mbi.feed.processor.test

import ru.yandex.market.mbi.feed.processor.FunctionalTest
import kotlin.reflect.KProperty

/**
 * Делегат урлов в тестах. Чтобы каждый раз не писать метод с конкатенацией.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class ApiUrl(
    private val url: String
) {
    private val delim: String = if (url.startsWith("/")) "" else "/"

    operator fun getValue(thisRef: FunctionalTest, property: KProperty<*>): String {
        return "${thisRef.baseUrl}$delim$url"
    }
}
