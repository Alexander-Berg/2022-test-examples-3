package ru.yandex.market.logistics.les.compatibility

import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.base.EventPayload
import ru.yandex.market.logistics.les.mapper.component.MapperFactory
import ru.yandex.market.logistics.les.compatibility.crypto.AesFixedIvCipher
import ru.yandex.market.logistics.les.compatibility.dto.DateTimeDto
import ru.yandex.market.logistics.les.compatibility.dto.RequiredAndNullableFieldsDto
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


object LesSerializer {
    const val DEFAULT_PATH = "jsons"

    @JvmStatic
    fun main(args: Array<String>) {
        val objectMapperToUse = args[0]
        val objectMapper = if (objectMapperToUse == "client") MapperFactory.getClientObjectMapper()
            else MapperFactory.getApplicationObjectMapper(
                AesFixedIvCipher.SERIALIZER,
                AesFixedIvCipher.DESERIALIZER
            )

        val pathToStore = if (args.size > 1) args[1] else DEFAULT_PATH

        Files.createDirectories(Path.of(pathToStore))

        Data.objectsToSerialize.forEachIndexed { index, it ->
            objectMapper.writeValue(File("$pathToStore/${it.javaClass.simpleName}-$index.json"), it)
        }
    }

    object Data {
        val objectsToSerialize = listOf(
            buildEvent(DateTimeDto.TEST_OBJECT),
            buildEvent(RequiredAndNullableFieldsDto.TEST_OBJECT)
        )

        private fun buildEvent(payload: EventPayload) =
            Event("testSource", "eventId", 1643964894, "TEST_TYPE", payload, "Compatibility event test")
    }
}

