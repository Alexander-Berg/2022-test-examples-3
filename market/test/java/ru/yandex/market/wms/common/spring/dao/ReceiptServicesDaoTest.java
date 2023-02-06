package ru.yandex.market.wms.common.spring.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptServicesDao;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptServicesDaoTest extends ReceivingIntegrationTest {

    public static final String RECEIPT_KEY_2 = "KEY2";
    public static final String RECEIPT_KEY_1 = "KEY1";
    public static final String SERVICE_CODE = "HIGH_PRIORITY";
    public static final String ANOTHER_SERVICE_CODE = "RANDOM_CODE";

    public static final String USER = "USER";
    @Autowired
    private ReceiptServicesDao receiptServicesDao;

    @Test
    @DatabaseSetup("/dao/receipt-services/before.xml")
    @ExpectedDatabase(value = "/dao/receipt-services/after.xml", assertionMode = NON_STRICT)
    void toggleReceiptServicesCoeff() {
        assertTrue(receiptServicesDao.checkReceiptServiceActivity(RECEIPT_KEY_1, SERVICE_CODE));
        receiptServicesDao.toggleReceiptServicesActivity(RECEIPT_KEY_1, SERVICE_CODE, false, USER);
        assertFalse(receiptServicesDao.checkReceiptServiceActivity(RECEIPT_KEY_1, SERVICE_CODE));
        receiptServicesDao.toggleReceiptServicesActivity(RECEIPT_KEY_1, SERVICE_CODE, true, USER);
        receiptServicesDao.toggleReceiptServicesActivity(RECEIPT_KEY_2, ANOTHER_SERVICE_CODE, true, USER);
    }
}
