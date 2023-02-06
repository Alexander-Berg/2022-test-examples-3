package ru.yandex.market.core.security.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.AgencyService;

@DbUnitDataSet(before = "AgencyCheckerComponentsUnitTest.csv")
class AgencyCheckerComponentsUnitTest extends FunctionalTest {

    @Autowired
    private AgencyService agencyService;


    @Test
    void isSupplierCampaignAgencyTest() {
        Assertions.assertTrue(agencyService.isSupplierCampaignAgency(1234L, 1L));
    }

    @Test
    void isSupplierCampaignAgencyWrongCampaignTest() {
        Assertions.assertFalse(agencyService.isSupplierCampaignAgency(1237L, 1L));
    }

    @Test
    void isSupplierCampaignAgencyWrongAgencyTest() {
        Assertions.assertFalse(agencyService.isSupplierCampaignAgency(1234L, 2L));
    }
}
