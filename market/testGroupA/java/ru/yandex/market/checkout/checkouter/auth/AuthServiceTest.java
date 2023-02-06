package ru.yandex.market.checkout.checkouter.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;

public class AuthServiceTest extends AbstractServicesTestBase {

    @Autowired
    private AuthService authService;

    @Test
    public void muidInContext() {
        var dbMuid = getMuid();
        var attributes = new CheckoutContextHolder.CheckoutAttributesHolder().getAttributes();
        Assertions.assertEquals(dbMuid + "", attributes.get("muid"));
    }

    private long getMuid() {
        AuthInfo authInfo = authService.auth(
                null,
                new UserInfo("127.0.0.1", "java 1.8.0"),
                HitRateGroup.UNLIMIT, true);
        Long muid = authInfo.getMuid();
        Assertions.assertNotNull(muid);
        return muid;
    }
}
