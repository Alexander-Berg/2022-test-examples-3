package ru.yandex.market.logistic.api;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.api.model.common.request.AbstractRequest;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.utils.HttpTemplate;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

public class HttpTemplateImpl implements HttpTemplate {

    private final RestTemplate restTemplate;
    private final MediaType mediaType;

    public HttpTemplateImpl(@Nonnull RestTemplate restTemplate,
                            @Nonnull MediaType mediaType) {
        this.restTemplate = restTemplate;
        this.mediaType = mediaType;
    }

    @Nonnull
    @Override
    public <Request extends AbstractRequest, Response extends AbstractResponse> ResponseWrapper<Response> executePost(
        @Nonnull RequestWrapper<Request> request,
        @Nonnull TypeReference<ResponseWrapper<Response>> responseClass,
        @Nonnull String url) {
        return executePost(request, responseClass, url, new LinkedMultiValueMap<>());
    }

    @Nonnull
    @Override
    public <Request extends AbstractRequest, Response extends AbstractResponse> ResponseWrapper<Response> executePost(
        @Nonnull RequestWrapper<Request> request,
        @Nonnull TypeReference<ResponseWrapper<Response>> responseClass,
        @Nonnull String url,
        @Nonnull MultiValueMap<String, String> headers
    ) {
        return execute(request, POST, responseClass, url, headers);
    }

    @Nonnull
    @Override
    public <Response> Response executeGet(@Nonnull Class<Response> responseClass, @Nonnull String url) {
        return execute(GET, responseClass, url);
    }

    @Nonnull
    public <Request extends AbstractRequest, Response extends AbstractResponse> ResponseWrapper<Response> execute(
        @Nonnull RequestWrapper<Request> request,
        HttpMethod httpMethod,
        @Nonnull TypeReference<ResponseWrapper<Response>> responseClass,
        @Nonnull String url,
        @Nonnull MultiValueMap<String, String> headers
    ) {

        ResponseEntity<ResponseWrapper<Response>> responseEntity = restTemplate.exchange(
            url,
            httpMethod,
            createRequestEntity(request, mediaType, headers),
            ParameterizedTypeReference.forType(responseClass.getType())
        );

        return extractResponseBody(responseEntity);
    }

    @Nonnull
    public <Response> Response execute(HttpMethod httpMethod,
                                       @Nonnull Class<Response> responseClass,
                                       @Nonnull String url) {
        ResponseEntity<Response> responseEntity = restTemplate.exchange(
            url,
            httpMethod,
            createRequestEntity(mediaType),
            responseClass
        );

        return extractResponseBody(responseEntity);
    }

    @Nonnull
    private <T> T extractResponseBody(ResponseEntity<T> responseEntity) {
        return Optional.ofNullable(responseEntity)
            .map(ResponseEntity::getBody)
            .orElseThrow(() -> new RuntimeException("Failed to extract response body"));
    }

    @Nonnull
    private <T> HttpEntity<T> createRequestEntity(
        T request,
        MediaType mediaType,
        MultiValueMap<String, String> headers
    ) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(mediaType);
        httpHeaders.addAll(headers);

        return new HttpEntity<>(request, httpHeaders);
    }

    @Nonnull
    private HttpEntity<?> createRequestEntity(MediaType mediaType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(mediaType);

        return new HttpEntity<>(httpHeaders);
    }
}
