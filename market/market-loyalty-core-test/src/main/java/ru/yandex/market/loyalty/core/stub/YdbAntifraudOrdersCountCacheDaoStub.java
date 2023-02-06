package ru.yandex.market.loyalty.core.stub;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.loyalty.core.dao.ydb.caches.AntifraudOrdersCountCacheDao;
import ru.yandex.market.loyalty.lightweight.OneTimeSupplier;

public class YdbAntifraudOrdersCountCacheDaoStub implements StubDao, AntifraudOrdersCountCacheDao {

    private final static int ENTRY_TTL_SECONDS = 300;
    private final Map<Long, List<Pair<Instant, Integer>>> storage = new HashMap<>();

    private final Clock clock;

    public YdbAntifraudOrdersCountCacheDaoStub(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void clear() {
        storage.clear();
    }

    @Override
    public int getOrInsert(Long uid, OneTimeSupplier<Integer> valueSupplier) {
        List<Pair<Instant, Integer>> valuesMap = storage.getOrDefault(uid, List.of());

        Optional<Pair<Instant, Integer>> max = valuesMap.stream()
                .filter(pair -> pair.getFirst().isAfter(clock.instant()))
                .max(Comparator.comparing(Pair::getFirst));
        if (max.isEmpty()) {
            Integer value = valueSupplier.get();
            storage.computeIfAbsent(uid, aLong ->
                    List.of(new Pair<>(clock.instant().plusSeconds(ENTRY_TTL_SECONDS), value)));
            return value;
        } else {
            return max.get().getSecond();
        }
    }
}
