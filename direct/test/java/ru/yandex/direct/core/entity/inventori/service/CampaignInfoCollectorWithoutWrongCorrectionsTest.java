package ru.yandex.direct.web.entity.inventori.service;

import java.time.LocalDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.inventori.service.CampaignInfoCollector;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachStrategy;
import ru.yandex.direct.inventori.model.request.TrafficTypeCorrections;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientInventoryAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientInventoryModifier;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxReachStrategy;


@CoreTest
@RunWith(SpringRunner.class)
public class CampaignInfoCollectorWithoutWrongCorrectionsTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignInfoCollector collector;

    private ClientInfo clientInfo;

    private static final TrafficTypeCorrections DEFAULT_TRAFFIC_TYPE_CORRECTIONS =
            new TrafficTypeCorrections(null, null, null, null, null, null);

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void getCampaignPredictionRequestForCampaignsTestWithCorrestrionINBANNER() throws JsonProcessingException {
        AutobudgetMaxReachStrategy strategy = autobudgetMaxReachStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();
        var bidModifierInfo = steps.bidModifierSteps()
                .createCampaignBidModifier(createEmptyClientInventoryModifier()
                        .withInventoryAdjustments(Arrays.asList(
                                createDefaultClientInventoryAdjustment()
                                        .withInventoryType(InventoryType.INBANNER).withLastChange(LocalDateTime.now())))
                        .withEnabled(true)
                        .withLastChange(LocalDateTime.now()), campaignInfo);
        ObjectMapper objectMapper = new ObjectMapper();
        var correctionsById = collector.collectCampaignsCorrections(campaignInfo.getShard(), Arrays.asList(campaignInfo.getCampaignId()));
        TrafficTypeCorrections expected = new TrafficTypeCorrections(100, 100, 100, 100, 110, 100);
        assertEquals(expected, correctionsById.values().stream().findFirst().get());
    }

    @Test
    public void getCampaignPredictionRequestForCampaignsTestWithoutNonNullCorrestrions() throws JsonProcessingException {
        AutobudgetMaxReachStrategy strategy = autobudgetMaxReachStrategy();
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), null).withStrategy(strategy));
        Campaign campaign = campaignInfo.getCampaign();
        ObjectMapper objectMapper = new ObjectMapper();
        var correctionsById = collector.collectCampaignsCorrections(campaignInfo.getShard(), Arrays.asList(campaignInfo.getCampaignId()));
        System.out.println(correctionsById.size());
        assertEquals(DEFAULT_TRAFFIC_TYPE_CORRECTIONS, correctionsById.values().toArray()[0]);
    }
}
