package ru.yandex.market.partner.mvc.controller.partner.meta;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.mvc.controller.program.BusinessPartnersRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataSet(before = "ProgramMetaControllerFunctionalTest.before.csv")
class PartnerMetaControllerFunctionalTest extends FunctionalTest {

    private static final String URL = "/business/{businessId}/partners/meta";

    private static final Long BUSINESS_ID = 100L;


    @Test
    public void testGetShop() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + URL,
                createBusinessPartnerRequest(List.of(111L, 222L)),
                BUSINESS_ID
        );
        JsonTestUtil.assertEquals(response, getClass(), "json/testGetShop.json");
    }

    @Test
    public void testGetSupplier() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + URL,
                createBusinessPartnerRequest(List.of(333L)),
                BUSINESS_ID
        );
        JsonTestUtil.assertEquals(response, getClass(), "json/testGetSupplier.json");
    }

    @Test
    public void testGetShopAndSupplier() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + URL,
                createBusinessPartnerRequest(List.of(222L, 333L)),
                BUSINESS_ID
        );
        JsonTestUtil.assertEquals(response, getClass(), "json/testGetShopAndSupplier.json");
    }

    @Test
    public void testNoPartnerPlacementType() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + URL,
                createBusinessPartnerRequest(List.of(444L)),
                BUSINESS_ID
        );
        JsonTestUtil.assertEquals(response, getClass(), "json/testNoPartnerPlacementType.json");
    }

    @Test
    public void testGetUnknownPartner() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + URL,
                createBusinessPartnerRequest(List.of(999L)),
                BUSINESS_ID
        );
        JsonTestUtil.assertEquals(response, getClass(), "json/noOutput.json");
    }

    @Test
    public void testNoInput() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + URL,
                createBusinessPartnerRequest(Collections.emptyList()),
                BUSINESS_ID
        );
        JsonTestUtil.assertEquals(response, getClass(), "json/noOutput.json");
    }


    private BusinessPartnersRequest createBusinessPartnerRequest(List<Long> partnerIds) {
        BusinessPartnersRequest businessPartnersRequest = new BusinessPartnersRequest();
        businessPartnersRequest.setPartnerIds(partnerIds);

        return businessPartnersRequest;
    }
}
