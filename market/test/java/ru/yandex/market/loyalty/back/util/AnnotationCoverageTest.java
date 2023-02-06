package ru.yandex.market.loyalty.back.util;

import org.junit.Test;

import ru.yandex.market.loyalty.core.utils.CommonTestUtils;

public class AnnotationCoverageTest {
    @Test
    public void checkAllSchedulersHasOurType() {
        CommonTestUtils.checkAllExecutorServicesIsWrapped("ru.yandex.market.loyalty.back");
    }

}
