package ru.yandex.market.loyalty.core.load.mock;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.loyalty.core.service.blackbox.domain.BlackBoxResponse;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static net.bytebuddy.matcher.ElementMatchers.anyOf;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RestTemplateMockTest {
    @Test
    public void exchange_100PercentCache() {
        RestTemplate originalRestTemplateMock = Mockito.mock(RestTemplate.class);
        RestTemplateMock restTemplateMock = new RestTemplateMock(originalRestTemplateMock, 10000, 100, 50L, "load");
        URI uri = getUri();
        when(originalRestTemplateMock.exchange(eq(uri), any(), any(), eq(BlackBoxResponse.class)))
                .thenReturn(ResponseEntity.ok(BlackBoxResponse.builder().setAge(140).build()));
        when(originalRestTemplateMock.exchange(eq(uri), eq(HttpMethod.POST), any(), eq(BlackBoxResponse.class)))
                .thenReturn(ResponseEntity.ok(BlackBoxResponse.builder().setAge(130).build()));

        ResponseEntity<BlackBoxResponse> exchange = restTemplateMock.exchange(
                uri, HttpMethod.GET, HttpEntity.EMPTY, BlackBoxResponse.class);
        ResponseEntity<BlackBoxResponse> exchange1 = restTemplateMock.exchange(
                uri, HttpMethod.GET, HttpEntity.EMPTY, BlackBoxResponse.class);

        ResponseEntity<BlackBoxResponse> exchange3 = restTemplateMock.exchange(
                uri, HttpMethod.POST, HttpEntity.EMPTY, BlackBoxResponse.class);

        verify(originalRestTemplateMock, times(2))
                .exchange(eq(uri), any(), any(), eq(BlackBoxResponse.class));

        assertEquals(exchange, exchange1);
        assertNotEquals(exchange, exchange3);
    }

    @Test
    public void exchange_10PercentCache() {
        RestTemplate originalRestTemplateMock = Mockito.mock(RestTemplate.class);
        RestTemplateMock restTemplateMock = new RestTemplateMock(originalRestTemplateMock, 10000, 10, 50L, "load");
        URI uri = getUri();
        when(originalRestTemplateMock.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(BlackBoxResponse.class)))
                .thenReturn(ResponseEntity.ok(BlackBoxResponse.builder().setAge(140).build()));
        when(originalRestTemplateMock.exchange(eq(uri), eq(HttpMethod.POST), any(), eq(BlackBoxResponse.class)))
                .thenReturn(ResponseEntity.ok(BlackBoxResponse.builder().setAge(130).build()));
        when(originalRestTemplateMock.exchange(eq(uri), eq(HttpMethod.DELETE), any(), eq(BlackBoxResponse.class)))
                .thenReturn(ResponseEntity.ok(BlackBoxResponse.builder().setAge(120).build()));

        ResponseEntity<BlackBoxResponse> exchange = restTemplateMock.exchange(
                uri, HttpMethod.GET, HttpEntity.EMPTY, BlackBoxResponse.class);
        ResponseEntity<BlackBoxResponse> exchange1 = restTemplateMock.exchange(
                uri, HttpMethod.GET, HttpEntity.EMPTY, BlackBoxResponse.class);

        ResponseEntity<BlackBoxResponse> exchange3 = restTemplateMock.exchange(
                uri, HttpMethod.POST, HttpEntity.EMPTY, BlackBoxResponse.class);
        ResponseEntity<BlackBoxResponse> exchange4 = restTemplateMock.exchange(
                uri, HttpMethod.DELETE, HttpEntity.EMPTY, BlackBoxResponse.class);

        verify(originalRestTemplateMock, times(1))
                .exchange(eq(uri), any(), any(), eq(BlackBoxResponse.class));

        assertEquals(exchange, exchange1);
        assertEquals(exchange, exchange3);
        assertEquals(exchange, exchange4);
    }

    @Test
    public void exchange_50PercentCache() {
        RestTemplate originalRestTemplateMock = Mockito.mock(RestTemplate.class);
        RestTemplateMock restTemplateMock = new RestTemplateMock(originalRestTemplateMock, 10000, 50, 50L, "load");
        URI uri = getUri();
        when(originalRestTemplateMock.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(BlackBoxResponse.class)))
                .thenReturn(ResponseEntity.ok(BlackBoxResponse.builder().setAge(140).build()));
        when(originalRestTemplateMock.exchange(eq(uri), eq(HttpMethod.POST), any(), eq(BlackBoxResponse.class)))
                .thenReturn(ResponseEntity.ok(BlackBoxResponse.builder().setAge(130).build()));
        when(originalRestTemplateMock.exchange(eq(uri), eq(HttpMethod.DELETE), any(), eq(BlackBoxResponse.class)))
                .thenReturn(ResponseEntity.ok(BlackBoxResponse.builder().setAge(150).build()));

        ResponseEntity<BlackBoxResponse> exchange = restTemplateMock.exchange(
                uri, HttpMethod.GET, HttpEntity.EMPTY, BlackBoxResponse.class);
        ResponseEntity<BlackBoxResponse> exchange1 = restTemplateMock.exchange(
                uri, HttpMethod.GET, HttpEntity.EMPTY, BlackBoxResponse.class);

        ResponseEntity<BlackBoxResponse> exchange3 = restTemplateMock.exchange(
                uri, HttpMethod.POST, HttpEntity.EMPTY, BlackBoxResponse.class);
        ResponseEntity<BlackBoxResponse> exchange4 = restTemplateMock.exchange(
                uri, HttpMethod.DELETE, HttpEntity.EMPTY, BlackBoxResponse.class);

        verify(originalRestTemplateMock, times(2))
                .exchange(eq(uri), any(), any(), eq(BlackBoxResponse.class));

        assertEquals(exchange, exchange1);
        assertTrue(List.of(getAge(exchange3), getAge(exchange)).contains(getAge(exchange4)));
    }

    private int getAge(ResponseEntity<BlackBoxResponse> entity) {
        if (entity != null) {
            BlackBoxResponse body = entity.getBody();
            if (body != null) {
                return body.getAge();
            }
        }
        return 0;
    }


    @Test(expected = WrongEnvironmentException.class)
    public void exchange_shouldFailOnProdEnv() {
        RestTemplate originalRestTemplateMock = Mockito.mock(RestTemplate.class);
        new RestTemplateMock(originalRestTemplateMock, 10000, 10, 50L, "prod");
    }

    private URI getUri() {
        return UriComponentsBuilder.fromUriString("someUrl")
                .queryParam("method", "userinfo")
                .queryParam("format", "json")
                .build().toUri();
    }
}
