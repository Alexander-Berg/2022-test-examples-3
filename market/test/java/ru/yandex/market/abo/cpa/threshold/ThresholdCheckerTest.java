package ru.yandex.market.abo.cpa.threshold;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.abo.api.entity.forecast.ShopForecast;
import ru.yandex.market.abo.core.calendar.WorkHour;
import ru.yandex.market.abo.core.cutoff.CutoffManager;
import ru.yandex.market.abo.core.exception.ExceptionalShopReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.forecast.ShopForecastManager;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketManager;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.core.abo.AboCutoff;
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffRequest;
import ru.yandex.market.mbi.api.client.entity.shops.ProgramState;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.cpa.threshold.ThresholdChecker.FIRST_TIME_THRESHOLD_CUTOFF_PERIOD_IN_DAYS;
import static ru.yandex.market.abo.cpa.threshold.ThresholdChecker.SECOND_TIME_THRESHOLD_CUTOFF_PERIOD_IN_DAYS;

/**
 * @author artemmz
 * @date 02.08.17.
 */
public class ThresholdCheckerTest {
    private static final long SHOP_ID = 774L;
    private static final int FORECAST_CNT = 1;

    @InjectMocks
    private ThresholdChecker thresholdChecker;
    @Mock
    private CutoffManager cutoffManager;
    @Mock
    private ShopForecastManager shopForecastManager;
    @Mock
    private ExceptionalShopsService exceptionalShopsService;
    @Mock
    private ShopForecast shopForecast;
    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private Shop shop;
    @Mock
    private WorkHour workHour;
    @Mock
    private RecheckTicketManager recheckTicketManager;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private TransactionTemplate pgTransactionTemplate;
    @Mock
    private ExecutorService pool;
    @Captor
    private ArgumentCaptor<OpenAboCutoffRequest> cutoffRequestCaptor;

    private final Set<Long> exceptionalShops = new HashSet<>();
    private final Set<Long> shopsWithOpenCutoff = new HashSet<>();
    private final ThreadLocal<Boolean> shopHadRecentCutoffForLast4Month = new ThreadLocal<>();
    private final ThreadLocal<Integer> cutoffCountFor6MonthWithReset = new ThreadLocal<>();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        Stream.of(exceptionalShops, shopsWithOpenCutoff).forEach(Collection::clear);
        shopHadRecentCutoffForLast4Month.set(false);
        cutoffCountFor6MonthWithReset.set(0);

        when(exceptionalShopsService.loadShops(any(ExceptionalShopReason.class))).thenReturn(exceptionalShops);
        when(cutoffManager.shopsWithActualAboCutoff()).thenReturn(shopsWithOpenCutoff);
        when(cutoffManager.shopHadRecentCutoffForLast4Month(SHOP_ID))
                .thenAnswer(invocation -> shopHadRecentCutoffForLast4Month.get());
        when(cutoffManager.cutoffCountFor6MonthWithReset(SHOP_ID))
                .thenAnswer(invocation -> cutoffCountFor6MonthWithReset.get());
        when(shopForecastManager.getForecastNonCached(anyLong(), any())).thenReturn(shopForecast);
        when(shopForecast.getForecastCount()).thenReturn(FORECAST_CNT);
        when(shopForecast.getErrorCount()).thenReturn(FORECAST_CNT + 1);
        when(shopForecast.needSwitchOff()).thenReturn(true);
        when(mbiApiService.getShop(SHOP_ID)).thenReturn(shop);
        when(shopInfoService.getShopInfo(SHOP_ID)).thenReturn(createShopInfo());
        when(shop.getCpc()).thenReturn(ProgramState.ON);
        when(shop.getId()).thenReturn(SHOP_ID);

        when(pgTransactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback) invocation.getArguments()[0]).doInTransaction(new SimpleTransactionStatus()));
        TestHelper.mockExecutorService(pool);
    }

    @Test
    public void switchOffShopFirstTime() {
        switchOffShop(FIRST_TIME_THRESHOLD_CUTOFF_PERIOD_IN_DAYS);
    }

    @Test
    public void switchOffShopSecondTime() {
        shopHadRecentCutoffForLast4Month.set(true);
        cutoffCountFor6MonthWithReset.set(1);
        switchOffShop(SECOND_TIME_THRESHOLD_CUTOFF_PERIOD_IN_DAYS);
    }

    private void switchOffShop(int forDays) {
        thresholdChecker.checkForErrorThreshold(SHOP_ID);
        verify(recheckTicketManager).addTicketIfNotExists(eq(SHOP_ID), eq(RecheckTicketType.CUTOFF_APPROVE), any());

        thresholdChecker.switchOffShop(SHOP_ID, Collections.emptyList());
        verify(cutoffManager).openAboCutoff(eq(SHOP_ID), cutoffRequestCaptor.capture(), any());
        verify(workHour).add(any(), eq((int) TimeUnit.DAYS.toHours(forDays)));

        OpenAboCutoffRequest cutoffRequest = cutoffRequestCaptor.getValue();
        assertEquals(AboCutoff.CPC_QUALITY, cutoffRequest.getCutoffType());
    }

    @Test
    public void doNotSwitchOff() {
        when(shopForecast.needSwitchOff()).thenReturn(false);
        thresholdChecker.checkForErrorThreshold(SHOP_ID);
        verify(recheckTicketManager, never()).addTicketIfNotExists(eq(SHOP_ID), any(), any());
    }

    @Test
    public void exceptionalShop() {
        exceptionalShops.add(SHOP_ID);
        thresholdChecker.checkForErrorThreshold(SHOP_ID);
        verify(recheckTicketManager, never()).addTicketIfNotExists(eq(SHOP_ID), any(), any());
    }

    @Test
    public void cutoffAlreadyOpened() {
        shopsWithOpenCutoff.add(SHOP_ID);
        when(shop.getCpc()).thenReturn(ProgramState.OFF);
        thresholdChecker.checkForErrorThreshold(SHOP_ID);
        verify(recheckTicketManager, never()).addTicketIfNotExists(eq(SHOP_ID), any(), any());
        verifyNoMoreInteractions(mbiApiService);
    }

    @Test
    public void switchedOffInMbi() {
        when(shop.getCpc()).thenReturn(ProgramState.OFF);
        thresholdChecker.checkForErrorThreshold(SHOP_ID);
        verify(recheckTicketManager, never()).addTicketIfNotExists(eq(SHOP_ID), any(), any());
    }

    private static ShopInfo createShopInfo() {
        ShopInfo shopInfo = new ShopInfo();
        shopInfo.setName("");
        return shopInfo;
    }
}
