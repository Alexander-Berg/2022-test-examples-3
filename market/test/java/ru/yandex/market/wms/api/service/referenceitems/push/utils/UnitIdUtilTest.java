package ru.yandex.market.wms.api.service.referenceitems.push.utils;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.api.service.referenceitems.utils.UnitIdUtil;

public class UnitIdUtilTest {
    @Test
    void checkSimpleUnitId() {
        UnitId result =
                UnitIdUtil.calc("e75019", "649164");

        UnitId expected = new UnitId("e75019", 649164L, "e75019");

        Assert.assertEquals(result, expected);
    }
}
