package ru.yandex.market.wms.api.service.referenceitems.push.utils;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.api.service.referenceitems.utils.BoxCountUtil;

public class BoxCountUtilTest {
    @Test
    void checkDefault() {
        Integer result = BoxCountUtil.calc(0);
        Integer expected = 1;
        Assert.assertEquals(result, expected);
    }

    @Test
    void checkCommon() {
        Integer result = BoxCountUtil.calc(2);
        Integer expected = 2;
        Assert.assertEquals(result, expected);
    }
}
