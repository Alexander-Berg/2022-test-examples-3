package ru.yandex.market.tsup.core.cache.sly_cacher.service;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tpl.common.data_provider.filter.ProviderFilter;
import ru.yandex.market.tpl.common.data_provider.meta.ProviderMeta;
import ru.yandex.market.tpl.common.data_provider.primitive.SimpleIdFilter;
import ru.yandex.market.tpl.common.data_provider.provider.DataProvider;
import ru.yandex.market.tsup.core.cache.sly_cacher.CacheProvider;
import ru.yandex.market.tsup.core.cache.sly_cacher.CachingKey;
import ru.yandex.market.tsup.core.cache.sly_cacher.fetcher.CachedData;
import ru.yandex.market.tsup.service.data_provider.log.DataProviderLogService;
import ru.yandex.market.tsup.service.data_provider.primitive.external.lms.logistic_point.LogisticPointProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CacheReadWriteServiceTest {
    private final CacheProvider cacheProvider = Mockito.mock(CacheProvider.class);
    private final DataProviderLogService logService = Mockito.mock(DataProviderLogService.class);
    private final TestableClock clock = new TestableClock();
    private final CacheReadWriteService service = new CacheReadWriteService(cacheProvider, logService, clock);

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.ofEpochMilli(1_000), ZoneId.of("UTC"));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(cacheProvider, logService);
    }

    @Test
    void storeToCache() {
        CachingKey cachingKey = new CachingKey("group", "key");
        service.storeToCache(cachingKey, List.of(new Dto("a", 1)));

        verify(cacheProvider).set(
            eq(cachingKey),
            argThat((ArgumentMatcher<String>) actual -> {
                try {
                    IntegrationTestUtils.assertJson("sly_cacher/data.json", actual);
                    return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                    return false;
                }
            })
        );
    }

    @Test
    void getFromCache() throws IOException {
        CachingKey cachingKey = new CachingKey("group", "key");
        Type type = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{Dto.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        when(cacheProvider.exists(cachingKey)).thenReturn(false);

        SimpleIdFilter filter = SimpleIdFilter.builder().id(1L).build();
        Optional<CachedData<Object>> fromCache =
            service.getFromCache(cachingKey, type, TestProvider.class, filter, clock.millis(), new AtomicBoolean(false));

        Assertions
            .assertThat(fromCache)
            .isEqualTo(Optional.empty());

        verify(cacheProvider).exists(eq(cachingKey));
    }

    @Test
    void getFromCacheMissing() throws IOException {
        CachingKey cachingKey = new CachingKey("group", "key");
        Type t = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{Dto.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        when(cacheProvider.exists(cachingKey)).thenReturn(true);
        when(cacheProvider.get(eq(cachingKey)))
            .thenReturn(IntegrationTestUtils.extractFileContent("sly_cacher/data.json"));

        SimpleIdFilter filter = SimpleIdFilter.builder().id(1L).build();
        Optional<CachedData<Object>> fromCache =
            service.getFromCache(cachingKey, t, TestProvider.class, filter, clock.millis(), new AtomicBoolean(false));

        Assertions
            .assertThat(fromCache)
            .isEqualTo(Optional.of(new CachedData<>(
                List.of(new Dto("a", 1)),
                1_000L
            )));

        verify(logService).logCacheable(eq(TestProvider.class), eq(filter), eq(true), any());
        verify(cacheProvider).exists(eq(cachingKey));
        verify(cacheProvider).get(eq(cachingKey));
    }

    @Test
    void getGroupName() {
        Assertions
            .assertThat(CacheReadWriteService.getGroupName(new LogisticPointProvider(null)))
            .isEqualTo("primitive.external.lms.logistic_point.LogisticPointProvider");
    }

    @Test
    void getTypeReference() throws NoSuchMethodException, IOException {
        TestProvider provider = new TestProvider();
        String json = IntegrationTestUtils.extractFileContent("sly_cacher/data.json");
        Method method = TestProvider.class.getMethod("provide", ProviderFilter.class, ProviderMeta.class);

        TypeReference<?> typeReference = CacheReadWriteService.getTypeReference(method.getGenericReturnType());

        ObjectMapper objectMapper = new ObjectMapper();
        Object actual = objectMapper.readValue(json, typeReference);
        Object expected = provider.provide(null, null);

        Assertions.assertThat(actual).isEqualTo(new CachedData<>(expected, 1000));
    }

    static class TestProvider implements DataProvider<List<Dto>, ProviderFilter> {

        @Override
        public List<Dto> provide(
            ProviderFilter filter,
            @Nullable ProviderMeta meta
        ) {
            return List.of(new Dto("a", 1));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Dto {
        String v1;
        int v2;
    }
}
