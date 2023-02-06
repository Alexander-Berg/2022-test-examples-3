package ru.yandex.direct.intapi.entity.bidmodifiers.controller;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.bidmodifier.BannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerTypeAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPositionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.TrafaretPosition;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignBidModifierInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierBannerTypeConditionWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierBannerTypeWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierInventoryConditionWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierInventoryWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierTrafaretPositionConditionWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierTrafaretPositionWeb;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifiersListWebResponse;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBannerTypeAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDesktop;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultInventoryAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultIosBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultTrafaretPositionAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyBannerTypeModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyInventoryModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyTrafaretPositionModifier;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BidModifiersControllerGetTest {
    @Autowired
    private Steps steps;

    @Autowired
    private BidModifiersController controller;

    private MockMvc mockMvc;

    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
    }

    @Test
    public void getCampaignLevelDesktopBidModifiers() throws Exception {
        CampaignBidModifierInfo modifier = steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultBidModifierDesktop(campaignInfo.getCampaignId()), campaignInfo);

        mockMvc.perform(get("/bidmodifiers/get").param("campaignId", Long.toString(campaignInfo.getCampaignId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content()
                        .json(String.format(
                                "{'desktop_multiplier': {'hierarchical_multiplier_id': %d, 'multiplier_pct': %d}}",
                                modifier.getBidModifiers().get(0).getId(), TestBidModifiers.DEFAULT_PERCENT)));
    }

    @Test
    public void getGroupLevelDesktopBidModifiers() throws Exception {
        AdGroupBidModifierInfo modifier = steps.bidModifierSteps().createAdGroupBidModifier(
                createDefaultBidModifierDesktop(campaignInfo.getCampaignId()), adGroupInfo);

        mockMvc.perform(get("/bidmodifiers/get")
                        .param("campaignId", Long.toString(campaignInfo.getCampaignId()))
                        .param("adGroupId", Long.toString(adGroupInfo.getAdGroupId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content()
                        .json(String.format(
                                "{'desktop_multiplier': {'hierarchical_multiplier_id': %d, 'multiplier_pct': %d}}",
                                modifier.getBidModifierId(), TestBidModifiers.DEFAULT_PERCENT)));
    }

    @Test
    public void getCampaignLevelMobileBidModifiers() throws Exception {
        CampaignBidModifierInfo modifier = steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultBidModifierMobile(campaignInfo.getCampaignId()), campaignInfo);

        mockMvc.perform(get("/bidmodifiers/get").param("campaignId", Long.toString(campaignInfo.getCampaignId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content()
                        .json(String.format(
                                "{'mobile_multiplier': {'hierarchical_multiplier_id': %d, 'os_type': null, " +
                                        "'multiplier_pct': %d}}",
                                modifier.getBidModifiers().get(0).getId(), TestBidModifiers.DEFAULT_PERCENT)));
    }

    @Test
    public void getGroupLevelMobileBidModifiers() throws Exception {
        AdGroupBidModifierInfo modifier = steps.bidModifierSteps().createAdGroupBidModifier(
                createDefaultBidModifierMobile(campaignInfo.getCampaignId()), adGroupInfo);

        mockMvc.perform(get("/bidmodifiers/get")
                        .param("campaignId", Long.toString(campaignInfo.getCampaignId()))
                        .param("adGroupId", Long.toString(adGroupInfo.getAdGroupId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content()
                        .json(String.format(
                                "{'mobile_multiplier': {'hierarchical_multiplier_id': %d, 'os_type': null, " +
                                        "'multiplier_pct': %d}}",
                                modifier.getBidModifierId(), TestBidModifiers.DEFAULT_PERCENT)));
    }

    @Test
    public void getCampaignLevelMobileWithOsBidModifiers() throws Exception {
        CampaignBidModifierInfo modifier = steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultIosBidModifierMobile(campaignInfo.getCampaignId()), campaignInfo);

        mockMvc.perform(get("/bidmodifiers/get").param("campaignId", Long.toString(campaignInfo.getCampaignId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content()
                        .json(String.format(
                                "{'mobile_multiplier': {'hierarchical_multiplier_id': %d, 'os_type': 'ios', " +
                                        "'multiplier_pct': %d}}",
                                modifier.getBidModifiers().get(0).getId(), TestBidModifiers.DEFAULT_PERCENT)));
    }

    @Test
    public void getGroupLevelMobileWithOsBidModifiers() throws Exception {
        AdGroupBidModifierInfo modifier = steps.bidModifierSteps().createAdGroupBidModifier(
                createDefaultIosBidModifierMobile(campaignInfo.getCampaignId()), adGroupInfo);

        mockMvc.perform(get("/bidmodifiers/get")
                        .param("campaignId", Long.toString(campaignInfo.getCampaignId()))
                        .param("adGroupId", Long.toString(adGroupInfo.getAdGroupId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content()
                        .json(String.format(
                                "{'mobile_multiplier': {'hierarchical_multiplier_id': %d, 'os_type': 'ios', " +
                                        "'multiplier_pct': %d}}",
                                modifier.getBidModifierId(), TestBidModifiers.DEFAULT_PERCENT)));
    }

    @Test
    public void getCampaignLevelBannerTypeModifiers() throws Exception {
        List<BidModifierBannerTypeAdjustment> adjustments = asList(
                createDefaultBannerTypeAdjustment()
                        .withBannerType(BannerType.CPM_BANNER)
                        .withPercent(120),
                createDefaultBannerTypeAdjustment()
                        .withBannerType(BannerType.CPM_OUTDOOR)
                        .withPercent(230)
        );
        CampaignBidModifierInfo modifier = steps.bidModifierSteps().createCampaignBidModifier(
                createEmptyBannerTypeModifier()
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withBannerTypeAdjustments(adjustments),
                campaignInfo);

        BidModifiersListWebResponse expected = new BidModifiersListWebResponse()
                .withBidModifierBannerTypeWeb(new BidModifierBannerTypeWeb()
                        .withHierarchicalMultiplierId(modifier.getBidModifiers().get(0).getId())
                        .withConditions(mapList(adjustments, this::bannerTypeAdjustmentToWebResponse))
                        .withEnabled(1));

        mockMvc.perform(get("/bidmodifiers/get")
                        .param("campaignId", Long.toString(campaignInfo.getCampaignId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        BidModifiersListWebResponse response = controller.getBidModifiers(campaignInfo.getCampaignId(), null);
        assertThat(response, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void getCampaignLevelInventoryTypeModifiers() throws Exception {
        List<BidModifierInventoryAdjustment> adjustments = asList(
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.INSTREAM_WEB)
                        .withPercent(230),
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.INPAGE)
                        .withPercent(120),
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.INAPP)
                        .withPercent(340),
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.INBANNER)
                        .withPercent(450),
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.REWARDED)
                        .withPercent(560)
        );
        CampaignBidModifierInfo modifier = steps.bidModifierSteps().createCampaignBidModifier(
                createEmptyInventoryModifier()
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withInventoryAdjustments(adjustments),
                campaignInfo);

        BidModifiersListWebResponse expected = new BidModifiersListWebResponse()
                .withBidModifierInventory(new BidModifierInventoryWeb()
                        .withHierarchicalMultiplierId(modifier.getBidModifiers().get(0).getId())
                        .withConditions(mapList(adjustments, this::inventoryAdjustmentToWebResponse))
                        .withEnabled(1));

        mockMvc.perform(get("/bidmodifiers/get")
                        .param("campaignId", Long.toString(campaignInfo.getCampaignId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        BidModifiersListWebResponse response = controller.getBidModifiers(campaignInfo.getCampaignId(), null);
        assertThat(response, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void getCampaignLevelTrafaretPositionTypeModifiers() throws Exception {
        List<BidModifierTrafaretPositionAdjustment> adjustments = asList(
                createDefaultTrafaretPositionAdjustment()
                        .withTrafaretPosition(TrafaretPosition.ALONE)
                        .withPercent(230),
                createDefaultTrafaretPositionAdjustment()
                        .withTrafaretPosition(TrafaretPosition.SUGGEST)
                        .withPercent(120)
        );
        CampaignBidModifierInfo modifier = steps.bidModifierSteps().createCampaignBidModifier(
                createEmptyTrafaretPositionModifier()
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withTrafaretPositionAdjustments(adjustments),
                campaignInfo);

        BidModifiersListWebResponse expected = new BidModifiersListWebResponse()
                .withBidModifierTrafaretPosition(new BidModifierTrafaretPositionWeb()
                        .withHierarchicalMultiplierId(modifier.getBidModifiers().get(0).getId())
                        .withConditions(mapList(adjustments, this::trafaretPositionAdjustmentToWebResponse))
                        .withEnabled(1));

        mockMvc.perform(get("/bidmodifiers/get")
                        .param("campaignId", Long.toString(campaignInfo.getCampaignId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        BidModifiersListWebResponse response = controller.getBidModifiers(campaignInfo.getCampaignId(), null);
        assertThat(response, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    private BidModifierBannerTypeConditionWeb bannerTypeAdjustmentToWebResponse(
            BidModifierBannerTypeAdjustment adjustment) {
        return new BidModifierBannerTypeConditionWeb()
                .withId(adjustment.getId())
                .withMultiplierType(BannerType.toSource(adjustment.getBannerType()).getLiteral())
                .withPercent(adjustment.getPercent());
    }

    private BidModifierInventoryConditionWeb inventoryAdjustmentToWebResponse(
            BidModifierInventoryAdjustment adjustment) {
        return new BidModifierInventoryConditionWeb()
                .withId(adjustment.getId())
                .withMultiplierType(InventoryType.toSource(adjustment.getInventoryType()).getLiteral())
                .withPercent(adjustment.getPercent());
    }

    private BidModifierTrafaretPositionConditionWeb trafaretPositionAdjustmentToWebResponse(
            BidModifierTrafaretPositionAdjustment adjustment) {
        return new BidModifierTrafaretPositionConditionWeb()
                .withId(adjustment.getId())
                .withMultiplierType(TrafaretPosition.toSource(adjustment.getTrafaretPosition()).getLiteral())
                .withPercent(adjustment.getPercent());
    }
}
