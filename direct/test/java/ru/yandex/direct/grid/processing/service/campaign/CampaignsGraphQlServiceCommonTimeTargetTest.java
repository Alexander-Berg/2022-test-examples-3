package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import graphql.ExecutionResult;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsCommonTimeTarget;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsTimeTargeting;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.test.utils.differ.AlwaysEqualsDiffer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toTimeTarget;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_TIME_TARGETING;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignsGraphQlService.CAMPAIGNS_COMMON_TIME_TARGET;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQlServiceCommonTimeTargetTest {
    private static final String MUTATION_NAME = UPDATE_CAMPAIGNS_TIME_TARGETING;
    private static final String QUERY_NAME = CAMPAIGNS_COMMON_TIME_TARGET;
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
    private static final String QUERY_TEMPLATE = "{\n" +
            "   campaignsCommonTimeTarget(input: %s) {\n" +
            "        idTimeZone\n" +
            "        timeBoard\n" +
            "        enabledHolidaysMode" +
            "        holidaysSettings {" +
            "           isShow" +
            "           startHour" +
            "           endHour" +
            "           rateCorrections" +
            "        }" +
            "        useWorkingWeekends" +
            "    }\n" +
            "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaignsTimeTargeting,
            GdUpdateCampaignPayload> UPDATE_GOALS_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaignsTimeTargeting.class, GdUpdateCampaignPayload.class);
    private static final DefaultCompareStrategy COMPARE_STRATEGY = onlyExpectedFields().forFields(newPath("originalTimeTarget")).useDiffer(new AlwaysEqualsDiffer());

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
    private static final TimeTarget CUSTOM_TIME_TARGET = TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX" +
            "2ABCDEFGHIJKLMNOPQRSTUVWX" +
            "3ABCDEFGHIJKLMNOPQRSTUVWX" +
            "4ABCDE5ABCDE6A7A");
    private static final TimeTarget DEFAULT_TIME_TARGET = toTimeTarget(defaultGdTimeTarget());
    private CampaignInfo campaignWithDefaultTimeTarget;
    private CampaignInfo campaignWithCustomTimeTarget1;
    private CampaignInfo campaignWithCustomTimeTarget2;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        campaignWithDefaultTimeTarget = steps.campaignSteps().createCampaign(
                activeTextCampaign(null, null)
                        .withTimeTarget(DEFAULT_TIME_TARGET), clientInfo);
        campaignWithCustomTimeTarget1 = steps.campaignSteps().createCampaign(
                activeTextCampaign(null, null)
                        .withTimeTarget(CUSTOM_TIME_TARGET), clientInfo);
        campaignWithCustomTimeTarget2 = steps.campaignSteps().createCampaign(
                activeTextCampaign(null, null)
                        .withTimeTarget(CUSTOM_TIME_TARGET), clientInfo);

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void test_OneDefaultTimeTarget() {
        var request = new GdCampaignsCommonTimeTarget()
                .withCampaignIds(Set.of(campaignWithDefaultTimeTarget.getCampaignId()));

        String query = String.format(QUERY_TEMPLATE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        Assertions.assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data).containsOnlyKeys(QUERY_NAME);

        var typeReference = new TypeReference<GdTimeTarget>() {
        };
        GdTimeTarget payload = GraphQlJsonUtils.convertValue(data.get(QUERY_NAME), typeReference);

        assertThat(toTimeTarget(payload)).is(matchedBy(beanDiffer(DEFAULT_TIME_TARGET).useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void test_OneCustomTimeTarget() {
        var request = new GdCampaignsCommonTimeTarget()
                .withCampaignIds(Set.of(campaignWithCustomTimeTarget1.getCampaignId()));

        String query = String.format(QUERY_TEMPLATE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        Assertions.assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data).containsOnlyKeys(QUERY_NAME);

        var typeReference = new TypeReference<GdTimeTarget>() {
        };
        GdTimeTarget payload = GraphQlJsonUtils.convertValue(data.get(QUERY_NAME), typeReference);

        assertThat(toTimeTarget(payload)).is(matchedBy(beanDiffer(CUSTOM_TIME_TARGET).useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void test_TwoWithDifferentTimeTarget() {
        var request = new GdCampaignsCommonTimeTarget()
                .withCampaignIds(Set.of(campaignWithDefaultTimeTarget.getCampaignId(),
                        campaignWithCustomTimeTarget1.getCampaignId()));

        String query = String.format(QUERY_TEMPLATE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        Assertions.assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data).containsOnlyKeys(QUERY_NAME);

        var typeReference = new TypeReference<GdTimeTarget>() {
        };
        GdTimeTarget payload = GraphQlJsonUtils.convertValue(data.get(QUERY_NAME), typeReference);

        var actualTimeTarget = toTimeTarget(payload);
        assertThat(actualTimeTarget).is(matchedBy(beanDiffer(DEFAULT_TIME_TARGET).useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void test_TwoWithEqualTimeTarget() {
        var request = new GdCampaignsCommonTimeTarget()
                .withCampaignIds(Set.of(campaignWithCustomTimeTarget1.getCampaignId(),
                        campaignWithCustomTimeTarget2.getCampaignId()));

        String query = String.format(QUERY_TEMPLATE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        Assertions.assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data).containsOnlyKeys(QUERY_NAME);

        var typeReference = new TypeReference<GdTimeTarget>() {
        };
        GdTimeTarget payload = GraphQlJsonUtils.convertValue(data.get(QUERY_NAME), typeReference);

        assertThat(toTimeTarget(payload)).is(matchedBy(beanDiffer(CUSTOM_TIME_TARGET).useCompareStrategy(COMPARE_STRATEGY)));
    }
}
