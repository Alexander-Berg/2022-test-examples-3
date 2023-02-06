package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
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
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignOpts;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.core.entity.campaign.model.Dialog;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.client.model.ClientNds;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.dialogs.repository.ClientDialogsRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignSource;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignCheckPositionInterval;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignEmailEvent;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignEmailSettings;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignNotification;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsSettings;
import ru.yandex.direct.grid.model.utils.GridTimeUtils;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.EDA;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.SERVICE;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.entity.campaign.CampaignNotificationUtils.getAvailableEmailEvents;
import static ru.yandex.direct.core.entity.campaign.CampaignNotificationUtils.getAvailableSmsFlags;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.RECOMMENDATIONS_MANAGEMENT_ENABLED;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.DYNAMIC;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.MCBANNER;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.MOBILE_CONTENT;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.PERFORMANCE;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE_MOBILE;
import static ru.yandex.direct.core.testing.data.TestCampaigns.DEFAULT_BANNER_HREF_PARAMS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMcBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMobileContentCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns.fullContentPromotionCampaign;
import static ru.yandex.direct.currency.MoneyUtils.subtractPercentPrecisely;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CONTENT_PROMOTION_COLLECTION;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CONTENT_PROMOTION_EDA;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CONTENT_PROMOTION_SERVICE;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CONTENT_PROMOTION_VIDEO;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CPM_YNDX_FRONTPAGE;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.TEXT;
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
public class CampaignGraphQlServiceTest {

    public static Object[] parameters() {
        return new Object[][]{
                {true},
                {false}
        };
    }

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
            + "      rowset {\n"
            + "        id\n"
            + "        isRecommendationsManagementEnabled\n"
            + "        availableAdGroupTypes\n"
            + "        index\n"
            + "        walletId\n"
            + "        name\n"
            + "        startDate\n"
            + "        endDate\n"
            + "        groupsCount\n"
            + "        source\n"
            + "        ... on GdTextCampaign {\n"
            + "          placementTypes\n"
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
            + "        ... on GdMcBannerCampaign {\n"
            + "          minusKeywords\n"
            + "          abSegmentRetargetingCondition {\n"
            + "              isMetrikaAvailable \n"
            + "          }\n"
            + "          abSegmentStatisticRetargetingCondition {\n"
            + "              isMetrikaAvailable \n"
            + "          }\n"
            + "          attributionModel\n"
            + "        }\n"
            + "        ... on GdDynamicCampaign {\n"
            + "          placementTypes\n"
            + "          minusKeywords\n"
            + "          allowedPageIds\n"
            + "          abSegmentRetargetingCondition {\n"
            + "              isMetrikaAvailable \n"
            + "          }\n"
            + "          abSegmentStatisticRetargetingCondition {\n"
            + "              isMetrikaAvailable \n"
            + "          }\n"
            + "          enableCompanyInfo\n"
            + "          attributionModel\n"
            + "        }\n"
            + "        ... on GdMobileContentCampaign {\n"
            + "          minusKeywords\n"
            + "          allowedPageIds\n"
            + "        }\n"
            + "        ... on GdSmartCampaign {\n"
            + "          minusKeywords\n"
            + "          abSegmentRetargetingCondition {\n"
            + "              isMetrikaAvailable \n"
            + "          }\n"
            + "          abSegmentStatisticRetargetingCondition {\n"
            + "              isMetrikaAvailable \n"
            + "          }\n"
            + "          attributionModel\n"
            + "          bannerHrefParams\n"
            + "        }\n"
            + "        ... on GdContentPromotionCampaign {\n"
            + "          minusKeywords\n"
            + "          attributionModel\n"
            + "        }\n"
            + "        ... on GdCpmYndxFrontpageCampaign {\n"
            + "          allowedFrontpageType\n"
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
            + "      totalCampaigns {\n"
            + "        totalSumRest\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private GdCampaignsContainer campaignsContainer;
    private CampaignInfo campaignInfoOne;
    private CampaignInfo campaignInfoTwo;
    private CampaignInfo campaignInfoWallet;
    private ContentPromotionCampaignInfo contentPromotionCampaignInfo;
    private CampaignInfo dynamicCampaignInfo;
    private CampaignInfo mobileContentCampaignInfo;
    private ContentPromotionCampaignInfo anotherContentPromotionCampaignInfo;
    private ContentPromotionCampaignInfo contentPromotionServiceCampaignInfo;
    private ContentPromotionCampaignInfo contentPromotionEdaCampaignInfo;
    private CampaignInfo cpmYndxFrontpageCampaignInfo;
    private CampaignInfo mcBannerCampaignInfoOne;
    private CampaignInfo mcBannerCampaignInfoTwo;
    private GridGraphQLContext context;
    private ClientNds clientNds;

