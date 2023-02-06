package ru.yandex.market.abo.cpa.pushapi.stat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.cpa.cart_diff.CartDiffService;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 09/10/2019.
 */
public class PushApiStatServiceTest {
    private static final LocalDate MAX_STAT_DATE = LocalDate.now().minusDays(1);
    private static final LocalDate MIN_STAT_DATE = MAX_STAT_DATE.minusDays(10);
    private static final long SHOP_ID = 1;
    private static final long CART_CNT_1 = 2;
    private static final long CART_CNT_2 = 4;
    private static final Integer DIFF_CNT = 3;


    @InjectMocks
    PushApiStatService pushApiStatService;
    @Mock
    PushApiLogStatRepo pushApiLogStatRepo;
    @Mock
    CartDiffService cartDiffService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(pushApiLogStatRepo.findByShopWithDateStatDateBetween(any(), any())).thenReturn(List.of(
                initStat(MIN_STAT_DATE, SHOP_ID, CART_CNT_1, 0),
                initStat(MAX_STAT_DATE, SHOP_ID, CART_CNT_2, 0)
        ));
        when(cartDiffService.getShopStat(eq(Set.of(SHOP_ID)), any(), any(), any())).thenReturn(Map.of(
                SHOP_ID, DIFF_CNT
        ));
    }

    @Test
    void testShopDiffStats() {
        var stat = pushApiStatService.shopDiffStats(MIN_STAT_DATE.minusDays(1), MAX_STAT_DATE.plusDays(1));
        verify(cartDiffService).getShopStat(Set.of(SHOP_ID), CartDiffType.ITEM_PRICE, MIN_STAT_DATE, MAX_STAT_DATE);
        verify(cartDiffService).getShopStat(Set.of(SHOP_ID), CartDiffType.ITEM_COUNT, MIN_STAT_DATE, MAX_STAT_DATE);
        assertEquals((double) DIFF_CNT / (CART_CNT_1 + CART_CNT_2), stat.get(0).getPriceDiffRate());
        assertEquals((double) DIFF_CNT / (CART_CNT_1 + CART_CNT_2), stat.get(0).getStockDiffRate());

        LocalDate from = MIN_STAT_DATE.plusDays(1);
        LocalDate to = MAX_STAT_DATE.minusDays(2);
        pushApiStatService.shopDiffStats(from, to);

        verify(cartDiffService).getShopStat(Set.of(SHOP_ID), CartDiffType.ITEM_PRICE, from, to);
        verify(cartDiffService).getShopStat(Set.of(SHOP_ID), CartDiffType.ITEM_COUNT, from, to);
    }

    public static PushApiLogStat initStat(LocalDate statDate,
                                          long shopId,
                                          long cartCnt,
                                          long acceptCnt) {
        PushApiLogStat stat = new PushApiLogStat();
        PushApiLogStatKey key = new PushApiLogStatKey(statDate, shopId);

        stat.setShopWithDate(key);
        stat.setCartCnt(cartCnt);
        stat.setAcceptCnt(acceptCnt);
        stat.setTotalCnt(cartCnt + acceptCnt);
        stat.setTotalCntSuccess(0L);
        stat.setCartCntSuccess(0L);
        stat.setAcceptCntSuccess(0L);
        stat.setStatusCnt(0L);
        stat.setStatusCntSuccess(0L);
        stat.setStocksCnt(0L);
        stat.setStocksCntSuccess(0L);
        return stat;
    }
}
