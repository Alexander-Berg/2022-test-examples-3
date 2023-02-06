package ru.yandex.direct.intapi.entity.inventori.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class InventoriControllerBookedCampaignsTest {
    private static final String EXPECTED_BOOKED_CAMPAIGNS_CONTROLLER_MAPPING = "/inventori/booked_campaigns";
    private static final List<String> INVENTORI_TARGET_TAGS =
            List.of("realty_c2_m", "realty_c1_m", "autoru_r1_d", "autoru_super_d");

    @Autowired
    private InventoriController controller;

    @Autowired
    private Steps steps;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private MockMvc mockMvc;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        clientInfo = steps.clientSteps().createDefaultClient();
        ppcPropertiesSupport.set("inventori_target_tags", String.join(", ", INVENTORI_TARGET_TAGS));
    }

    @Test
    public void cpmYndxFrontpageAdGroup_AndGoodTagsTest() throws Exception {
        var pricePackage = createPricePackage(AdGroupType.CPM_YNDX_FRONTPAGE, INVENTORI_TARGET_TAGS);
        var cpmPriceCampaign = createCampaign(pricePackage);
        checkExpected(cpmPriceCampaign.getId());
    }

    @Test
    public void cpmYndxFrontpageAdGroup_ButExtraTagsTest() throws Exception {
        var extraTags = new ArrayList<>(INVENTORI_TARGET_TAGS);
        extraTags.add("realty_c1000_m");
        var pricePackage = createPricePackage(AdGroupType.CPM_YNDX_FRONTPAGE, extraTags);
        var cpmPriceCampaign = createCampaign(pricePackage);

        checkExpected(cpmPriceCampaign.getId());
    }

    @Test
    public void cpmYndxFrontpageAdGroup_ButNoTagsTest() throws Exception {
        var pricePackage = createPricePackage(AdGroupType.CPM_YNDX_FRONTPAGE, null);
        var cpmPriceCampaign = createCampaign(pricePackage);

        checkExpected(cpmPriceCampaign.getId());
    }

    @Test
    public void cpmVideoFrontpageAdGroup_NoTagsTest() throws Exception {
        var pricePackage = createPricePackage(AdGroupType.CPM_VIDEO, null)
                .withIsFrontpage(true);
        var cpmPriceCampaign = createCampaign(pricePackage);

        checkExpected(cpmPriceCampaign.getId());
    }

    @Test
    public void cpmVideoAdGroup_ButGoodTagsTest() throws Exception {
        var pricePackage = createPricePackage(AdGroupType.CPM_VIDEO, INVENTORI_TARGET_TAGS);
        var cpmPriceCampaign = createCampaign(pricePackage);

        checkExpected(cpmPriceCampaign.getId());
    }

    @Test
    public void cpmVideoAdGroup_ButExtraTagsTest() throws Exception {
        var extraTags = new ArrayList<>(INVENTORI_TARGET_TAGS);
        extraTags.add("realty_c1000_m");
        var pricePackage = createPricePackage(AdGroupType.CPM_VIDEO, extraTags);
        var cpmPriceCampaign = createCampaign(pricePackage);

        checkNotExpected(cpmPriceCampaign.getId());
    }

    @Test
    public void cpmVideoAdGroup_AndNullTagsTest() throws Exception {
        var pricePackage = createPricePackage(AdGroupType.CPM_VIDEO, null);
        var cpmPriceCampaign = createCampaign(pricePackage);
        checkNotExpected(cpmPriceCampaign.getId());
    }


    private List<Long> getResponse() throws Exception {
        String r = mockMvc
                .perform(get(EXPECTED_BOOKED_CAMPAIGNS_CONTROLLER_MAPPING))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Arrays.asList(JsonUtils.fromJson(r, Long[].class));
    }

    private void checkExpected(List<Long> expectedResult) throws Exception {
        List<Long> actualResult = getResponse();
        assertThat(actualResult).containsAll(expectedResult);
    }
    private void checkExpected(Long expectedCampaignId) throws Exception {
        checkExpected(List.of(expectedCampaignId));
    }

    private void checkNotExpected(List<Long> notExpectedResult) throws Exception {
        List<Long> actualResult = getResponse();
        assertThat(actualResult).isNotIn(notExpectedResult);
    }
    private void checkNotExpected(Long notExpectedCampaignId) throws Exception {
        checkNotExpected(List.of(notExpectedCampaignId));
    }

    private PricePackage createPricePackage(AdGroupType adGroupType, List<String> targetTags) {
        return steps.pricePackageSteps()
                .createPricePackage(approvedPricePackage()
                        .withCurrency(CurrencyCode.RUB)
                        .withStatusApprove(StatusApprove.YES)
                        .withAllowedTargetTags(targetTags)
                        .withAvailableAdGroupTypes(Set.of(adGroupType)))
                .getPricePackage();
    }

    private CpmPriceCampaign createCampaign(PricePackage pricePackage) {
        var cpmPriceCampaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withStatusShow(true);
        steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, cpmPriceCampaign);
        return cpmPriceCampaign;
    }
}
