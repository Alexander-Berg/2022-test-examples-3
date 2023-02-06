package ru.yandex.market.partner.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/**
 * @author fbokovikov
 */
public class FunctionalTestHelper {

    private static final Logger log = LoggerFactory.getLogger(FunctionalTestHelper.class);

    private final static RestTemplate REST_TEMPLATE = createJsonRestTemplate();
    private final static RestTemplate XML_REST_TEMPLATE = createXmlRestTemplate();

    private static RestTemplate createJsonRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(
                Arrays.asList(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        createJacksonConverter(),
                        new FormHttpMessageConverter(),
                        new ProtobufHttpMessageConverter()
                )
        );
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        return restTemplate;
    }

    private static MappingJackson2HttpMessageConverter createJacksonConverter() {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(createObjectMapper());
        return jacksonConverter;
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    private static RestTemplate createXmlRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(Collections.singletonList(new Jaxb2RootElementHttpMessageConverter()));
        return restTemplate;
    }

    public static <T> ResponseEntity<T> getXml(String url, Class<T> tClass, Object... uriVariables) {
        return logAndPropagateOrReturn(() -> {
            HttpEntity<String> request = new HttpEntity<>(null, headers());
            return XML_REST_TEMPLATE.exchange(url, HttpMethod.GET, request, tClass, uriVariables);
        });
    }

    public static <T> ResponseEntity<T> get(String url, Class<T> tClass, Object... uriVariables) {
        return logAndPropagateOrReturn(() -> {
            HttpEntity<String> request = new HttpEntity<>(null, headers());
            return REST_TEMPLATE.exchange(url, HttpMethod.GET, request, tClass, uriVariables);
        });
    }

    public static <T> ResponseEntity<T> get(URI uri, Class<T> tClass) {
        return logAndPropagateOrReturn(() -> {
            HttpEntity<String> request = new HttpEntity<>(null, headers());
            return REST_TEMPLATE.exchange(uri, HttpMethod.GET, request, tClass);
        });
    }

    public static ResponseEntity<String> get(String url, Object... uriVariables) {
        return exchange(url, null, HttpMethod.GET, uriVariables);
    }

    public static <T> ResponseEntity<T> get(RequestEntity<?> requestEntity, Class<T> clazz) {
        return exchange(
                requestEntity.getUrl().toString(),
                requestEntity.getBody(),
                requestEntity.getMethod(),
                clazz
        );
    }

    public static ResponseEntity<String> get(RequestEntity<?> requestEntity) {
        return get(requestEntity, String.class);
    }

    public static ResponseEntity<String> post(String url) {
        return post(url, (Object) null);
    }

    public static ResponseEntity<String> post(String url, Object body, Object... uriVariables) {
        return exchange(url, body, HttpMethod.POST, uriVariables);
    }

    public static <T> ResponseEntity<T> post(String url, Object body, Class<T> clazz, Object... uriVariables) {
        return exchange(url, body, HttpMethod.POST, clazz, uriVariables);
    }

    public static ResponseEntity<String> patch(String url, Object body, Object... uriVariables) {
        return exchange(url, body, HttpMethod.PATCH, uriVariables);
    }

    public static ResponseEntity<String> put(String url, Object body, Object... uriVariables) {
        return exchange(url, body, HttpMethod.PUT, uriVariables);
    }

    public static void delete(String url, Object... uriVariables) {
        logAndPropagate(() -> REST_TEMPLATE.delete(url, uriVariables));
    }

    public static void delete(String url, Map<String, ?> uriVariables) {
        logAndPropagate(() -> REST_TEMPLATE.delete(url, uriVariables));
    }

    public static ResponseEntity<String> deleteWithBody(String url, Object body) {
        return exchange(url, body, HttpMethod.DELETE);
    }

    public static ResponseEntity<String> exchange(String url, Object body, HttpMethod method, Object... uriVariables) {
        return exchange(url, body, method, String.class, uriVariables);
    }

    public static <T> ResponseEntity<T> exchange(String url, Object body, HttpMethod method, Class<T> clazz, Object... uriVariables) {
        return logAndPropagateOrReturn(() -> {
            HttpEntity<?> request;
            if (!(body instanceof HttpEntity)) {
                request = new HttpEntity<>(body, headers());
            } else {
                HttpEntity<?> httpEntity = (HttpEntity<?>) body;
                HttpHeaders headers = new HttpHeaders();
                headers.addAll(httpEntity.getHeaders());
                if (CollectionUtils.isEmpty(headers.getAccept())) {
                    headers.setAccept(Collections.singletonList(MediaType.ALL));
                }
                request = new HttpEntity<>(httpEntity.getBody(), headers);
            }
            return REST_TEMPLATE.exchange(url, method, request, clazz, uriVariables);
        });
    }

    public static <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpHeaders headers, Class<T> tClass) {
        return REST_TEMPLATE.exchange(url, method, new HttpEntity<>(null, headers), tClass);
    }

    public static HttpEntity<?> createMultipartHttpEntity(
            String fileParamName,
            Resource resource,
            Consumer<MultiValueMap<String, Object>> paramsEnricher
    ) {
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add(fileParamName, resource);
        paramsEnricher.accept(multipart);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(multipart, headers);
    }

    public static HttpEntity<?> createMultipartHttpEntity(
            Map<String, Resource> resources,
            Consumer<MultiValueMap<String, Object>> paramsEnricher
    ) {
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        resources.forEach(multipart::add);
        paramsEnricher.accept(multipart);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(multipart, headers);
    }

    @Nullable
    public static <T> T execute(String url,
                                HttpMethod method,
                                @Nullable RequestCallback requestCallback,
                                @Nullable ResponseExtractor<T> responseExtractor,
                                Object... uriVariables) {
        return logAndPropagateOrReturn(() ->
                REST_TEMPLATE.execute(url, method, requestCallback, responseExtractor, uriVariables)
        );
    }

    @Nullable
    public static <T> T execute(URI uri,
                                HttpMethod method,
                                @Nullable RequestCallback requestCallback,
                                @Nullable ResponseExtractor<T> responseExtractor) {
        return logAndPropagateOrReturn(() ->
                REST_TEMPLATE.execute(uri, method, requestCallback, responseExtractor)
        );
    }

    private static HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public static String getResource(Class<?> c, String fileName) {
        try {
            return IOUtils.toString(c.getResource(fileName), Charset.defaultCharset());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static <T> T logAndPropagateOrReturn(Supplier<T> callable) {
        try {
            return callable.get();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw logException(e);
        }
    }

    private static void logAndPropagate(Runnable runnable) {
        try {
            runnable.run();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw logException(e);
        }
    }

    private static HttpStatusCodeException logException(HttpStatusCodeException e) {
        log.warn("Error during request. Response:\n" + e.getResponseBodyAsString(), e);
        return e;
    }
}
