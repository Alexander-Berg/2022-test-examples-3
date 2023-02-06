package ru.yandex.market.checkout.checkouter.storage.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.auth.AuthService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.storage.util.PaymentUidCheckUtil.checkClient;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.MUID;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.SBER_ID;

public class PaymentUidCheckUtilTest extends AbstractServicesTestBase {

    private static final long PASSPORT_UID = 1L;

    @Autowired
    private AuthService authService;

    @Test
    public void checkClientForUserRole() {
        assertTrue(checkClient(authService, PASSPORT_UID, new ClientInfo(ClientRole.USER, PASSPORT_UID)));
        assertFalse(checkClient(authService, 2L, new ClientInfo(ClientRole.USER, PASSPORT_UID)));

        // Для muid user_id на платеже не заполняется
        assertTrue(checkClient(authService, null, new ClientInfo(ClientRole.USER, MUID)));
        assertFalse(checkClient(authService, MUID, new ClientInfo(ClientRole.USER, MUID)));

        // Для СберИд user_id на платеже не заполняется
        assertTrue(checkClient(authService, null, new ClientInfo(ClientRole.USER, SBER_ID)));
        assertFalse(checkClient(authService, SBER_ID, new ClientInfo(ClientRole.USER, SBER_ID)));
    }
}
