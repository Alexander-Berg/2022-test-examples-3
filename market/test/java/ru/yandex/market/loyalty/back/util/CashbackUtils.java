package ru.yandex.market.loyalty.back.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.market.loyalty.core.dao.ydb.UserAccrualsCacheDao;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.model.ydb.UserAccrualsCacheEntry;
import ru.yandex.market.loyalty.lightweight.OneTimeSupplier;

@Service
public class CashbackUtils {
    @Autowired
    private UserAccrualsCacheDao userAccrualsCacheDao;
    @Autowired
    private ClockForTests clock;

    @SafeVarargs
    public final void createUserAccruals(long uid, long promoId, Pair<Long, YandexWalletTransactionStatus>... data) {
        List<UserAccrualsCacheEntry.AccrualStatusesWithAmount> accruals = new ArrayList<>();
        for (Pair<Long, YandexWalletTransactionStatus> t : data) {
            accruals.add(createUserAccrual(t.getKey(), promoId, t.getRight()));
        }

        userAccrualsCacheDao.getOrInsert(uid, new OneTimeSupplier<>(() -> accruals));
    }

    private UserAccrualsCacheEntry.AccrualStatusesWithAmount createUserAccrual(long reward, long promoId,
                                                                               YandexWalletTransactionStatus status) {
        return new UserAccrualsCacheEntry.AccrualStatusesWithAmount(
                status, YandexWalletRefundTransactionStatus.NOT_QUEUED,
                BigDecimal.valueOf(reward), promoId, Timestamp.from(clock.instant())
        );
    }
}
