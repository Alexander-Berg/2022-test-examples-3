package ru.yandex.market.logistic.gateway.client.config;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.logistics.util.client.HttpTemplate;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

public class HttpTemplateImpl extends HttpTemplate {

    private final RestTemplate restTemplate;
    private final MediaType mediaType;

    public HttpTemplateImpl(@Nonnull String baseHost,
                            @Nonnull RestTemplate restTemplate,
                            @Nonnull MediaType mediaType) {
        super(baseHost);
        this.restTemplate = restTemplate;
        this.mediaType = mediaType;
    }

    @Override
    @Nonnull
    public <Request, Response> Response executePost(@Nonnull Request request,
                                                    @Nonnull Class<Response> responseClass,
                                                    @Nonnull String... uriFragments) {
        return execute(request, POST, responseClass, Collections.emptyMap(), uriFragments);
    }

    @Override
    public <Request> void executePost(@Nonnull Request request, @Nonnull String... uriFragments) {
        execute(request, POST, Collections.emptyMap(), uriFragments);
    }

    @Override
    @Nonnull
    public <Response> Response executeGet(@Nonnull Class<Response> responseClass,
                                          @Nonnull Map<String, Set<String>> paramMap,
                                          @Nonnull String... uriFragments) {
        return execute(GET, responseClass, paramMap, uriFragments);
    }

    @Nonnull
    public <Request, Response> Response execute(@Nonnull Request request,
                                                HttpMethod httpMethod,
                                                @Nonnull Class<Response> responseClass,
                                                @Nonnull Map<String, Set<String>> paramMap,
                                                @Nonnull String... uriFragments) {
        ResponseEntity<Response> responseEntity = restTemplate.exchange(
            buildUrl(paramMap, uriFragments),
            httpMethod,
            createRequestEntity(request, mediaType),
            responseClass
        );

        return extractResponseBody(responseEntity);
    }

    @Nonnull
    public <Request, Response> Response execute(HttpMethod httpMethod,
                                                @Nonnull Class<Response> responseClass,
                                                @Nonnull Map<String, Set<String>> paramMap,
                                                @Nonnull String... uriFragments) {
        ResponseEntity<Response> responseEntity = restTemplate.exchange(
            buildUrl(paramMap, uriFragments),
            httpMethod,
            createRequestEntity(mediaType),
            responseClass
        );

        return extractResponseBody(responseEntity);
    }

    private <Request> void execute(@Nonnull Request request,
                                   HttpMethod httpMethod,
                                   @Nonnull Map<String, Set<String>> paramMap,
                                   @Nonnull String... uriFragments) {
        restTemplate.exchange(
            buildUrl(paramMap, uriFragments),
            httpMethod,
            createRequestEntity(request, mediaType),
            Void.class
        );
    }

    @Nonnull
    private <T> T extractResponseBody(ResponseEntity<T> responseEntity) {
        return Optional.ofNullable(responseEntity)
            .map(ResponseEntity::getBody)
            .orElseThrow(() -> new RuntimeException("Failed to extract response body"));
    }

    private String buildUrl(@Nonnull Map<String, Set<String>> paramMap, @Nonnull String... uriFragments) {
        MultiValueMap<String, String> paramMultiMap = new LinkedMultiValueMap<>();

        paramMap.keySet().forEach(key -> {
            paramMultiMap.put(key, paramMap.get(key).stream().collect(Collectors.toList()));
        });

        return UriComponentsBuilder
            .fromHttpUrl(baseUri)
            .queryParams(paramMultiMap)
            .pathSegment(uriFragments)
            .toUriString();
    }

    private <T> HttpEntity<T> createRequestEntity(T request, MediaType mediaType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(mediaType);

        return new HttpEntity<>(request, httpHeaders);
    }

    private HttpEntity<?> createRequestEntity(MediaType mediaType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(mediaType);

        return new HttpEntity<>(httpHeaders);
    }
}
