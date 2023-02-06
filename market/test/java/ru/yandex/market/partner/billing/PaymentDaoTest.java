package ru.yandex.market.partner.billing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.billing.dto.PersonPaymentMethod;
import ru.yandex.market.partner.test.context.FunctionalTest;

class PaymentDaoTest extends FunctionalTest {

    private static final long CLIENT_ID = 111L;
    private static final long NON_EXISTENT_CLIENT_ID = -1L;
    private static final long PAYSYS_ID = 1001L;
    private static final long PERSON_ID = 1613L;

    @Autowired
    private PaymentDao paymentDao;

    @DbUnitDataSet(before = "csv/testGetLastPaymentMethod.before.csv")
    @Test
    void testGetLastPaymentMethod() {
        var result = paymentDao.getLastPaymentMethodByClient(CLIENT_ID);
        Assertions.assertEquals(PersonPaymentMethod.of(PAYSYS_ID, PERSON_ID), result);
    }

    @DbUnitDataSet(before = "csv/testGetLastPaymentMethod.before.csv")
    @Test
    void testGetLastPaymentMethodByNonExistentClient() {
        Assertions.assertNull(paymentDao.getLastPaymentMethodByClient(NON_EXISTENT_CLIENT_ID));
    }

    @DbUnitDataSet(
            before = "csv/testSaveExistingLastPaymentMethod.before.csv",
            after = "csv/testSaveExistingLastPaymentMethod.after.csv"
    )
    @Test
    void testSaveExistingLastPaymentMethod() {
        paymentDao.saveLastPaymentMethodForClient(CLIENT_ID, PersonPaymentMethod.of(PAYSYS_ID, PERSON_ID));
    }

    @DbUnitDataSet(
            before = "csv/testSaveNonExistingLastPaymentMethod.before.csv",
            after = "csv/testSaveNonExistingLastPaymentMethod.after.csv"
    )
    @Test
    void testSaveNewLastPaymentMethod() {
        paymentDao.saveLastPaymentMethodForClient(CLIENT_ID, PersonPaymentMethod.of(PAYSYS_ID, PERSON_ID));
    }
}
