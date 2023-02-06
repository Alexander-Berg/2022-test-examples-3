package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyCampaignByCampaignType;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignRepositoryGetNewLastProcessedCampaignIdTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final int TEST_SHARD = 1;
    private static final Long ZERO = 0L;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository campaignRepository0;

    @Autowired
    private Steps steps;

    private ClientId clientId;
    private Long uid;

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
    public void createClientInShard() {
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo().withShard(TEST_SHARD));
        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();
    }

    @Test
    public void returnsZeroOnEmptyTable() {
        steps.campaignSteps().runWithEmptyCampaignsTables(TEST_SHARD, dsl ->
                assertEquals(ZERO, campaignRepository.getNewLastProcessedCampaignId(dsl))
        );
    }

    @Test
    public void returnsZeroOnTableWithEmptyCampaign() {
        Campaign emptyCampaign = emptyCampaignByCampaignType(campaignType, clientId, uid);

        steps.campaignSteps().runWithEmptyCampaignsTables(TEST_SHARD, dsl -> {
            campaignRepository0.addCampaigns(dsl, clientId, singletonList(emptyCampaign));

            assertEquals(ZERO, campaignRepository.getNewLastProcessedCampaignId(dsl));
        });
    }

    @Test
    public void returnsCampaignIdIncreasedByOneOnTableWithSingleCampaign() {
        Campaign activeCampaign = activeCampaignByCampaignType(campaignType, clientId, uid);

        steps.campaignSteps().runWithEmptyCampaignsTables(TEST_SHARD, dsl -> {
            campaignRepository0.addCampaigns(dsl, clientId, singletonList(activeCampaign));
            Long expectedId = activeCampaign.getId() + 1;

            assertEquals(expectedId, campaignRepository.getNewLastProcessedCampaignId(dsl));
        });
    }

    @Test
    public void returnsMaximumNotEmptyCampaignIdIncreasedByOneOnTableWithSeveralCampaigns() {
        Campaign activeCampaign1 = activeCampaignByCampaignType(campaignType, clientId, uid);
        Campaign activeCampaign2 = activeCampaignByCampaignType(campaignType, clientId, uid);
        Campaign emptyCampaign1 = emptyCampaignByCampaignType(campaignType, clientId, uid);
        Campaign emptyCampaign2 = emptyCampaignByCampaignType(campaignType, clientId, uid);

        steps.campaignSteps().runWithEmptyCampaignsTables(TEST_SHARD, dsl -> {
            List<Campaign> campaigns =
                    asList(activeCampaign1, activeCampaign2, emptyCampaign1, emptyCampaign2);


            campaignRepository0.addCampaigns(dsl, clientId, campaigns);
            Long expectedId = activeCampaign2.getId() + 1;

            assertEquals(expectedId, campaignRepository.getNewLastProcessedCampaignId(dsl));
        });
    }
}
