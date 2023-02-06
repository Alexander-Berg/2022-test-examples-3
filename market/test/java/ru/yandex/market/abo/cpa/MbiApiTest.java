package ru.yandex.market.abo.cpa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.message.Messages;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
public class MbiApiTest extends EmptyTest {
    private static long TEST_SHOP = 774L;

    @Autowired
    public MbiApiClient mbiApiClient;
    @Autowired
    public MbiApiService mbiApiService;

    @Test
    public void testGetCpaShops() {
        // cycle
        assertNotNull(mbiApiService.getCpaShop(TEST_SHOP));
    }

    @Test
    public void testNotification() {
        String xmlBody = "<abo-info>" +
                "   <order id=\"47592\" shop-order-id=\"85\"/>" +
                "   <order id=\"47540\" shop-order-id=\"84\"/>" +
                "</abo-info>";

        boolean result = mbiApiService.sendMessageToShop(TEST_SHOP, Messages.MBI.CPA_OLD_PPROCESSING, xmlBody);
        System.out.println("Result = " + result);
    }
}
