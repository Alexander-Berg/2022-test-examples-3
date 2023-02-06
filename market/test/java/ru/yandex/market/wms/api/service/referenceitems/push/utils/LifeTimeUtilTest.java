package ru.yandex.market.wms.api.service.referenceitems.push.utils;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.api.service.referenceitems.utils.LifeTimeUtil;
import ru.yandex.market.wms.common.lgw.dto.LifetimeIndicator;

public class LifeTimeUtilTest {
    @Test
    void shouldNull() {
        Integer result = LifeTimeUtil.calc(LifetimeIndicator.IGNORE_LIFETIME.getValue(), 0);
        Integer expected = null;
        Assert.assertEquals(result, expected);
    }

    @Test
    void shouldToExpireDays() {
        Integer result = LifeTimeUtil.calc(LifetimeIndicator.TRACK_LIFETIME.getValue(), 10);
        Integer expected = 10;
        Assert.assertEquals(result, expected);
    }
}
