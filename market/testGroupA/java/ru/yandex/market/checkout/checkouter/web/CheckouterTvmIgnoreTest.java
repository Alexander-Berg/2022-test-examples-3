package ru.yandex.market.checkout.checkouter.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.common.util.TvmIgnoreUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author kl1san
 */
public class CheckouterTvmIgnoreTest extends AbstractServicesTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
            "/ping",
            "/properties",
            "/zoo-task-config/task/actual",
            "/features"
    })
    public void checkIgnoredTvmPatterns(String pattern) {
        assertTrue(TvmIgnoreUtil.shouldIgnore(pattern));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/should/miss/properties",
            "/checkout?rgb=BLUE&minifyOutlets=true&allowPrepaidForNoAuth=true&uid=",
            "/checkout",
            "/orders/by-uid/123",
            "/orders/by-uid/123/",
            "/properties/myPrettyProperty",
            "/zoo-task-config/ta/sk/actual",
            "/features/muidThroughDb"
    })
    public void checkNotIgnoredTvmPatterns(String pattern) {
        assertFalse(TvmIgnoreUtil.shouldIgnore(pattern));
    }
}
