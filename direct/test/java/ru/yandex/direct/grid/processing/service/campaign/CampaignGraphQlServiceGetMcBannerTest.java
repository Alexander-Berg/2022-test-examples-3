package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpcPerCamp;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpcPerCamprStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMcBannerCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты на получение данных по ГО/MCBANNER кампании
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignGraphQlServiceGetMcBannerTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        ... on GdMcBannerCampaign {\n"
            + "          id\n"
            + "          name\n"
            + "          metrikaCounters\n"
            + "          meaningfulGoals {\n"
            + "              goalId\n"
            + "          }\n"
            + "          strategy {\n"
            + "             budget {\n"
            + "                 sum\n"
            + "             }\n"
            + "             ... on GdCampaignStrategyAvgCpcPerCamp {\n"
            + "                 avgBid\n"
            + "                 bid\n"
            + "                 platform\n"
            + "                 __typename\n"
            + "             }\n"
            + "          }\n"
            + "          bidModifiers {\n"
            + "              adjustments {\n"
            + "                  percent\n"
            + "              }\n"
            + "          }\n"
            + "          notification {\n"
            + "             emailSettings {\n"
            + "                 email\n"
            + "                 stopByReachDailyBudget\n"
            + "                 xlsReady\n"
            + "             }\n"
            + "          }\n"
            + "          dayBudget\n"
            + "          hasExtendedGeoTargeting\n"
            + "          hasAddOpenstatTagToUrl\n"
            + "          hasAddMetrikaTagToUrl\n"
            + "          minusKeywords\n"
            + "          disabledPlaces\n"
            + "          disabledIps\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private GridContextProvider gridContextProvider;

    private GridGraphQLContext context;
    private UserInfo userInfo;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();

        context = ContextHelper.buildContext(userInfo.getUser()).withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void testGetCampaign() {
        List<Long> metrikaCounters = List.of(5L, 55L);
        String email = "email@ya.ru";
        BigDecimal dayBudget = BigDecimal.valueOf(555.55);
        List<String> minusKeywords = List.of("minus1", "minus2");
        List<String> disabledDomains = List.of("domain1", "domain2");
        List<String> disabledIps = List.of("8.8.8.8", "8.8.8.9");

        TypedCampaignInfo campaignInfo = steps.typedCampaignSteps()
                .createMcBannerCampaign(userInfo, clientInfo, defaultMcBannerCampaignWithSystemFields(clientInfo)
                        .withMetrikaCounters(metrikaCounters)
                        .withEnablePausedByDayBudgetEvent(true)
                        .withEnableOfflineStatNotice(true)
                        .withEmail(email)
                        .withDayBudget(dayBudget)
                        .withHasExtendedGeoTargeting(true)
                        .withHasAddOpenstatTagToUrl(true)
                        .withHasAddMetrikaTagToUrl(true)
                        .withMinusKeywords(minusKeywords)
                        .withDisabledDomains(disabledDomains)
                        .withDisabledIps(disabledIps));

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getCampaign().getId());

        Long campaignId = GraphQLUtils.getDataValue(rowset, "0/id");
        String name = GraphQLUtils.getDataValue(rowset, "0/name");
        List<Integer> actualMetrikaCounters = GraphQLUtils.getDataValue(rowset, "0/metrikaCounters");
        String actualEmail = GraphQLUtils.getDataValue(rowset, "0/notification/emailSettings/email");
        Boolean stopByReachDailyBudget =
                GraphQLUtils.getDataValue(rowset, "0/notification/emailSettings/stopByReachDailyBudget");
        Boolean xlsReady = GraphQLUtils.getDataValue(rowset, "0/notification/emailSettings/xlsReady");
        BigDecimal actualDayBudget = GraphQLUtils.getDataValue(rowset, "0/dayBudget");
        Boolean extendedGeoTargeting = GraphQLUtils.getDataValue(rowset, "0/hasExtendedGeoTargeting");
        Boolean hasAddOpenstatTagToUrl = GraphQLUtils.getDataValue(rowset, "0/hasAddOpenstatTagToUrl");
        Boolean hasAddMetrikaTagToUrl = GraphQLUtils.getDataValue(rowset, "0/hasAddMetrikaTagToUrl");
        List<String> actualMinusKeywords = GraphQLUtils.getDataValue(rowset, "0/minusKeywords");
        List<String> actualDisabledDomains = GraphQLUtils.getDataValue(rowset, "0/disabledPlaces");
        List<String> actualDisabledIps = GraphQLUtils.getDataValue(rowset, "0/disabledIps");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(campaignId).as("id кампании")
                    .isEqualTo(campaignInfo.getCampaign().getId());
            soft.assertThat(name).as("название кампании")
                    .isEqualTo(campaignInfo.getCampaign().getName());
            soft.assertThat(actualMetrikaCounters).as("счетчики метрики")
                    .containsOnlyElementsOf(mapList(metrikaCounters, Long::intValue));
            soft.assertThat(actualEmail).as("email")
                    .isEqualTo(email);
            soft.assertThat(stopByReachDailyBudget).as("остановка по достижении дневного бюджета")
                    .isTrue();
            soft.assertThat(xlsReady).as("готовность XLS-отчетов")
                    .isTrue();
            soft.assertThat(actualDayBudget).as("дневной бюджет")
                    .isEqualTo(dayBudget);
            soft.assertThat(extendedGeoTargeting).as("расширенный географический таргетинг")
                    .isTrue();
            soft.assertThat(hasAddOpenstatTagToUrl).as("добавить метку _openstat к ссылкам")
                    .isTrue();
            soft.assertThat(hasAddMetrikaTagToUrl).as("размечать ссылки для Метрики")
                    .isTrue();
            soft.assertThat(actualMinusKeywords).as("минус-фразы")
                    .containsOnlyElementsOf(minusKeywords);
            soft.assertThat(actualDisabledDomains).as("запрещенные площадки")
                    .containsOnlyElementsOf(disabledDomains);
            soft.assertThat(actualDisabledIps).as("запрещение показов по IP-адресам")
                    .containsOnlyElementsOf(disabledIps);
        });
    }

    @Test
    public void testGetCampaign_WithCpcPerCampStrategy() {
        BigDecimal avgBid = BigDecimal.valueOf(100.5);
        BigDecimal bid = BigDecimal.valueOf(101.1);
        BigDecimal sum = BigDecimal.valueOf(5000);

        TypedCampaignInfo campaignInfo = steps.typedCampaignSteps().createMcBannerCampaign(userInfo, clientInfo,
                defaultMcBannerCampaignWithSystemFields(clientInfo)
                        .withStrategy((DbStrategy) defaultAverageCpcPerCamprStrategy(avgBid, bid, sum)
                                .withPlatform(CampaignsPlatform.SEARCH)
                                .withAutobudget(CampaignsAutobudget.YES)));

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getCampaign().getId());

        BigDecimal strategySum = GraphQLUtils.getDataValue(rowset, "0/strategy/budget/sum");
        BigDecimal strategyAvgBid = GraphQLUtils.getDataValue(rowset, "0/strategy/avgBid");
        BigDecimal strategyBid = GraphQLUtils.getDataValue(rowset, "0/strategy/bid");
        String strategyPlatform = GraphQLUtils.getDataValue(rowset, "0/strategy/platform");
        String strategyType = GraphQLUtils.getDataValue(rowset, "0/strategy/__typename");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(strategySum).as("недельный бюджет")
                    .isEqualTo(sum);
            soft.assertThat(strategyAvgBid).as("cpc")
                    .isEqualTo(avgBid);
            soft.assertThat(strategyBid).as("максимальная цена клика")
                    .isEqualTo(bid);
            soft.assertThat(strategyPlatform).as("на поиске")
                    .isEqualTo(CampaignsPlatform.SEARCH.name());
            soft.assertThat(strategyType).as("тип стратегии")
                    .isEqualTo(GdCampaignStrategyAvgCpcPerCamp.class.getSimpleName());
        });
    }

    @Test
    public void testGetCampaign_WithoutMetrikaCounters() {
        TypedCampaignInfo campaignInfo = steps.typedCampaignSteps().createMcBannerCampaign(userInfo, clientInfo,
                defaultMcBannerCampaignWithSystemFields(clientInfo)
                        .withMetrikaCounters(emptyList()));

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getCampaign().getId());

        List<Long> metrikaCounters = GraphQLUtils.getDataValue(rowset, "0/metrikaCounters");
        assertThat(metrikaCounters).as("счетчики метрики")
                .isNull();
    }

    @Test
    public void testGetCampaign_WithMeaningfulGoals() {
        List<MeaningfulGoal> meaningfulGoals = List.of(new MeaningfulGoal()
                .withGoalId(ENGAGED_SESSION_GOAL_ID)
                .withConversionValue(BigDecimal.ONE));

        TypedCampaignInfo campaignInfo = steps.typedCampaignSteps()
                .createMcBannerCampaign(userInfo, clientInfo, defaultMcBannerCampaignWithSystemFields(clientInfo)
                        .withMeaningfulGoals(meaningfulGoals));

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getCampaign().getId());

        List<Object> actualMeaningfulGoals = GraphQLUtils.getDataValue(rowset, "0/meaningfulGoals");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualMeaningfulGoals).as("ключевые цели")
                    .hasSize(1);

            Long meaningfulGoal = GraphQLUtils.getDataValue(actualMeaningfulGoals, "0/goalId");
            soft.assertThat(meaningfulGoal).as("ключевая цель")
                    .isEqualTo(ENGAGED_SESSION_GOAL_ID);
        });
    }

    @Test
    public void testGetCampaign_WithBidModifiers() {
        Integer bidModifierPercent = 15;
        BidModifierDemographics bidModifier = new BidModifierDemographics()
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withDemographicsAdjustments(List.of(new BidModifierDemographicsAdjustment()
                        .withPercent(bidModifierPercent)
                        .withAge(AgeType._0_17)
                        .withGender(GenderType.FEMALE)));

        TypedCampaignInfo campaignInfo = steps.typedCampaignSteps()
                .createMcBannerCampaign(userInfo, clientInfo, defaultMcBannerCampaignWithSystemFields(clientInfo)
                        .withBidModifiers(List.of(bidModifier)));

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getCampaign().getId());

        List<Object> actualBidModifiers = GraphQLUtils.getDataValue(rowset, "0/bidModifiers");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualBidModifiers).as("Корректировки ставок")
                    .hasSize(1);

            Object actualPercent = GraphQLUtils.getDataValue(actualBidModifiers, "0/adjustments/0/percent");
            soft.assertThat(actualPercent).as("процент к ставке")
                    .isEqualTo(bidModifierPercent);
        });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> sendRequestAndGetRowset(Long campaignId) {
        GdCampaignsContainer campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsContainer.getFilter().setCampaignIdIn(singleton(campaignId));

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));

        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Map<String, Object> clientData = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("client");
        Map<String, Object> adGroupsData = (Map<String, Object>) clientData.get("campaigns");
        return (List<Map<String, Object>>) adGroupsData.get("rowset");
    }
}
