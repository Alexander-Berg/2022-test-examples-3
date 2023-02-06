package ru.yandex.market.api.admin;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.junit.Test;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.util.cache.ReloadableCache;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public class ReloadCacheServiceTest extends ContainerTestBase {

    @Inject
    private List<ReloadableCache> caches;

    @Test
    public void uniqueCacheKeys() {
        Multimap<String, ReloadableCache> multimap = Multimaps.index(caches, x -> x.getCacheInfo().getKey());

        for (String key : multimap.keySet()) {
            Collection<ReloadableCache> list = multimap.get(key);
            if (list.size() > 1) {
                throw new IllegalArgumentException(
                        String.format("There are some caches with equal cache key = '%s'. Corresponding class names: %s.",
                                key,
                                list.stream().map(x -> x.getClass().getCanonicalName()).collect(Collectors.joining(", "))));
            }
        }
    }
}
