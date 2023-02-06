package ru.yandex.market.abo.core.inbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.shop.ShopInfoService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author imelnikov
 * @date 29.05.17
 */
public class InboxFilterServiceTest extends EmptyTest {

    @Autowired
    private InboxFilterService externalServices;

    @Test
    public void workingTime() {
        ShopInfoService shopInfoService = mock(ShopInfoService.class);
        when(shopInfoService.getShopOwnRegion(anyLong())).thenReturn(65L); // Новосибирск
        externalServices.setShopInfoService(shopInfoService);

        assertTrue(externalServices.isWorkingTime(1, DateUtil.convertToDate("2017-10-27 06:00:00").toInstant()));
        assertFalse(externalServices.isWorkingTime(1, DateUtil.convertToDate("2017-10-27 05:59:59").toInstant()));
    }

}
