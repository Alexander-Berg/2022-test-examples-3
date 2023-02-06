package ru.yandex.market.billing.controller.auction;

import java.util.List;

import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.auction.BidId;
import ru.yandex.market.auction.SyncBidComponentService;

/**
 * Тесты для {@link SyncBidsController}.
 * todo переделать на честные функциональные
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class SyncBidsControllerTest {
    private static final long SOME_FEED = 1001L;
    private static final long SHOP_774 = 774L;
    private static final String SOME_OFFER_1 = "some_offer_id1";
    private static final String SOME_OFFER_2 = "some_offer_id2";
    private static final List<BidId> SOME_BIDS = ImmutableList.of(
            new BidId(SHOP_774, SOME_FEED, SOME_OFFER_1),
            new BidId(SHOP_774, SOME_FEED, SOME_OFFER_2)
    );

    private static final BidSyncRequestDto REQUEST_DTO = new BidSyncRequestDto(
            SOME_BIDS
    );

    private static final ShopListRequestDto SHOP_LIST_REQUEST_DTO = new ShopListRequestDto(
            ImmutableList.of(1L, 2L)
    );

    private SyncBidsController controller;
    @Mock
    private SyncBidComponentService syncBidComponentService;

    @BeforeEach
    void beforeEach() {
        controller = new SyncBidsController(syncBidComponentService);
        Mockito.reset(syncBidComponentService);
    }

    @DisplayName("Синхронизация карточных оферов")
    @Test
    void test_syncCardBids() {
        controller.syncCardBids(REQUEST_DTO);
        Mockito.verify(syncBidComponentService).
                syncBasedOnCardBids(
                        Mockito.argThat(arg -> arg.equals(SOME_BIDS)),
                        Mockito.any()
                );

        Mockito.verifyNoMoreInteractions(syncBidComponentService);
    }

    @DisplayName("Синхронизация не карточных оферов")
    @Test
    void test_syncSearchBids() {
        controller.syncSearchBids(REQUEST_DTO);
        Mockito.verify(syncBidComponentService).
                syncBasedOnSearchBids(
                        Mockito.argThat(arg -> arg.equals(SOME_BIDS)),
                        Mockito.any()
                );

        Mockito.verifyNoMoreInteractions(syncBidComponentService);
    }

    @DisplayName("Синхронизация оферов неизвестного типа")
    @Test
    void test_syncUnknownBids() {
        controller.syncUnknownBids(REQUEST_DTO);
        Mockito.verify(syncBidComponentService).
                syncBasedOnMinBids(
                        Mockito.argThat(arg -> arg.equals(SOME_BIDS)),
                        Mockito.any()
                );

        Mockito.verifyNoMoreInteractions(syncBidComponentService);
    }

    @DisplayName("Сброс fee")
    @Test
    void test_resetFeeBids() {
        controller.resetFeeBids(SHOP_LIST_REQUEST_DTO);
        Mockito.verify(syncBidComponentService).
                resetShopFeeBids(
                        Mockito.argThat(arg -> arg.equals(ImmutableList.of(1L, 2L))),
                        Mockito.any()
                );

        Mockito.verifyNoMoreInteractions(syncBidComponentService);
    }
}