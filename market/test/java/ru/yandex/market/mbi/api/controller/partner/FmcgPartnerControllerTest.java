package ru.yandex.market.mbi.api.controller.partner;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.mbi.api.client.entity.partner.FmcgPartnerInfo;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

class FmcgPartnerControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "FmcgPartnerControllerTest.csv")
    void testGetPartnerFmcgInfo() {
        List<FmcgPartnerInfo> fmcgPartners = mbiApiClient.getAllFmcgPartners();

        List<FmcgPartnerInfo> expected = Arrays.asList(
                new FmcgPartnerInfo(1, "Имя магазина 1", "Организация 1", OrganizationType.ZAO, null),
                new FmcgPartnerInfo(2, "Имя магазина 2", "Организация 2", OrganizationType.OOO, "http://my-site"));
        assertThat(fmcgPartners, containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DbUnitDataSet(before = "FmcgPartnerControllerTest.csv")
    void testXmlRepresentation() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(
                "http://localhost:" + port + "/partners/by_type/fmcg",
                String.class
        );

        MbiAsserts.assertXmlEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<fmcg-partners>\n" +
                        "   <partners>\n" +
                        "      <partner id=\"1\" name=\"Имя магазина 1\" organization-name=\"Организация 1\" organization-type=\"2\" />\n" +
                        "      <partner id=\"2\" name=\"Имя магазина 2\" organization-name=\"Организация 2\" organization-type=\"1\" domain=\"http://my-site\"/>\n" +
                        "   </partners>\n" +
                        "</fmcg-partners>",
                responseEntity.getBody());
    }

    /**
     * Проверка отсутствия поставщиков, у которых заявка в статусе {@link PartnerApplicationStatus#CLOSED} или
     * {@link PartnerApplicationStatus#CANCELLED}, а также без заявок.
     */
    @Test
    @DbUnitDataSet(before = "FmcgPartnerControllerTest.excluded.csv")
    void testExcludedPartners() {
        List<FmcgPartnerInfo> fmcgPartners = mbiApiClient.getAllFmcgPartners();
        assertThat(fmcgPartners, empty());
    }
}
