package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsDayBudget;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdDayBudgetShowMode;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_DAY_BUDJET;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceMassDayBudgetTest {
    private static final BigDecimal DAY_BUDGET = Currencies.getCurrency(CurrencyCode.RUB).getMinDayBudget();
    private static final String MUTATION_NAME = UPDATE_CAMPAIGNS_DAY_BUDJET;
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

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaignsDayBudget, GdUpdateCampaignPayload> UPDATE_CAMPAIGNS_DAY_BUDGET_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaignsDayBudget.class, GdUpdateCampaignPayload.class);

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
    public CampaignTypedRepository campaignTypedRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private User operator;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void testMassSetDayBudget_successSingle() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var input = createRequest(List.of(campaignInfo), DAY_BUDGET, DayBudgetShowMode.STRETCHED);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_DAY_BUDGET_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(campaignInfo.getCampaignId());
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));
        var actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                Set.of(campaignInfo.getCampaignId())).get(0);

        SoftAssertions.assertSoftly(soft ->
        {
            soft.assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
            soft.assertThat(actualCampaign.getDayBudget()).isEqualByComparingTo(DAY_BUDGET);
            soft.assertThat(actualCampaign.getDayBudgetShowMode()).isEqualTo(DayBudgetShowMode.STRETCHED);
        });
    }

    @Test
    public void testMassSetDayBudget_successMulti() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var anotherCampaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var input = createRequest(List.of(campaignInfo, anotherCampaignInfo), DAY_BUDGET, DayBudgetShowMode.STRETCHED);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_DAY_BUDGET_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(campaignInfo.getCampaignId());
        var anotherExpectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(anotherCampaignInfo.getCampaignId());
        var actualCampaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                Set.of(campaignInfo.getCampaignId(), anotherCampaignInfo.getCampaignId()));

        SoftAssertions.assertSoftly(soft ->
        {
            soft.assertThat(payload.getUpdatedCampaigns()).is(matchedBy(containsInAnyOrder(expectedPayloadItem,
                    anotherExpectedPayloadItem)));
            soft.assertThat(actualCampaigns).allMatch(baseCampaign ->
                    ((TextCampaign) baseCampaign).getDayBudget().compareTo(DAY_BUDGET) == 0);
            soft.assertThat(actualCampaigns).allMatch(baseCampaign ->
                    ((TextCampaign) baseCampaign).getDayBudgetShowMode().equals(DayBudgetShowMode.STRETCHED));
        });
    }

    @Test
    public void testMassSetDayBudget_validationError() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var input = createRequest(List.of(campaignInfo), DAY_BUDGET.subtract(BigDecimal.TEN),
                DayBudgetShowMode.STRETCHED);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_DAY_BUDGET_MUTATION, input, operator);

        assertThat(payload.getValidationResult().getErrors()).hasSize(1);
    }

    @Test
    public void testMassSetDayBudget_incorrectRequest() {
        var campaignInfo = new CampaignInfo().withCampaign(new Campaign().withId(-1L));
        var input = createRequest(List.of(campaignInfo),
                DAY_BUDGET,
                DayBudgetShowMode.DEFAULT_);

        var query = String.format(MUTATION_TEMPLATE, MUTATION_NAME, graphQlSerialize(input));
        var executionResult = processor.processQuery(null, query, null, buildContext(operator));

        assertThat(executionResult.getErrors()).hasSize(1);
    }

    @Test
    public void testMassSetDayBudget_CpmCampaign_WithFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        var campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        var input = createRequest(List.of(campaignInfo), DAY_BUDGET.subtract(BigDecimal.TEN),
                DayBudgetShowMode.STRETCHED);
        var result = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_DAY_BUDGET_MUTATION, input, operator);

        assertThat(result.getValidationResult().getErrors()).hasSize(1);
        GdDefect defect = new GdDefect()
                .withPath("campaignUpdateItems[0].id")
                .withCode("CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS");
        assertThat(result.getValidationResult().getErrors().get(0)).isEqualTo(defect);
    }

    private static GdUpdateCampaignsDayBudget createRequest(List<CampaignInfo> infos,
                                                            BigDecimal dayBudget, DayBudgetShowMode showMode) {
        return new GdUpdateCampaignsDayBudget()
                .withCampaignIds(mapList(infos, CampaignInfo::getCampaignId))
                .withDayBudget(dayBudget)
                .withDayBudgetShowMode(toGdDayBudgetShowMode(showMode));
    }

}
