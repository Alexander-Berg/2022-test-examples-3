package ru.yandex.market.logistics.calendaring.util

import org.apache.commons.io.IOUtils
import java.io.IOException
import java.lang.ClassLoader.getSystemResourceAsStream
import java.nio.charset.StandardCharsets
import java.util.*

@Throws(IOException::class)
fun getFileContent(fileName: String): String {
    return IOUtils.toString(
        Objects.requireNonNull(
            getSystemResourceAsStream(fileName)),
        StandardCharsets.UTF_8).trim()
}
