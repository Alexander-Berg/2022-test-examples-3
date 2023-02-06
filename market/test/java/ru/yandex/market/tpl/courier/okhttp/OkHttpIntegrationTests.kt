package ru.yandex.market.tpl.courier.okhttp

import io.kotest.assertions.throwables.shouldNotThrow
import okhttp3.MediaType.Companion.toMediaType
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.arch.android.content.MimeTypes

class OkHttpIntegrationTests {

    @Test
    fun `Создание MediaType из MimeTypes#IMAGE_UNKNOWN не падает с ошибкой`() {
        shouldNotThrow<Throwable> {
            MimeTypes.IMAGE_UNKNOWN.toMediaType()
        }
    }
}