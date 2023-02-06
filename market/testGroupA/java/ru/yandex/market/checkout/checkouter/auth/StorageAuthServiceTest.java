package ru.yandex.market.checkout.checkouter.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.MUID;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.SBER_ID;

public class StorageAuthServiceTest extends AbstractWebTestBase {

    @Autowired
    private AuthService authService;

    @Test
    public void withMuid() {
        assertTrue(authService.isNoAuth(MUID), String.valueOf(MUID));
        assertTrue(authService.isNotPassportUid(MUID), String.valueOf(MUID));

        final long muid2 = (1L << 60) | 1;
        assertTrue(authService.isNoAuth(muid2), String.valueOf(muid2));
        assertTrue(authService.isNotPassportUid(muid2), String.valueOf(muid2));
    }

    @Test
    public void withSberId() {
        final long sberId1 = 2190550858753437194L;
        assertFalse(authService.isNoAuth(sberId1), String.valueOf(sberId1));
        assertTrue(authService.isNotPassportUid(sberId1), String.valueOf(sberId1));

        assertFalse(authService.isNoAuth(SBER_ID), String.valueOf(SBER_ID));
        assertTrue(authService.isNotPassportUid(SBER_ID), String.valueOf(SBER_ID));
    }

    @Test
    public void withPassportUid() {
        final long uid1 = 1L;
        assertFalse(authService.isNoAuth(uid1), String.valueOf(uid1));
        assertFalse(authService.isNotPassportUid(uid1), String.valueOf(uid1));

        final long uid2 = (1L << 61);
        assertFalse(authService.isNoAuth(uid2), String.valueOf(uid2));
        assertFalse(authService.isNotPassportUid(uid2), String.valueOf(uid2));
    }

    @Test
    public void getValidUid() {
        assertNull(authService.getPassportUid(null));

        final Order order = new Order();
        assertNull(authService.getPassportUid(order));

        order.setUid(MUID);
        assertNull(authService.getPassportUid(order));

        order.setUid(SBER_ID);
        assertNull(authService.getPassportUid(order));

        order.setUid(1L);
        assertEquals(1L, authService.getPassportUid(order).longValue());

        order.setNoAuth(true);
        assertNull(authService.getPassportUid(order));
    }
}
