package ru.yandex.market.tsup.core.cache.sly_cacher.service;

import java.lang.reflect.Method;
import java.util.Optional;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.data_provider.meta.ProviderMeta;
import ru.yandex.market.tpl.common.data_provider.primitive.SimpleIdFilter;
import ru.yandex.market.tpl.common.data_provider.provider.DataProvider;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.cache.sly_cacher.SlyCached;

class CacheInvalidationServiceTest extends AbstractContextualTest {
    public static final String SAMPLE_STRING = "abcd";
    @Autowired
    private CacheInvalidationService service;

    @Autowired
    private TestInvalidationStrategy testInvalidationStrategy;

    @Test
    void getProvideMethod() {
        TestDataProvider dp = new TestDataProvider();
        Optional<Method> method = dp.getProvideMethod();
        Assertions.assertThat(method).isNotEmpty();
        Assertions.assertThat(method.map(m -> invokeMethod(dp, m))).isEqualTo(Optional.of(SAMPLE_STRING));
    }

    @Test
    void callInvalidationStrategy() {
        testInvalidationStrategy.reset();

        TestDataProvider dp = new TestDataProvider();
        Method method = dp.getProvideMethod().get();
        service.callInvalidationStrategy(dp, method);

        Assertions
            .assertThat(testInvalidationStrategy.isExecuted())
            .isTrue();
        Assertions
            .assertThat(testInvalidationStrategy.getDataProvider())
            .isEqualTo(dp);
        Assertions
            .assertThat(testInvalidationStrategy.getFilterClass())
            .isEqualTo(SimpleIdFilter.class);
        Assertions
            .assertThat(testInvalidationStrategy.getAnnotation())
            .isEqualTo(method.getAnnotation(SlyCached.class));
    }

    @SneakyThrows
    private Object invokeMethod(
        TestDataProvider dp,
        Method m
    ) {
        return m.invoke(dp, null, null);
    }

    private static class TestDataProvider implements DataProvider<String, SimpleIdFilter> {
        @SlyCached(invalidationStrategy = TestInvalidationStrategy.class)
        @Override
        public String provide(
            SimpleIdFilter filter,
            @Nullable ProviderMeta meta
        ) {
            return SAMPLE_STRING;
        }
    }
}
