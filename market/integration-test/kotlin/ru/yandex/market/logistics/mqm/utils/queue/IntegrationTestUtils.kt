package ru.yandex.market.logistics.mqm.utils.queue

import org.apache.commons.io.IOUtils
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils
import java.nio.charset.StandardCharsets

fun extractFileContent(relativePath: String): String {
    try {
        IntegrationTestUtils.inputStreamFromResource(relativePath).use { inputStream ->
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8)
        }
    } catch (e: Exception) {
        throw RuntimeException("Error during reading from file $relativePath", e)
    }
}
