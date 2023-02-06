package ru.yandex.direct.grid.processing.service.campaign.meaningfulgoals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddTextCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdMeaningfulGoalRequest;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.feature.FeatureName.CRR_STRATEGY_ALLOWED;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdAddTextCampaign;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class CampaignMutationAddCampaignWithMeaningfulGoalWithMetrikaValueSourceTest {
    private static final String MUTATION_NAME = "addCampaigns";
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
            + "    addedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddCampaigns, GdAddCampaignPayload> ADD_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private MetrikaClientStub metrikaClient;

    private User operator;
    private ClientInfo clientInfo;
    private int counterId;
    private int goalId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        counterId = RandomNumberUtils.nextPositiveInteger();
        goalId = RandomNumberUtils.nextPositiveInteger();

        metrikaClient.addUserCounter(clientInfo.getUid(), counterId);
        metrikaClient.addCounterGoal(counterId, goalId);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), CRR_STRATEGY_ALLOWED, true);
    }

    @Test
    public void addTextCampaignWithMeaningfulGoalValueFromMetrika_FeatureEnabled_Success() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA, true);
        GdAddTextCampaign gdAddTextCampaign =
                defaultGdAddTextCampaign(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withMetrikaCounters(List.of(counterId))
                .withMeaningfulGoals(List.of(new GdMeaningfulGoalRequest()
                        .withGoalId((long) goalId)
                        .withIsMetrikaSourceOfValue(true)
                        .withConversionValue(BigDecimal.valueOf(11111))))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_CRR)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withGoalId((long) goalId)
                                .withCrr(100L)))
                .withIsSimplifiedStrategyViewEnabled(true);

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withTextCampaign(gdAddTextCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);

        TextCampaign actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(gdAddCampaignPayload.getAddedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getIsSimplifiedStrategyViewEnabled()).isTrue();
    }

    @Test
    public void addTextCampaignWithMeaningfulGoalValueFromMetrika_FeatureDisabled_HasErrors() {
        GdAddTextCampaign gdAddTextCampaign =
                defaultGdAddTextCampaign(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withMetrikaCounters(List.of(counterId))
                .withMeaningfulGoals(List.of(new GdMeaningfulGoalRequest()
                        .withGoalId((long) goalId)
                        .withIsMetrikaSourceOfValue(true)
                        .withConversionValue(BigDecimal.valueOf(11111))))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_CRR)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withGoalId((long) goalId)
                                .withCrr(100L)))
                .withIsSimplifiedStrategyViewEnabled(true);

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withTextCampaign(gdAddTextCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNotNull();
    }
}
