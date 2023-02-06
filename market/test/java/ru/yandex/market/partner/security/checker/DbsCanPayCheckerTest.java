package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.DefaultCampaignable;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

public class DbsCanPayCheckerTest extends FunctionalTest {
    @Autowired
    private DbsCanPayChecker dbsCanPayChecker;

    private final static Long DBS_PRODUCT_CAMPAIGN_ID = 104L;
    private final static Long CPC_PRODUCT_CAMPAIGN_ID = 105L;
    private final static Long NOT_EXIST_CAMPAIGN_ID = 371629L;

    @Test
    @DbUnitDataSet(before = "DbsCanPayCheckerTest.before.csv")
    void testGood() {
        long uid = 1;
        Assertions.assertTrue(dbsCanPayChecker.checkTyped(
                new DefaultCampaignable(DBS_PRODUCT_CAMPAIGN_ID, uid, uid),
                new Authority("test", "SHOP")));
        Assertions.assertFalse(dbsCanPayChecker.checkTyped(
                new DefaultCampaignable(CPC_PRODUCT_CAMPAIGN_ID, uid, uid),
                new Authority("test", "SHOP")));
        Assertions.assertFalse(dbsCanPayChecker.checkTyped(
                new DefaultCampaignable(NOT_EXIST_CAMPAIGN_ID, uid, uid),
                new Authority("test", "SHOP")));
    }

}
