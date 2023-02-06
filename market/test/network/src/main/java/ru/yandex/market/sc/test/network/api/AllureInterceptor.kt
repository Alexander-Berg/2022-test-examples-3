package ru.yandex.market.sc.test.network.api

import io.qameta.allure.kotlin.Allure.step
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import ru.yandex.market.sc.test.network.arch.ext.safeString
import ru.yandex.market.sc.test.network.arch.ext.string
import javax.inject.Inject

class AllureInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.pathSegments.joinToString("/")

        return step("Посылаем ${request.method} запрос в эндпоинт $path") {
            try {
                logRequest(request)
                chain.proceed(request).also { logResponse(it) }
            } catch (e: Exception) {
                throw e.also { logResponse(null, exception = it) }
            }
        }
    }

    private fun logRequest(request: Request) = with(request) {
        step("Запрос на сервер") {
            val namesAndValues = url.queryParameterNames.joinToString("\n", "\t") {
                it + ": " + url.queryParameterValues(it)
            }

            if (namesAndValues.isNotBlank()) {
                step("Параметры: \n$namesAndValues")
            }


            val queryBody = body ?: return@step
            if (queryBody.contentLength() > 0) {
                step("Тело запроса: ${queryBody.string()}")
            }
        }
    }

    private fun logResponse(response: Response?, exception: Exception? = null) {
        step("Получаем ответ от сервера") {
            response?.run {
                step("x-market-req-id: ${header("x-market-req-id", "error")}")
                step("$code $message")
                body?.let {
                    if (it.contentLength() > 0) step("Тело ответа: ${it.safeString()}")
                }
            }

            exception?.run {
                step("Ошибка: $message")
            }
        }
    }
}