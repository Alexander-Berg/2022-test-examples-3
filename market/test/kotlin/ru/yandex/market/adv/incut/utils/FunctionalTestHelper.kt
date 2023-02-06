package ru.yandex.market.adv.incut.utils

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class FunctionalTestHelper {

    companion object {

        private val REST_TEMPLATE: RestTemplate = RestTemplate()

        fun <T> get(url: String, tClass: Class<T>, vararg uriVariables: Any?): T {
            return getAsEntity(url, tClass, *uriVariables).body
        }

        fun get(url: String): String {
            return get(url, String::class.java)
        }

        fun post(url: String, body: Any? = null, vararg uriVariables: Any?): String {
            return postForEntity(url, body, *uriVariables).body!!
        }

        fun postForEntity(url: String, body: Any? = null, vararg uriVariables: Any?): ResponseEntity<String> {
            val entity = HttpEntity(body, headers())
            return REST_TEMPLATE.exchange(url, HttpMethod.POST, entity, String::class.java, *uriVariables)
        }

        fun put(url: String, body: Any? = null, vararg uriVariables: Any?): String? {
            return putForEntity(url, body, *uriVariables).body
        }

        fun delete(url: String, body: Any? = null, vararg uriVariables: Any?): String? {
            return deleteForEntity(url, body, *uriVariables).body
        }

        fun deleteForEntity(url: String, body: Any? = null, vararg uriVariables: Any?): ResponseEntity<String?> {
            val entity = HttpEntity(body, headers())
            return REST_TEMPLATE.exchange(url, HttpMethod.DELETE, entity, String::class.java, *uriVariables)
        }

        fun putForEntity(url: String, body: Any? = null, vararg uriVariables: Any?): ResponseEntity<String> {
            val entity = HttpEntity(body, headers())
            return REST_TEMPLATE.exchange(url, HttpMethod.PUT, entity, String::class.java, *uriVariables)
        }

        fun <T> getAsEntity(url: String, tClass: Class<T>?, vararg uriVariables: Any?): ResponseEntity<T> {
            val request = HttpEntity<String?>(null, headers())
            return REST_TEMPLATE.exchange(url, HttpMethod.GET, request, tClass, *uriVariables)
        }

        private fun headers(): HttpHeaders {
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            return headers
        }
    }
}
