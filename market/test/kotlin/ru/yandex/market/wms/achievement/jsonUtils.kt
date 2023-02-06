package ru.yandex.market.wms.achievement

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource
import ru.yandex.market.wms.achievement.configuration.ObjectMapperConfig.Companion.objectMapper

inline fun <reified T> String.fromJson(): T = objectMapper.readValue(this)
fun String.toTree(): JsonNode = objectMapper.readTree(this)
inline fun <reified T> T.serialize(): String = objectMapper.writeValueAsString(this)

fun resourceAsString(path: String): String = String(ClassPathResource(path).file.readBytes())

