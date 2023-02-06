package ru.yandex.market.api.partner.helper;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.spring.RestTemplateFactory;

/**
 * Расширенный утильный класс для осуществления запросов к HTTP серверу.
 * Date: 23.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class PartnerApiFunctionalTestHelper {

    private static final RestTemplate REST_TEMPLATE = RestTemplateFactory.createRestTemplate();

    private PartnerApiFunctionalTestHelper() {
    }

    /**
     * DELETE с json телом
     */
    @Nonnull
    public static ResponseEntity<String> deleteForJson(String url, @Nullable String request, String uid) {
        return exchange(url, request, HttpMethod.DELETE, jsonHeaders(uid));
    }

    /**
     * POST с json телом
     */
    @Nonnull
    public static ResponseEntity<String> postForJson(String url, @Nullable String request, String uid) {
        return exchange(url, request, HttpMethod.POST, jsonHeaders(uid));
    }

    /**
     * POST с xml телом
     */
    public static String postForXml(String url, @Nullable String request, String uid) {
        return REST_TEMPLATE.postForObject(url, new HttpEntity<>(request, xmlHeaders(uid)), String.class);
    }

    /**
     * GET с xml телом ответа
     */
    @Nonnull
    public static ResponseEntity<String> getForXml(String url, String uid) {
        return exchange(url, null, HttpMethod.GET, xmlHeaders(uid));
    }

    /**
     * GET с json телом ответа
     */
    @Nonnull
    public static ResponseEntity<String> getForJson(String url, String uid) {
        return exchange(url, null, HttpMethod.GET, jsonHeaders(uid));
    }

    @Nonnull
    private static ResponseEntity<String> exchange(String url,
                                                   @Nullable String request,
                                                   HttpMethod method,
                                                   HttpHeaders headers) {
        return REST_TEMPLATE.exchange(url, method, new HttpEntity<>(request, headers), String.class);
    }

    @Nonnull
    private static HttpHeaders jsonHeaders(String uid) {
        return httpHeaders(uid, MediaType.APPLICATION_JSON);
    }

    @Nonnull
    private static HttpHeaders xmlHeaders(String uid) {
        return httpHeaders(uid, MediaType.TEXT_XML);
    }

    @Nonnull
    private static HttpHeaders httpHeaders(String uid, MediaType mediaType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(mediaType);
        httpHeaders.setAccept(Collections.singletonList(mediaType));
        httpHeaders.set("X-AuthorizationService", "Mock");
        httpHeaders.set("Cookie", "yandexuid = " + uid);
        return httpHeaders;
    }
}
