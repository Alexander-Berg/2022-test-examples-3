package ru.yandex.market.wms.inbound_management.config

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.ClientResponse
import reactor.core.publisher.Mono
import ru.yandex.market.wms.core.base.request.CoreRequest
import ru.yandex.market.wms.core.base.response.CoreResponse
import ru.yandex.market.wms.core.client.CoreClient

interface CoreClientStub : CoreClient {
    override fun <T : CoreRequest> doPutRequest(path: String, request: T): ResponseEntity<Void>? {
        throw NotImplementedError()
    }

    override fun <T : CoreRequest, R : CoreResponse> doPostRequest(
        path: String,
        request: T,
        responseClass: Class<R>
    ): R {
        throw NotImplementedError()
    }

    override fun <T : CoreRequest, R : CoreResponse> doPostRequest(
        path: String,
        request: T,
        responseClass: Class<R>,
        onStatus: List<Pair<(HttpStatus) -> Boolean, (ClientResponse) -> Mono<out Throwable>>>
    ): R {
        throw NotImplementedError()
    }

    override fun <T : CoreRequest> doPostRequest(
        path: String,
        request: T,
        onStatus: List<Pair<(HttpStatus) -> Boolean, (ClientResponse) -> Mono<out Throwable>>>
    ): ResponseEntity<Void>? {
        throw NotImplementedError()
    }

    override fun <T : CoreRequest> doPostRequest(path: String, request: T): ResponseEntity<Void>? {
        throw NotImplementedError()
    }

    override fun <R : CoreResponse> doGetRequest(path: String, responseClass: Class<R>): R {
        throw NotImplementedError()
    }

    override fun <R : CoreResponse> doGetRequest(
        path: String,
        params: Map<String, String>?,
        responseClass: Class<R>
    ): R {
        throw NotImplementedError()
    }

    override fun <R : CoreResponse> doGetRequest(
        path: String,
        params: Map<String, String>?,
        responseClass: Class<R>,
        onStatus: List<Pair<(HttpStatus) -> Boolean, (ClientResponse) -> Mono<out Throwable>>>
    ): R {
        throw NotImplementedError()
    }

    override fun doGetRequest(path: String, params: Map<String, String>?) {
        throw NotImplementedError()
    }

    override fun doGetRequest(
        path: String,
        params: Map<String, String>?,
        onStatus: List<Pair<(HttpStatus) -> (Boolean), (ClientResponse) -> (Mono<out Throwable>)>>
    ) {
        throw NotImplementedError()
    }

    override fun <R> doGetRequest(
        path: String,
        params: Map<String, String>?,
        elementTypeRef: ParameterizedTypeReference<R>
    ): R {
        throw NotImplementedError()
    }

    override fun <R : CoreResponse> doGetRequestMono(
        path: String,
        params: MultiValueMap<String, String>,
        responseClass: Class<R>
    ): Mono<R> {
        throw NotImplementedError()
    }

    override fun <T : CoreRequest, R : CoreResponse> doPostRequest(
        path: String,
        params: Map<String, String>?,
        request: T,
        responseClass: Class<R>
    ): R {
        throw NotImplementedError()
    }
}
