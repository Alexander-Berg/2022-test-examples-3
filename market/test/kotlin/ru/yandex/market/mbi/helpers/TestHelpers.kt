package ru.yandex.market.mbi.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ru.yandex.market.common.test.util.StringTestUtil
import kotlin.reflect.KClass

val defaultTestMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

inline fun <reified T> KClass<*>.loadTestEntity(
    fileName: String,
    mapper: ObjectMapper = defaultTestMapper
): T = mapper.readValue(StringTestUtil.getString(this.java, fileName), T::class.java)

inline fun <reified T> KClass<*>.loadTestEntities(
    fileName: String,
    mapper: ObjectMapper = defaultTestMapper
): List<T> {
    val s = StringTestUtil.getString(this.java, fileName)
    val typeRef = mapper.typeFactory.constructCollectionType(ArrayList::class.java, T::class.java)
    return mapper.readValue(s, typeRef)
}

fun KClass<*>.loadResourceAsString(fileName: String): String {
    return StringTestUtil.getString(this.java, fileName)
}
