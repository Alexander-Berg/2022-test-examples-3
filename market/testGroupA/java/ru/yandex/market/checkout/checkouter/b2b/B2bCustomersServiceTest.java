package ru.yandex.market.checkout.checkouter.b2b;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.tvm.TvmAuthorization;
import ru.yandex.market.checkout.checkouter.tvm.TvmAuthorizationType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class B2bCustomersServiceTest extends AbstractServicesTestBase {

    @Value("${market.checkouter.axapta.tvm.client_id}")
    private Long axaptaTvmClientId;

    @Autowired
    private B2bCustomersSecurityService b2bCustomersSecurityService;

    @Test
    void billPaidAvailableForAXAPTA() {
        checkouterProperties.setEnableB2bStrictTvm(true);
        TvmAuthorization tvmAuthorization = new TvmAuthorization(TvmAuthorizationType.SERVICE, axaptaTvmClientId);
        assertTrue(b2bCustomersSecurityService.allowedToNotifyBillPaid(tvmAuthorization));
    }

    @Test
    void billPaidAvailableWithoutStrictTvm() {
        checkouterProperties.setEnableB2bStrictTvm(false);
        TvmAuthorization tvmAuthorization = new TvmAuthorization(TvmAuthorizationType.USER, 123L);
        assertTrue(b2bCustomersSecurityService.allowedToNotifyBillPaid(tvmAuthorization));
    }

    @Test
    void billPaidRestricted() {
        checkouterProperties.setEnableB2bStrictTvm(true);
        TvmAuthorization tvmAuthorization = new TvmAuthorization(TvmAuthorizationType.USER, 123L);
        assertFalse(b2bCustomersSecurityService.allowedToNotifyBillPaid(tvmAuthorization));
    }
}
