package ru.yandex.market.jmf.tvm.support.test;

import javax.inject.Inject;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.http.impl.HttpClientConfiguration;
import ru.yandex.market.jmf.http.impl.HttpClientFactoryImpl;
import ru.yandex.market.jmf.http.test.HttpClientFactoryTestConfiguration;
import ru.yandex.market.jmf.tvm.support.TvmService;
import ru.yandex.market.jmf.tvm.support.impl.TvmHttpRequestFilter;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringJUnitConfig(classes = TvmHttpClientFilterStrategyTest.Config.class)
@TestPropertySource({
        "classpath:tvm/support/test/tvm-support-test.properties",
        "classpath:/ru/yandex/market/jmf/http/internal/external.properties",
})
public class TvmHttpClientFilterStrategyTest {

    @Inject
    private HttpClientFactoryImpl factory;
    @Inject
    private TvmService tvmService;

    @AfterEach
    public void tearDown() {
        Mockito.reset(tvmService);
    }

    @Test
    public void testService2_tvm_no() {
        HttpClientConfiguration conf = factory.getConfiguration("service2");

        TvmHttpRequestFilter filter = find(conf.getFilters(), TvmHttpRequestFilter.class);
        Assertions.assertNull(
                filter,
                "Не должны получить TvmHttpRequestFilter т.к. значение external.service2.tvm.application не " +
                        "установлено "
        );
    }

    @Test
    public void testService4_tvm_application() {
        HttpClientConfiguration conf = factory.getConfiguration("service4");

        TvmHttpRequestFilter filter = find(conf.getFilters(), TvmHttpRequestFilter.class);
        Assertions.assertNotNull(filter,
                "Должны получить TvmHttpRequestFilter т.к. значение external.service4.tvm.application " +
                        "установлено ");
        Assertions.assertEquals("37", filter.getApplicationId(), "Должны получить значение свойства external.service4.tvm.application из external" +
                ".properties");
    }

    @Test
    public void testService4_tvm_applicationRegistered() {
        factory.getConfiguration("service4");

        Mockito.verify(tvmService, Mockito.times(1)).register("37");
    }

    private <T> T find(Iterable<?> values, Class<T> clazz) {
        return (T) Iterables.find(values, Predicates.instanceOf(clazz), null);
    }

    @Configuration
    @Import({
            HttpClientFactoryTestConfiguration.class,
            TvmSupportTestConfiguration.class,
    })
    public static class Config {}
}