    private static CampaignAttributionModel defaultAttributionModel;

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

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
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private ClientNdsService clientNdsService;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Before
    public void initTestData() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        campaignInfoWallet = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(activeWalletCampaign(null, null)));

        clientNds = clientNdsService
                .getEffectiveClientNds(clientInfo.getClientId(), null, false);

        Campaign camp = activeTextCampaign(null, null)
                .withName("Name 2");
        camp.getBalanceInfo()
                .withWalletCid(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.TEN)
                .withSumSpent(BigDecimal.ONE);
        campaignInfoOne = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(camp));

        Dialog dialog = steps.dialogSteps().createStandaloneDefaultDialog(clientInfo).getDialog();
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
                .withClientInfo(clientInfo)
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
                .withClientInfo(clientInfo)
                .withCampaign(campThree));

        Campaign mcBannerCamp1 = activeMcBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withName("mcBanner camp 1");
        mcBannerCamp1.getBalanceInfo()
                .withWalletCid(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.TEN)
                .withSumSpent(BigDecimal.ONE);
        mcBannerCampaignInfoOne = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(mcBannerCamp1));

        Campaign mcBannerCamp2 = activeMcBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withName("mcBanner camp 2")
                .withStartTime(LocalDate.now())
                .withFinishTime(LocalDate.now().plusDays(14))
                .withMinusKeywords(List.of("one", "two", "three"));
        mcBannerCamp2.getBalanceInfo()
                .withWalletCid(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);
        mcBannerCampaignInfoTwo = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(mcBannerCamp2));

        var contentPromotionCampaign = fullContentPromotionCampaign()
                .withName("Content promotion")
                .withEndDate(LocalDate.now().plusDays(14))
                .withMinusKeywords(List.of("one", "two", "three"));
        var anotherContentPromotionCampaign = fullContentPromotionCampaign()
                .withName("Content promotion")
                .withEndDate(LocalDate.now().plusDays(14))
                .withMinusKeywords(List.of("one", "two", "three"));
        var contentPromotionServiceCampaign = fullContentPromotionCampaign()
                .withName("Content promotion service")
                .withEndDate(LocalDate.now().plusDays(14))
                .withMinusKeywords(List.of("one", "two", "three"));
        var contentPromotionEdaCampaign = fullContentPromotionCampaign()
                .withName("Content promotion eda")
                .withEndDate(LocalDate.now().plusDays(14))
                .withMinusKeywords(List.of("one", "two", "three"));
        contentPromotionEdaCampaign
                .withWalletId(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);
        contentPromotionCampaign
                .withWalletId(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);
        contentPromotionServiceCampaign
                .withWalletId(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);

        contentPromotionCampaignInfo = steps.contentPromotionCampaignSteps().createCampaign(clientInfo,
                contentPromotionCampaign);
        anotherContentPromotionCampaignInfo = steps.contentPromotionCampaignSteps().createCampaign(clientInfo,
                anotherContentPromotionCampaign);
        contentPromotionServiceCampaignInfo = steps.contentPromotionCampaignSteps().createCampaign(clientInfo,
                contentPromotionServiceCampaign);
        contentPromotionEdaCampaignInfo = steps.contentPromotionCampaignSteps().createCampaign(clientInfo,
                contentPromotionEdaCampaign);
        Campaign dynamicCampaign = activeDynamicCampaign(null, null)
                .withName("Dynamic campaign")
                .withMinusKeywords(List.of("one", "two", "three"))
                .withAllowedPageIds(List.of(11111L))
                .withEnableCompanyInfo(true)
                .withStartTime(LocalDate.now())
                .withFinishTime(LocalDate.now().plusDays(14));
        dynamicCampaign.getBalanceInfo()
                .withWalletCid(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);
        dynamicCampaignInfo = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(dynamicCampaign));

        Campaign mobileContentCampaign = activeMobileContentCampaign(null, null)
                .withName("Mobile Content Campaign")
                .withMinusKeywords(List.of("one", "two", "three"))
                .withAllowedPageIds(List.of(11111L))
                .withStartTime(LocalDate.now())
                .withFinishTime(LocalDate.now().plusDays(14));
        mobileContentCampaign.getBalanceInfo()
                .withWalletCid(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);
        mobileContentCampaignInfo = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(mobileContentCampaign));

        Campaign cpmYndxFrontpageCampaign = activeCpmYndxFrontpageCampaign(null, null)
                .withName("cpmYndxFrontpageCampaign")
                .withFinishTime(LocalDate.now().plusDays(14));
        cpmYndxFrontpageCampaign.getBalanceInfo()
                .withWalletCid(campaignInfoWallet.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);

        cpmYndxFrontpageCampaignInfo = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(cpmYndxFrontpageCampaign));
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                cpmYndxFrontpageCampaignInfo.getShard(),
                cpmYndxFrontpageCampaignInfo.getCampaignId(), ImmutableSet.of(FRONTPAGE, FRONTPAGE_MOBILE));

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();

        context = ContextHelper.buildContext(clientInfo.getChiefUserInfo().getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
        TestAuthHelper.setDirectAuthentication(context.getOperator());
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, true);

        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService(boolean recommendationManagementEnabled) {
        setRecommendationManagement(
                campaignInfoTwo.getShard(),
                campaignInfoTwo.getCampaignId(),
                recommendationManagementEnabled
        );
        campaignsContainer.getFilter().setCampaignIdIn(
                ImmutableSet.of(campaignInfoOne.getCampaignId(), campaignInfoTwo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

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
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", campaignInfoTwo.getCampaignId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes", ImmutableList.of(TEXT.name()))
                                        .put("index", 1)
                                        .put("allowedPageIds", campaignInfoTwo.getCampaign().getAllowedPageIds())
                                        .put("enableCompanyInfo", campaignInfoTwo.getCampaign().getEnableCompanyInfo())
                                        .put("excludePausedCompetingAds",
                                                campaignInfoTwo.getCampaign().getExcludePausedCompetingAds())
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", campaignInfoTwo.getCampaign().getName())
                                        .put("startDate", campaignInfoTwo.getCampaign().getStartTime().toString())
                                        .put("endDate", campaignInfoTwo.getCampaign().getFinishTime().toString())
                                        .put("groupsCount", 0L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("clientDialogId", campaignInfoTwo.getCampaign().getClientDialogId())
                                        .put("attributionModel", defaultAttributionModel.name())
                                        .put("minusKeywords", campaignInfoTwo.getCampaign().getMinusKeywords())
                                        .put("placementTypes", emptyList())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest", getTotalSum(List.of(campaignInfoOne, campaignInfoTwo)))
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
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(campaignInfoTwo.getShard(),
                        Collections.singleton(campaignInfoTwo.getCampaignId()));
        List<TextCampaign> textCampaigns = mapList(typedCampaigns, TextCampaign.class::cast);
        checkCampaignNotificationData(data, textCampaigns.get(0));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_McBannerCampaign(boolean recommendationManagementEnabled) {

        campaignsContainer.getFilter().setCampaignIdIn(
                ImmutableSet.of(mcBannerCampaignInfoOne.getCampaignId(), mcBannerCampaignInfoTwo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

        setRecommendationManagement(
                mcBannerCampaignInfoTwo.getShard(),
                mcBannerCampaignInfoTwo.getCampaignId(),
                recommendationManagementEnabled
        );

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        Campaign campaignWallet = campaignInfoWallet.getCampaign();
        Campaign campaign2 = mcBannerCampaignInfoTwo.getCampaign();

        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "wallets", Collections.singletonList(
                                ImmutableMap.of(
                                        "id", campaignWallet.getId(),
                                        "sum", Money.valueOf(campaignWallet.getBalanceInfo().getSum(),
                                                campaignInfoWallet.getClientInfo().getClient().getWorkCurrency()
                                        ).subtractNds(ClientSteps.DEFAULT_NDS).roundToCentDown().bigDecimalValue(),
                                        "currency", campaignWallet.getBalanceInfo().getCurrency().name()
                                )
                        ),
                        "campaigns", ImmutableMap.of(
                                "totalCount", 2,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", campaign2.getId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes", ImmutableList.of(MCBANNER.name()))
                                        .put("index", 1)
                                        .put("walletId", campaignWallet.getId())
                                        .put("name", campaign2.getName())
                                        .put("startDate", campaign2.getStartTime().toString())
                                        .put("endDate", campaign2.getFinishTime().toString())
                                        .put("groupsCount", 0L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("attributionModel", defaultAttributionModel.name())
                                        .put("minusKeywords", campaign2.getMinusKeywords())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest",
                                        getTotalSum(List.of(mcBannerCampaignInfoOne, mcBannerCampaignInfoTwo)))
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(containsInAnyOrder(mcBannerCampaignInfoOne.getCampaignId(), campaign2.getId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "abSegmentRetargetingCondition"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "abSegmentStatisticRetargetingCondition"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(mcBannerCampaignInfoTwo.getShard(),
                        Collections.singleton(campaign2.getId()));
        List<McBannerCampaign> campaigns = mapList(typedCampaigns, McBannerCampaign.class::cast);
        checkCampaignNotificationData(data, campaigns.get(0));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_DynamicCampaign(boolean recommendationManagementEnabled) {
        setRecommendationManagement(
                dynamicCampaignInfo.getShard(),
                dynamicCampaignInfo.getCampaignId(),
                recommendationManagementEnabled
        );
        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(dynamicCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);
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
                                "totalCount", 1,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", dynamicCampaignInfo.getCampaignId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes", ImmutableList.of(DYNAMIC.name()))
                                        .put("index", 0)
                                        .put("allowedPageIds", dynamicCampaignInfo.getCampaign().getAllowedPageIds())
                                        .put("enableCompanyInfo",
                                                dynamicCampaignInfo.getCampaign().getEnableCompanyInfo())
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", dynamicCampaignInfo.getCampaign().getName())
                                        .put("startDate", dynamicCampaignInfo.getCampaign().getStartTime().toString())
                                        .put("endDate", dynamicCampaignInfo.getCampaign().getFinishTime().toString())
                                        .put("groupsCount", 0L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("attributionModel", defaultAttributionModel.name())
                                        .put("minusKeywords", dynamicCampaignInfo.getCampaign().getMinusKeywords())
                                        .put("placementTypes", emptyList())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest", getTotalSum(List.of(dynamicCampaignInfo)))
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(contains(dynamicCampaignInfo.getCampaignId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "abSegmentRetargetingCondition"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "abSegmentStatisticRetargetingCondition"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(dynamicCampaignInfo.getShard(),
                        Collections.singleton(dynamicCampaignInfo.getCampaignId()));
        List<DynamicCampaign> dynamicCampaigns = mapList(typedCampaigns, DynamicCampaign.class::cast);
        checkCampaignNotificationData(data, dynamicCampaigns.get(0));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_MobileContentCampaign(boolean recommendationManagementEnabled) {
        setRecommendationManagement(
                mobileContentCampaignInfo.getShard(),
                mobileContentCampaignInfo.getCampaignId(),
                recommendationManagementEnabled
        );
        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(mobileContentCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);
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
                                "totalCount", 1,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", mobileContentCampaignInfo.getCampaignId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes", ImmutableList.of(MOBILE_CONTENT.name()))
                                        .put("index", 0)
                                        .put("allowedPageIds",
                                                mobileContentCampaignInfo.getCampaign().getAllowedPageIds())
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", mobileContentCampaignInfo.getCampaign().getName())
                                        .put("startDate",
                                                mobileContentCampaignInfo.getCampaign().getStartTime().toString())
                                        .put("endDate",
                                                mobileContentCampaignInfo.getCampaign().getFinishTime().toString())
                                        .put("groupsCount", 0L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("minusKeywords",
                                                mobileContentCampaignInfo.getCampaign().getMinusKeywords())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest", getTotalSum(List.of(mobileContentCampaignInfo)))
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(contains(mobileContentCampaignInfo.getCampaignId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(mobileContentCampaignInfo.getShard(),
                        Collections.singleton(mobileContentCampaignInfo.getCampaignId()));
        List<MobileContentCampaign> mobileContentCampaigns = mapList(typedCampaigns, MobileContentCampaign.class::cast);
        checkCampaignNotificationData(data, mobileContentCampaigns.get(0));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_SmartCampaign(boolean recommendationManagementEnabled) {
        SmartCampaign smartCampaign = createSmartCampaign(campaignInfoWallet.getClientInfo());
        setRecommendationManagement(
                campaignInfoWallet.getShard(),
                smartCampaign.getId(),
                recommendationManagementEnabled
        );

        campaignsContainer.getFilter().setCampaignIdIn(Set.of(smartCampaign.getId()));
        campaignsContainer.getLimitOffset().setOffset(0);
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors()).isEmpty();
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
                                "totalCount", 1,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", smartCampaign.getId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes", ImmutableList.of(PERFORMANCE.name()))
                                        .put("index", 0)
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", smartCampaign.getName())
                                        .put("startDate", smartCampaign.getStartDate().toString())
                                        .put("endDate", smartCampaign.getEndDate().toString())
                                        .put("groupsCount", 0L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("attributionModel", defaultAttributionModel.name())
                                        .put("minusKeywords", smartCampaign.getMinusKeywords())
                                        .put("bannerHrefParams", smartCampaign.getBannerHrefParams())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of("totalSumRest", BigDecimal.ZERO)
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(contains(smartCampaign.getId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "abSegmentRetargetingCondition"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "abSegmentStatisticRetargetingCondition"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useMatcher(anything());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(campaignInfoWallet.getShard(),
                        Collections.singleton(smartCampaign.getId()));
        List<SmartCampaign> smartCampaigns = mapList(typedCampaigns, SmartCampaign.class::cast);
        checkCampaignNotificationData(data, smartCampaigns.get(0));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_CpmYndxFrontpageCampaign_WithFeatureOn(boolean recommendationManagementEnabled) {
        setRecommendationManagement(
                cpmYndxFrontpageCampaignInfo.getShard(),
                cpmYndxFrontpageCampaignInfo.getCampaignId(),
                recommendationManagementEnabled
        );
        steps.featureSteps().addClientFeature(cpmYndxFrontpageCampaignInfo.getClientId(),
                FeatureName.CPM_YNDX_FRONTPAGE_ON_GRID, true);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(cpmYndxFrontpageCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 1,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", cpmYndxFrontpageCampaignInfo.getCampaignId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes", ImmutableList.of(CPM_YNDX_FRONTPAGE.name()))
                                        .put("index", 0)
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", cpmYndxFrontpageCampaignInfo.getCampaign().getName())
                                        .put("startDate",
                                                cpmYndxFrontpageCampaignInfo.getCampaign().getStartTime().toString())
                                        .put("endDate",
                                                cpmYndxFrontpageCampaignInfo.getCampaign().getFinishTime().toString())
                                        .put("groupsCount", 0L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("allowedFrontpageType", List.of(FRONTPAGE.name(), FRONTPAGE_MOBILE.name()))
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest", getTotalSum(List.of(cpmYndxFrontpageCampaignInfo)))
                        )
                )
        );
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(contains(cpmYndxFrontpageCampaignInfo.getCampaignId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "allowedFrontpageType"))
                .useMatcher(containsInAnyOrder(FRONTPAGE.name(), FRONTPAGE_MOBILE.name()))
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_CpmYndxFrontpageCampaign_WithFeatureOff(boolean recommendationManagementEnabled) {
        setRecommendationManagement(
                cpmYndxFrontpageCampaignInfo.getShard(),
                cpmYndxFrontpageCampaignInfo.getCampaignId(),
                recommendationManagementEnabled
        );
        steps.featureSteps().addClientFeature(cpmYndxFrontpageCampaignInfo.getClientId(),
                FeatureName.CPM_YNDX_FRONTPAGE_ON_GRID, false);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(cpmYndxFrontpageCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 0,
                                "campaignIds", emptyList(),
                                "rowset", emptyList(),
                                "totalCampaigns", ImmutableMap.of("totalSumRest", BigDecimal.ZERO)
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_ContentPromotionCampaign_WithFeatureOn(boolean recommendationManagementEnabled) {
        setRecommendationManagement(
                contentPromotionCampaignInfo.getShard(),
                contentPromotionCampaignInfo.getCampaignId(),
                recommendationManagementEnabled
        );
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 1,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", contentPromotionCampaignInfo.getCampaignId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes",
                                                ImmutableList.of(CONTENT_PROMOTION_COLLECTION.name()))
                                        .put("index", 0)
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", contentPromotionCampaignInfo.getTypedCampaign().getName())
                                        .put("startDate",
                                                contentPromotionCampaignInfo.getTypedCampaign().getStartDate().toString())
                                        .put("endDate",
                                                contentPromotionCampaignInfo.getTypedCampaign().getEndDate().toString())
                                        .put("groupsCount", 0L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("attributionModel", defaultAttributionModel.name())
                                        .put("minusKeywords",
                                                contentPromotionCampaignInfo.getTypedCampaign().getMinusKeywords())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest", getTypedTotalSum(List.of(contentPromotionCampaignInfo)))
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(contains(contentPromotionCampaignInfo.getCampaignId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(contentPromotionCampaignInfo.getShard(),
                        Collections.singleton(contentPromotionCampaignInfo.getCampaignId()));
        List<ContentPromotionCampaign> contentPromotionCampaigns = mapList(typedCampaigns,
                ContentPromotionCampaign.class::cast);
        checkCampaignNotificationData(data, contentPromotionCampaigns.get(0));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_ContentPromotionCampaign_WithBothFeaturesOn_BothAdGroupTypesAllowed(
            boolean recommendationManagementEnabled
    ) {
        setRecommendationManagement(
                contentPromotionCampaignInfo.getShard(),
                contentPromotionCampaignInfo.getCampaignId(),
                recommendationManagementEnabled
        );
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 1,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", contentPromotionCampaignInfo.getCampaignId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("index", 0)
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", contentPromotionCampaignInfo.getTypedCampaign().getName())
                                        .put("startDate",
                                                contentPromotionCampaignInfo.getTypedCampaign().getStartDate().toString())
                                        .put("endDate",
                                                contentPromotionCampaignInfo.getTypedCampaign().getEndDate().toString())
                                        .put("groupsCount", 0L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("attributionModel", defaultAttributionModel.name())
                                        .put("minusKeywords",
                                                contentPromotionCampaignInfo.getTypedCampaign().getMinusKeywords())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest", getTypedTotalSum(List.of(contentPromotionCampaignInfo)))
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(contains(contentPromotionCampaignInfo.getCampaignId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "availableAdGroupTypes"))
                .useMatcher(contains(CONTENT_PROMOTION_COLLECTION.name(), CONTENT_PROMOTION_VIDEO.name()))
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(contentPromotionCampaignInfo.getShard(),
                        Collections.singleton(contentPromotionCampaignInfo.getCampaignId()));
        List<ContentPromotionCampaign> contentPromotionCampaigns = mapList(typedCampaigns,
                ContentPromotionCampaign.class::cast);
        checkCampaignNotificationData(data, contentPromotionCampaigns.get(0));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_ContentPromotionCampaign_WithVideoAdGroup_OnlyVideoAdGroupTypeAllowed(
            boolean recommendationManagementEnabled
    ) {
        setRecommendationManagement(
                contentPromotionCampaignInfo.getShard(),
                contentPromotionCampaignInfo.getCampaignId(),
                recommendationManagementEnabled
        );
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);
        steps.contentPromotionAdGroupSteps().createDefaultAdGroup(contentPromotionCampaignInfo, VIDEO);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 1,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", contentPromotionCampaignInfo.getCampaignId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes",
                                                ImmutableList.of(CONTENT_PROMOTION_VIDEO.name()))
                                        .put("index", 0)
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", contentPromotionCampaignInfo.getTypedCampaign().getName())
                                        .put("startDate",
                                                contentPromotionCampaignInfo.getTypedCampaign().getStartDate().toString())
                                        .put("endDate",
                                                contentPromotionCampaignInfo.getTypedCampaign().getEndDate().toString())
                                        .put("groupsCount", 1L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("attributionModel", defaultAttributionModel.name())
                                        .put("minusKeywords",
                                                contentPromotionCampaignInfo.getTypedCampaign().getMinusKeywords())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest", getTypedTotalSum(List.of(contentPromotionCampaignInfo)))
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(contains(contentPromotionCampaignInfo.getCampaignId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(contentPromotionCampaignInfo.getShard(),
                        Collections.singleton(contentPromotionCampaignInfo.getCampaignId()));
        List<ContentPromotionCampaign> contentPromotionCampaigns = mapList(typedCampaigns,
                ContentPromotionCampaign.class::cast);
        checkCampaignNotificationData(data, contentPromotionCampaigns.get(0));
    }

    @Test
    public void testService_ContentPromotionCampaign_WithVideoAdGroup_VideoFeatureOff_NoCampaignReturned() {
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);
        steps.contentPromotionAdGroupSteps().createDefaultAdGroup(contentPromotionCampaignInfo, VIDEO);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 0,
                                "campaignIds", emptyList(),
                                "rowset", emptyList(),
                                "totalCampaigns", ImmutableMap.of("totalSumRest", BigDecimal.ZERO)
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testService_ContentPromotionCampaign_WithCollectionAdGroup_CollectionsFeatureOff_NoCampaignReturned() {
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);
        steps.contentPromotionAdGroupSteps().createDefaultAdGroup(contentPromotionCampaignInfo, COLLECTION);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 0,
                                "campaignIds", emptyList(),
                                "rowset", emptyList(),
                                "totalCampaigns", ImmutableMap.of("totalSumRest", BigDecimal.ZERO)
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testService_ContentPromotionCampaign_WithTwoCollectionAdGroups_CollectionsFeatureOff_NoneOfCampaignsReturned() {
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);

        steps.contentPromotionAdGroupSteps().createDefaultAdGroup(contentPromotionCampaignInfo, COLLECTION);
        steps.contentPromotionAdGroupSteps().createDefaultAdGroup(anotherContentPromotionCampaignInfo, COLLECTION);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionCampaignInfo.getCampaignId(),
                        anotherContentPromotionCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 0,
                                "campaignIds", emptyList(),
                                "rowset", emptyList(),
                                "totalCampaigns", ImmutableMap.of("totalSumRest", BigDecimal.ZERO)
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testService_ContentPromotionCampaign_WithFeaturesOff() {
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 0,
                                "campaignIds", emptyList(),
                                "rowset", emptyList(),
                                "totalCampaigns", ImmutableMap.of("totalSumRest", BigDecimal.ZERO)
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_ContentPromotionCampaign_WithServiceAdGroup_ServicesFeatureOn_CampaignReturned(
            boolean recommendationManagementEnabled
    ) {
        setRecommendationManagement(
                contentPromotionServiceCampaignInfo.getShard(),
                contentPromotionServiceCampaignInfo.getCampaignId(),
                recommendationManagementEnabled
        );
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID, true);
        steps.contentPromotionAdGroupSteps().createDefaultAdGroup(contentPromotionServiceCampaignInfo, SERVICE);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionServiceCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 1,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", contentPromotionServiceCampaignInfo.getCampaignId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes",
                                                ImmutableList.of(CONTENT_PROMOTION_SERVICE.name()))
                                        .put("index", 0)
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", contentPromotionServiceCampaignInfo.getTypedCampaign().getName())
                                        .put("startDate",
                                                contentPromotionServiceCampaignInfo.getTypedCampaign().getStartDate().toString())
                                        .put("endDate",
                                                contentPromotionServiceCampaignInfo.getTypedCampaign().getEndDate().toString())
                                        .put("groupsCount", 1L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("attributionModel", defaultAttributionModel.name())
                                        .put("minusKeywords",
                                                contentPromotionServiceCampaignInfo.getTypedCampaign().getMinusKeywords())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest", getTypedTotalSum(List.of(contentPromotionServiceCampaignInfo)))
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(contains(contentPromotionServiceCampaignInfo.getCampaignId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(contentPromotionServiceCampaignInfo.getShard(),
                        Collections.singleton(contentPromotionServiceCampaignInfo.getCampaignId()));
        List<ContentPromotionCampaign> contentPromotionCampaigns = mapList(typedCampaigns,
                ContentPromotionCampaign.class::cast);
        checkCampaignNotificationData(data, contentPromotionCampaigns.get(0));
    }

    @Test
    public void testService_ContentPromotionCampaign_WithServiceAdGroup_ServicesFeatureOff_CampaignNotReturned() {
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID, false);
        steps.contentPromotionAdGroupSteps().createDefaultAdGroup(contentPromotionServiceCampaignInfo, SERVICE);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionServiceCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 0,
                                "campaignIds", emptyList(),
                                "rowset", emptyList(),
                                "totalCampaigns", ImmutableMap.of("totalSumRest", BigDecimal.ZERO)
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }


    @Test
    @TestCaseName("option recommendation management enabled: {0}")
    @Parameters(method = "parameters")
    public void testService_ContentPromotionCampaign_WithEdaAdGroup_EdaFeatureOn_CampaignReturned(
            boolean recommendationManagementEnabled
    ) {
        setRecommendationManagement(
                contentPromotionEdaCampaignInfo.getShard(),
                contentPromotionEdaCampaignInfo.getCampaignId(),
                recommendationManagementEnabled
        );
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID, false);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_EDA_INTERFACE, true);
        steps.contentPromotionAdGroupSteps().createDefaultAdGroup(contentPromotionEdaCampaignInfo, EDA);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionEdaCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 1,
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", contentPromotionEdaCampaignInfo.getCampaignId())
                                        .put("isRecommendationsManagementEnabled", recommendationManagementEnabled)
                                        .put("availableAdGroupTypes",
                                                ImmutableList.of(CONTENT_PROMOTION_EDA.name()))
                                        .put("index", 0)
                                        .put("walletId", campaignInfoWallet.getCampaignId())
                                        .put("name", contentPromotionEdaCampaignInfo.getTypedCampaign().getName())
                                        .put("startDate",
                                                contentPromotionEdaCampaignInfo.getTypedCampaign().getStartDate().toString())
                                        .put("endDate",
                                                contentPromotionEdaCampaignInfo.getTypedCampaign().getEndDate().toString())
                                        .put("groupsCount", 1L)
                                        .put("source", GdCampaignSource.DIRECT.name())
                                        .put("attributionModel", defaultAttributionModel.name())
                                        .put("minusKeywords",
                                                contentPromotionEdaCampaignInfo.getTypedCampaign().getMinusKeywords())
                                        .build()),
                                "totalCampaigns", ImmutableMap.of(
                                        "totalSumRest", getTypedTotalSum(List.of(contentPromotionEdaCampaignInfo)))
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "campaignIds"))
                .useMatcher(contains(contentPromotionEdaCampaignInfo.getCampaignId()))
                .forFields(newPath("client", "campaigns", "rowset", "\\d+", "notification"))
                .useMatcher(anything())
                .forFields(newPath("client", "campaigns", "totalCampaigns", "totalSumRest"))
                .useDiffer(new BigDecimalDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(contentPromotionEdaCampaignInfo.getShard(),
                        Collections.singleton(contentPromotionEdaCampaignInfo.getCampaignId()));
        List<ContentPromotionCampaign> contentPromotionCampaigns = mapList(typedCampaigns,
                ContentPromotionCampaign.class::cast);
        checkCampaignNotificationData(data, contentPromotionCampaigns.get(0));
    }

    @Test
    public void testService_ContentPromotionCampaign_WithEdaAdGroup_EdaFeatureOff_CampaignNotReturned() {
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID, true);
        steps.featureSteps().addClientFeature(contentPromotionCampaignInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_EDA_INTERFACE, false);
        steps.adGroupSteps().createDefaultContentPromotionAdGroup(contentPromotionEdaCampaignInfo, EDA);

        campaignsContainer.getFilter().setCampaignIdIn(
                Set.of(contentPromotionEdaCampaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(0);

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
                                "totalCount", 0,
                                "campaignIds", emptyList(),
                                "rowset", emptyList(),
                                "totalCampaigns", ImmutableMap.of("totalSumRest", BigDecimal.ZERO)
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
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

    private static void checkCampaignNotificationData(Map<String, Object> data,
                                                      CommonCampaign actualCampaign) {
        var clientData = (Map<String, Object>) data.get("client");
        var campaignsData = (Map<String, Object>) clientData.get("campaigns");
        var rowset = (List<Map<String, Object>>) campaignsData.get("rowset");
        var notificationData = (Map<String, Object>) rowset.get(0).get("notification");

        GdCampaignNotification gdCampaignNotification;
        if (actualCampaign instanceof DynamicCampaign) {
            gdCampaignNotification = getExpectedNotificationData((DynamicCampaign) actualCampaign);
        } else if (actualCampaign instanceof SmartCampaign) {
            gdCampaignNotification = getExpectedNotificationData((SmartCampaign) actualCampaign);
        } else if (actualCampaign instanceof ContentPromotionCampaign) {
            gdCampaignNotification = getExpectedNotificationData((ContentPromotionCampaign) actualCampaign);
        } else if (actualCampaign instanceof MobileContentCampaign) {
            gdCampaignNotification = getExpectedNotificationData((MobileContentCampaign) actualCampaign);
        } else if (actualCampaign instanceof McBannerCampaign) {
            gdCampaignNotification = getExpectedNotificationData((McBannerCampaign) actualCampaign);
        } else {
            gdCampaignNotification = getExpectedNotificationData((TextCampaign) actualCampaign);
        }

        var campaignNotification = GraphQlJsonUtils.convertValue(notificationData, GdCampaignNotification.class);
        assertThat(campaignNotification)
                .is(matchedBy(beanDiffer(gdCampaignNotification).useCompareStrategy(onlyExpectedFields())));
    }

    private static GdCampaignNotification getExpectedNotificationData(TextCampaign actualCampaign) {
        return getExpectedNotificationData(actualCampaign,
                toGdCampaignCheckPositionInterval(actualCampaign.getCheckPositionIntervalEvent()),
                actualCampaign.getEnableSendAccountNews());
    }

    private static GdCampaignNotification getExpectedNotificationData(McBannerCampaign actualCampaign) {
        return getExpectedNotificationData(actualCampaign, null, actualCampaign.getEnableSendAccountNews());
    }

    private static GdCampaignNotification getExpectedNotificationData(DynamicCampaign actualCampaign) {
        return getExpectedNotificationData(actualCampaign,
                toGdCampaignCheckPositionInterval(actualCampaign.getCheckPositionIntervalEvent()),
                actualCampaign.getEnableSendAccountNews());
    }

    private static GdCampaignNotification getExpectedNotificationData(MobileContentCampaign actualCampaign) {
        return getExpectedNotificationData(actualCampaign, null, actualCampaign.getEnableSendAccountNews());
    }

    private static GdCampaignNotification getExpectedNotificationData(SmartCampaign actualCampaign) {
        return getExpectedNotificationData(actualCampaign, null, actualCampaign.getEnableSendAccountNews());
    }

    private static GdCampaignNotification getExpectedNotificationData(ContentPromotionCampaign actualCampaign) {
        return getExpectedNotificationData(actualCampaign, null, null);
    }

    private static GdCampaignNotification getExpectedNotificationData(
            CommonCampaign actualCampaign,
            GdCampaignCheckPositionInterval checkPositionInterval,
            Boolean xlsReady) {
        var allowedEvents =
                getAvailableEmailEvents(actualCampaign.getWalletId(), actualCampaign.getType(), null);
        var emailSettings = new GdCampaignEmailSettings()
                .withEmail(actualCampaign.getEmail())
                .withAllowedEvents(mapSet(allowedEvents, GdCampaignEmailEvent::fromSource))
                .withWarningBalance(actualCampaign.getWarningBalance())
                .withXlsReady(xlsReady)
                .withStopByReachDailyBudget(actualCampaign.getEnablePausedByDayBudgetEvent())
                .withSendAccountNews(actualCampaign.getEnableSendAccountNews())
                .withCheckPositionInterval(checkPositionInterval);

        Set<SmsFlag> availableSmsFlags =
                getAvailableSmsFlags(isValidId(actualCampaign.getWalletId()), actualCampaign.getType());
        var smsSettings = new GdCampaignSmsSettings()
                .withEvents(mapSet(availableSmsFlags,
                        flag -> toGdCampaignSmsEventInfo(flag, actualCampaign.getSmsFlags())))
                .withSmsTime(GridTimeUtils.toGdTimeInterval(actualCampaign.getSmsTime()));

        return new GdCampaignNotification()
                .withEmailSettings(emailSettings)
                .withSmsSettings(smsSettings);
    }

    private BigDecimal getTotalSum(List<CampaignInfo> campaignInfos) {
        return subtractPercentPrecisely(StreamEx.of(campaignInfos)
                .map(c -> c.getCampaign().getBalanceInfo().getSum()
                        .subtract(c.getCampaign().getBalanceInfo().getSumSpent()))
                .reduce(BigDecimal.ZERO, BigDecimal::add), clientNds.getNds());
    }

    private BigDecimal getTypedTotalSum(List<ContentPromotionCampaignInfo> campaignInfos) {
        return subtractPercentPrecisely(StreamEx.of(campaignInfos)
                .map(c -> c.getTypedCampaign().getSum()
                        .subtract(c.getTypedCampaign().getSumSpent()))
                .reduce(BigDecimal.ZERO, BigDecimal::add), clientNds.getNds());

    }

    private SmartCampaign createSmartCampaign(ClientInfo clientInfo) {
        SmartCampaign campaign = TestCampaigns.defaultSmartCampaignWithSystemFields(clientInfo);
        campaign.setWalletId(campaignInfoWallet.getCampaignId());
        campaign.setBannerHrefParams(DEFAULT_BANNER_HREF_PARAMS);

        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                clientInfo.getShard(), clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(),
                clientInfo.getUid());
        List<Long> campaignIds =
                campaignModifyRepository.addCampaigns(dslContextProvider.ppc(clientInfo.getShard()),
                        addCampaignParametersContainer, Collections.singletonList(campaign));

        return (SmartCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(), campaignIds).get(0);
    }

    private void setRecommendationManagement(int shard, long campaignId, boolean value) {
        ru.yandex.direct.core.entity.campaign.model.Campaign campaign = campaignRepository.getCampaigns(
                shard,
                List.of(campaignId)
        ).get(0);
        ModelChanges<ru.yandex.direct.core.entity.campaign.model.Campaign> changes = new ModelChanges<>(
                campaignId,
                ru.yandex.direct.core.entity.campaign.model.Campaign.class
        );
        EnumSet<CampaignOpts> newOpts = EnumSet.copyOf(campaign.getOpts());
        if (value) {
            newOpts.add(RECOMMENDATIONS_MANAGEMENT_ENABLED);
        } else {
            newOpts.remove(RECOMMENDATIONS_MANAGEMENT_ENABLED);
        }
        changes.process(newOpts, ru.yandex.direct.core.entity.campaign.model.Campaign.OPTS);
        campaignRepository.updateCampaigns(shard, List.of(changes.applyTo(campaign)));
    }
}
