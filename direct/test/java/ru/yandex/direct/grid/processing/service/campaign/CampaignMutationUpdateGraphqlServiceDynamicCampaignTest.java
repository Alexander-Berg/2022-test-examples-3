package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.DynamicsSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.BidsDynamicStatusbssynced;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsDynamicRecord;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateDynamicCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageBidStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.data.TestGdUpdateCampaigns.defaultDynamicCampaign;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS;

/**
 * Тесты на обновление ДО кампании
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationUpdateGraphqlServiceDynamicCampaignTest {
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final TemplateMutation<GdUpdateCampaigns, GdUpdateCampaignPayload> UPDATE_CAMPAIGN_MUTATION =
            new TemplateMutation<>(UPDATE_CAMPAIGNS, MUTATION_TEMPLATE,
                    GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private DynamicsSteps dynamicsSteps;
    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;
    private int shard;

    private static CampaignAttributionModel defaultAttributionModel;


    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        shard = userInfo.getShard();
        operator = userInfo.getUser();

        TestAuthHelper.setDirectAuthentication(operator);

        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    /**
     * Проверяем изменение ставок при смене стратегии с автобюджета на ручную
     */
    @Test
    public void updateDynamicCampaign_checkStrategyChangeToManual() {
        Campaign campaign = activeDynamicCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withStrategy(averageBidStrategy());
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
        BidsDynamicRecord bidsDynamic = dynamicsSteps.addDefaultBidsDynamic(adGroupInfo);

        GdUpdateDynamicCampaign gdUpdateDynamicCampaign = defaultDynamicCampaign(campaignInfo.getCampaignId(),
                defaultAttributionModel);
        gdUpdateDynamicCampaign.getBiddingStategy()
                .withStrategy(GdCampaignStrategy.DIFFERENT_PLACES);

        GdUpdateCampaignPayload response = sendRequest(gdUpdateDynamicCampaign);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getUpdatedCampaigns()).hasSize(1);
            soft.assertThat(response.getValidationResult()).isNull();
        });

        List<BidsDynamicRecord> bidsDynamicRecords =
                dynamicsSteps.getBidsDynamicRecordsByCampaignIds(shard, singleton(campaignInfo.getCampaignId()));

        BidsDynamicRecord expectRecord = bidsDynamic.copy();
        expectRecord.setDynId(bidsDynamic.getDynId());
        expectRecord.setPrice(CurrencyCode.RUB.getCurrency().getDefaultPrice().setScale(2, HALF_UP));
        expectRecord.setPriceContext(CurrencyCode.RUB.getCurrency().getDefaultPrice().setScale(2, HALF_UP));
        expectRecord.setStatusbssynced(BidsDynamicStatusbssynced.No);

        assertThat(bidsDynamicRecords).hasSize(1);
        assertRecordsEquals(bidsDynamicRecords.get(0), expectRecord);
    }

    /**
     * Проверяем изменение ставок при изменении стратегии с ручной на автобюджет
     */
    @Test
    public void updateDynamicCampaign_checkStrategyChangeToAutobudget() {
        Campaign campaign = activeDynamicCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withStrategy(manualStrategy());
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
        BidsDynamicRecord bidsDynamic = dynamicsSteps.addDefaultBidsDynamic(adGroupInfo);

        GdUpdateDynamicCampaign gdUpdateDynamicCampaign = defaultDynamicCampaign(campaignInfo.getCampaignId(),
                defaultAttributionModel)
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CLICK)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withAvgBid(BigDecimal.valueOf(50))
                                .withSum(BigDecimal.valueOf(5000))));

        GdUpdateCampaignPayload response = sendRequest(gdUpdateDynamicCampaign);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getUpdatedCampaigns()).hasSize(1);
            soft.assertThat(response.getValidationResult()).isNull();
        });

        List<BidsDynamicRecord> bidsDynamicRecords =
                dynamicsSteps.getBidsDynamicRecordsByCampaignIds(shard, singleton(campaignInfo.getCampaignId()));

        BidsDynamicRecord expectRecord = bidsDynamic.copy();
        expectRecord.setDynId(bidsDynamic.getDynId());
        expectRecord.setAutobudgetpriority(3L);
        expectRecord.setStatusbssynced(BidsDynamicStatusbssynced.No);

        assertThat(bidsDynamicRecords).hasSize(1);
        assertRecordsEquals(bidsDynamicRecords.get(0), expectRecord);
    }

    private GdUpdateCampaignPayload sendRequest(GdUpdateDynamicCampaign campaign) {
        GdUpdateCampaignUnion gdUpdateCampaignUnion = new GdUpdateCampaignUnion().withDynamicCampaign(campaign);
        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(List.of(gdUpdateCampaignUnion));
        return processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);
    }

    private void assertRecordsEquals(BidsDynamicRecord actual, BidsDynamicRecord expected) {
        SoftAssertions soft = new SoftAssertions();
        for (String colName : List.of("dyn_id", "autobudgetPriority", "price", "price_context", "statusBsSynced")) {
            soft.assertThat(actual.get(colName)).as(colName + " fields expected to be equal")
                    .isEqualTo(expected.get(colName));
        }
        soft.assertAll();
    }
}
