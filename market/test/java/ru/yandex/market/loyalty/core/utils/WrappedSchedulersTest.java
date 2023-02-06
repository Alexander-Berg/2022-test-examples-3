package ru.yandex.market.loyalty.core.utils;

import org.junit.Test;

import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.checkAllExecutorServicesIsWrapped;

public class WrappedSchedulersTest {
    @Test
    public void checkAllSchedulersHasOurType() {
        checkAllExecutorServicesIsWrapped("ru.yandex.market.loyalty.core");
    }
}
