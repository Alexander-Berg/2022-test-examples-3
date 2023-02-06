package ru.yandex.market.contentmapping.utils

object ResourceUtils {
    fun loadString(name: String): String = javaClass.classLoader.getResource(name)?.readText()
            ?: throw IllegalArgumentException("Can't find resource $name")
}
