package ru.yandex.market.loyalty.api;

import org.junit.Test;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;

import java.io.IOException;

import static ru.yandex.market.loyalty.test.ToStringChecker.checkToStringInSameModule;
import static ru.yandex.market.loyalty.test.ToStringChecker.excludeByClasses;

public class CheckToStringTest {
    @Test
    public void checkToStringInApiModule() throws IOException {
        checkToStringInSameModule(
            excludeByClasses(MarketLoyaltyException.class)
        );
    }
}
