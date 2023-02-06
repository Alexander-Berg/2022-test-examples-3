package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.Dialog;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.dialogs.repository.ClientDialogsRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignEmailEvent;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignEmailSettings;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignNotification;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsSettings;
import ru.yandex.direct.grid.model.utils.GridTimeUtils;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilter;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.CampaignNotificationUtils.getAvailableEmailEvents;
import static ru.yandex.direct.core.entity.campaign.CampaignNotificationUtils.getAvailableSmsFlags;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignDataConverter.toGdCampaignCheckPositionInterval;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignDataConverter.toGdCampaignSmsEventInfo;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.isValidId;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;

/**
 * Тест на сервис, проверяем в основном то, что фильтры и сортировки работают.
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignsGraphQlServiceTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    wallets {\n"
            + "      id\n"
            + "      sum\n"
            + "      currency\n"
            + "    }\n"
            + "    campaigns(input: %s) {\n"
            + "      totalCount\n"
            + "      campaignIds\n"
            + "      filter {\n"
            + "        archived\n"
            + "      }\n"
            + "      rowset {\n"
            + "        id\n"
            + "        index\n"
            + "        walletId\n"
            + "        name\n"
            + "        startDate\n"
            + "        endDate\n"
            + "        ... on GdTextCampaign {\n"
            + "          minusKeywords\n"
            + "          allowedPageIds\n"
            + "          abSegmentRetargetingCondition {\n"
            + "              isMetrikaAvailable \n"
            + "          }\n"
            + "          abSegmentStatisticRetargetingCondition {\n"
            + "              isMetrikaAvailable \n"
            + "          }\n"
            + "          enableCompanyInfo\n"
            + "          excludePausedCompetingAds\n"
            + "          clientDialogId\n"
            + "          attributionModel\n"
            + "        }\n"
            + "        notification {\n"
            + "          smsSettings {\n"
            + "            smsTime {\n"
            + "              startTime {\n"
            + "                 hour, minute\n"
            + "              }\n"
            + "              endTime {\n"
            + "                 hour, minute\n"
            + "              }\n"
            + "            }\n"
            + "            events {\n"
            + "              event\n"
            + "              checked\n"
            + "            }\n"
            + "          }\n"
            + "          emailSettings {\n"
            + "            email,\n"
            + "            allowedEvents,\n"
            + "            checkPositionInterval,\n"
            + "            warningBalance,\n"
            + "            sendAccountNews,\n"
            + "            xlsReady,\n"
            + "            stopByReachDailyBudget\n"
            + "          }\n"
            + "        }"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private GdCampaignsContainer campaignsContainer;
    private CampaignInfo campaignInfoOne;
    private CampaignInfo campaignInfoTwo;
    private CampaignInfo campaignInfoWallet;
    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private ClientDialogsRepository clientDialogsRepository;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    @Before
    public void initTestData() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        campaignInfoWallet = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(userInfo.getClientInfo())
                .withCampaign(activeWalletCampaign(null, null)));

        Campaign camp = activeTextCampaign(null, null)
                .withName("Name 2");
        camp.getBalanceInfo()
                .withWalletCid(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);
        campaignInfoOne = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(userInfo.getClientInfo())
                .withCampaign(camp));

        Dialog dialog = steps.dialogSteps().createStandaloneDefaultDialog(userInfo.getClientInfo()).getDialog();
        Campaign campTwo = activeTextCampaign(null, null)
                .withName("Name 0")
                .withStartTime(LocalDate.now())
                .withAllowedPageIds(List.of(11111L))
                .withEnableCompanyInfo(true)  //TODO-perezhoginnik поменять на false и исправить
                .withExcludePausedCompetingAds(true)
                .withFinishTime(LocalDate.now().plusDays(14))
                .withClientDialogId(dialog.getId())
                .withMinusKeywords(List.of("one", "two", "three"));
        campTwo.getBalanceInfo()
                .withWalletCid(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);
        campaignInfoTwo = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(userInfo.getClientInfo())
                .withCampaign(campTwo));
        clientDialogsRepository.addDialogToCampaign(campaignInfoTwo.getShard(), campaignInfoTwo.getCampaignId(),
                dialog.getId());

        Campaign campThree = activeTextCampaign(null, null)
                .withName("Name 1");
        campThree.getBalanceInfo()
                .withWalletCid(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);
        steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(userInfo.getClientInfo())
                .withCampaign(campThree));

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
        TestAuthHelper.setDirectAuthentication(context.getOperator());
        steps.featureSteps().addClientFeature(userInfo.getClientInfo().getClientId(),
                FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, true);
    }


    public static Object[] parameters() {
        return new Object[][]{
                {true},
                {false},
        };
    }

    @Test
    @TestCaseName("use filterKey instead filter: {0}")
    @Parameters(method = "parameters")
    public void testService(boolean replaceFilterToFilterKey) {
        campaignsContainer.getFilter().setCampaignIdIn(
                ImmutableSet.of(campaignInfoOne.getCampaignId(), campaignInfoTwo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

        GdCampaignFilter expectedFilter = campaignsContainer.getFilter();

        if (replaceFilterToFilterKey) {
            String jsonFilter = JsonUtils.toJson(campaignsContainer.getFilter());
            String key = filterShortcutsSteps.saveFilter(campaignInfoOne.getClientId(), jsonFilter);

            campaignsContainer.setFilter(null);
            campaignsContainer.setFilterKey(key);
        }

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "wallets", Collections.singletonList(
                                ImmutableMap.of(
                                        "id", campaignInfoWallet.getCampaignId(),
                                        "sum", Money.valueOf(
                                                campaignInfoWallet.getCampaign().getBalanceInfo().getSum(),
                                                campaignInfoWallet.getClientInfo().getClient().getWorkCurrency()
                                        ).subtractNds(ClientSteps.DEFAULT_NDS).roundToCentDown().bigDecimalValue(),
                                        "currency",
                                        campaignInfoWallet.getCampaign().getBalanceInfo().getCurrency().name()
                                )
                        ),
                        "campaigns", ImmutableMap.of(
                                "totalCount", 2,
                                "filter", ImmutableMap.<String, Object>builder()
                                        .put("archived", expectedFilter.getArchived())
                                        .build(),
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", campaignInfoTwo.getCampaignId())
                                        .put("index", 1)
                                        .put("allowedPageIds", campaignInfoTwo.getCampaign().getAllowedPageIds())
                                        .put("enableCompanyInfo", campaignInfoTwo.getCampaign().getEnableCompanyInfo())
                                        .put("excludePausedCompetingAds",
                                                campaignInfoTwo.getCampaign().getExcludePausedCompetingAds())
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", campaignInfoTwo.getCampaign().getName())
                                        .put("startDate", campaignInfoTwo.getCampaign().getStartTime().toString())
                                        .put("endDate", campaignInfoTwo.getCampaign().getFinishTime().toString())
                                        .put("clientDialogId", campaignInfoTwo.getCampaign().getClientDialogId())
                                        .put("attributionModel",
                                                campaignConstantsService.getDefaultAttributionModel().name())
                                        .put("minusKeywords", campaignInfoTwo.getCampaign().getMinusKeywords())
                                        .build())
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(containsInAnyOrder(campaignInfoOne.getCampaignId(), campaignInfoTwo.getCampaignId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "abSegmentRetargetingCondition"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "abSegmentStatisticRetargetingCondition"))
                .useMatcher(anything());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(campaignInfoTwo.getShard(),
                        Collections.singleton(campaignInfoTwo.getCampaignId()));
        List<TextCampaign> textCampaigns = mapList(typedCampaigns, TextCampaign.class::cast);
        checkCampaignNotificationData(data, textCampaigns.get(0));
    }

    @Test
    public void testService_whenFilterKeyNotFound() {
        campaignsContainer.getFilter().setCampaignIdIn(
                ImmutableSet.of(campaignInfoOne.getCampaignId(), campaignInfoTwo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

        campaignsContainer.setFilter(null);
        campaignsContainer.setFilterKey(RandomStringUtils.randomAlphabetic(10));

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "campaigns", ImmutableMap.of(
                                "totalCount", 0,
                                "filter", ImmutableMap.<String, Object>builder()
                                        .build(),
                                "rowset", List.of(),
                                "campaignIds", List.of()
                        )
                )
        );
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "filter", "archived"))
                .useMatcher(nullValue())
                .forFields(newPath("client", "wallets"))
                .useMatcher(anything());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void checkAllowedPageIdsField_expectGetNoRightsError() {
        steps.featureSteps().addClientFeature(campaignInfoWallet.getClientId(),
                FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, false);
        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(campaignInfoOne.getCampaignId(), campaignInfoTwo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        assertThat(processor.processQuery(null, query, null, context).getErrors())
                .hasSize(1)
                .extracting(GraphQLError::getMessage)
                .allMatch(errorMessage -> errorMessage.endsWith("No rights for field " + GdTextCampaign.ALLOWED_PAGE_IDS.name()));
    }

    @SuppressWarnings("unchecked")
    private static void checkCampaignNotificationData(Map<String, Object> data, TextCampaign actualCampaign) {
        var clientData = (Map<String, Object>) data.get("client");
        var campaignsData = (Map<String, Object>) clientData.get("campaigns");
        var rowset = (List<Map<String, Object>>) campaignsData.get("rowset");
        var notificationData = (Map<String, Object>) rowset.get(0).get("notification");

        var campaignNotification = GraphQlJsonUtils.convertValue(notificationData, GdCampaignNotification.class);
        assertThat(campaignNotification)
                .is(matchedBy(beanDiffer(getExpectedNotificationData(actualCampaign))));
    }

    private static GdCampaignNotification getExpectedNotificationData(TextCampaign actualCampaign) {
        var allowedEvents =
                getAvailableEmailEvents(actualCampaign.getWalletId(), actualCampaign.getType(), null);
        var emailSettings = new GdCampaignEmailSettings()
                .withEmail(actualCampaign.getEmail())
                .withAllowedEvents(mapSet(allowedEvents, GdCampaignEmailEvent::fromSource))
                .withWarningBalance(actualCampaign.getWarningBalance())
                .withXlsReady(actualCampaign.getEnableOfflineStatNotice())
                .withStopByReachDailyBudget(actualCampaign.getEnablePausedByDayBudgetEvent())
                .withSendAccountNews(actualCampaign.getEnableSendAccountNews())
                .withCheckPositionInterval(
                        toGdCampaignCheckPositionInterval(actualCampaign.getCheckPositionIntervalEvent()));

        Set<SmsFlag> availableSmsFlags = getAvailableSmsFlags(isValidId(actualCampaign.getWalletId()), true);
        var smsSettings = new GdCampaignSmsSettings()
                .withEvents(mapSet(availableSmsFlags,
                        flag -> toGdCampaignSmsEventInfo(flag, actualCampaign.getSmsFlags())))
                .withSmsTime(GridTimeUtils.toGdTimeInterval(actualCampaign.getSmsTime()));

        return new GdCampaignNotification()
                .withEmailSettings(emailSettings)
                .withSmsSettings(smsSettings);
    }

}
