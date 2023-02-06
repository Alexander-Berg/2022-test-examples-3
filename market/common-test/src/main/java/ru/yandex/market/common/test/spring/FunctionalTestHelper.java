package ru.yandex.market.common.test.spring;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Утильный для класс для осуществления запросов к HTTP серверу.
 *
 * @author fbokovikov
 */
public class FunctionalTestHelper {

    private static final RestTemplate REST_TEMPLATE = RestTemplateFactory.createRestTemplate();

    private FunctionalTestHelper() {
        throw new UnsupportedOperationException("Can not instantiate util class");
    }

    public static <T> ResponseEntity<T> get(String url, Class<T> tClass, Object... uriVariables) {
        HttpEntity<String> request = new HttpEntity<>(null, headers());
        return REST_TEMPLATE.exchange(url, HttpMethod.GET, request, tClass, uriVariables);
    }

    public static ResponseEntity<String> get(String url, Object... uriVariables) {
        return exchange(url, null, HttpMethod.GET, uriVariables);
    }

    public static ResponseEntity<String> post(String url) {
        return post(url, (Object) null);
    }

    public static ResponseEntity<String> post(String url, Object body, Object... uriVariables) {
        return exchange(url, body, HttpMethod.POST, uriVariables);
    }

    public static <T> ResponseEntity<String> post(String url, HttpEntity<T> httpEntity) {
        return REST_TEMPLATE.postForEntity(url, httpEntity, String.class);
    }

    public static String postForXml(String url, String request) {
        return REST_TEMPLATE.postForObject(url, new HttpEntity<>(request, xmlHeaders()), String.class);
    }

    public static String postForJson(String url, String body) {
        return REST_TEMPLATE.postForObject(url, new HttpEntity<>(body, jsonHeaders()), String.class);
    }

    public static ResponseEntity<String> put(String url, Object body, Object... uriVariables) {
        return exchange(url, body, HttpMethod.PUT, uriVariables);
    }

    public static <T> void put(String url, HttpEntity<T> httpEntity) {
        REST_TEMPLATE.put(url, httpEntity, String.class);
    }

    public static void putForXml(String url, String request) {
        REST_TEMPLATE.put(url, new HttpEntity<>(request, xmlHeaders()));
    }

    public static void putForJson(String url, String body) {
        REST_TEMPLATE.put(url, new HttpEntity<>(body, jsonHeaders()));
    }

    public static void delete(String url, Object... uriVariables) {
        REST_TEMPLATE.delete(url);
    }

    public static void delete(String url, Map<String, ?> uriVariables) {
        REST_TEMPLATE.delete(url, uriVariables);
    }
    public static ResponseEntity<String> exchange(String url, Object body, HttpMethod method, Object... uriVariables) {
        HttpEntity<Object> request = new HttpEntity<>(body, headers());
        return REST_TEMPLATE.exchange(url, method, request, String.class, uriVariables);
    }

    @Nullable
    public static <T> T execute(String url,
                                HttpMethod method,
                                @Nullable RequestCallback requestCallback,
                                @Nullable ResponseExtractor<T> responseExtractor,
                                Object... uriVariables) {
        return REST_TEMPLATE.execute(url, method, requestCallback, responseExtractor, uriVariables);
    }

    private static HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        return headers;
    }

    private static HttpHeaders xmlHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_XML);
        return httpHeaders;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return httpHeaders;
    }
}

