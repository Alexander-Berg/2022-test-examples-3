package ru.yandex.market.mbi.feed.processor

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.function.Supplier

class FunctionalTestHelper {

    companion object {
        private val REST_TEMPLATE = createJsonRestTemplate()
        private val log: Logger = LoggerFactory.getLogger(FunctionalTestHelper::class.java)

        private fun createJsonRestTemplate(): RestTemplate {
            val restTemplate = RestTemplate()
            restTemplate.messageConverters = Arrays.asList(
                ByteArrayHttpMessageConverter(),
                StringHttpMessageConverter(StandardCharsets.UTF_8),
                createJacksonConverter(),
                FormHttpMessageConverter(),
                ProtobufHttpMessageConverter()
            )
            restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory()
            return restTemplate
        }

        private fun createJacksonConverter(): MappingJackson2HttpMessageConverter {
            // fixme: надо сюда нормально заинжектить бин из ru.yandex.market.mbi.feed.processor.mapper.ObjectMapperConfig#objectMapper()
            val objectMapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            return MappingJackson2HttpMessageConverter(objectMapper)
        }

        fun <T> get(uri: URI, tClass: Class<T>): ResponseEntity<T>? {
            return logAndPropagateOrReturn {
                val request =
                    HttpEntity<String?>(
                        null,
                        headers()
                    )
                REST_TEMPLATE.exchange(
                    uri,
                    HttpMethod.GET,
                    request,
                    tClass
                )
            }
        }

        private fun <T> logAndPropagateOrReturn(callable: Supplier<T>): T {
            return try {
                callable.get()
            } catch (e: HttpClientErrorException) {
                throw logException(e)!!
            } catch (e: HttpServerErrorException) {
                throw logException(e)!!
            }
        }

        private fun logException(e: HttpStatusCodeException): HttpStatusCodeException? {
            log.warn(
                """
            Error during request. Response:
            ${e.responseBodyAsString}
            """
                    .trimIndent(),
                e
            )
            return e
        }

        private fun headers(): HttpHeaders? {
            val headers = HttpHeaders()
            headers.accept = listOf(MediaType.ALL)
            headers.contentType = MediaType.APPLICATION_JSON
            return headers
        }
    }
}
