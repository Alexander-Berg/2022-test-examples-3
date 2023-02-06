package ru.yandex.market.wms.api.service.referenceitems.push.utils;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.wms.api.service.referenceitems.utils.RemainingLifetimesUtil;

public class RemainingLifetimesUtilTest {

    @Test
    void checkSimpleRemainingLifetimes() {
        RemainingLifetimes result =
                RemainingLifetimesUtil.calc(720, 60, 60, 10);

        RemainingLifetimes expected = new RemainingLifetimes(
                new ShelfLives(new ShelfLife(720), new ShelfLife(60)),
                new ShelfLives(new ShelfLife(60), new ShelfLife(10))
        );

        Assert.assertEquals(result, expected);
    }

}
