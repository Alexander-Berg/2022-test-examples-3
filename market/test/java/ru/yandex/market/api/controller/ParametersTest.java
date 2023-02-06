package ru.yandex.market.api.controller;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.user.order.MarketUid;

public class ParametersTest extends UnitTestBase {

    @Test
    public void checkValidHybridMuid() {
        // вызов системы
        String header = "1152921504640186026:UR0hJwfCScv1ikWQe89OcBJBWoHT3V";
        MarketUid muid = Parameters.MarketUidParser.extractHybridMuid(header);
        // проверка утверждений
        Assert.assertNotNull(muid);
        Assert.assertEquals(Long.valueOf(1152921504640186026l), muid.getMuid());
        Assert.assertEquals("1152921504640186026:UR0hJwfCScv1ikWQe89OcBJBWoHT3V", muid.getSignature());
    }

    @Test
    public void checkHybridMuidWithoutSignature() {
        // вызов системы
        String header = "1152921504640186026";
        MarketUid muid = Parameters.MarketUidParser.extractHybridMuid(header);
        // проверка утверждений
        Assert.assertNull(muid);
    }

    @Test
    public void checkHybridMuidWithoutWrongValue() {
        // вызов системы
        String header = "a152921504640186026:UR0hJwfCScv1ikWQe89OcBJBWoHT3V";
        MarketUid muid = Parameters.MarketUidParser.extractHybridMuid(header);
        // проверка утверждений
        Assert.assertNull(muid);
    }

}
