package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsTimeTargeting;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.libs.timetarget.TimeTarget;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_TIME_TARGETING;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceMassTimeTargetingTest {
    private static final long KALININGRAD_TIMEZONE_ID = 131L;
    private static final String MUTATION_NAME = UPDATE_CAMPAIGNS_TIME_TARGETING;
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

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaignsTimeTargeting,
            GdUpdateCampaignPayload> UPDATE_GOALS_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaignsTimeTargeting.class, GdUpdateCampaignPayload.class);

    @Autowired
    private GraphQlTestExecutor testExecutor;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    public Steps steps;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private User operator;
    private CampaignInfo validCampaignInfo;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        validCampaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void testSuccess() {
        TimeTarget timeTarget = TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX" +
                "2ABCDEFGHIJKLMNOPQRSTUVWX" +
                "3ABCDEFGHIJKLMNOPQRSTUVWX" +
                "4ABCDE5ABCDE6A7A");

        GdTimeTarget gdTimeTarget = CampaignDataConverter.toGdTimeTarget(timeTarget)
                .withIdTimeZone(KALININGRAD_TIMEZONE_ID);

        var input = createRequest(validCampaignInfo, gdTimeTarget);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(validCampaignInfo.getCampaignId());
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(validCampaignInfo.getShard(),
                        Set.of(validCampaignInfo.getCampaignId())).get(0);

        assertThat(actualCampaign.getTimeTarget()).is(matchedBy(beanDiffer(timeTarget)));
        assertThat(actualCampaign.getTimeZoneId()).is(matchedBy(beanDiffer(KALININGRAD_TIMEZONE_ID)));
    }

    @Test
    public void testWrongRequest() {
        GdTimeTarget gdTimeTarget = defaultGdTimeTarget()
                .withTimeBoard(emptyList());

        GdUpdateCampaignsTimeTargeting input = createRequest(validCampaignInfo, gdTimeTarget);

        var query = String.format(MUTATION_TEMPLATE, MUTATION_NAME, graphQlSerialize(input));
        var executionResult = processor.processQuery(null, query, null, buildContext(operator));

        assertThat(executionResult.getErrors()).hasSize(1);
    }

    @Test
    public void testMassSetTimeTargeting_CpmCampaign_WithFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        TimeTarget timeTarget = TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX" +
                "2ABCDEFGHIJKLMNOPQRSTUVWX" +
                "3ABCDEFGHIJKLMNOPQRSTUVWX" +
                "4ABCDE5ABCDE6A7A");

        GdTimeTarget gdTimeTarget = CampaignDataConverter.toGdTimeTarget(timeTarget)
                .withIdTimeZone(KALININGRAD_TIMEZONE_ID);
        var campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        var input = createRequest(campaignInfo, gdTimeTarget);
        var result = testExecutor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator);

        assertThat(result.getValidationResult().getErrors()).hasSize(1);
        GdDefect defect = new GdDefect()
                .withPath("campaignUpdateItems[0].id")
                .withCode("CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS");
        assertThat(result.getValidationResult().getErrors().get(0)).isEqualTo(defect);
    }

    private static GdUpdateCampaignsTimeTargeting createRequest(CampaignInfo info, GdTimeTarget timeTarget) {
        return new GdUpdateCampaignsTimeTargeting()
                .withCampaignIds(List.of(info.getCampaignId()))
                .withTimeTarget(timeTarget);
    }
}
