package ru.yandex.market.jmf.http.impl;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.http.test.HttpClientFactoryTestConfiguration;
import ru.yandex.market.request.trace.Module;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = HttpClientFactoryTestConfiguration.class)
@TestPropertySource("classpath:/ru/yandex/market/jmf/http/internal/external.properties")
public class HttpClientFactoryTest {

    @Inject
    HttpClientFactoryImpl factory;

    @Test
    public void testService1_connectTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                17, conf.getConnectTimeout(),
                "Должны получить значение свойства external.service1.connectTimeout из external.properties");
    }

    @Test
    public void testService1_maxConnections() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                19, conf.getMaxConnections(),
                "Должны получить значение свойства external.service1.maxConnections из external.properties");
    }

    @Test
    public void testService1_maxRedirects() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                2, conf.getMaxRedirects(), "Должны получить значение свойства external.service1.maxRedirects из " +
                        "external.properties");
    }

    @Test
    public void testService1_module() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                Module.MARKET_CONTENT_API, conf.getModule(),
                "Должны получить значение свойства external.service1.module из external.properties");
    }

    @Test
    public void testService1_readTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                11, conf.getReadTimeout(), "Должны получить значение свойства external.service1.readTimeout из external.properties");
    }

    @Test
    public void testService1_requestTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                13, conf.getRequestTimeout(), "Должны получить значение свойства external.service1.requestTimeout из external.properties");
    }

    @Test
    public void testService1_url() {
        HttpClientConfiguration conf = factory.getConfiguration("service1");

        Assertions.assertEquals(
                "http://example.com/service1", conf.getBaseUrl(), "Должны получить значение свойства external" +
                        ".service1.url из external.properties");
    }

    @Test
    public void testService2_default_connectTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                7, conf.getConnectTimeout(), "Должны получить значение свойства external.default.connectTimeout из external.properties");
    }

    @Test
    public void testService2_default_maxConnections() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                2, conf.getMaxConnections(), "Должны получить значение свойства external.default.maxConnections из external.properties");
    }

    @Test
    public void testService2_default_maxRedirects() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                1, conf.getMaxRedirects(), "Должны получить значение свойства external.default.maxRedirects из external.properties");
    }

    @Test
    public void testService2_default_readTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                3, conf.getReadTimeout(), "Должны получить значение свойства external.default.readTimeout из external.properties");
    }

    @Test
    public void testService2_default_requestTimeout() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        Assertions.assertEquals(
                5, conf.getRequestTimeout(), "Должны получить значение свойства external.default.requestTimeout из external.properties");
    }
}
