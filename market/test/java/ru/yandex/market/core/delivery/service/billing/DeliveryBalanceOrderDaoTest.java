package ru.yandex.market.core.delivery.service.billing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DeliveryBalanceOrderDaoTest extends FunctionalTest {

    private static final String BIC_STR_9_CHARS = "bic-9-dig";
    private static final long SELLER_CLIENT_ID = 707L;
    private static final long PERSON_ID = 404L;
    private static final String ACCOUNT_NUM_STR_20_CHARS = "acccount-number---20";
    private static final String POST_CODE_6_CHARS = "post-6";
    private static final String INN_12_CHARS = "inn-code--12";
    private static final String KPP_9_CHARS = "0000-0000";
    private static final long SHOP_ID = 774L;
    private static final long CONTRACT_ID = 808L;
    private static final long BALANCE_ORDER_ID = 789L;
    private static final DeliveryBalanceOrder TEST_ORDER = new DeliveryBalanceOrder(
            SHOP_ID,
            PERSON_ID,
            BALANCE_ORDER_ID,
            BIC_STR_9_CHARS,
            ACCOUNT_NUM_STR_20_CHARS,
            "contactName",
            "email@emaildomain.com",
            "numberStr-81234567890",
            POST_CODE_6_CHARS,
            INN_12_CHARS,
            KPP_9_CHARS,
            BalanceContractType.GENERAL,
            SELLER_CLIENT_ID,
            CONTRACT_ID
    );
    @Autowired
    DeliveryBalanceOrderDao deliveryBalanceOrderDao;

    /**
     * Получение заказа из хранилища.
     */
    @Test
    @DbUnitDataSet(
            before = "db/DeliveryBalanceOrderDaoTest_find_before.csv"
    )
    void test_find() {
        DeliveryBalanceOrder codOrder = deliveryBalanceOrderDao.find(SHOP_ID);
        ReflectionAssert.assertReflectionEquals(TEST_ORDER, codOrder);
    }

}
