package ru.yandex.market.partner.security.checker;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.DefaultCampaignable;
import ru.yandex.market.core.security.checker.ModelManagingShopChecker;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "ModelManagingShopCheckerTest.csv")
public class ModelManagingShopCheckerTest extends FunctionalTest {
    private static final int USER_ID = 14109428;
    @Autowired
    private ModelManagingShopChecker modelManagingShopChecker;

    @Test
    public void test() {
        Assertions.assertTrue(checkAccess(10774));
        Assertions.assertFalse(checkAccess(10775));
        Assertions.assertFalse(checkAccess(10776));
        Assertions.assertTrue(checkAccess(20664));
        Assertions.assertFalse(checkAccess(20665));
        Assertions.assertFalse(checkAccess(20666));
    }

    private boolean checkAccess(int campaignId) {
        return modelManagingShopChecker.checkTyped(new DefaultCampaignable(campaignId, USER_ID, USER_ID), new Authority());
    }
}
