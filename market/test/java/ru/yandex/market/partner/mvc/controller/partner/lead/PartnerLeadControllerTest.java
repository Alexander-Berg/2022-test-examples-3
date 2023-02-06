package ru.yandex.market.partner.mvc.controller.partner.lead;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link PartnerLeadController}
 */
public class PartnerLeadControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(after = "PartnerLeadControllerTest.testAddLead.after.csv")
    void testAddLead() {
        PartnerLeadDTO partnerLeadDTO = PartnerLeadDTO.builder()
                .setCounterId("1")
                .setYandexUid("2")
                .setFirstName("Jerry")
                .setLastName("Little")
                .setEmail("test@mail.com")
                .setLogin("testLogin")
                .setPhone("+74912992267")
                .setIsAgree(true)
                .setIsAdvAgree(false)
                .setAssortment("500")
                .setCategory("Toys")
                .setSourceName("express")
                .build();
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/leads", partnerLeadDTO);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonTestUtil.assertEquals(response, PartnerLeadControllerTest.class,
                "PartnerLeadControllerTest.testAddLead.response.json");
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadControllerTest.testUpdatePartner.before.csv",
            after = "PartnerLeadControllerTest.testUpdatePartner.after.csv")
    void testUpdatePartner() {
        PartnerLeadDTO partnerLeadDTO = PartnerLeadDTO.builder()
                .setFirstName("John")
                .setLastName("Huge")
                .setEmail("test34@mail.com")
                .setLogin("testLogin864")
                .setPhone("+74912992200")
                .setAssortment("<500")
                .setCategory("BOOKS")
                .build();
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/leads/2?campaignId=100123", partnerLeadDTO);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadControllerTest.testUpdatePartnerOnly.before.csv",
            after = "PartnerLeadControllerTest.testUpdatePartnerOnly.after.csv")
    void testUpdatePartnerOnly() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/leads/2?campaignId=100123");

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
