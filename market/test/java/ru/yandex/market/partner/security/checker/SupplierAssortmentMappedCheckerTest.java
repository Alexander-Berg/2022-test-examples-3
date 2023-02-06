package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.junit.Assert.assertTrue;

/**
 * @author stani on 23.02.18.
 * Тесты для {@link SupplierAssortmentMappedChecker}
 */
@DbUnitDataSet(before = "assortmentCommitedCheckerTest.csv")
class SupplierAssortmentMappedCheckerTest extends FunctionalTest {

    @Autowired
    SupplierAssortmentMappedChecker supplierAssortmentMappedChecker;

    @Test
    void testSupplierWithApprovedOffers() {
        assertTrue(checkTyped(1L));
    }

    @Test
    void testSupplierWithoutApprovedOffers() {
        Assertions.assertFalse(checkTyped(2L));
    }


    @Test
    void testCampaignNotExists() {
        Assertions.assertFalse(checkTyped(404L));
    }

    private boolean checkTyped(long campaignId) {
        return supplierAssortmentMappedChecker.checkTyped(new MockPartnerRequest(-1L, -1L, -1L, campaignId), null);
    }

}
