package ru.yandex.market.abo.core.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.test.MockFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
public class CommonShopInfoServiceTest {
    private static final long SHOP_ID = 774L;
    private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException();
    @InjectMocks
    private CommonShopInfoService commonShopInfoService;

    @Mock
    private MbiApiService mbiApiService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testMbiNull() {
        when(mbiApiService.getCpaShop(anyLong())).thenReturn(null);
        when(mbiApiService.getShop(anyLong())).thenReturn(null);
        when(mbiApiService.getShopParams(anyLong())).thenReturn(null);
        when(mbiApiService.getShopFeatures(anyLong())).thenReturn(null);

        CommonShopInfo cachedShop = commonShopInfoService.getCachedShop(SHOP_ID);
        assertNull(cachedShop);
    }

    @Test
    public void testMbiFail() {
        when(mbiApiService.getCpaShop(anyLong())).thenThrow(RUNTIME_EXCEPTION);
        when(mbiApiService.getShop(anyLong())).thenThrow(RUNTIME_EXCEPTION);
        when(mbiApiService.getShopParams(anyLong())).thenThrow(RUNTIME_EXCEPTION);
        when(mbiApiService.getShopFeatures(anyLong())).thenThrow(RUNTIME_EXCEPTION);

        CommonShopInfo cachedShop = commonShopInfoService.getCachedShop(SHOP_ID);
        assertNull(cachedShop);
    }

    @Test
    public void testMbiOk() {
        doAnswer(invocation -> {
            Long shopId = (Long) invocation.getArguments()[0];
            return MockFactory.getCpaShopInfoMock(shopId);
        }).when(mbiApiService).getCpaShop(anyLong());

        doAnswer(invocation -> {
            Long shopId = (Long) invocation.getArguments()[0];
            return MockFactory.getShop(shopId);
        }).when(mbiApiService).getShop(anyLong());

        CommonShopInfo cachedShop = commonShopInfoService.getCachedShop(SHOP_ID);
        assertNotNull(cachedShop);
    }
}

