package ru.yandex.market.loyalty.admin.archivation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.counter.CounterKey;
import ru.yandex.market.loyalty.core.dao.counter.CounterService;
import ru.yandex.market.loyalty.core.dao.counter.CounterType;
import ru.yandex.market.loyalty.core.model.StoredCounter;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.service.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.admin.archivation.TransactionCleanupService.DELETE_AFTER;

/**
 * @author artemmz
 */
public class TransactionCleanupServiceTest extends CleanupServiceTest {
    private static final int COIN_COUNT = 100;
    private static final int USE_COIN_COUNT = COIN_COUNT / 5;
    private static final int REVERT_COIN_COUNT = USE_COIN_COUNT / 2;
    private static final int COUPON_COUNT = 20;
    private static final int DELETED_COIN_COUNT = 10;
    private static final int AUTH_NO_AUTH_SPLIT_FACTOR = 2;
    private static final CounterKey COUNTER_KEY = new CounterKey(CounterType.LAST_SCANNED_TRANSACTION_FOR_DELETION);
    @Autowired
    TransactionCleanupService transactionCleanupService;
    @Autowired
    CounterService counterService;

    @Before
    public void setUp() throws Exception {
        configurationService.set(ConfigurationService.TRANSACTION_CLEANING_ENABLED, true);
    }

    @Test
    public void testBatchCleaning() {
        List<CoinKey> coins = createAndUseAndRevertCoins(COIN_COUNT, USE_COIN_COUNT, AUTH_NO_AUTH_SPLIT_FACTOR, REVERT_COIN_COUNT);
        List<Coupon> coupons = useCoupons(COUPON_COUNT);

        checkCreatedCoinData(coins, coins.size(), USE_COIN_COUNT - REVERT_COIN_COUNT);
        checkCreatedCouponData(coupons);

        var removedCoinsTransactions = removeCoinDataReturningTransactionIds(coins);
        checkTransactionData(removedCoinsTransactions, removedCoinsTransactions.size());

        clock.spendTime(DELETE_AFTER);
        processOldTransactionsWithNoCoins();

        checkTransactionData(removedCoinsTransactions, 0);
        assertEquals(
                getTransactions(coins, coupons).stream().max(Long::compareTo).orElseThrow(),
                counterService.findBy(COUNTER_KEY).map(StoredCounter::getValue).orElseThrow()
        );
    }

    @Test
    public void testBatchCleaning_doNotDeleteFresh() {
        List<CoinKey> coins = createCoins(COIN_COUNT, AUTH_NO_AUTH_SPLIT_FACTOR);
        Long minTrId = getTransactions(coins).stream().min(Long::compareTo).orElseThrow();
        var removedCoinsTransactions = removeCoinDataReturningTransactionIds(
                coins
        );
        checkTransactionData(removedCoinsTransactions, DELETED_COIN_COUNT);

        processOldTransactionsWithNoCoins();
        checkTransactionData(removedCoinsTransactions, DELETED_COIN_COUNT);

        Long lastProcessedTrId = counterService.findBy(COUNTER_KEY).map(StoredCounter::getValue).orElseThrow();
        assertTrue(lastProcessedTrId > 0 && lastProcessedTrId <= minTrId); // есть всякие побочные транзакции при заведении промо и пр
    }

    @Test
    public void testBatchCleaning_allTransactionsHaveCoins() {
        List<CoinKey> coins = createCoins(COIN_COUNT, AUTH_NO_AUTH_SPLIT_FACTOR);
        checkCreatedCoinData(coins);
        List<Long> coinTransactions = getTransactions(coins);
        checkTransactionData(new HashSet<>(coinTransactions), COIN_COUNT);

        clock.spendTime(DELETE_AFTER);
        processOldTransactionsWithNoCoins();
        checkTransactionData(new HashSet<>(coinTransactions), COIN_COUNT);
        assertEquals(
                coinTransactions.stream().max(Long::compareTo).orElseThrow(),
                counterService.findBy(COUNTER_KEY).map(StoredCounter::getValue).orElseThrow()
        );
    }

    @Test
    public void testBatchCleaning_doNotCleanMoreThanMaxTransactionId() {
        configurationService.set(ConfigurationService.MAX_TRANSACTION_TO_CLEAN, 0L);
        List<CoinKey> coins = createCoins(COIN_COUNT, AUTH_NO_AUTH_SPLIT_FACTOR);

        checkCreatedCoinData(coins);

        var removedCoinsTransactions = removeCoinDataReturningTransactionIds(coins);
        checkTransactionData(removedCoinsTransactions, DELETED_COIN_COUNT);

        clock.spendTime(DELETE_AFTER);
        processOldTransactionsWithNoCoins();

        checkTransactionData(removedCoinsTransactions, removedCoinsTransactions.size());
        assertEquals(
                (Long) 0L,
                counterService.findBy(COUNTER_KEY).map(StoredCounter::getValue).orElseThrow()
        );
    }

    private Set<Long> removeCoinDataReturningTransactionIds(List<CoinKey> coins) {
        Collections.shuffle(coins);
        List<Long> coinIdsToDelete = coins.stream()
                .limit(DELETED_COIN_COUNT)
                .map(CoinKey::getId)
                .collect(Collectors.toList());

        Set<Long> historyIdsToDelete = getHistoryRecordsIdsByIds(coinIdsToDelete);
        Map<Long, Long> historyXIdsToDeleteWithTransactions = getHistoryXRecordsIdsWithTransactions(historyIdsToDelete);

        coinHistoryDao.removeRecordsX(new ArrayList<>(historyXIdsToDeleteWithTransactions.keySet()));
        coinHistoryDao.removeRecords(new ArrayList<>(historyIdsToDelete));
        coinDao.removeCoinNoAuthRecords(coinIdsToDelete);
        coinDao.removeCoinRecords(coinIdsToDelete);

        return new HashSet<>(historyXIdsToDeleteWithTransactions.values());
    }

    private void checkTransactionData(Set<Long> transactionIds, int expectedCount) {
        assertEquals(expectedCount * 2, transactionDao.getTransactionRows(transactionIds).size());
        assertEquals(expectedCount, metaTransactionDao.getTransactions(transactionIds).size());
    }

    private void processOldTransactionsWithNoCoins() {
        transactionCleanupService.processOldTransactionsWithNoCoins(COIN_COUNT / 10, COIN_COUNT / 5,
                Duration.ofSeconds(10));
    }

}
