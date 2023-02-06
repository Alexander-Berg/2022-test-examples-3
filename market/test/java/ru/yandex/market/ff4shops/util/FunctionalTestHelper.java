package ru.yandex.market.ff4shops.util;

import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.text.Charsets;

/**
 * @author fbokovikov
 */
public final class FunctionalTestHelper {

    private static final RestTemplate REST_TEMPLATE = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    static {
        REST_TEMPLATE.setMessageConverters(List.of(
                new StringHttpMessageConverter(Charsets.UTF_8)
        ));
    }

    private FunctionalTestHelper() {
    }

    @Nonnull
    public static ResponseEntity<String> postForXml(String url, Object body) {
        HttpEntity<?> httpEntity = new HttpEntity<>(body, xmlHeaders());
        return REST_TEMPLATE.postForEntity(url, httpEntity, String.class);
    }

    public static ResponseEntity<String> postForEntity(String url, String body, HttpHeaders headers) {
        return REST_TEMPLATE.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }

    public static ResponseEntity<String> putForEntity(String url, String body, HttpHeaders headers) {
        return REST_TEMPLATE.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    public static ResponseEntity<String> getForEntity(String url, HttpHeaders headers) {
        HttpEntity<?> entity = new HttpEntity<>(headers);
        return REST_TEMPLATE.exchange(url, HttpMethod.GET, entity, String.class);
    }

    public static ResponseEntity<String> getForEntity(String url) {
        return REST_TEMPLATE.exchange(url, HttpMethod.GET, null, String.class);
    }

    public static HttpHeaders xmlHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.TEXT_XML);
        return httpHeaders;
    }

    public static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
