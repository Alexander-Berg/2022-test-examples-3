package ru.yandex.market.logistics.les.compatibility

import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.mapper.component.MapperFactory
import ru.yandex.market.logistics.les.compatibility.crypto.AesFixedIvCipher
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object LesDeserializer {

    @JvmStatic
    fun main(args: Array<String>) {
        val objectMapperToUse = args[0]
        val objectMapper = if (objectMapperToUse == "client") MapperFactory.getClientObjectMapper()
        else MapperFactory.getApplicationObjectMapper(
            AesFixedIvCipher.SERIALIZER,
            AesFixedIvCipher.DESERIALIZER
        )

        val failOnUnrecognizedClass = args[1].toBoolean()
        val jsonsDirectory = if (args.size > 2) args[2] else LesSerializer.DEFAULT_PATH

        if (!Files.exists(Path.of(jsonsDirectory))) {
            throw RuntimeException("Directory $jsonsDirectory not found")
        }

        val directory = File(jsonsDirectory)
        val familiarPayloadClasses = LesSerializer.Data.objectsToSerialize.map { it.payload!!::class.java }
        for (file in directory.listFiles()!!) {
            val actualObject = objectMapper.readValue(file, Event::class.java)
            val actualObjectPayloadClass = actualObject.payload!!::class.java

            if (!familiarPayloadClasses.contains(actualObjectPayloadClass)) {
                if (failOnUnrecognizedClass) {
                    throw RuntimeException("Cannot find deserializer for file ${file.name}")
                } else {
                    continue
                }
            }

            val expectedObject =
                LesSerializer.Data.objectsToSerialize.find { it.payload!!::class.java == actualObjectPayloadClass }

            if (!actualObject.equals(expectedObject)) {
                throw RuntimeException("Objects are not equal\nexpected: $expectedObject\nactual: $actualObject")
            }
        }
    }
}
