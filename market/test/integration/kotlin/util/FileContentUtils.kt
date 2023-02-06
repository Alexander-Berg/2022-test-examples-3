package ru.yandex.market.logistics.calendaring.util

import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

object FileContentUtils {
    fun getFileContent(fileName: String): String {
        return IOUtils.toString(
            ClassLoader.getSystemResourceAsStream(fileName)!!,
            StandardCharsets.UTF_8
        ).trim()
    }
}
