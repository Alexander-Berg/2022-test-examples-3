package ru.yandex.market.partner.mvc.controller.offer.draft;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональные тесты для {@link DraftOffersMetadataController}
 */
@DbUnitDataSet(before = "DraftOffersMetadataControllerFunctionalTest.before.csv")
class DraftOffersMetadataControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private DraftOffersMetadataService metadataService;

    @Autowired
    private SupplierService supplierService;


    @Test
    public void draftOffersGetTest() {
        getDraftOffersAndCheck(10774L, 774L, 11);
    }

    @Test
    public void draftOffersGetFromWhiteTest() {
        getDraftOffersAndCheck(20778L, 778L, 12);
    }

    @Test
    public void draftOffersGetFromWhiteRepeatedTest() {
        getDraftOffersAndCheck(20779L, 779L, 14);
    }

    private void getDraftOffersAndCheck(long campaignId, long supplierId, int offersCount) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                String.format("%s/suppliers/%d/draft-offers", baseUrl, campaignId));

        String expected = String.format("{\n" +
                "   \"supplierId\":%d," +
                "   \"offersCount\":%d" +
                "}", supplierId, offersCount);
        JsonTestUtil.assertEquals(response, expected);
    }
}
