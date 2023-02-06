package ru.yandex.market.bidding.engine;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@ExtendWith(MockitoExtension.class)
class LoadShopActionTest {

    private static final long SHOP_ID_774 = 774;
    private static final int WAIT_TO_S = 0;

    @Mock
    private BasicPartner basicPartnerMock;

    @Mock
    private BasicBiddingEngine basicBiddingEngineMock;

    /**
     * Проверка того, что если на момент вызова {@link LoadShopAction#onCancel()}, магазин уже прогружен,
     * то не теряем эту информацию.
     */
    @DisplayName("LoadResult=DONE after PREPARE->CANCEL for shop already in cache")
    @Test
    void test_cancelWhenAlreadyExists() {
        Mockito.when(basicBiddingEngineMock.partner(SHOP_ID_774))
                .thenReturn(basicPartnerMock);

        final Lock lock = new ReentrantLock();
        lock.lock();
        try {
            final LoadShopAction loadShopAction = new LoadShopAction(SHOP_ID_774, basicBiddingEngineMock, lock);

            final Action.Outcome outcome = loadShopAction.prepare();
            assertThat(outcome, is(Action.Outcome.SKIP));

            final LoadPartnerResult resultAfterPrepare = loadShopAction.result(WAIT_TO_S);
            assertThat(resultAfterPrepare, is(LoadPartnerResult.DONE));

            loadShopAction.onCancel();
            final LoadPartnerResult resultAfterCancel = loadShopAction.result(WAIT_TO_S);
            assertThat(resultAfterCancel, is(LoadPartnerResult.DONE));
        } finally {
            lock.unlock();
        }
    }

    @DisplayName("LoadResult=RETRY after PREPARE->CANCEL for shop not in cache")
    @Test
    void test_cancelWhenNotExist() {
        Mockito.when(basicBiddingEngineMock.partner(SHOP_ID_774))
                .thenReturn(null);

        final Lock lock = new ReentrantLock();
        lock.lock();
        try {
            final LoadShopAction loadShopAction = new LoadShopAction(SHOP_ID_774, basicBiddingEngineMock, lock);

            final Action.Outcome outcome = loadShopAction.prepare();
            assertThat(outcome, is(Action.Outcome.SLOW));

            final LoadPartnerResult resultAfterPrepare = loadShopAction.result(WAIT_TO_S);
            assertThat(resultAfterPrepare, is(LoadPartnerResult.RETRY));

            loadShopAction.onCancel();
            final LoadPartnerResult resultAfterCancel = loadShopAction.result(WAIT_TO_S);
            assertThat(resultAfterCancel, is(LoadPartnerResult.RETRY));
        } finally {
            lock.unlock();
        }
    }

}