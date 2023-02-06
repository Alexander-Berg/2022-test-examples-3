package ru.yandex.market.partner.campaign;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.diff.NodeMatcher;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональные тесты на {@link GetCampaignsBriefInfoServantlet}.
 *
 * @author fbokovikov
 */
class GetCampaignsBriefInfoFunctionalTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "testMarketOnly.csv")
    void testMarketOnlyUser() {
        final String response = FunctionalTestHelper.get(getUrl(100500L)).getBody();
        assertResponse(response, "testNoCampaigns.xml");
    }

    @Test
    void testClientIdNotFound() {
        final String response = FunctionalTestHelper.get(getUrl(100500L)).getBody();
        assertResponse(response, "testNoCampaigns.xml");
    }

    @Test
    @DbUnitDataSet(before = "testShopCampaigns.csv")
    void testShopCampaigns() {
        checkResponse("testShopCampaigns.xml");
    }

    @Test
    @DbUnitDataSet(before = "testSupplierCampaign.csv")
    @DisplayName("Возвращается несколько кампаний поставщика, привязанных к одному клиенту")
    void testSupplierCampaigns() {
        checkResponse("testSupplierCampaigns.xml");
    }

    @Test
    @DbUnitDataSet(before = {"agencyUsers.csv", "testAgencyCampaigns.csv"})
    @DisplayName("Возвращаются кампании агентства с поставщиками, у которых проставлен флаг для доступа агентства")
    void testAgencyCampaigns() {
        checkResponse("testAgencyCampaigns.xml");
    }

    @Test
    @DbUnitDataSet(before = {"agencyUsers.csv", "testAgencyOneClientIdSeveralCampaigns.csv"})
    @DisplayName("Кампании агентства с поставщиками с флагом для доступа агентств; один clientId ")
    void testAgencyOneClientIdSeveralCampaigns() {
        checkResponse("testAgencyOneClientIdSeveralCampaigns.xml");
    }

    @Test
    @DbUnitDataSet(before = {"agencyUsers.csv", "testAgencySeveralClientIds.csv"})
    @DisplayName("Кампании агентства с поставщиками с флагом для доступа агентств; разные clientId")
    void testAgencySeveralClientIds() {
        checkResponse("testAgencySeveralClientIds.xml");
    }

    @Test
    void testBalanceReturnNull() {
        final String response = FunctionalTestHelper.get(getUrl(1248L)).getBody();
        assertResponse(response, "testNoCampaigns.xml");
    }

    private String getUrl(long euid) {
        return UriComponentsBuilder.fromUriString(baseUrl + "/getCampaignsBriefInfo")
                .queryParam("euid", euid)
                .toUriString();
    }

    private void checkResponse(String expectedXmlFile) {
        final String actualContent =
                Preconditions.checkNotNull(FunctionalTestHelper.get(getUrl(1248L)).getBody());

        assertResponse(actualContent, expectedXmlFile);
    }

    private NodeMatcher matchIgnorePlacementTypeOrder() {
        return new DefaultNodeMatcher(ElementSelectors.conditionalBuilder()
                .whenElementIsNamed("partner-placement-program-type")
                .thenUse(ElementSelectors.byNameAndText)
                .elseUse(ElementSelectors.Default)
                .build());
    }

    private void assertResponse(String actualContent, String expectedXmlFile) {
        final String expectedContent = FunctionalTestHelper.getResource(getClass(), expectedXmlFile);

        MatcherAssert.assertThat(actualContent, MbiMatchers.xmlEquals(
                expectedContent, matchIgnorePlacementTypeOrder(), ImmutableSet.of("servant", "version", "host",
                        "executing-time", "actions")));
    }

}
