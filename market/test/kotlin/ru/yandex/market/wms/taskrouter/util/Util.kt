package ru.yandex.market.wms.taskrouter.util

fun loadTestResource(path: String): String {
    val resource = object {}.javaClass.classLoader.getResource(path)
        ?: throw NoSuchElementException("Could not load resource from $path")
    return resource.readText()
}
