package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author stani on 23.02.18.
 */
@DbUnitDataSet(before = "supplierFeedExistsCheckerTest.csv")
public class SupplierFeedExistsCheckerTest extends FunctionalTest {

    @Autowired
    private SupplierFeedExistsChecker supplierFeedExistsChecker;

    @Test
    void hasNotDatacampPrice() {
        assertFalse(checkTyped(1L));
    }

    @Test
    void hasDatacampPrice() {
        assertTrue(checkTyped(2L));
    }

    @Test
    void campaignNotExist() {
        assertFalse(checkTyped(404L));
    }

    private boolean checkTyped(long campaignId) {
        MockPartnerRequest request = new MockPartnerRequest(-1L, -1L, -1L, campaignId);
        return supplierFeedExistsChecker.checkTyped(request, null);
    }
}
