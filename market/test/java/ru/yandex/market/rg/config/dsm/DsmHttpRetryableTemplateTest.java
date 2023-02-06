package ru.yandex.market.rg.config.dsm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.rg.config.FunctionalTest;

class DsmHttpRetryableTemplateTest extends FunctionalTest {

    @Autowired
    DsmHttpRetryableTemplate testDsmHttpRetryableTemplate;

    @Autowired
    RestTemplate testDsmClientRestTemplate;

    @Test
    void testSuccess() {
        Mockito.when(testDsmClientRestTemplate.exchange(Mockito.anyString(),
                Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class), Mockito.any(Class.class)))
                .thenReturn(new ResponseEntity("OK", HttpStatus.OK));

        String response = testDsmHttpRetryableTemplate.executePost(new Object(), String.class, "abc");
        Assertions.assertEquals(response, "OK");
        Mockito.verify(testDsmClientRestTemplate, Mockito.only())
                .exchange(Mockito.anyString(), Mockito.any(HttpMethod.class),
                        Mockito.any(HttpEntity.class), Mockito.any(Class.class));
    }

    @Test
    void testThreeRetries() {
        Mockito.when(testDsmClientRestTemplate.exchange(Mockito.anyString(), Mockito.any(HttpMethod.class),
                Mockito.any(HttpEntity.class), Mockito.any(Class.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        Assertions.assertThrows(
                RestClientException.class,
                () -> testDsmHttpRetryableTemplate.executePost(new Object(), String.class, "abc"));
        Mockito.verify(testDsmClientRestTemplate, Mockito.times(3))
                .exchange(Mockito.anyString(), Mockito.any(HttpMethod.class),
                        Mockito.any(HttpEntity.class), Mockito.any(Class.class));
    }
}
