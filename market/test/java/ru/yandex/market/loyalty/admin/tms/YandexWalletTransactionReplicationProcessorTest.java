package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.http.Compressor;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletNewTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPriority;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class YandexWalletTransactionReplicationProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @YtHahn
    @Autowired
    private YandexWalletTransactionReplicationProcessor yandexWalletTransactionReplicationProcessor;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private PromoManager promoManager;
    @YtHahn
    @Autowired
    private Yt yt;

    @Test
    public void shouldMarkAndResetSyncTime() throws Exception {
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.ONE)
        );

        when(yt.transactions().start(any(Optional.class), anyBoolean(), any(Duration.class)))
                .thenReturn(GUID.valueOf("1-8e9c4f69-a3bc6964-3e99ab10"));

        yandexWalletTransactionDao.enqueueTransactions(
                null,
                "campaign-1",
                List.of(YandexWalletNewTransaction.builder()
                        .setAmount(BigDecimal.ONE)
                        .setReferenceId("ref-1")
                        .setProductId("product-1")
                        .setUid(DEFAULT_UID)
                        .build()),
                null,
                null,
                promo.getId(),
                YandexWalletTransactionPriority.HIGH
        );

        YandexWalletTransaction transaction1 =
                yandexWalletTransactionDao.query(YandexWalletTransactionStatus.IN_QUEUE, 1).get(0);
        assertNull(transaction1.getHahnSyncAt());

        yandexWalletTransactionReplicationProcessor.replication(Duration.ofMinutes(1));

        YandexWalletTransaction transaction2 =
                yandexWalletTransactionDao.query(YandexWalletTransactionStatus.IN_QUEUE, 1).get(0);
        assertNotNull(transaction2.getHahnSyncAt());

        yandexWalletTransactionDao.updateStatus(transaction2.getId(), transaction2.getStatus(),
                YandexWalletTransactionStatus.CANCELLED, null);
        YandexWalletTransaction transaction3 =
                yandexWalletTransactionDao.query(YandexWalletTransactionStatus.CANCELLED, 1).get(0);
        assertNull(transaction3.getHahnSyncAt());
    }


    @Test
    public void shouldInsertTransaction() throws Exception {
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.ONE)
        );

        yandexWalletTransactionDao.enqueueTransactions(
                null,
                "campaign-1",
                List.of(YandexWalletNewTransaction.builder()
                        .setAmount(BigDecimal.ONE)
                        .setReferenceId("ref-1")
                        .setProductId("product-1")
                        .setUid(DEFAULT_UID)
                        .build()),
                null,
                null,
                promo.getId(),
                YandexWalletTransactionPriority.HIGH
        );

        YandexWalletTransaction transaction1 =
                yandexWalletTransactionDao.query(YandexWalletTransactionStatus.IN_QUEUE, 1).get(0);
        assertNull(transaction1.getHahnSyncAt());

        yandexWalletTransactionReplicationProcessor.replication(Duration.ofMinutes(1));

        verify(yt.tables(), times(1)).insertRows(eq(Optional.empty()),
                Mockito.any(YPath.class),
                eq(true),
                eq(false),
                eq(true),
                eq(YTableEntryTypes.JACKSON_UTF8),
                any(Iterator.class),
                any(Compressor.class));
        verify(yt.tables(), never()).deleteRows(eq(Optional.empty()),
                Mockito.any(YPath.class),
                eq(true),
                eq(YTableEntryTypes.JACKSON_UTF8),
                any(Iterable.class),
                any(Compressor.class));
    }

    @Test
    public void shouldDeleteTransaction() throws Exception {
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.ONE)
        );

        yandexWalletTransactionDao.enqueueTransactions(
                null,
                "campaign-1",
                List.of(YandexWalletNewTransaction.builder()
                        .setAmount(BigDecimal.ONE)
                        .setReferenceId("ref-1")
                        .setProductId("product-1")
                        .setUid(DEFAULT_UID)
                        .build()),
                null,
                null,
                promo.getId(),
                YandexWalletTransactionPriority.HIGH
        );

        YandexWalletTransaction transaction1 =
                yandexWalletTransactionDao.query(YandexWalletTransactionStatus.IN_QUEUE, 1).get(0);
        yandexWalletTransactionDao.updateStatus(transaction1.getId(), transaction1.getStatus(),
                YandexWalletTransactionStatus.CANCELLED, null);

        yandexWalletTransactionReplicationProcessor.replication(Duration.ofMinutes(1));
        verify(yt.tables(), never()).insertRows(eq(Optional.empty()),
                Mockito.any(YPath.class),
                eq(true),
                eq(false),
                eq(true),
                eq(YTableEntryTypes.JACKSON_UTF8),
                any(Iterator.class),
                any(Compressor.class));
        verify(yt.tables(), times(1)).deleteRows(eq(Optional.empty()),
                Mockito.any(YPath.class),
                eq(true),
                eq(YTableEntryTypes.JACKSON_UTF8),
                any(Iterable.class),
                any(Compressor.class));
    }
}
