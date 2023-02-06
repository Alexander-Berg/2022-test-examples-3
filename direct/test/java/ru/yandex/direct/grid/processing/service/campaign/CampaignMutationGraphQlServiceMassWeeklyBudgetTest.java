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

import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageBidStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudget;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudgetShowMode;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsWeeklyBudget;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_WEEKLY_BUDGET;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceMassWeeklyBudgetTest {

    private static final BigDecimal MAX_WEEKLY_BUDGET = Currencies.getCurrency(CurrencyCode.RUB).getMaxAutobudget();

    private static final String MUTATION_NAME = UPDATE_CAMPAIGNS_WEEKLY_BUDGET;

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

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaignsWeeklyBudget, GdUpdateCampaignPayload> UPDATE_WEEKLY_BUDGET_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaignsWeeklyBudget.class, GdUpdateCampaignPayload.class);

    @Autowired
    private GraphQlTestExecutor testExecutor;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    public Steps steps;

    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private User operator;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void testMassSetWeeklyBudget_successSingle() {
        var expectedStrategy = createAverageBidStrategyWithoutDayBudget();
        var activeTextCampaign = TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(expectedStrategy);

        var campaignInfo = steps.campaignSteps().createCampaign(activeTextCampaign, clientInfo);
        var input = createRequest(List.of(campaignInfo), MAX_WEEKLY_BUDGET);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_WEEKLY_BUDGET_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(campaignInfo.getCampaignId());
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));
        var actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                Set.of(campaignInfo.getCampaignId())).get(0);

        SoftAssertions.assertSoftly(soft ->
        {
            soft.assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
            //убедимся что ничего лишнего не обновилось
            soft.assertThat(actualCampaign.getDayBudget())
                    .isEqualByComparingTo(expectedStrategy.getDayBudget().getDayBudget());
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getAvgBid()).isEqualByComparingTo(expectedStrategy.getAverageBid());
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getSum()).isEqualByComparingTo(MAX_WEEKLY_BUDGET);
        });
    }

    @Test
    public void testMassSetWeeklyBudget_successMulti() {
        var expectedStrategy = createAverageBidStrategyWithoutDayBudget();

        //ТГО
        var activeTextCampaign = TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(expectedStrategy);
        var textCampaignInfo = steps.campaignSteps().createCampaign(activeTextCampaign, clientInfo);

        //динамические объявления
        var activeDynamicsCampaign = TestCampaigns.activeDynamicCampaign(null, null)
                .withStrategy(expectedStrategy);
        var dynamicsCampaignInfo = steps.campaignSteps().createCampaign(activeDynamicsCampaign, clientInfo);

        var input =
                createRequest(List.of(textCampaignInfo, dynamicsCampaignInfo), MAX_WEEKLY_BUDGET);
        var actualPayload = testExecutor.doMutationAndGetPayload(UPDATE_WEEKLY_BUDGET_MUTATION, input, operator);

        var textExpectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(textCampaignInfo.getCampaignId());
        var dynamicsExpectedPayloadItem =
                new GdUpdateCampaignPayloadItem().withId(dynamicsCampaignInfo.getCampaignId());

        var actualTextCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                Set.of(textCampaignInfo.getCampaignId())).get(0);
        var actualDynamicsCampaign =
                (DynamicCampaign) campaignTypedRepository.getTypedCampaigns(dynamicsCampaignInfo.getShard(),
                        Set.of(dynamicsCampaignInfo.getCampaignId())).get(0);

        SoftAssertions.assertSoftly(soft ->
        {
            soft.assertThat(actualPayload.getUpdatedCampaigns()).is(matchedBy(containsInAnyOrder(textExpectedPayloadItem,
                    dynamicsExpectedPayloadItem)));

            //ТГО
            soft.assertThat(actualTextCampaign.getDayBudget())
                    .isEqualByComparingTo(expectedStrategy.getDayBudget().getDayBudget());
            soft.assertThat(actualTextCampaign.getStrategy().getStrategyData().getAvgBid()).isEqualByComparingTo(expectedStrategy.getAverageBid());
            soft.assertThat(actualTextCampaign.getStrategy().getStrategyData().getSum()).isEqualByComparingTo(MAX_WEEKLY_BUDGET);

            //ДО
            soft.assertThat(actualDynamicsCampaign.getDayBudget())
                    .isEqualByComparingTo(expectedStrategy.getDayBudget().getDayBudget());
            soft.assertThat(actualDynamicsCampaign.getStrategy().getStrategyData().getAvgBid()).isEqualByComparingTo(expectedStrategy.getAverageBid());
            soft.assertThat(actualDynamicsCampaign.getStrategy().getStrategyData().getSum()).isEqualByComparingTo(MAX_WEEKLY_BUDGET);
        });
    }

    @Test
    public void testMassDisableWeeklyBudget_successSingle() {
        var expectedStrategy = createAverageBidStrategyWithoutDayBudget();
        var activeTextCampaign = TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(expectedStrategy);

        var campaignInfo = steps.campaignSteps().createCampaign(activeTextCampaign, clientInfo);
        var input = createRequest(List.of(campaignInfo), null);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_WEEKLY_BUDGET_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(campaignInfo.getCampaignId());
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));
        var actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                Set.of(campaignInfo.getCampaignId())).get(0);

        SoftAssertions.assertSoftly(soft ->
        {
            soft.assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
            //убедимся что ничего лишнего не обновилось
            soft.assertThat(actualCampaign.getDayBudget())
                    .isEqualByComparingTo(expectedStrategy.getDayBudget().getDayBudget());
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getAvgBid()).isEqualByComparingTo(expectedStrategy.getAverageBid());
            //недельный бюджет не задан
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getSum()).isNull();
        });
    }

    @Test
    public void testMassSetWeeklyBudget_weeklyBudgetMoreThanMaximum_validationError() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var input = createRequest(List.of(campaignInfo),
                MAX_WEEKLY_BUDGET.add(BigDecimal.ONE));
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_WEEKLY_BUDGET_MUTATION, input, operator);

        assertThat(payload.getValidationResult().getErrors()).hasSize(1);
    }

    @Test
    public void testMassSetWeeklyBudget_invalidCampaignId_validationError() {
        var campaignInfo = new CampaignInfo().withCampaign(new Campaign().withId(-1L));
        var input = createRequest(List.of(campaignInfo), MAX_WEEKLY_BUDGET);

        var query = String.format(MUTATION_TEMPLATE, MUTATION_NAME, graphQlSerialize(input));
        var executionResult = processor.processQuery(null, query, null, buildContext(operator));

        assertThat(executionResult.getErrors()).hasSize(1);
    }

    @Test
    public void testMassSetWeeklyBudget_nonAutobudgetCampaign_preValidationError() {
        var expectedStrategy = TestCampaigns.manualStrategy();
        var activeTextCampaign = TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(expectedStrategy);
        var campaignInfo = steps.campaignSteps().createCampaign(activeTextCampaign, clientInfo);
        var input = createRequest(List.of(campaignInfo), MAX_WEEKLY_BUDGET);

        var actualPayload = testExecutor.doMutationAndGetPayload(UPDATE_WEEKLY_BUDGET_MUTATION, input, operator);
        assertThat(actualPayload.getUpdatedCampaigns()).isEmpty();

        //дополнительно убедимся что у кампании не появились настройки автобюджета
        var actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                Set.of(campaignInfo.getCampaignId())).get(0);

        SoftAssertions.assertSoftly(soft ->
        {
            soft.assertThat(actualCampaign.getStrategy().isAutoBudget()).isFalse();
            soft.assertThat(actualCampaign.getDayBudget())
                    .isEqualByComparingTo(expectedStrategy.getDayBudget().getDayBudget());
            soft.assertThat(actualCampaign.getDayBudgetShowMode().name())
                    .isEqualTo(expectedStrategy.getDayBudget().getShowMode().name());
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getSum()).isNull();

        });
    }

    private static GdUpdateCampaignsWeeklyBudget createRequest(List<CampaignInfo> infos,
                                                               BigDecimal weeklyBudget) {
        return new GdUpdateCampaignsWeeklyBudget()
                .withCampaignIds(mapList(infos, CampaignInfo::getCampaignId))
                .withWeeklyBudget(weeklyBudget);
    }

    private static AverageBidStrategy createAverageBidStrategyWithoutDayBudget() {
        return new AverageBidStrategy()
                .withAverageBid(new BigDecimal(15))
                .withMaxWeekSum(new BigDecimal(3000))
                .withDayBudget(new DayBudget()
                        .withDailyChangeCount(0L)
                        .withDayBudget(BigDecimal.ZERO)
                        .withShowMode(DayBudgetShowMode.DEFAULT));
    }
}
