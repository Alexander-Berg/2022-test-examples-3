package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignCounts;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignByCampaignType;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignRepositoryGetCampaignCountsForCountLimitCheckTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private int shard;
    private Campaign campaign;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        campaign = activeCampaignByCampaignType(campaignType, clientId, clientInfo.getUid());
    }

    @Test
    public void getCampaignCountsForCountLimitCheckTest_ManyCampaigns() {
        steps.campaignSteps().createCampaign(campaign.withStatusEmpty(false), clientInfo);
        steps.campaignSteps().createCampaign(
                activeCampaignByCampaignType(campaignType, clientId, clientInfo.getUid())
                        .withArchived(false), clientInfo);
        steps.campaignSteps().createCampaign(
                activeCampaignByCampaignType(campaignType, clientId, clientInfo.getUid())
                        .withArchived(true), clientInfo);
        var result = campaignRepository.getCampaignCountsForCountLimitCheck(shard, clientId.asLong());
        assertEquals(new CampaignCounts(3, 2), result);
    }

    @Test
    public void getCampaignCountsForCountLimitCheckTest_expectOneCampaign() {
        steps.campaignSteps().createCampaign(campaign.withStatusEmpty(false), clientInfo);
        var result = campaignRepository.getCampaignCountsForCountLimitCheck(shard, clientId.asLong());
        assertEquals(new CampaignCounts(1, 1), result);
    }

    @Test
    public void getCampaignCountsForCountLimitCheckTest_expectEmpty() {
        steps.campaignSteps().createCampaign(campaign.withStatusEmpty(true), clientInfo);
        var result = campaignRepository.getCampaignCountsForCountLimitCheck(shard, clientId.asLong());
        assertEquals(new CampaignCounts(0, 0), result);
    }

    @Test
    public void getCampaignCountsForCountLimitCheckTest_withoutCampaigns() {
        var result = campaignRepository.getCampaignCountsForCountLimitCheck(shard, clientId.asLong());
        assertEquals(new CampaignCounts(0, 0), result);
    }
}
