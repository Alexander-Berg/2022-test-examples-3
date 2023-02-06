package ru.yandex.market.abo.core.quality_monitoring.dsbs;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.shop.ShopFlagService;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.cpa.order.delivery.ShopOrderDelivery;
import ru.yandex.market.abo.cpa.order.delivery.ShopOrderDeliveryRepo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.quality_monitoring.dsbs.DsbsDeliveredStatusMonitoring.MISSING_DELIVERED;

/**
 * @author artemmz
 * @date 21/09/2020.
 */
class DsbsDeliveredStatusMonitoringTest {
    private static final long SHOP_ID = 42342342L;

    @InjectMocks
    DsbsDeliveredStatusMonitoring dsbsDeliveredStatusMonitoring;

    @Mock
    private ShopOrderDeliveryRepo shopOrderDeliveryRepo;
    @Mock
    private ShopFlagService flagService;
    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private ShopOrderDelivery shopOrderDelivery;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private StartrekTicketManager startrekTicketManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(shopOrderDeliveryRepo.loadOrdersWithEmptyDelivered(anyInt())).thenReturn(List.of(shopOrderDelivery));
        when(shopOrderDelivery.getShopId()).thenReturn(SHOP_ID);
        when(shopOrderDelivery.getEstimatedDeliveryDate()).thenReturn(LocalDateTime.now());
        when(flagService.shopFlagExists(eq(MISSING_DELIVERED), eq(SHOP_ID), any(LocalDateTime.class))).thenReturn(false);
        when(shopInfoService.getShopInfo(SHOP_ID)).thenReturn(new ShopInfo());
    }

    @Test
    void sendMessage() {
        dsbsDeliveredStatusMonitoring.monitor();
        verify(mbiApiService).sendMessageToShop(eq(SHOP_ID), anyInt(), any());
        verify(flagService).addShopFlag(MISSING_DELIVERED, SHOP_ID);
    }

    @Test
    void antiSpam() {
        when(flagService.shopFlagExists(eq(MISSING_DELIVERED), eq(SHOP_ID), any(LocalDateTime.class))).thenReturn(true);
        dsbsDeliveredStatusMonitoring.monitor();
        verifyNoMoreInteractions(mbiApiService);
        verify(flagService, never()).addShopFlag(MISSING_DELIVERED, SHOP_ID);
    }
}
