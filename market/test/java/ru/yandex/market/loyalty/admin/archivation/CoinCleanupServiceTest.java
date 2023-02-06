package ru.yandex.market.loyalty.admin.archivation;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.accounting.TransactionEntry;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoinNoAuth;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.admin.archivation.CoinCleanupService.DAYS_BEFORE_CLEANING;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EXPIRATION_DAYS;

/**
 * @author artemmz
 */
public class CoinCleanupServiceTest extends CleanupServiceTest {
    private static final int COIN_CNT = 100;
    private static final int USE_COIN_CNT = COIN_CNT / 10;
    private static final int AUTH_VS_NO_AUTH_SPLIT_FACTOR = 2;
    private static final int NO_AUTH_USED_CNT = USE_COIN_CNT - USE_COIN_CNT / AUTH_VS_NO_AUTH_SPLIT_FACTOR;
    private static final int AUTH_USED_CNT = USE_COIN_CNT - NO_AUTH_USED_CNT;
    /**
     * 2 for creation & usage each AUTH_USED_CNT and NO_AUTH_USED_CNT + 1 for binding NO_AUTH_USED_CNT
     */
    private static final int USED_HISTORY_CNT = AUTH_USED_CNT * 2 + NO_AUTH_USED_CNT * 2 + NO_AUTH_USED_CNT;

    @Autowired
    CoinCleanupService coinCleanupService;
    @Autowired
    ArchivationStateDao archivationStateDao;

    @Before
    public void setUp() throws Exception {
        configurationService.set(ConfigurationService.COIN_CLEANING_ENABLED, true);
    }

    @Test
    public void testNoEndlessLoops() {
        assertTrue(findExpiredCoins().isEmpty());
        assertNull(coinCleanupService.processBatch(100));
    }

    @Test
    public void testBatchCleaning() throws InterruptedException {
        Promo promo = createSmartShoppingPromo();
        Long spendingEmissionAccId = promo.getSpendingEmissionAccountId();
        Long budgetEmissionAccId = promo.getBudgetEmissionAccountId();

        List<CoinKey> createdCoins = createAndUseCoins(promo, COIN_CNT, USE_COIN_CNT, AUTH_VS_NO_AUTH_SPLIT_FACTOR);
        checkCreatedCoinData(createdCoins, COIN_CNT, USE_COIN_CNT); // no expired, records in history & transaction tables

        clock.spendTime(Math.max(DEFAULT_EXPIRATION_DAYS, DAYS_BEFORE_CLEANING) * 2L + 1, ChronoUnit.DAYS);
        coinService.lifecycle.expireCoins(COIN_CNT, 0, 1, false);
        var balanceBeforeCleaning = transactionDao.getCorrespondingBalance(spendingEmissionAccId, budgetEmissionAccId);
        checkExpiredCoinData(createdCoins, false); // expired & with records in all tables

        Long exportId = coinCleanupService.processOldCoinData(COIN_CNT / 10, Duration.ofSeconds(5));
        assertNotNull(exportId);
        checkExpiredCoinData(createdCoins, true); // tables cleaned, only used coins left

        Pair<ExportPayload, ArchivationState> payloadWithState = archivationStateDao.getPayload(exportId);
        assertEquals(ArchivationState.DELETED_AND_FINISHED, payloadWithState.getValue());

        var balanceAfterCleaning = transactionDao.getCorrespondingBalance(spendingEmissionAccId, budgetEmissionAccId);
        assertEquals(balanceBeforeCleaning, balanceAfterCleaning);

        List<TransactionEntry> foldedSpendingTransactions =
                transactionDao.getTransactionRowsByAccount(spendingEmissionAccId, COIN_CNT);
        assertTrue(foldedSpendingTransactions.size() <= USE_COIN_CNT * 2);
        assertTrue(transactionDao.getTransactionRowsByAccount(budgetEmissionAccId, COIN_CNT).size() <= USE_COIN_CNT * 2);
        assertEquals(payloadWithState.getKey().getNewTransaction(),
                foldedSpendingTransactions.stream().map(TransactionEntry::getTransactionId).max(Long::compareTo).orElse(null));
    }

    private void checkExpiredCoinData(List<CoinKey> coinKeys, boolean cleaned) {
        long noAuthExpiredCnt = findNoAuthCoins(coinKeys).stream()
                .map(CoinNoAuth::getCoinKey)
                .map(coinService.search::getCoin)
                .map(optCoin -> optCoin.orElse(null))
                .filter(Objects::nonNull)
                .filter(coin -> coin.getStatus() == CoreCoinStatus.EXPIRED)
                .count();
        assertEquals(cleaned, noAuthExpiredCnt == 0);
        assertEquals(cleaned, findExpiredCoins().isEmpty());

        Set<Long> historyRecordsIds = getHistoryRecordsIds(coinKeys);
        if (!cleaned) {
            assertTrue(historyRecordsIds.size() > COIN_CNT * 2); // creation + expiration/usage + binging for noAuth
        } else {
            assertEquals(USED_HISTORY_CNT, historyRecordsIds.size());
        }

        var historyXRecordsIdsWithTransactions = getHistoryXRecordsIdsWithTransactions(historyRecordsIds);
        assertEquals(cleaned ? USE_COIN_CNT * 2 : COIN_CNT * 2, historyXRecordsIdsWithTransactions.size());

        var transactionRows =
                transactionDao.getTransactionRows(new HashSet<>(historyXRecordsIdsWithTransactions.values()));
        if (!cleaned) {
            assertTrue(transactionRows.size() > COIN_CNT * 2);
        } else {
            assertTrue(transactionRows.size() < COIN_CNT);
        }

        var metaTransactions =
                metaTransactionDao.getTransactions(new ArrayList<>(historyXRecordsIdsWithTransactions.values()));
        if (!cleaned) {
            assertTrue(metaTransactions.size() > COIN_CNT);
        } else {
            assertEquals(USE_COIN_CNT * 2, metaTransactions.size()); // creation & usage
        }
    }

}
