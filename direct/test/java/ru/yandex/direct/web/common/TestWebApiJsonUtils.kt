package ru.yandex.direct.web.common

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

inline fun <reified T : Any> JsonNode.getValue(path: String): T {
    val split = path.split("/")
    var currentData = this
    for (pathPart in split) {
        if (currentData is ObjectNode) {
            currentData = currentData.get(pathPart)
            continue
        }
        if (currentData is ArrayNode) {
            val index = pathPart.toInt()
            currentData = currentData.get(index)
            continue
        }
        throw IllegalArgumentException(String.format("Unknown path: [%s]", path))
    }

    when (T::class) {
        Boolean::class -> {
            return currentData.booleanValue() as T
        }
        String::class -> {
            return currentData.textValue() as T
        }
        Long::class -> {
            return currentData.longValue() as T
        }
        Int::class -> {
            return currentData.intValue() as T
        }
        Double::class -> {
            return currentData.doubleValue() as T
        }
        Float::class -> {
            return currentData.floatValue() as T
        }
        Short::class -> {
            return currentData.shortValue() as T
        }
        else -> {
            return currentData as T
        }
    }
}
