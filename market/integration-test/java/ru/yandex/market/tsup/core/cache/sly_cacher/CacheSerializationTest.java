package ru.yandex.market.tsup.core.cache.sly_cacher;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.data_provider.filter.ProviderFilter;
import ru.yandex.market.tpl.common.data_provider.provider.DataProvider;
import ru.yandex.market.tpl.common.data_provider.util.ReflectionUtils;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.cache.sly_cacher.invalidation.CacheRefresher;

public class CacheSerializationTest extends AbstractContextualTest {
    @Autowired
    private CacheRefresher cacheRefresher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("getCacheableProviderFilters")
    void testFilterParse(Class<? extends ProviderFilter> filterClass) throws IOException {
        JsonNode filter = objectMapper.readValue("{}", JsonNode.class);
        cacheRefresher.extractFilter(filter, filterClass);
    }

    static Stream<Arguments> getCacheableProviderFilters() {
        Reflections reflections = new Reflections("ru/yandex/market/tsup");
        return reflections.getSubTypesOf(DataProvider.class)
            .stream()
            .map(dp -> ReflectionUtils.getMethod(dp, "provide", ProviderFilter.class))
            .flatMap(Optional::stream)
            .filter(m -> m.getAnnotation(SlyCached.class) != null)
            .map(m -> m.getParameterTypes()[0])
            .map(Arguments::of);
    }
}
