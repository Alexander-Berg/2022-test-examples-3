package ru.yandex.market.wms.inbound_management.config

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.util.MultiValueMap
import reactor.core.publisher.Mono
import ru.yandex.market.wms.transportation.client.TransportationClient
import ru.yandex.market.wms.transportation.core.model.request.TransportationRequest
import ru.yandex.market.wms.transportation.core.model.response.TransportationResponse

interface TransportationClientStub : TransportationClient {
    override fun <T : TransportationRequest?, R : TransportationResponse?> processRequest(
        method: HttpMethod?,
        path: String?,
        request: T,
        returnType: ParameterizedTypeReference<R>?
    ): R {
        throw NotImplementedError()
    }

    override fun <R : TransportationResponse?> processRequest(
        method: HttpMethod?,
        path: String?,
        queryParams: MultiValueMap<String, String>?,
        returnType: ParameterizedTypeReference<R>?
    ): R {
        throw NotImplementedError()
    }

    override fun <T : TransportationRequest?, R : TransportationResponse?> processRequest(
        method: HttpMethod?,
        path: String?,
        request: T,
        onStatus: MutableMap<HttpStatus, Mono<out Throwable>>?,
        returnType: ParameterizedTypeReference<R>?
    ): R {
        throw NotImplementedError()
    }

    override fun <T : TransportationRequest?, R : TransportationResponse?> proocessRequestAsync(
        method: HttpMethod?,
        path: String?,
        request: T,
        returnType: ParameterizedTypeReference<R>?
    ): Mono<R> {
        throw NotImplementedError()
    }
}
