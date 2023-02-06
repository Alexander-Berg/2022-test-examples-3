package ru.yandex.market.loyalty.core.stub;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ru.yandex.market.loyalty.core.dao.ydb.UserBlockPromoDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserBlockPromo;

import java.time.Clock;

public class YdbUserBlockPromoDaoStub implements StubDao, UserBlockPromoDao {

    private final Clock clock;
    private final ConcurrentMap<Long, UserBlockPromo> storage = new ConcurrentHashMap<>();

    public YdbUserBlockPromoDaoStub(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void clear() {
        storage.clear();
    }

    @Override
    public List<UserBlockPromo> getUserBlockedPromo(long uid) {
        return Optional.ofNullable(storage.get(uid))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    @Override
    public List<UserBlockPromo> getUserBlockedPromo(long uid, String threshold) {
        return getUserBlockedPromo(uid);
    }

    @Override
    public void upsertUserBlockPromo(long uid, String thresholdName, boolean enabled) {
        final UserBlockPromo userBlockPromo = new UserBlockPromo(uid, enabled, clock.instant(), thresholdName);
        storage.remove(uid);
        storage.put(uid, userBlockPromo);
    }

    @Override
    public void updateUserBlockPromo(long uid, String thresholdName, boolean enabled) {
        upsertUserBlockPromo(uid, thresholdName, enabled);
    }

    @Override
    public long getUserBlockedPromosSize() {
        return storage.size() == 0 ? 1 : storage.size();
    }
}
