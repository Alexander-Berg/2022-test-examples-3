package ru.yandex.market.abo.core.premod.helper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author mixey
 *         @date 26.11.2008
 *         Time: 18:38:56
 */
public class OrderItemsHelperTest extends EmptyTest {

    @Autowired
    private OrderItemsHelper orderItemsHelper;

    @Test
    public void test() {
        orderItemsHelper.createOffers();
    }
}
