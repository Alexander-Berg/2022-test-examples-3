package ru.yandex.market.fulfillment.wrap.core.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.logistics.util.client.HttpTemplate;

class HttpTemplateImplTest extends SoftAssertionSupport {

    private static final String BASE_URL = "http://bdsm.net:6666";
    private static final MediaType MEDIA_TYPE = MediaType.APPLICATION_JSON;

    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final HttpTemplate httpTemplate = new HttpTemplateImpl(BASE_URL, restTemplate, MEDIA_TYPE);

    @Test
    void executePost() {
        Request request = new Request(1, 2);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MEDIA_TYPE);

        HttpEntity<Request> httpEntity = new HttpEntity<>(request, httpHeaders);

        String[] uriFragments = {"execute", "post"};

        Mockito.when(restTemplate.exchange("http://bdsm.net:6666/execute/post",
            HttpMethod.POST, httpEntity, Response.class))
            .thenReturn(new ResponseEntity<>(new Response(true), HttpStatus.OK));

        Response response = httpTemplate.executePost(request, Response.class, uriFragments);

        Mockito.verify(restTemplate).exchange("http://bdsm.net:6666/execute/post",
            HttpMethod.POST, httpEntity, Response.class);

        softly.assertThat(response.getSuccess())
            .as("Asserting the response success field")
            .isTrue();
    }

    @Test
    void executeGet() {
        Map<String, Set<String>> paramMap = new HashMap<>();
        paramMap.put("first", new HashSet<>(Collections.singletonList("1")));
        paramMap.put("second", new HashSet<>(Collections.singletonList("2")));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MEDIA_TYPE);

        HttpEntity<Request> httpEntity = new HttpEntity<>(httpHeaders);

        String[] uriFragments = {"execute", "get"};

        Mockito.when(restTemplate.exchange("http://bdsm.net:6666/execute/get?first=1&second=2",
            HttpMethod.GET, httpEntity, Response.class))
            .thenReturn(new ResponseEntity<>(new Response(true), HttpStatus.OK));

        Response response = httpTemplate.executeGet(Response.class, paramMap, uriFragments);

        Mockito.verify(restTemplate).exchange("http://bdsm.net:6666/execute/get?first=1&second=2",
            HttpMethod.GET, httpEntity, Response.class);

        softly.assertThat(response.getSuccess())
            .as("Asserting the response success field")
            .isTrue();
    }

    static class Request {

        private Integer first;

        private Integer second;

        Request(Integer first, Integer second) {
            this.first = first;
            this.second = second;
        }

        public Integer getFirst() {
            return first;
        }

        public Request setFirst(Integer first) {
            this.first = first;
            return this;
        }

        public Integer getSecond() {
            return second;
        }

        public Request setSecond(Integer second) {
            this.second = second;
            return this;
        }
    }

    static class Response {

        private Boolean success;

        Response(Boolean success) {
            this.success = success;
        }

        public Boolean getSuccess() {
            return success;
        }

        public Response setSuccess(Boolean success) {
            this.success = success;
            return this;
        }
    }
}
