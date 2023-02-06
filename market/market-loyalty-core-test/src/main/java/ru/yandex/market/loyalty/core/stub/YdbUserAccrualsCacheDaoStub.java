package ru.yandex.market.loyalty.core.stub;

import ru.yandex.market.loyalty.core.dao.ydb.UserAccrualsCacheDao;
import ru.yandex.market.loyalty.core.model.ydb.UserAccrualsCacheEntry;
import ru.yandex.market.loyalty.lightweight.OneTimeSupplier;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;

public class YdbUserAccrualsCacheDaoStub implements StubDao, UserAccrualsCacheDao {

    private final ConcurrentMap<Long, List<UserAccrualsCacheEntry.AccrualStatusesWithAmount>> storage =
            new ConcurrentHashMap<>();

    @Override
    public void clear() {
        storage.clear();
    }

    @Override
    public List<UserAccrualsCacheEntry.AccrualStatusesWithAmount> getOrInsert(@NotNull Long uid,
                                                                              OneTimeSupplier<List<UserAccrualsCacheEntry.AccrualStatusesWithAmount>> valueSupplier) {
        List<UserAccrualsCacheEntry.AccrualStatusesWithAmount> accrualStatusesWithAmounts = storage.get(uid);
        if (accrualStatusesWithAmounts == null) {
            accrualStatusesWithAmounts = valueSupplier.get();
            storage.put(uid, valueSupplier.get());
        }
        return accrualStatusesWithAmounts;
    }
}
