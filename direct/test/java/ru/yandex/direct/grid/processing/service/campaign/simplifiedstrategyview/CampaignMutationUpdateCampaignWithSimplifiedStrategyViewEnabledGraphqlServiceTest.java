package ru.yandex.direct.grid.processing.service.campaign.simplifiedstrategyview;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateTextCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdUpdateDynamicCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdUpdateSmartCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdUpdateTextCampaign;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class CampaignMutationUpdateCampaignWithSimplifiedStrategyViewEnabledGraphqlServiceTest {
    private static final String MUTATION_NAME = "updateCampaigns";
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
    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaigns, GdUpdateCampaignPayload> UPDATE_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private MetrikaClientStub metrikaClient;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;

    private static CampaignAttributionModel defaultAttributionModel;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    @Test
    public void shouldUpdateTextCampaign() {
        var textCampaignInfo = steps.textCampaignSteps().createDefaultCampaign(clientInfo);
        GdUpdateTextCampaign gdUpdateTextCampaign = defaultGdUpdateTextCampaign(textCampaignInfo.getTypedCampaign(),
                defaultAttributionModel);

        gdUpdateTextCampaign.withIsSimplifiedStrategyViewEnabled(false);

        GdUpdateCampaignUnion campaignUnion = new GdUpdateCampaignUnion()
                .withTextCampaign(gdUpdateTextCampaign);

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(textCampaignInfo.getId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();

        TextCampaign actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(gdUpdateCampaignPayload.getUpdatedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getIsSimplifiedStrategyViewEnabled()).isFalse();

    }

    @Test
    public void shouldUpdateSmartCampaign() {
        //given
        var campaignInfo = steps.smartCampaignSteps().createDefaultCampaign(clientInfo);
        var updateSmartCampaign = defaultGdUpdateSmartCampaign(
                campaignInfo.getTypedCampaign(), defaultAttributionModel
        ).withIsSimplifiedStrategyViewEnabled(false);
        var expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(campaignInfo.getId())
        );

        var campaignUnion = new GdUpdateCampaignUnion()
                .withSmartCampaign(updateSmartCampaign);

        var input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        metrikaClient.addUserCounterIds(clientInfo.getUid(), updateSmartCampaign.getMetrikaCounters());

        //when
        var result = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        //then
        assertThat(result.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(result.getValidationResult()).isNull();

        var actualCampaign = (SmartCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(result.getUpdatedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getIsSimplifiedStrategyViewEnabled()).isFalse();
    }

    @Test
    public void shouldUpdateDynamicCampaign() {
        //given
        var campaignInfo = steps.dynamicCampaignSteps().createDefaultCampaign(clientInfo);
        var updateDynamicCampaign = defaultGdUpdateDynamicCampaign(
                campaignInfo.getTypedCampaign(), defaultAttributionModel
        ).withIsSimplifiedStrategyViewEnabled(false);
        var expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(campaignInfo.getId())
        );

        var campaignUnion = new GdUpdateCampaignUnion()
                .withDynamicCampaign(updateDynamicCampaign);

        var input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        //when
        var result = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        //then
        assertThat(result.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(result.getValidationResult()).isNull();

        var actualCampaign = (DynamicCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(result.getUpdatedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getIsSimplifiedStrategyViewEnabled()).isFalse();
    }
}
