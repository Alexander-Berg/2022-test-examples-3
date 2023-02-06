package ru.yandex.market.api.partner.context;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.common.test.spring.RestTemplateFactory;

/**
 * @author fbokovikov
 */
public class FunctionalTestHelper {

    public static final long DEFAULT_UID = 67282295L;

    private static final RestTemplate REST_TEMPLATE = RestTemplateFactory.createRestTemplate();

    static {
        REST_TEMPLATE.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    private FunctionalTestHelper() {
        throw new UnsupportedOperationException("Can not instantiate util class");
    }

    private static HttpHeaders getHeadersWithAuthMock(long uid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-AuthorizationService", "Mock");
        headers.add("Cookie", String.format("yandexuid = %d;", uid));
        return headers;
    }

    private static HttpHeaders getHeadersWithAuthMock() {
        return getHeadersWithAuthMock(DEFAULT_UID);
    }

    public static ResponseEntity<String> makeRequestWithContentType(URI uri, HttpMethod method, Format format, String body) {
        HttpHeaders headers = getHeadersWithAuthMock();
        headers.setContentType(format.getContentType());
        headers.setAccept(Collections.singletonList(format.getContentType()));
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        return REST_TEMPLATE.exchange(uri, method, request, String.class);
    }

    public static <T> ResponseEntity<T> makeRequestWithContentType(String url, HttpMethod method, Class<T> tClass, MediaType contentType) {
        HttpHeaders headers = getHeadersWithAuthMock();
        headers.setContentType(contentType);
        headers.setAccept(List.of(contentType));
        HttpEntity<String> request = new HttpEntity<>(headers);
        return REST_TEMPLATE.exchange(url, method, request, tClass);
    }

    public static <T> ResponseEntity<T> makeRequestWithContentType(String url, HttpMethod method, String body, Class<T> tClass, MediaType contentType) {
        HttpHeaders headers = getHeadersWithAuthMock();
        headers.setContentType(contentType);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        return REST_TEMPLATE.exchange(url, method, request, tClass);
    }

    public static <T> ResponseEntity<T> makeRequest(String url, HttpMethod method, Class<T> tClass) {
        HttpEntity<T> request = new HttpEntity<>(getHeadersWithAuthMock());
        return REST_TEMPLATE.exchange(url, method, request, tClass);
    }

    public static <T> ResponseEntity<T> makeRequest(String url, HttpMethod method, Class<T> tClass, long uid) {
        HttpEntity<T> request = new HttpEntity<>(getHeadersWithAuthMock(uid));
        return REST_TEMPLATE.exchange(url, method, request, tClass);
    }

    public static ResponseEntity<String> makeRequest(String url, HttpMethod method, long uid) {
        HttpEntity<String> request = new HttpEntity<>(getHeadersWithAuthMock(uid));
        return REST_TEMPLATE.exchange(url, method, request, String.class);
    }

    public static ResponseEntity<String> makeRequest(String url, HttpMethod method, Format format) {
        return makeRequest(url, method, format, null, String.class, null);
    }

    public static ResponseEntity<String> makeRequest(String url, HttpMethod method, Format format, long uid) {
        return makeRequest(url, method, format, null, String.class, uid);
    }

    public static ResponseEntity<String> makeRequest(String url, HttpMethod method, Format format, String body) {
        return makeRequest(url, method, format, body, String.class, null);
    }

    public static <T> ResponseEntity<T> makeRequest(String url, HttpMethod method, Format format, Class<T> tClass) {
        return makeRequest(url, method, format, null, tClass, null);
    }

    public static <T> ResponseEntity<T> makeRequest(String url, HttpMethod method, Format format, @Nullable String body, Class<T> tClass) {
        return makeRequest(url, method, format, body, tClass, null);
    }

    public static <T> ResponseEntity<T> makeRequest(String url, HttpMethod method, Format format, @Nullable String body, Class<T> tClass, Long uid) {
        HttpHeaders headers = (uid == null) ? getHeadersWithAuthMock() : getHeadersWithAuthMock(uid);
        headers.setAccept(Collections.singletonList(format.getContentType()));
        if (method != HttpMethod.GET) {
            headers.setContentType(format.getContentType());
        }
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        return REST_TEMPLATE.exchange(url, method, request, tClass);
    }

    public static <T> ResponseEntity<T> makeRequest(URI uri, HttpMethod method, Class<T> tClass) {
        HttpHeaders headers = getHeadersWithAuthMock();
        HttpEntity<T> request = new HttpEntity<>(null, headers);
        return REST_TEMPLATE.exchange(uri, method, request, tClass);
    }

    public static ResponseEntity<String> makeRequest(URI uri, HttpMethod method, Format format) {
        return makeRequest(uri, method, format, null, String.class);
    }

    public static ResponseEntity<String> makeRequest(URI uri, HttpMethod method, Format format, String body) {
        return makeRequest(uri, method, format, body, String.class);
    }

    public static <T> ResponseEntity<T> makeRequest(URI uri, HttpMethod method, Format format, Class<T> tClass) {
        return makeRequest(uri, method, format, null, tClass);
    }

    public static <T> ResponseEntity<T> makeRequest(URI uri, HttpMethod method, Format format, @Nullable String body, Class<T> tClass) {
        HttpHeaders headers = getHeadersWithAuthMock();
        headers.setAccept(Collections.singletonList(format.getContentType()));
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        return REST_TEMPLATE.exchange(uri, method, request, tClass);
    }

    public static <T> ResponseEntity<T> makeRequest(String url, HttpMethod method, HttpHeaders headers, Class<T> tClass) {
        return REST_TEMPLATE.exchange(url, method, new HttpEntity<>(null, headers), tClass);
    }

    public static ResponseEntity<String> makeMultipartRequest(URI uri, MultipartFile multipartFile, Format format) {
        try {
            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add(
                    multipartFile.getName(),
                    new ByteArrayResource(multipartFile.getBytes()) {
                        @Override
                        public String getFilename() {
                            return multipartFile.getOriginalFilename();
                        }
                    });
            HttpHeaders httpHeaders = getHeadersWithAuthMock();
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            httpHeaders.setAccept(Collections.singletonList(format.getContentType()));
            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(multipartBody, httpHeaders);
            return REST_TEMPLATE.exchange(uri, HttpMethod.POST, httpEntity, String.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
