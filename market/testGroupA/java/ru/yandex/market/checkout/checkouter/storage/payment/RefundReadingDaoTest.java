package ru.yandex.market.checkout.checkouter.storage.payment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.pay.RefundSearchRequest;

/**
 * @author kukabara
 */
public class RefundReadingDaoTest extends AbstractServicesTestBase {

    @Autowired
    private RefundReadingDao refundReadingDao;

    @Test
    public void testLoadRefunds() {
        Assertions.assertTrue(refundReadingDao.loadRefunds(1L).isEmpty());
    }

    @Test
    public void testCountRefundsForMissingPayment() {
        RefundSearchRequest refundSearchRequest = new RefundSearchRequest();
        refundSearchRequest.setOrderId(-1L);
        Assertions.assertEquals(0, refundReadingDao.countRefunds(refundSearchRequest));
    }
}
