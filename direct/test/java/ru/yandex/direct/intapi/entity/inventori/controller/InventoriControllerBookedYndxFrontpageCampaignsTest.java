package ru.yandex.direct.intapi.entity.inventori.controller;

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

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
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
public class InventoriControllerBookedYndxFrontpageCampaignsTest {
    private static final String EXPECTED_BOOKED_YNDX_FRONTPAGE_CAMPAIGNS_CONTROLLER_MAPPING
            = "/inventori/booked_yndxfrontpage_campaigns";

    @Autowired
    private InventoriController controller;

    @Autowired
    private Steps steps;

    private MockMvc mockMvc;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void cpmYndxFrontpageAdGroupTest() throws Exception {
        var cpmPriceCampaign = createCampaignByPackage(AdGroupType.CPM_YNDX_FRONTPAGE, true);
        checkExpected(cpmPriceCampaign.getId());
    }

    @Test
    public void cpmVideoFrontpageAdGroupTest() throws Exception {
        var cpmPriceCampaign = createCampaignByPackage(AdGroupType.CPM_VIDEO, true);
        checkExpected(cpmPriceCampaign.getId());
    }

    @Test
    public void cpmVideoNotFrontpageAdGroupTest() throws Exception {
        var cpmPriceCampaign = createCampaignByPackage(AdGroupType.CPM_VIDEO, false);
        checkNotExpected(cpmPriceCampaign.getId());
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

    private List<Long> getResponse() throws Exception {
        String r = mockMvc
                .perform(get(EXPECTED_BOOKED_YNDX_FRONTPAGE_CAMPAIGNS_CONTROLLER_MAPPING))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Arrays.asList(JsonUtils.fromJson(r, Long[].class));
    }
    private CpmPriceCampaign createCampaignByPackage(AdGroupType adGroupType, boolean isFrontpage) {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(approvedPricePackage()
                        .withCurrency(CurrencyCode.RUB)
                        .withStatusApprove(StatusApprove.YES)
                        .withIsFrontpage(isFrontpage)
                        .withAvailableAdGroupTypes(Set.of(adGroupType)))
                .getPricePackage();
        var cpmPriceCampaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withStatusShow(true);
        steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, cpmPriceCampaign);
        return cpmPriceCampaign;
    }
}
