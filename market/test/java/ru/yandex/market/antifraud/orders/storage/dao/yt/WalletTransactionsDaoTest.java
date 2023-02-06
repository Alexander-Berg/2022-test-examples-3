package ru.yandex.market.antifraud.orders.storage.dao.yt;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import ru.yandex.market.antifraud.orders.config.YtTablePaths;
import ru.yandex.market.antifraud.orders.test.utils.AntifraudTestUtils;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WalletTransactionsDaoTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    YtClient ytAntifraudClient;

    @Mock
    YtTablePaths tablePaths;

    @InjectMocks
    WalletTransactionsDao walletTransactionsDao;

    @Before
    public void init() {
        when(ytAntifraudClient.waitProxies())
            .thenReturn(CompletableFuture.completedFuture(null));
        when(tablePaths.getWalletTransactions())
            .thenReturn("//home/market/production/market-promo/antifraud/yandex_wallet_transactions");
    }

    @Test
    public void readTransactionsWithPromoKeys() {
        when(ytAntifraudClient.selectRows(any(SelectRowsRequest.class), any()))
            .thenReturn(CompletableFuture.completedFuture(List.of()));

        walletTransactionsDao.readTransactions(List.of(123L, 124L), List.of("promo_1", "promo_2"))
            .join();

        verify(ytAntifraudClient)
            .selectRows(AntifraudTestUtils.ytQuery(
                " * FROM " +
                    "[//home/market/production/market-promo/antifraud/yandex_wallet_transactions] " +
                    "WHERE uid IN (123, 124) AND promo_key IN ('promo_1', 'promo_2')"
            ), any());
    }

    @Test
    public void readTransactionsWithoutPromoKeys() {
        when(ytAntifraudClient.selectRows(any(SelectRowsRequest.class), any()))
            .thenReturn(CompletableFuture.completedFuture(List.of()));

        walletTransactionsDao.readTransactions(List.of(123L, 124L), List.of())
            .join();

        verify(ytAntifraudClient)
            .selectRows(AntifraudTestUtils.ytQuery(
                " * FROM " +
                    "[//home/market/production/market-promo/antifraud/yandex_wallet_transactions] " +
                    "WHERE uid IN (123, 124)"
            ), any());
    }
}