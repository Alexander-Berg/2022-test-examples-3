package ru.yandex.direct.grid.processing.service.constant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutConstants;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupValidationService;
import ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService;
import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.service.validation.BannerConstants;
import ru.yandex.direct.core.entity.banner.service.validation.BannerLettersConstants;
import ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstants;
import ru.yandex.direct.core.entity.banner.type.body.BannerWithBodyConstants;
import ru.yandex.direct.core.entity.banner.type.callouts.BannerWithCalloutsConstants;
import ru.yandex.direct.core.entity.banner.type.displayhref.BannerWithDisplayHrefConstraints;
import ru.yandex.direct.core.entity.banner.type.href.BannerWithHrefConstants;
import ru.yandex.direct.core.entity.banner.type.pixels.BannerPixelsConstants;
import ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService;
import ru.yandex.direct.core.entity.banner.type.titleextension.BannerWithTitleExtensionConstants;
import ru.yandex.direct.core.entity.banner.type.turbolanding.BannerWithTurbolandingConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.client.Constants;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.feature.service.FeatureHelper;
import ru.yandex.direct.core.entity.image.service.ImageConstants;
import ru.yandex.direct.core.entity.image.service.validation.ImageConstraints;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseConstraints;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates;
import ru.yandex.direct.core.entity.minuskeywordspack.service.validation.MinusKeywordsPackValidationService;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService;
import ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkConstants;
import ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkSetValidationService;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.entity.vcard.service.validation.AddVcardValidationService;
import ru.yandex.direct.core.entity.vcard.service.validation.InstantMessengerValidator;
import ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator;
import ru.yandex.direct.core.entity.vcard.service.validation.PointOnMapValidator;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.mock.PlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.constants.GdCampaignDefaultValues;
import ru.yandex.direct.grid.processing.model.constants.GdDefaultValues;
import ru.yandex.direct.grid.processing.model.constants.GdDirectConstantsAdGroupValidationData;
import ru.yandex.direct.grid.processing.model.constants.GdDirectConstantsCampaignValidationData;
import ru.yandex.direct.grid.processing.model.constants.GdDirectConstantsValidationData;
import ru.yandex.direct.grid.processing.model.constants.GdInternalAdPlacesInfoData;
import ru.yandex.direct.grid.processing.model.constants.GdInternalTemplatePlacesData;
import ru.yandex.direct.grid.processing.model.constants.GdInternalTemplateResourcesData;
import ru.yandex.direct.grid.processing.model.constants.GdMetroStation;
import ru.yandex.direct.grid.processing.model.constants.GdMetroStationsData;
import ru.yandex.direct.grid.processing.model.constants.GdTimezone;
import ru.yandex.direct.grid.processing.model.constants.GdTimezoneGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdRelevanceMatchCategory;
import ru.yandex.direct.grid.processing.model.strategy.defaults.GdPackageStrategiesDefaults;
import ru.yandex.direct.grid.processing.model.strategy.defaults.GdPackageStrategyDefaultValues;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.banner.BannerDataConverter;
import ru.yandex.direct.grid.processing.service.campaign.CampaignValidationService;
import ru.yandex.direct.grid.processing.service.group.validation.AdGroupMassActionsValidationService;
import ru.yandex.direct.grid.processing.service.offlinereport.OfflineReportValidationService;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GridValidationConstants;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.utils.TextConstants;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_DAY_BUDGET_DAILY_CHANGE_COUNT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PAY_FOR_CONVERSION_AVG_CPA_WARNING_RATIO_DEFAULT_VALUE;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill.allFreelancerSkills;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.constant.PackageStrategyDefaultValuesUtils.getPackageStrategiesDefaults;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConstantGraphQlServiceProcessingTest {
    private static final String QUERY = "{\n"
            + "  constants {\n"
            + "    strategyConstants {\n"
            + "      maxDailyBudgetChangesPerDay\n"
            + "      payForConversionAvgCpaWarningRatioDefaultValue\n"
            + "    }\n"
            + "    defaultValues {\n"
            + "      packageStrategies {\n"
            + "        defaultStrategy {\n"
            + "          type\n"
            + "        }\n"
            + "      }\n"
            + "      campaign {\n"
            + "         campaignType\n"
            + "         defaultWarningBalance\n"
            + "         defaultCheckPositionInterval\n"
            + "         defaultBroadMatchLimit\n"
            + "         defaultBroadMatchAllGoalsId\n"
            + "         defaultSmsTimeInterval {\n"
            + "             startTime {\n"
            + "                 hour\n"
            + "                 minute\n"
            + "             }\n"
            + "             endTime {\n"
            + "                 hour\n"
            + "                 minute\n"
            + "             }\n"
            + "         }\n"
            + "         defaultContextLimit\n"
            + "         defaultHasEnableCpcHold\n"
            + "         defaultExcludePausedCompetingAds\n"
            + "         defaultAddOpenstatTagToUrl\n"
            + "         defaultAddMetrikaTagToUrl\n"
            + "         defaultEnableCompanyInfo\n"
            + "         defaultIsAloneTrafaretAllowed\n"
            + "         defaultHasTurboSmarts\n"
            + "         defaultTimeTarget {\n"
            + "             timeBoard\n"
            + "             useWorkingWeekends\n"
            + "             enabledHolidaysMode\n"
            + "             idTimeZone\n"
            + "             holidaysSettings {\n"
            + "                 isShow\n"
            + "             }\n"
            + "         }\n"
            + "         isRecommendationsManagementEnabled\n"
            + "         isPriceRecommendationsManagementEnabled\n"
            + "      }\n"
            + "    }\n"
            + "    agencyOfflineReportMaximumDate\n"
            + "    agencyKpiOfflineReportMaximumDate\n"
            + "    agencyKpiOfflineReportMinimumDate\n"
            + "    shortcutRetargetingConditionIds\n"
            + "    currencyConstants(codes: [" + CurrencyCode.CHF + ", " + CurrencyCode.RUB + "]) {\n"
            + "      code\n"
            + "      minDailyBudget\n"
            + "      maxDailyBudget\n"
            + "      auctionStep\n"
            + "      autobudgetAvgCpaWarning\n"
            + "      autobudgetAvgPriceWarning\n"
            + "      autobudgetMaxPriceWarning\n"
            + "      autobudgetSumWarning\n"
            + "      bigRate\n"
            + "      defaultAutobudget\n"
            + "      defaultPrice\n"
            + "      directDefaultPay\n"
            + "      maxAutobudget\n"
            + "      maxAutobudgetBid\n"
            + "      maxAutopayCard\n"
            + "      maxAutopayRemaining\n"
            + "      maxAutopayYamoney\n"
            + "      maxClientArchive\n"
            + "      maxCpmPrice\n"
            + "      maxCpmFrontpagePrice\n"
            + "      maxDailyBudgetForPeriod\n"
            + "      maxPrice\n"
            + "      maxShowBid\n"
            + "      maxTopaySuggest\n"
            + "      minAutobudget\n"
            + "      minAutobudgetAvgCpa\n"
            + "      minAutobudgetAvgCpm\n"
            + "      minAutobudgetAvgPrice\n"
            + "      minAutobudgetBid\n"
            + "      minAutopay\n"
            + "      minCpcCpaPerformance\n"
            + "      minCpmPrice\n"
            + "      minCpmFrontpagePrice\n"
            + "      minDailyBudgetForPeriod\n"
            + "      minImagePrice\n"
            + "      minPay\n"
            + "      minPrice\n"
            + "      minPriceForMfa\n"
            + "      minSumInterpreteAsPayment\n"
            + "      minTransferMoney\n"
            + "      minWalletDayBudget\n"
            + "      moneymeterMaxMiddleSum\n"
            + "      moneymeterMiddlePriceMin\n"
            + "      moneymeterTypicalMiddleSumIntervalEnd\n"
            + "      moneymeterTypicalMiddleSumIntervalBegin\n"
            + "      recommendedSumToPay\n"
            + "      minAutobudgetClicksBundle\n"
            + "      autobudgetClicksBundleWarning\n"
            + "      isoNumCode\n"
            + "      payForConversionMinReservedSumDefaultValue\n"
            + "      maxAutobudgetClicksBundle\n"
            + "      precisionDigitCount\n"
            + "    }\n"
            + "    configuration {\n"
            + "      turbolandingUnifiedApiUrl\n"
            + "      canvasUiDomain\n"
            + "      balanceDomain\n"
            + "    }\n"
            + "    timezoneGroups {\n"
            + "      groupNick\n"
            + "      timezones {\n"
            + "        id\n"
            + "        timezone\n"
            + "        name\n"
            + "        offsetSeconds\n"
            + "        mskOffset\n"
            + "        gmtOffset\n"
            + "        offsetStr\n"
            + "      }\n"
            + "    }\n"
            + "    autotargetingCategories {\n"
            + "      category\n"
            + "      checked\n"
            + "      disabled\n"
            + "    }\n"
            + "    validation {\n"
            + "      campaignConstants {\n"
            + "        maxAllowedPageIdsLength\n"
            + "        minWarningBalanceInPercent\n"
            + "        maxWarningBalanceInPercent\n"
            + "        maxNameLength\n"
            + "        maxMetrikaCountersNumberForTextCampaign\n"
            + "        supplySidePlatforms\n"
            + "        maxDisabledIpsCount\n"
            + "        minBroadMatchLimit\n"
            + "        maxBroadMatchLimit\n"
            + "        maxDisabledPlacesCount\n"
            + "        maxCampaignTagCount\n"
            + "        maxCampaignTagNameLength\n"
            + "        maxAllowedPageIdsCount\n"
            + "        autoContextLimit\n"
            + "        minContextLimit\n"
            + "        maxContextLimit\n"
            + "        showsDisabledContextLimit\n"
            + "        noContextLimit\n"
            + "        maxCampaignsCountPerUpdate\n"
            + "        minInternalCampaignRotationGoalId\n"
            + "        minInternalCampaignRestrictionValue\n"
            + "      }\n"
            + "      adGroupConstants {\n"
            + "        maxAdGroupsCountPerUpdate\n"
            + "        maxNameLength\n"
            + "        maxMinusKeywordsTextLength\n"
            + "      }\n"
            + "      adConstants {\n"
            + "        maxLengthTitle\n"
            + "        maxLengthMobileTitle\n"
            + "        maxLengthTitleExtension\n"
            + "        maxLengthBody\n"
            + "        maxLengthMobileBody\n"
            + "        maxLengthTitleWord\n"
            + "        maxLengthBodyWord\n"
            + "        maxNumberOfNarrowCharacters\n"
            + "        maxLengthHref\n"
            + "        maxLengthDisplayHref\n"
            + "        maxLengthTurbolandingParams\n"
            + "        maxAdsInAdgroup\n"
            + "        maxCalloutsCountOnAd\n"
            + "        maxYaAudiencePixelsCountOnAd\n"
            + "        maxNotYaAudiencePixelsCountOnAd\n"
            + "        allowAdLetters\n"
            + "        allowAdDisplayHrefLetters\n"
            + "        narrowSymbols\n"
            + "        aggregationDomains\n"
            + "        adTypesSupportsImages\n"
            + "        imageAdImagesAllowedSizes {\n"
            + "          width\n"
            + "          height\n"
            + "        }\n"
            + "        mcbannerImagesAllowedSizes {\n"
            + "          width\n"
            + "          height\n"
            + "        }\n"
            + "      }\n"
            + "     vcardConstants {\n"
            + "        companyNameMaxLength\n"
            + "        contactPersonMaxLength\n"
            + "        contactEmailMaxLength\n"
            + "        extraMessageMaxLength\n"
            + "        countryMaxLength\n"
            + "        cityMaxLength\n"
            + "        streetMaxLength\n"
            + "        houseWithBuildingMaxLength\n"
            + "        apartmentMaxLength\n"
            + "        instantMessengerLoginMaxLength\n"
            + "        pointOnMapLongitudeMin\n"
            + "        pointOnMapLongitudeMax\n"
            + "        pointOnMapLatitudeMin\n"
            + "        pointOnMapLatitudeMax\n"
            + "        countryCodeMaxLength\n"
            + "        cityCodeMaxLength\n"
            + "        phoneNumberMinLength\n"
            + "        phoneNumberMaxLength\n"
            + "        extensionMaxLength\n"
            + "        entirePhoneMinLength\n"
            + "        entirePhoneMaxLength\n"
            + "        entirePhoneWithExtensionMaxLength\n"
            + "      }\n"
            + "     calloutConstants {\n"
            + "        calloutTextMaxLength\n"
            + "        calloutsOnClientMaxCount\n"
            + "        calloutsOnClientMaxCountWithDeleted\n"
            + "        calloutLettersAllowed\n"
            + "      }\n"
            + "     sitelinkConstants {\n"
            + "        sitelinkMaxCount\n"
            + "        maxTitleLength\n"
            + "        maxHrefLength\n"
            + "        maxDescriptionLength\n"
            + "        allowLetters\n"
            + "      }\n"
            + "     libMinusKeywordsConstants {\n"
            + "        libMinusKeywordsTextMaxLength\n"
            + "        maxLibraryPacksCount\n"
            + "        maxLinkedPacksToOneAdGroup\n"
            + "        maxNameLength\n"
            + "        maxWordsInMinusPhrase\n"
            + "        nameAllowLetters\n"
            + "        textAllowLetters\n"
            + "      }\n"
            + "     minusKeywordsConstants {\n"
            + "         normalizedMinusKeywordsMaxLength\n"
            + "     }\n"
            + "     keywordsConstants {\n"
            + "        keywordMaxLength\n"
            + "        wordMaxLength\n"
            + "        wordsMaxCount\n"
            + "      }\n"
            + "    }\n"
            + "    metroStations(input: {filter:{}}) {\n"
            + "      rowset {\n"
            + "        geoRegionId\n"
            + "        metroStationId\n"
            + "        metroStationName\n"
            + "      }\n"
            + "    }\n"
            + "    internalTemplatePlaces(input: {filter:{placesIds:[" + TemplatePlaceRepositoryMockUtils.PLACE_1
            + "]}}) {\n"
            + "      rowset {\n"
            + "        placeId\n"
            + "        templateId\n"
            + "      }\n"
            + "    }\n"
            + "    internalAdPlacesInfo(input: {filter:{placesIds:[" + PlaceRepositoryMockUtils.PLACE_1.getId() + ", "
            + PlaceRepositoryMockUtils.MODERATED_PLACE_5.getId() + "]}}) {\n"
            + "      rowset {\n"
            + "        placeId\n"
            + "        fullDescription\n"
            + "        isModerated\n"
            + "      }\n"
            + "    }\n"
            + "    internalTemplateResources(input: {filter: {templateIds:["
            + TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1 + "]}}) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        templateId\n"
            + "        description\n"
            + "        optionsRequired\n"
            + "      }\n"
            + "    }\n"
            + "    " + ConstantGraphQlService.INTERNAL_PAGES_INFO_RESOLVER + " {\n"
            + "      pageId\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String SKILL_QUERY = "query{constants{freelancerSkillTypes{skillCode}}}";
    private static final String FL_COUNT_QUERY = "query{constants{freelancersCount}}";

    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Value("${turbo_landings.url}")
    private String turbolandingUnifiedApiUrl;

    @Value("${canvas.ui_domain}")
    private String canvasUiDomain;

    @Value("${balance.domain}")
    private String balanceDomain;

    @Autowired
    private ClientGeoService clientGeoService;

    @Autowired
    private ConstantDataService constantDataService;

    @Autowired
    private AgencyOfflineReportParametersService agencyOfflineReportParametersService;

    @Autowired
    private SspPlatformsRepository sspPlatformsRepository;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private FeatureSteps featureSteps;

    @Autowired
    private OfflineReportValidationService offlineReportValidationService;

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        context = ContextHelper.buildContext(userInfo.getUser());
        featureSteps.setCurrentClient(userInfo.getClientId());
    }

    @Test
    public void testService() {
        ExecutionResult result = processor.processQuery(null, QUERY, null, context);
        HashMap<String, Object> expectedForChf = new HashMap<>();
        expectedForChf.put("code", CurrencyCode.CHF.toString());
        expectedForChf.put("minDailyBudget", Currencies.getCurrency(CurrencyCode.CHF).getMinDayBudget());
        expectedForChf.put("maxDailyBudget", Currencies.getCurrency(CurrencyCode.CHF).getMaxDailyBudgetAmount());
        expectedForChf.put("auctionStep", Currencies.getCurrency(CurrencyCode.CHF).getAuctionStep());
        expectedForChf.put("autobudgetAvgCpaWarning",
                Currencies.getCurrency(CurrencyCode.CHF).getAutobudgetAvgCpaWarning());
        expectedForChf.put("autobudgetAvgPriceWarning",
                Currencies.getCurrency(CurrencyCode.CHF).getAutobudgetAvgPriceWarning());
        expectedForChf.put("autobudgetMaxPriceWarning",
                Currencies.getCurrency(CurrencyCode.CHF).getAutobudgetMaxPriceWarning());
        expectedForChf.put("autobudgetSumWarning", Currencies.getCurrency(CurrencyCode.CHF).getAutobudgetSumWarning());
        expectedForChf.put("bigRate", Currencies.getCurrency(CurrencyCode.CHF).getBigRate());
        expectedForChf.put("defaultAutobudget", Currencies.getCurrency(CurrencyCode.CHF).getDefaultAutobudget());
        expectedForChf.put("defaultPrice", Currencies.getCurrency(CurrencyCode.CHF).getDefaultPrice());
        expectedForChf.put("directDefaultPay", Currencies.getCurrency(CurrencyCode.CHF).getDirectDefaultPay());
        expectedForChf.put("maxAutobudget", Currencies.getCurrency(CurrencyCode.CHF).getMaxAutobudget());
        expectedForChf.put("maxAutobudgetBid", Currencies.getCurrency(CurrencyCode.CHF).getMaxAutobudgetBid());
        expectedForChf.put("maxAutopayCard", Currencies.getCurrency(CurrencyCode.CHF).getMaxAutopayCard());
        expectedForChf.put("maxAutopayRemaining", Currencies.getCurrency(CurrencyCode.CHF).getMaxAutopayRemaining());
        expectedForChf.put("maxAutopayYamoney", Currencies.getCurrency(CurrencyCode.CHF).getMaxAutopayYamoney());
        expectedForChf.put("maxClientArchive", Currencies.getCurrency(CurrencyCode.CHF).getMaxClientArchive());
        expectedForChf.put("maxCpmPrice", Currencies.getCurrency(CurrencyCode.CHF).getMaxCpmPrice());
        expectedForChf.put("maxCpmFrontpagePrice", Currencies.getCurrency(CurrencyCode.CHF).getMaxCpmPrice());
        expectedForChf.put("maxDailyBudgetForPeriod",
                Currencies.getCurrency(CurrencyCode.CHF).getMaxDailyBudgetForPeriod());
        expectedForChf.put("maxPrice", Currencies.getCurrency(CurrencyCode.CHF).getMaxPrice());
        expectedForChf.put("maxShowBid", Currencies.getCurrency(CurrencyCode.CHF).getMaxShowBid());
        expectedForChf.put("maxTopaySuggest", Currencies.getCurrency(CurrencyCode.CHF).getMaxTopaySuggest());
        expectedForChf.put("minAutobudget", Currencies.getCurrency(CurrencyCode.CHF).getMinAutobudget());
        expectedForChf.put("minAutobudgetAvgCpa", Currencies.getCurrency(CurrencyCode.CHF).getMinAutobudgetAvgCpa());
        expectedForChf.put("minAutobudgetAvgCpm", Currencies.getCurrency(CurrencyCode.CHF).getMinAutobudgetAvgCpm());
        expectedForChf.put("minAutobudgetAvgPrice",
                Currencies.getCurrency(CurrencyCode.CHF).getMinAutobudgetAvgPrice());
        expectedForChf.put("minAutobudgetBid", Currencies.getCurrency(CurrencyCode.CHF).getMinAutobudgetBid());
        expectedForChf.put("minAutopay", Currencies.getCurrency(CurrencyCode.CHF).getMinAutopay());
        expectedForChf.put("minCpcCpaPerformance", Currencies.getCurrency(CurrencyCode.CHF).getMinCpcCpaPerformance());
        expectedForChf.put("minCpmPrice", Currencies.getCurrency(CurrencyCode.CHF).getMinCpmPrice());
        expectedForChf.put("minCpmFrontpagePrice", Currencies.getCurrency(CurrencyCode.CHF).getMinCpmFrontpagePrice());
        expectedForChf.put("minDailyBudgetForPeriod",
                Currencies.getCurrency(CurrencyCode.CHF).getMinDailyBudgetForPeriod());
        expectedForChf.put("minImagePrice", Currencies.getCurrency(CurrencyCode.CHF).getMinImagePrice());
        expectedForChf.put("minPay", Currencies.getCurrency(CurrencyCode.CHF).getMinPay());
        expectedForChf.put("minPrice", Currencies.getCurrency(CurrencyCode.CHF).getMinPrice());
        expectedForChf.put("minPriceForMfa", Currencies.getCurrency(CurrencyCode.CHF).getMinPriceForMfa());
        expectedForChf.put("minSumInterpreteAsPayment",
                Currencies.getCurrency(CurrencyCode.CHF).getMinSumInterpreteAsPayment());
        expectedForChf.put("minTransferMoney", Currencies.getCurrency(CurrencyCode.CHF).getMinTransferMoney());
        expectedForChf.put("minWalletDayBudget", Currencies.getCurrency(CurrencyCode.CHF).getMinWalletDayBudget());
        expectedForChf.put("moneymeterMaxMiddleSum",
                Currencies.getCurrency(CurrencyCode.CHF).getMoneymeterMaxMiddleSum());
        expectedForChf.put("moneymeterMiddlePriceMin",
                Currencies.getCurrency(CurrencyCode.CHF).getMoneymeterMiddlePriceMin());
        expectedForChf.put("moneymeterTypicalMiddleSumIntervalEnd",
                Currencies.getCurrency(CurrencyCode.CHF).getMoneymeterTypicalMiddleSumIntervalEnd());
        expectedForChf.put("moneymeterTypicalMiddleSumIntervalBegin",
                Currencies.getCurrency(CurrencyCode.CHF).getMoneymeterTypicalMiddleSumIntervalBegin());
        expectedForChf.put("recommendedSumToPay", Currencies.getCurrency(CurrencyCode.CHF).getRecommendedSumToPay());
        expectedForChf.put("minAutobudgetClicksBundle",
                Currencies.getCurrency(CurrencyCode.CHF).getMinAutobudgetClicksBundle());
        expectedForChf.put("autobudgetClicksBundleWarning",
                Currencies.getCurrency(CurrencyCode.CHF).getAutobudgetClicksBundleWarning());
        expectedForChf.put("isoNumCode", Currencies.getCurrency(CurrencyCode.CHF).getIsoNumCode());
        expectedForChf.put("payForConversionMinReservedSumDefaultValue",
                Currencies.getCurrency(CurrencyCode.CHF).getPayForConversionMinReservedSumDefaultValue());
        expectedForChf.put("maxAutobudgetClicksBundle",
                Currencies.getCurrency(CurrencyCode.CHF).getMaxAutobudgetClicksBundle());
        expectedForChf.put("precisionDigitCount", Currencies.getCurrency(CurrencyCode.CHF).getPrecisionDigitCount());

        HashMap<String, Object> expectedForRub = new HashMap<>();
        expectedForRub.put("code", CurrencyCode.RUB.toString());
        expectedForRub.put("minDailyBudget", Currencies.getCurrency(CurrencyCode.RUB).getMinDayBudget());
        expectedForRub.put("maxDailyBudget", Currencies.getCurrency(CurrencyCode.RUB).getMaxDailyBudgetAmount());
        expectedForRub.put("auctionStep", Currencies.getCurrency(CurrencyCode.RUB).getAuctionStep());
        expectedForRub.put("autobudgetAvgCpaWarning",
                Currencies.getCurrency(CurrencyCode.RUB).getAutobudgetAvgCpaWarning());
        expectedForRub.put("autobudgetAvgPriceWarning",
                Currencies.getCurrency(CurrencyCode.RUB).getAutobudgetAvgPriceWarning());
        expectedForRub.put("autobudgetMaxPriceWarning",
                Currencies.getCurrency(CurrencyCode.RUB).getAutobudgetMaxPriceWarning());
        expectedForRub.put("autobudgetSumWarning", Currencies.getCurrency(CurrencyCode.RUB).getAutobudgetSumWarning());
        expectedForRub.put("bigRate", Currencies.getCurrency(CurrencyCode.RUB).getBigRate());
        expectedForRub.put("defaultAutobudget", Currencies.getCurrency(CurrencyCode.RUB).getDefaultAutobudget());
        expectedForRub.put("defaultPrice", Currencies.getCurrency(CurrencyCode.RUB).getDefaultPrice());
        expectedForRub.put("directDefaultPay", Currencies.getCurrency(CurrencyCode.RUB).getDirectDefaultPay());
        expectedForRub.put("maxAutobudget", Currencies.getCurrency(CurrencyCode.RUB).getMaxAutobudget());
        expectedForRub.put("maxAutobudgetBid", Currencies.getCurrency(CurrencyCode.RUB).getMaxAutobudgetBid());
        expectedForRub.put("maxAutopayCard", Currencies.getCurrency(CurrencyCode.RUB).getMaxAutopayCard());
        expectedForRub.put("maxAutopayRemaining", Currencies.getCurrency(CurrencyCode.RUB).getMaxAutopayRemaining());
        expectedForRub.put("maxAutopayYamoney", Currencies.getCurrency(CurrencyCode.RUB).getMaxAutopayYamoney());
        expectedForRub.put("maxClientArchive", Currencies.getCurrency(CurrencyCode.RUB).getMaxClientArchive());
        expectedForRub.put("maxCpmPrice", Currencies.getCurrency(CurrencyCode.RUB).getMaxCpmPrice());
        expectedForRub.put("maxCpmFrontpagePrice", Currencies.getCurrency(CurrencyCode.RUB).getMaxCpmPrice());
        expectedForRub.put("maxDailyBudgetForPeriod",
                Currencies.getCurrency(CurrencyCode.RUB).getMaxDailyBudgetForPeriod());
        expectedForRub.put("maxPrice", Currencies.getCurrency(CurrencyCode.RUB).getMaxPrice());
        expectedForRub.put("maxShowBid", Currencies.getCurrency(CurrencyCode.RUB).getMaxShowBid());
        expectedForRub.put("maxTopaySuggest", Currencies.getCurrency(CurrencyCode.RUB).getMaxTopaySuggest());
        expectedForRub.put("minAutobudget", Currencies.getCurrency(CurrencyCode.RUB).getMinAutobudget());
        expectedForRub.put("minAutobudgetAvgCpa", Currencies.getCurrency(CurrencyCode.RUB).getMinAutobudgetAvgCpa());
        expectedForRub.put("minAutobudgetAvgCpm", Currencies.getCurrency(CurrencyCode.RUB).getMinAutobudgetAvgCpm());
        expectedForRub.put("minAutobudgetAvgPrice",
                Currencies.getCurrency(CurrencyCode.RUB).getMinAutobudgetAvgPrice());
        expectedForRub.put("minAutobudgetBid", Currencies.getCurrency(CurrencyCode.RUB).getMinAutobudgetBid());
        expectedForRub.put("minAutopay", Currencies.getCurrency(CurrencyCode.RUB).getMinAutopay());
        expectedForRub.put("minCpcCpaPerformance", Currencies.getCurrency(CurrencyCode.RUB).getMinCpcCpaPerformance());
        expectedForRub.put("minCpmPrice", Currencies.getCurrency(CurrencyCode.RUB).getMinCpmPrice());
        expectedForRub.put("minCpmFrontpagePrice", Currencies.getCurrency(CurrencyCode.RUB).getMinCpmFrontpagePrice());
        expectedForRub.put("minDailyBudgetForPeriod",
                Currencies.getCurrency(CurrencyCode.RUB).getMinDailyBudgetForPeriod());
        expectedForRub.put("minImagePrice", Currencies.getCurrency(CurrencyCode.RUB).getMinImagePrice());
        expectedForRub.put("minPay", Currencies.getCurrency(CurrencyCode.RUB).getMinPay());
        expectedForRub.put("minPrice", Currencies.getCurrency(CurrencyCode.RUB).getMinPrice());
        expectedForRub.put("minPriceForMfa", Currencies.getCurrency(CurrencyCode.RUB).getMinPriceForMfa());
        expectedForRub.put("minSumInterpreteAsPayment",
                Currencies.getCurrency(CurrencyCode.RUB).getMinSumInterpreteAsPayment());
        expectedForRub.put("minTransferMoney", Currencies.getCurrency(CurrencyCode.RUB).getMinTransferMoney());
        expectedForRub.put("minWalletDayBudget", Currencies.getCurrency(CurrencyCode.RUB).getMinWalletDayBudget());
        expectedForRub.put("moneymeterMaxMiddleSum",
                Currencies.getCurrency(CurrencyCode.RUB).getMoneymeterMaxMiddleSum());
        expectedForRub.put("moneymeterMiddlePriceMin",
                Currencies.getCurrency(CurrencyCode.RUB).getMoneymeterMiddlePriceMin());
        expectedForRub.put("moneymeterTypicalMiddleSumIntervalEnd",
                Currencies.getCurrency(CurrencyCode.RUB).getMoneymeterTypicalMiddleSumIntervalEnd());
        expectedForRub.put("moneymeterTypicalMiddleSumIntervalBegin",
                Currencies.getCurrency(CurrencyCode.RUB).getMoneymeterTypicalMiddleSumIntervalBegin());
        expectedForRub.put("recommendedSumToPay", Currencies.getCurrency(CurrencyCode.RUB).getRecommendedSumToPay());
        expectedForRub.put("minAutobudgetClicksBundle",
                Currencies.getCurrency(CurrencyCode.RUB).getMinAutobudgetClicksBundle());
        expectedForRub.put("autobudgetClicksBundleWarning",
                Currencies.getCurrency(CurrencyCode.RUB).getAutobudgetClicksBundleWarning());
        expectedForRub.put("isoNumCode", Currencies.getCurrency(CurrencyCode.RUB).getIsoNumCode());
        expectedForRub.put("payForConversionMinReservedSumDefaultValue",
                Currencies.getCurrency(CurrencyCode.RUB).getPayForConversionMinReservedSumDefaultValue());
        expectedForRub.put("maxAutobudgetClicksBundle",
                Currencies.getCurrency(CurrencyCode.RUB).getMaxAutobudgetClicksBundle());
        expectedForRub.put("precisionDigitCount", Currencies.getCurrency(CurrencyCode.RUB).getPrecisionDigitCount());

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Map.of(
                "constants",
                Map.ofEntries(
                        entry("strategyConstants", Map.of(
                                "maxDailyBudgetChangesPerDay", MAX_DAY_BUDGET_DAILY_CHANGE_COUNT,
                                "payForConversionAvgCpaWarningRatioDefaultValue",
                                PAY_FOR_CONVERSION_AVG_CPA_WARNING_RATIO_DEFAULT_VALUE)),
                        entry("defaultValues", getExpectedDefaultValuesData()),
                        entry("currencyConstants", List.of(expectedForChf, expectedForRub)),
                        entry("configuration",
                                Map.of(
                                        "turbolandingUnifiedApiUrl", turbolandingUnifiedApiUrl,
                                        "canvasUiDomain", canvasUiDomain,
                                        "balanceDomain", balanceDomain)),
                        entry("timezoneGroups", getExpectedTimezoneGroups()),
                        entry("validation", getExpectedValidationData()),
                        entry("metroStations", Map.of(GdMetroStationsData.ROWSET.name(), getExpectedMetroStations())),
                        entry("agencyOfflineReportMaximumDate",
                                agencyOfflineReportParametersService.getMaximumAvailableDateAsString()),
                        entry("agencyKpiOfflineReportMaximumDate",
                                offlineReportValidationService.getAgencyKpiMaximumAvailableDateAsString()),
                        entry("agencyKpiOfflineReportMinimumDate",
                                offlineReportValidationService.getAgencyKpiMinimumAvailableDateAsString()),
                        entry("shortcutRetargetingConditionIds",
                                RetargetingConditionShortcutService.RETARGETING_CONDITION_SHORTCUT_DEFAULT_IDS),
                        entry("internalTemplatePlaces",
                                Map.of(GdInternalTemplatePlacesData.ROWSET.name(),
                                        List.of(
                                                Map.of(
                                                        "placeId", TemplatePlaceRepositoryMockUtils.PLACE_1,
                                                        "templateId",
                                                        TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1
                                                ),
                                                Map.of(
                                                        "placeId", TemplatePlaceRepositoryMockUtils.PLACE_1,
                                                        "templateId",
                                                        TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_2_WITHOUT_RESOURCES
                                                ),
                                                Map.of(
                                                        "placeId", TemplatePlaceRepositoryMockUtils.PLACE_1,
                                                        "templateId",
                                                        TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_3_WITH_IMAGE
                                                ),
                                                Map.of(
                                                        "placeId", TemplatePlaceRepositoryMockUtils.PLACE_1,
                                                        "templateId",
                                                        TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_4_URL_IMG
                                                ),
                                                Map.of(
                                                        "placeId", TemplatePlaceRepositoryMockUtils.PLACE_1,
                                                        "templateId",
                                                        TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_WITH_AGE_VARIABLE
                                                ),
                                                Map.of(
                                                        "placeId", TemplatePlaceRepositoryMockUtils.PLACE_1,
                                                        "templateId",
                                                        TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES
                                                )
                                        ))
                        ),
                        entry("internalAdPlacesInfo",
                                Map.of(GdInternalAdPlacesInfoData.ROWSET.name(),
                                        List.of(
                                                Map.of(
                                                        "placeId", PlaceRepositoryMockUtils.PLACE_1.getId(),
                                                        "fullDescription",
                                                        PlaceRepositoryMockUtils.PLACE_1.getDescription(),
                                                        "isModerated", false
                                                ),
                                                Map.of(
                                                        "placeId", PlaceRepositoryMockUtils.MODERATED_PLACE_5.getId(),
                                                        "fullDescription",
                                                        PlaceRepositoryMockUtils.MODERATED_PLACE_5.getDescription(),
                                                        "isModerated", true
                                                )
                                        ))
                        ),
                        entry("internalTemplateResources",
                                Map.of(GdInternalTemplateResourcesData.ROWSET.name(),
                                        List.of(
                                                Map.of(
                                                        "id",
                                                        TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED,
                                                        "templateId",
                                                        TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1,
                                                        "description",
                                                        TemplateResourceRepositoryMockUtils.RESOURCE_DESCRIPTION,
                                                        "optionsRequired", true
                                                )
                                        ))
                        ),
                        entry("autotargetingCategories",
                                List.of(
                                        Map.of(
                                                "category", GdRelevanceMatchCategory.EXACT_MARK.name(),
                                                "checked", true,
                                                "disabled", true
                                        ),
                                        Map.of(
                                                "category", GdRelevanceMatchCategory.ALTERNATIVE_MARK.name(),
                                                "checked", true,
                                                "disabled", false
                                        ),
                                        Map.of(
                                                "category", GdRelevanceMatchCategory.COMPETITOR_MARK.name(),
                                                "checked", true,
                                                "disabled", false
                                        ),
                                        Map.of(
                                                "category", GdRelevanceMatchCategory.BROADER_MARK.name(),
                                                "checked", true,
                                                "disabled", false
                                        ),
                                        Map.of(
                                                "category", GdRelevanceMatchCategory.ACCESSORY_MARK.name(),
                                                "checked", true,
                                                "disabled", false
                                        )
                                )),
                        entry(ConstantGraphQlService.INTERNAL_PAGES_INFO_RESOLVER, Collections.emptyList()))
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(getCompareStrategy())));
    }

    @Test
    public void freelancerSkillTypes_success() {
        ExecutionResult result = processor.processQuery(null, SKILL_QUERY, null, context);
        Object data = result.getData();
        List<String> skillNames = getSkillCodes(data);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getErrors()).isEmpty();
            allFreelancerSkills().forEach(
                    skill -> softly.assertThat(skillNames).contains(skill.getSkillCode())
            );
        });
    }

    private List<String> getSkillCodes(Object srcData) {
        LinkedHashMap data = (LinkedHashMap) srcData;
        LinkedHashMap constants = (LinkedHashMap) data.get("constants");
        ArrayList skillTypes = (ArrayList) constants.get("freelancerSkillTypes");
        List<String> skillCodes = new ArrayList<>(skillTypes.size());
        for (Object skillType : skillTypes) {
            LinkedHashMap skillMap = (LinkedHashMap) skillType;
            String skillCode = (String) skillMap.get("skillCode");
            skillCodes.add(skillCode);
        }
        return skillCodes;
    }

    @Test
    public void freelancersCount_success() {
        ExecutionResult result = processor.processQuery(null, FL_COUNT_QUERY, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsKeys("constants");
        @SuppressWarnings("unchecked")
        Map<String, Object> constants = (Map<String, Object>) data.get("constants");
        assertThat(constants).containsKeys("freelancersCount");
    }

    private List<Map> getExpectedMetroStations() {
        GeoTree geoTree = clientGeoService.getClientTranslocalGeoTree(context.getOperator().getClientId());

        return StreamEx.of(geoTree.getMetroMap().values())
                .map(ConstantsConverter::toGdMetroStation)
                .sorted(Comparator.comparing(GdMetroStation::getMetroStationName))
                .map(m -> convertValue(m, Map.class))
                .toList();
    }

    private List<Map> getExpectedTimezoneGroups() {
        List<GdTimezoneGroup> groups = constantDataService.getTimezoneGroups();

        return groups.stream()
                .map(gdTimezoneGroup -> Map.of(
                        "groupNick", gdTimezoneGroup.getGroupNick(),
                        "timezones", gdTimezonesToListOfMaps(gdTimezoneGroup.getTimezones())
                )).collect(toList());
    }

    private List<Map> gdTimezonesToListOfMaps(List<GdTimezone> timezones) {
        return timezones.stream()
                .map(timezone -> Map.of(
                        "id", timezone.getId(),
                        "timezone", timezone.getTimezone(),
                        "name", timezone.getName(),
                        "offset", timezone.getOffsetSeconds(),
                        "mskOffset", timezone.getMskOffset(),
                        "gmtOffset", timezone.getGmtOffset(),
                        "offsetStr", timezone.getOffsetStr()
                ))
                .collect(toList());
    }

    private static Map<String, Map<String, Object>> getExpectedDefaultValuesData() {
        Map<String, Object> campaignDefaultValues = Map.ofEntries(
                entry(GdCampaignDefaultValues.CAMPAIGN_TYPE.name(),
                        GdCampaignType.TEXT.name()),
                entry(GdCampaignDefaultValues.DEFAULT_WARNING_BALANCE.name(),
                        CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE),
                entry(GdCampaignDefaultValues.DEFAULT_CHECK_POSITION_INTERVAL.name(),
                        DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL.name()),
                entry(GdCampaignDefaultValues.DEFAULT_BROAD_MATCH_LIMIT.name(),
                        CampaignConstants.BROAD_MATCH_LIMIT_DEFAULT),
                entry(GdCampaignDefaultValues.DEFAULT_BROAD_MATCH_ALL_GOALS_ID.name(),
                        CampaignConstants.BROAD_MATCH_ALL_GOALS_ID),
                entry(GdCampaignDefaultValues.DEFAULT_SMS_TIME_INTERVAL.name(),
                        convertValue(DefaultValuesUtils.DEFAULT_SMS_TIME_INTERVAL, Map.class)),
                entry(GdCampaignDefaultValues.DEFAULT_CONTEXT_LIMIT.name(),
                        CampaignConstants.DEFAULT_CONTEXT_LIMIT),
                entry(GdCampaignDefaultValues.DEFAULT_HAS_ENABLE_CPC_HOLD.name(),
                        CampaignConstants.DEFAULT_HAS_ENABLE_CPC_HOLD),
                entry(GdCampaignDefaultValues.DEFAULT_ENABLE_COMPANY_INFO.name(),
                        CampaignConstants.DEFAULT_ENABLE_COMPANY_INFO),
                entry(GdCampaignDefaultValues.DEFAULT_IS_ALONE_TRAFARET_ALLOWED.name(),
                        CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED),
                entry(GdCampaignDefaultValues.DEFAULT_HAS_TURBO_SMARTS.name(),
                        CampaignConstants.DEFAULT_HAS_TURBO_SMARTS),
                entry(GdCampaignDefaultValues.DEFAULT_EXCLUDE_PAUSED_COMPETING_ADS.name(),
                        CampaignConstants.DEFAULT_EXCLUDE_PAUSED_COMPETING_ADS),
                entry(GdCampaignDefaultValues.DEFAULT_ADD_OPENSTAT_TAG_TO_URL.name(),
                        CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL),
                entry(GdCampaignDefaultValues.DEFAULT_ADD_METRIKA_TAG_TO_URL.name(),
                        CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL),
                entry(GdCampaignDefaultValues.DEFAULT_TIME_TARGET.name(),
                        convertValue(DefaultValuesUtils.defaultGdTimeTarget(), Map.class)),
                entry(GdCampaignDefaultValues.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED.name(),
                        CampaignConstants.DEFAULT_IS_RECOMMENDATIONS_MANAGEMENT_ENABLED),
                entry(GdCampaignDefaultValues.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED.name(),
                        CampaignConstants.DEFAULT_IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED)
        );

        Map<String, Object> packageStrategiesDefaults = Map.ofEntries(
                entry(GdPackageStrategiesDefaults.DEFAULT_STRATEGY.name(),
                        Map.ofEntries(entry(
                                GdPackageStrategyDefaultValues.TYPE.name(),
                                getPackageStrategiesDefaults().getDefaultStrategy().getType().name())
                        )
                )
        );

        return Map.ofEntries(
                entry(GdDefaultValues.CAMPAIGN.name(), campaignDefaultValues),
                entry("packageStrategies", packageStrategiesDefaults)
        );
    }

    private static Map<String, Map<String, Object>> getExpectedValidationData() {
        Map<String, Object> campaignValidationData = Map.ofEntries(
                entry(GdDirectConstantsCampaignValidationData.MAX_ALLOWED_PAGE_IDS_LENGTH.name(),
                        ValidationConstantsService.MAX_ALLOWED_PAGE_IDS_LENGTH),
                entry(GdDirectConstantsCampaignValidationData.MIN_WARNING_BALANCE_IN_PERCENT.name(),
                        CampaignConstants.MIN_CAMPAIGN_WARNING_BALANCE),
                entry(GdDirectConstantsCampaignValidationData.MAX_WARNING_BALANCE_IN_PERCENT.name(),
                        CampaignConstants.MAX_CAMPAIGN_WARNING_BALANCE),
                entry(GdDirectConstantsCampaignValidationData.MAX_NAME_LENGTH.name(),
                        CampaignConstants.MAX_CAMPAIGN_NAME_LENGTH),
                entry(GdDirectConstantsCampaignValidationData.MAX_DISABLED_IPS_COUNT.name(),
                        CampaignConstants.MAX_DISABLED_IPS_COUNT),
                entry(GdDirectConstantsCampaignValidationData.MIN_BROAD_MATCH_LIMIT.name(),
                        CampaignConstants.BROAD_MATCH_LIMIT_MIN),
                entry(GdDirectConstantsCampaignValidationData.MAX_BROAD_MATCH_LIMIT.name(),
                        CampaignConstants.BROAD_MATCH_LIMIT_MAX),
                entry(GdDirectConstantsCampaignValidationData.MAX_METRIKA_COUNTERS_NUMBER_FOR_TEXT_CAMPAIGN.name(),
                        CampaignConstants.MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS),
                entry(GdDirectConstantsCampaignValidationData.MAX_DISABLED_PLACES_COUNT.name(),
                        Constants.DEFAULT_DISABLED_PLACES_COUNT_LIMIT),
                entry(GdDirectConstantsCampaignValidationData.MAX_ALLOWED_PAGE_IDS_COUNT.name(),
                        CampaignConstants.MAX_ALLOWED_PAGE_IDS_COUNT),
                entry(GdDirectConstantsCampaignValidationData.MAX_CAMPAIGN_TAG_COUNT.name(),
                        CampaignConstants.MAX_CAMPAIGN_TAG_COUNT),
                entry(GdDirectConstantsCampaignValidationData.MAX_CAMPAIGN_TAG_NAME_LENGTH.name(),
                        CampaignConstants.MAX_CAMPAIGN_TAG_NAME_LENGTH),
                entry(GdDirectConstantsCampaignValidationData.AUTO_CONTEXT_LIMIT.name(),
                        CampaignConstants.AUTO_CONTEXT_LIMIT),
                entry(GdDirectConstantsCampaignValidationData.MIN_CONTEXT_LIMIT.name(),
                        CampaignConstants.MIN_CONTEXT_LIMIT),
                entry(GdDirectConstantsCampaignValidationData.MAX_CONTEXT_LIMIT.name(),
                        CampaignConstants.MAX_CONTEXT_LIMIT),
                entry(GdDirectConstantsCampaignValidationData.SHOWS_DISABLED_CONTEXT_LIMIT.name(),
                        CampaignConstants.SHOWS_DISABLED_CONTEXT_LIMIT),
                entry(GdDirectConstantsCampaignValidationData.NO_CONTEXT_LIMIT.name(),
                        CampaignConstants.NO_CONTEXT_LIMIT),
                entry(GdDirectConstantsCampaignValidationData.MAX_CAMPAIGNS_COUNT_PER_UPDATE.name(),
                        CampaignValidationService.MAX_CAMPAIGNS_COUNT_PER_UPDATE),
                entry(GdDirectConstantsCampaignValidationData.MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID.name(),
                        (int) CampaignConstants.MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID),
                entry(GdDirectConstantsCampaignValidationData.MIN_INTERNAL_CAMPAIGN_RESTRICTION_VALUE.name(),
                        (int) CampaignConstants.MIN_INTERNAL_CAMPAIGN_RESTRICTION_VALUE)
        );

        Map<String, Object> adGroupValidationData = Map.ofEntries(
                entry(GdDirectConstantsAdGroupValidationData.MAX_AD_GROUPS_COUNT_PER_UPDATE.name(),
                        AdGroupMassActionsValidationService.MAX_AD_GROUPS_COUNT_PER_UPDATE),
                entry(GdDirectConstantsAdGroupValidationData.MAX_NAME_LENGTH.name(),
                        AdGroupValidationService.MAX_NAME_LENGTH),
                entry(GdDirectConstantsAdGroupValidationData.MAX_MINUS_KEYWORDS_TEXT_LENGTH.name(),
                        MinusPhraseConstraints.GROUP_MINUS_KEYWORDS_MAX_LENGTH)
        );

        Map<String, Object> adValidationData = Map.ofEntries(
                entry("mcbannerImagesAllowedSizes", toListOfSizeMap(ImageConstants.ALLOWED_SIZES_FOR_MCBANNER)),
                entry("maxLengthTitle", BannerConstantsService.MAX_LENGTH_TITLE),
                entry("maxLengthMobileTitle", BannerConstantsService.MAX_LENGTH_TITLE),
                entry("maxLengthTitleExtension", BannerWithTitleExtensionConstants.MAX_LENGTH_TITLE_EXTENSION),
                entry("maxLengthTitleWord", BannerConstantsService.MAX_LENGTH_TITLE_WORD),
                entry("maxLengthBody", BannerWithBodyConstants.MAX_LENGTH_BODY),
                entry("maxLengthMobileBody", BannerWithBodyConstants.MAX_LENGTH_MOBILE_BODY),
                entry("maxLengthBodyWord", BannerWithBodyConstants.MAX_LENGTH_BODY_WORD),
                entry("maxNumberOfNarrowCharacters", BannerTextConstants.MAX_NUMBER_OF_NARROW_CHARACTERS),
                entry("maxLengthHref", BannerWithHrefConstants.MAX_LENGTH_HREF),
                entry("maxLengthDisplayHref", BannerWithDisplayHrefConstraints.MAX_LENGTH_DISPLAY_HREF),
                entry("maxLengthTurbolandingParams", BannerWithTurbolandingConstants.MAX_LENGTH_TURBOLANDING_PARAMS),
                entry("maxAdsInAdgroup", BannerConstants.MAX_BANNERS_IN_ADGROUP),
                entry("maxCalloutsCountOnAd", BannerWithCalloutsConstants.MAX_CALLOUTS_COUNT_ON_BANNER),
                entry("maxYaAudiencePixelsCountOnAd", BannerPixelsConstants.MAX_YA_AUDIENCE_PIXELS_COUNT_ON_BANNER),
                entry("maxNotYaAudiencePixelsCountOnAd",
                        BannerPixelsConstants.MAX_NOT_YA_AUDIENCE_PIXELS_COUNT_ON_BANNER_DEFAULT),
                entry("allowAdLetters", BannerLettersConstants.ALLOW_BANNER_LETTERS_STR),
                entry("allowAdDisplayHrefLetters",
                        BannerWithDisplayHrefConstraints.ALLOW_BANNER_DISPLAY_HREF_LETTERS_STR),
                entry("narrowSymbols", BannerTextConstants.NARROW_SYMBOLS),
                entry("aggregationDomains", new ArrayList<>(GridValidationConstants.SITELINK_VALID_DOMAINS)),
                entry("adTypesSupportsImages", mapList(mapSet(ImageConstraints.BANNER_TYPES_SUPPORTS_IMAGE,
                        BannerDataConverter::toGdAdType), Enum::name))
        );

        Map<String, Object> vcardValidationData = Map.ofEntries(
                entry("companyNameMaxLength", AddVcardValidationService.COMPANY_NAME_MAX_LENGTH),
                entry("contactPersonMaxLength", AddVcardValidationService.CONTACT_PERSON_MAX_LENGTH),
                entry("contactEmailMaxLength", AddVcardValidationService.CONTACT_EMAIL_MAX_LENGTH),
                entry("extraMessageMaxLength", AddVcardValidationService.EXTRA_MESSAGE_MAX_LENGTH),
                entry("countryMaxLength", AddVcardValidationService.COUNTRY_MAX_LENGTH),
                entry("cityMaxLength", AddVcardValidationService.CITY_MAX_LENGTH),
                entry("streetMaxLength", AddVcardValidationService.STREET_MAX_LENGTH),
                entry("houseWithBuildingMaxLength", AddVcardValidationService.HOUSE_MAX_LENGTH),
                entry("apartmentMaxLength", AddVcardValidationService.APART_MAX_LENGTH),
                entry("instantMessengerLoginMaxLength", InstantMessengerValidator.LOGIN_MAX_LENGTH),
                entry("pointOnMapLongitudeMin", PointOnMapValidator.LONGITUDE_MIN),
                entry("pointOnMapLongitudeMax", PointOnMapValidator.LONGITUDE_MAX),
                entry("pointOnMapLatitudeMin", PointOnMapValidator.LATITUDE_MIN),
                entry("pointOnMapLatitudeMax", PointOnMapValidator.LATITUDE_MAX),
                entry("countryCodeMaxLength", PhoneValidator.COUNTRY_CODE_MAX_LENGTH),
                entry("cityCodeMaxLength", PhoneValidator.CITY_CODE_MAX_LENGTH),
                entry("phoneNumberMinLength", PhoneValidator.PHONE_NUMBER_MIN_LENGTH),
                entry("phoneNumberMaxLength", PhoneValidator.PHONE_NUMBER_MAX_LENGTH),
                entry("extensionMaxLength", PhoneValidator.EXTENSION_MAX_LENGTH),
                entry("entirePhoneMinLength", PhoneValidator.ENTIRE_PHONE_MIN_LENGTH),
                entry("entirePhoneMaxLength", PhoneValidator.ENTIRE_PHONE_MAX_LENGTH),
                entry("entirePhoneWithExtensionMaxLength", PhoneValidator.ENTIRE_PHONE_WITH_EXTENSION_MAX_LENGTH)
        );

        Map<String, Object> calloutValidationData = Map.ofEntries(
                entry("calloutTextMaxLength", CalloutConstants.MAX_CALLOUT_TEXT_LENGTH),
                entry("calloutsOnClientMaxCount", CalloutConstants.MAX_CALLOUTS_COUNT_ON_CLIENT),
                entry("calloutsOnClientMaxCountWithDeleted",
                        CalloutConstants.MAX_CALLOUTS_COUNT_ON_CLIENT_WITH_DELETED),
                entry("calloutLettersAllowed", CalloutConstants.ALLOW_CALLOUT_LETTERS)
        );

        Map<String, Object> sitelinkValidationData = Map.ofEntries(
                entry("sitelinkMaxCount", SitelinkSetValidationService.MAX_SITELINKS_PER_SET),
                entry("maxTitleLength", SitelinkConstants.MAX_SITELINK_TITLE_LENGTH),
                entry("maxHrefLength", SitelinkConstants.MAX_SITELINK_HREF_LENGTH),
                entry("maxDescriptionLength", SitelinkConstants.MAX_SITELINK_DESC_LENGTH),
                entry("allowLetters", SitelinkConstants.ALLOW_SITELINK_LETTERS)
        );

        Map<String, Object> libMinusKeywordsValidationData = Map.ofEntries(
                entry("libMinusKeywordsTextMaxLength", MinusPhraseConstraints.GROUP_MINUS_KEYWORDS_MAX_LENGTH),
                entry("maxLibraryPacksCount", MinusPhraseConstraints.MAX_LIBRARY_PACKS_COUNT),
                entry("maxLinkedPacksToOneAdGroup", MinusPhraseConstraints.MAX_LINKED_PACKS_TO_ONE_AD_GROUP),
                entry("maxNameLength", MinusKeywordsPackValidationService.MAX_NAME_LENGTH),
                entry("maxWordsInMinusPhrase", MinusPhraseConstraints.WORDS_MAX_COUNT),
                entry("nameAllowLetters", TextConstants.ALL),
                entry("textAllowLetters", MinusPhrasePredicates.ALLOW_MINUS_KEYWORD_CHARS)
        );

        Map<String, Object> minusKeywordsValidationData = Map.ofEntries(
                entry("normalizedMinusKeywordsMaxLength", MinusPhraseConstraints.CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH)
        );

        Map<String, Object> keywordsValidationData = Map.ofEntries(
                entry("keywordMaxLength", PhraseConstraints.KEYWORD_MAX_LENGTH),
                entry("wordMaxLength", PhraseConstraints.WORD_MAX_LENGTH),
                entry("wordsMaxCount", PhraseConstraints.WORDS_MAX_COUNT)
        );

        return Map.ofEntries(
                entry(GdDirectConstantsValidationData.CAMPAIGN_CONSTANTS.name(), campaignValidationData),
                entry(GdDirectConstantsValidationData.AD_GROUP_CONSTANTS.name(), adGroupValidationData),
                entry(GdDirectConstantsValidationData.AD_CONSTANTS.name(), adValidationData),
                entry(GdDirectConstantsValidationData.VCARD_CONSTANTS.name(), vcardValidationData),
                entry(GdDirectConstantsValidationData.CALLOUT_CONSTANTS.name(), calloutValidationData),
                entry(GdDirectConstantsValidationData.SITELINK_CONSTANTS.name(), sitelinkValidationData),
                entry(GdDirectConstantsValidationData.LIB_MINUS_KEYWORDS_CONSTANTS.name(),
                        libMinusKeywordsValidationData),
                entry(GdDirectConstantsValidationData.MINUS_KEYWORDS_CONSTANTS.name(), minusKeywordsValidationData),
                entry(GdDirectConstantsValidationData.KEYWORDS_CONSTANTS.name(), keywordsValidationData)
        );
    }

    private static List<Map> toListOfSizeMap(Set<ImageSize> sizeSet) {
        return StreamEx.of(sizeSet)
                .sorted(Comparator.comparing(ImageSize::getWidth))
                .map(size -> convertValue(size, Map.class))
                .toList();
    }

    private CompareStrategy getCompareStrategy() {
        var sizes = FeatureHelper.feature(FeatureName.ALLOW_PROPORTIONALLY_LARGER_IMAGES).enabled()
                ? ImageConstants.ALLOWED_SIZES_FOR_AD_IMAGE_ORIGINAL
                : ImageConstants.ALLOWED_SIZES_FOR_AD_IMAGE;
        return DefaultCompareStrategies.allFields()
                .forFields(newPath("constants", "validation", "adConstants", "imageAdImagesAllowedSizes"))
                .useMatcher(containsInAnyOrder(toListOfSizeMap(sizes).toArray()))
                .forFields(newPath("constants", "validation",
                        GdDirectConstantsValidationData.CAMPAIGN_CONSTANTS.name(), "supplySidePlatforms"))
                .useMatcher(containsInAnyOrder(sspPlatformsRepository.getAllSspPlatforms().toArray()));
    }
}
