package ru.yandex.market.abo.core.tel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author artemmz on 11.09.15.
 */

public class CallToOrderBinderTest extends EmptyTest {

    @Autowired
    CallToOrderBinder callToOrderBinder;

    @Test
    public void testBindCalls() throws Exception {
        callToOrderBinder.bindCalls();
    }
}
