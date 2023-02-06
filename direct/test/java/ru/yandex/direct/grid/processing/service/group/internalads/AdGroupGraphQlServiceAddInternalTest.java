package ru.yandex.direct.grid.processing.service.group.internalads;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import graphql.GraphQLError;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.CallerReferrersAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidTypesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DesktopInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DeviceIdsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.FeaturesInPPAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.HasLCookieAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.HasPassportIdAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLangsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InternalNetworkAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsDefaultYandexSearchAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsPPLoggedInAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsVirusedAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsYandexPlusAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledApp;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.PlusUserSegmentsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.QueryOptionsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.QueryReferersAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.SearchTextAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ShowDatesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.SidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TestIdsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TimeAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.UserAgentsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.UuidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YpCookiesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YsCookiesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.validation.AdGroupAdditionalTargetingsDefects;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEngine;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEnginesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserName;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceVendor;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceVendorsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.IsMobileAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.IsTabletAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.IsTouchAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamiliesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamily;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsName;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.entity.region.validation.RegionIdDefects;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingJoinType;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingUatraits;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingVersioned;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingBrowserEnginesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingBrowserNamesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingCallerReferrersRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingClidTypesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingClidsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingDesktopInstalledAppsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingDeviceIdsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingDeviceNamesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingDeviceVendorsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingFeaturesInPPRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingHasLCookieRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingHasPassportIdRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingInterfaceLangsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingInternalNetworkRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsDefaultYandexSearchRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsMobileRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsPPLoggedInRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsTabletRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsTouchRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsVirusedRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsYandexPlusRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingMobileInstalledAppsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingOsFamiliesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingOsNamesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingPlusUserSegmentsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingQueryOptionsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingQueryReferersRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingSearchTextRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingShowDatesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingSidsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingTestIdsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingTimeRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingUnion;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingUserAgentsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingUuidsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingYandexUidsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingYpCookiesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingYsCookiesRequest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddInternalAdGroups;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddInternalAdGroupsItem;
import ru.yandex.direct.grid.processing.model.retargeting.GdGoalMinimal;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItemReq;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdUpdateInternalAdRetargetingConditionItem;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTimeConstants.DAYS_PER_WEEK;
import static org.joda.time.DateTimeConstants.HOURS_PER_DAY;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.DistribSoftConstants.DISTRIB_SOFT;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang.RU;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang.UK;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.DEVICE_IDS;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.PLUS_USER_SEGMENTS;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.SEARCH_TEXT;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.SIDS;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.TIME_TARGETS;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.UUIDS;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleType.OR;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;
import static ru.yandex.direct.utils.ListUtils.integerToLongList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb.all;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceAddInternalTest {

    private static final String GROUP_NAME = "aaaa";
    private static final long GROUP_LEVEL = 1L;
    private static final int RF = 3;
    private static final int RF_RESET = 1;
    private static final int MAX_CLICKS_COUNT = 4;
    private static final int MAX_CLICKS_PERIOD = 60;
    private static final int MAX_STOPS_COUNT = 5;
    private static final int MAX_STOPS_PERIOD_GD = 0;
    private static final int MAX_STOPS_PERIOD_EXPECTED =
            CampaignConstants.MAX_CLICKS_AND_STOPS_PERIOD_WHOLE_CAMPAIGN_VALUE;
    private static final LocalDateTime START_TIME = LocalDateTime.now().withNano(0);
    private static final LocalDateTime FINISH_TIME = START_TIME.plusDays(100);
    private static final List<Integer> GROUP_REGION_IDS = singletonList((int) Region.MOSCOW_REGION_ID);

    private static final List<String> YAUID_LIST =
            ImmutableList.of("1021110101545123184", "2021110101545123184", "%42");
    private static final List<String> REFERERS_LIST = ImmutableList.of("%yandex.com.tr%", "%harita%");
    private static final EnumSet<InterfaceLang> INTERFACE_LANGS = EnumSet.of(RU, UK);
    private static final List<String> USER_AGENTS = ImmutableList.of("%Yandex%", "%YNDX-SB001%");
    private static final List<String> DEVICE_NAMES = ImmutableList.of("YNDX-SB001", "YNDX-SB002");
    private static final Set<Long> DESKTOP_INSTALLED_APPS = DISTRIB_SOFT.keySet();
    private static final Set<Long> CLID_TYPES = ImmutableSet.of(1L, 10L);
    private static final Set<Long> CLIDS = ImmutableSet.of(100500L, 100501L);
    private static final Set<String> QUERY_OPTIONS = ImmutableSet.of("test", "browser");
    private static final Set<Long> TEST_IDS = ImmutableSet.of(4444L, 5555L);
    private static final Set<String> YS_COOKIES = ImmutableSet.of("test", "ext");
    private static final Set<String> FEATURES_IN_PP = ImmutableSet.of("opapapa", "tyctyc");
    private static final Set<String> YP_COOKIES = ImmutableSet.of("test", "ext");
    private static final Set<LocalDate> SHOW_DATES = ImmutableSet.of(LocalDate.now(), LocalDate.now().plusYears(10));
    private static final Set<String> MOBILE_INSTALLED_APPS_URLS = ImmutableSet.of(
            "http://play.google.com/store/apps/details?id=ru.yandex.searchplugin",
            "https://apps.apple.com/ru/app/andeks-market/id425354015");
    private static final Set<String> INVALID_MOBILE_INSTALLED_APPS_URLS = ImmutableSet.of(
            "http://play.google.com/store/apps/details?id=ru.yandex.searchplugin",
            "https://yandex.ru",
            "https://apps.apple.com/ru/app/andeks-market/id425354015");

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    Steps steps;

    @Autowired
    AdGroupRepository adGroupRepository;

    @Autowired
    AdGroupAdditionalTargetingRepository adGroupAdditionalTargetingRepository;

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    UserRepository userRepository;

    private static final String ADD_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedAdGroupItems {\n"
            + "         adGroupId,\n"
            + "     }\n"
            + "  }\n"
            + "}";

    private static final TemplateMutation<GdAddInternalAdGroups, GdAddAdGroupPayload> ADD_MUTATION =
            new TemplateMutation<>("addInternalAdGroups", ADD_MUTATION_TEMPLATE,
                    GdAddInternalAdGroups.class, GdAddAdGroupPayload.class);

    private Integer shard;
    private User operator;
    private Long campaignId;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void addInternalAdGroup_Success() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(GROUP_NAME)
                .withLevel(GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD_EXPECTED)
                .withStartTime(START_TIME)
                .withFinishTime(FINISH_TIME)
                .withGeo(integerToLongList(GROUP_REGION_IDS));

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAdGroup_WithoutRf_Success() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId).withRf(null).withRfReset(null));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(GROUP_NAME)
                .withLevel(GROUP_LEVEL)
                .withRf(null)
                .withRfReset(null)
                .withMaxClicksCount(null)
                .withMaxClicksPeriod(null)
                .withMaxStopsCount(null)
                .withMaxStopsPeriod(null)
                .withGeo(integerToLongList(GROUP_REGION_IDS));

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAdGroup_WithoutRfReset_Success() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId).withRf(RF).withRfReset(null));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assertThat(actualAdGroup)
                .extracting("rf", "rfReset")
                .containsExactly(RF, null);
    }

    @Test
    public void addInternalAdGroup_WithInvalidRequest_ReturnsRequestError() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(null));
        List<GraphQLError> graphQLErrors = graphQlTestExecutor.doMutation(ADD_MUTATION, input, operator).getErrors();
        assertThat(graphQLErrors).isNotEmpty();
    }

    @Test
    public void addInternalAdGroup_WithNonExistentCampaignId_ReturnsValidationError() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(Long.MAX_VALUE));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAdGroups.ADD_ITEMS.name()), index(0),
                        field(GdAddInternalAdGroupsItem.CAMPAIGN_ID.name())),
                CampaignDefects.campaignNotFound())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAdGroup_OneValidAndOneWithNonExistentCampaignId_ReturnsNullForInvalid() {
        GdAddInternalAdGroups input = createRequest(
                createCorrectAddItem(Long.MAX_VALUE), // incorrect
                createCorrectAddItem(campaignId));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);


        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(payload.getAddedAdGroupItems()).hasSize(2);

            //noinspection unchecked
            softly.assertThat(payload.getAddedAdGroupItems()).is(
                    matchedBy(contains(nullValue(), notNullValue())));

            GdValidationResult expectedGdValidationResult = toGdValidationResult(
                    path(field(GdAddInternalAdGroups.ADD_ITEMS.name()), index(0),
                            field(GdAddInternalAdGroupsItem.CAMPAIGN_ID.name())),
                    CampaignDefects.campaignNotFound())
                    .withWarnings(null);
            softly.assertThat(payload.getValidationResult())
                    .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));

            Long adGroupId = payload.getAddedAdGroupItems().get(1).getAdGroupId();
            AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
            InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                    .withType(AdGroupType.INTERNAL)
                    .withName(GROUP_NAME)
                    .withLevel(GROUP_LEVEL)
                    .withRf(RF)
                    .withRfReset(RF_RESET)
                    .withMaxClicksCount(MAX_CLICKS_COUNT)
                    .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                    .withMaxStopsCount(MAX_STOPS_COUNT)
                    .withMaxStopsPeriod(MAX_STOPS_PERIOD_EXPECTED)
                    .withStartTime(START_TIME)
                    .withFinishTime(FINISH_TIME)
                    .withGeo(integerToLongList(GROUP_REGION_IDS));

            assertThat(actualAdGroup).is(
                    matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void addInternalAdGroup_WithNonExistentRegion_ReturnsValidationError() {
        GdAddInternalAdGroups input = createRequest(
                createCorrectAddItem(campaignId).withRegionIds(singletonList(Integer.MAX_VALUE)));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAdGroups.ADD_ITEMS.name()), index(0),
                        field(GdAddInternalAdGroupsItem.REGION_IDS.name())),
                RegionIdDefects.geoIncorrectRegions(Objects.toString(Integer.MAX_VALUE)))
                .withWarnings(null);

        DefaultCompareStrategy validationCompareStrategy = allFieldsExcept(
                newPath("errors", "\\d+", "params"));

        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(validationCompareStrategy)));
    }

    @Test
    public void addInternalAdGroup_WithInvalidTargetingValue_ReturnsValidationError() {
        GdAddInternalAdGroups input = createRequest(createAddItemWithOneTargeting(campaignId, List.of("%420")));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAdGroups.ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdGroupsItem.TARGETINGS.name()),
                        index(0),
                        field(GdAdditionalTargetingUnion.TARGETING_YANDEX_UIDS),
                        field(GdAdditionalTargetingYandexUidsRequest.VALUE.name())
                ),
                CommonDefects.invalidValue())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void addInternalAdGroup_WithInvalidVersionedTargeting_ReturnsValidationError() {
        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingOsFamilies(new GdAdditionalTargetingOsFamiliesRequest()
                        .withValue(singletonList(new GdAdditionalTargetingVersioned().withTargetingValueEntryId(2L)
                                .withMaxVersion("1111.1111")))
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY));
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId).withTargetings(List.of(union)));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAdGroups.ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdGroupsItem.TARGETINGS),
                        index(0),
                        field(GdAdditionalTargetingUnion.TARGETING_OS_FAMILIES),
                        field(GdAdditionalTargetingOsFamiliesRequest.VALUE.name())
                ),
                AdGroupAdditionalTargetingsDefects.incorrectVersion())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void addInternalAdGroup_WithMultipleVersionedTargetings_Success() {
        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingOsFamilies(new GdAdditionalTargetingOsFamiliesRequest()
                        .withValue(asList(
                                new GdAdditionalTargetingVersioned()
                                        .withTargetingValueEntryId(2L)
                                        .withMaxVersion("111.111"),
                                new GdAdditionalTargetingVersioned()
                                        .withTargetingValueEntryId(3L)
                                        .withMinVersion("999.999")))
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY));
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId).withTargetings(List.of(union)));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(GROUP_NAME)
                .withLevel(GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD_EXPECTED)
                .withFinishTime(FINISH_TIME)
                .withGeo(integerToLongList(GROUP_REGION_IDS));

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAdGroup_WithShowDatesTargeting_ReturnsValidationError() {
        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingShowDates(new GdAdditionalTargetingShowDatesRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(Collections.emptySet()));
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId).withTargetings(List.of(union)));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAdGroups.ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdGroupsItem.TARGETINGS.name()),
                        index(0),
                        field(GdAdditionalTargetingUnion.TARGETING_SHOW_DATES),
                        field(GdAdditionalTargetingShowDatesRequest.VALUE.name())
                ),
                CollectionDefects.notEmptyCollection())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void addInternalAdGroup_WithMobileInstalledAppTargeting_WithEmptyValue_ReturnsValidationError() {
        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingMobileInstalledApps(new GdAdditionalTargetingMobileInstalledAppsRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(Collections.emptySet()));
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId).withTargetings(List.of(union)));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAdGroups.ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdGroupsItem.TARGETINGS.name()),
                        index(0),
                        field(GdAdditionalTargetingUnion.TARGETING_MOBILE_INSTALLED_APPS),
                        field(GdAdditionalTargetingMobileInstalledAppsRequest.VALUE.name())
                ),
                CollectionDefects.notEmptyCollection())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void addInternalAdGroup_WithMobileInstalledAppTargeting_WithInvalidValue_ReturnsValidationError() {
        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingMobileInstalledApps(new GdAdditionalTargetingMobileInstalledAppsRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(INVALID_MOBILE_INSTALLED_APPS_URLS));
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId).withTargetings(List.of(union)));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAdGroups.ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdGroupsItem.TARGETINGS.name()),
                        index(0),
                        field(GdAdditionalTargetingUnion.TARGETING_MOBILE_INSTALLED_APPS),
                        field(GdAdditionalTargetingMobileInstalledAppsRequest.VALUE.name())
                ),
                CommonDefects.invalidValue())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void addInternalAdGroup_WithoutStartTime_Success() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId).withStartTime(null));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(GROUP_NAME)
                .withLevel(GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD_EXPECTED)
                .withFinishTime(FINISH_TIME)
                .withGeo(integerToLongList(GROUP_REGION_IDS));

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAdGroup_WithoutFinishTime_Success() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId).withFinishTime(null));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(GROUP_NAME)
                .withLevel(GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD_EXPECTED)
                .withStartTime(START_TIME)
                .withGeo(integerToLongList(GROUP_REGION_IDS));

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAdGroup_WithoutStartAndFinishTimes_Success() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId)
                .withStartTime(null).withFinishTime(null));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(GROUP_NAME)
                .withLevel(GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD_EXPECTED)
                .withGeo(integerToLongList(GROUP_REGION_IDS));

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAdGroup_WithBadStartAndFinishTimes_ReturnsValidationError() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItem(campaignId)
                .withStartTime(FINISH_TIME).withFinishTime(START_TIME));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAdGroups.ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdGroupsItem.FINISH_TIME.name())
                ),
                AdGroupDefects.finishTimeShouldBeGreaterThanStartTime())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void addInternalAdGroup_WithOneTargeting_Success() {
        GdAddInternalAdGroups input = createRequest(createAddItemWithOneTargeting(campaignId, YAUID_LIST));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();

        List<AdGroupAdditionalTargeting> targetingList =
                adGroupAdditionalTargetingRepository.getByAdGroupId(shard, adGroupId);

        assertThat(targetingList.get(0)).is(matchedBy(createYaUidTargetingMatcher(adGroupId)));
    }

    @Test
    public void addInternalAdGroup_WithAllTargetings_Success() {
        GdAddInternalAdGroups input = createRequest(createCorrectAddItemWithTargeting(campaignId));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();

        List<AdGroupAdditionalTargeting> targetingList =
                adGroupAdditionalTargetingRepository.getByAdGroupId(shard, adGroupId);
        assertThat(targetingList).is(matchedBy(containsInAnyOrder(
                createDefaultTargetingMatcher(adGroupId, IsVirusedAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, HasPassportIdAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, HasLCookieAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, InternalNetworkAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, IsMobileAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, IsTabletAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, IsTouchAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, YandexUidsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(YAUID_LIST))),
                createDefaultTargetingMatcher(adGroupId, QueryReferersAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(REFERERS_LIST))),
                createDefaultTargetingMatcher(adGroupId, CallerReferrersAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(REFERERS_LIST))),
                createDefaultTargetingMatcher(adGroupId, InterfaceLangsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(INTERFACE_LANGS))),
                createDefaultTargetingMatcher(adGroupId, UserAgentsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(USER_AGENTS))),
                createDefaultTargetingMatcher(adGroupId, BrowserEnginesAdGroupAdditionalTargeting.class,
                        hasProperty("value",
                                equalTo(singletonList(new BrowserEngine().withTargetingValueEntryId(1L)
                                        .withMinVersion("1.1").withMaxVersion("2.2"))))),
                createDefaultTargetingMatcher(adGroupId, BrowserNamesAdGroupAdditionalTargeting.class,
                        hasProperty("value",
                                equalTo(singletonList(new BrowserName().withTargetingValueEntryId(1L)
                                        .withMinVersion("1.1").withMaxVersion("2.2"))))),
                createDefaultTargetingMatcher(adGroupId, OsFamiliesAdGroupAdditionalTargeting.class,
                        hasProperty("value",
                                equalTo(asList(
                                        new OsFamily().withTargetingValueEntryId(2L)
                                                .withMinVersion("1.1").withMaxVersion("2.2"),
                                        new OsFamily().withTargetingValueEntryId(3L))))),
                createDefaultTargetingMatcher(adGroupId, OsNamesAdGroupAdditionalTargeting.class,
                        hasProperty("value",
                                equalTo(asList(
                                        new OsName().withTargetingValueEntryId(1L),
                                        new OsName().withTargetingValueEntryId(2L))))),
                createDefaultTargetingMatcher(adGroupId, DeviceVendorsAdGroupAdditionalTargeting.class,
                        hasProperty("value",
                                equalTo(singletonList(new DeviceVendor().withTargetingValueEntryId(1L))))),
                createDefaultTargetingMatcher(adGroupId, DeviceNamesAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(DEVICE_NAMES))),
                createDefaultTargetingMatcher(adGroupId, ShowDatesAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(SHOW_DATES))),
                createDefaultTargetingMatcher(adGroupId, DesktopInstalledAppsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(DESKTOP_INSTALLED_APPS))),
                createDefaultTargetingMatcher(adGroupId, ClidTypesAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(CLID_TYPES))),
                createDefaultTargetingMatcher(adGroupId, ClidsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(CLIDS))),
                createDefaultTargetingMatcher(adGroupId, QueryOptionsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(QUERY_OPTIONS))),
                createDefaultTargetingMatcher(adGroupId, TestIdsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(TEST_IDS))),
                createDefaultTargetingMatcher(adGroupId, YsCookiesAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(YS_COOKIES))),
                createDefaultTargetingMatcher(adGroupId, FeaturesInPPAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(FEATURES_IN_PP))),
                createDefaultTargetingMatcher(adGroupId, YpCookiesAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(YP_COOKIES))),
                createDefaultTargetingMatcher(adGroupId, IsYandexPlusAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, IsPPLoggedInAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, MobileInstalledAppsAdGroupAdditionalTargeting.class,
                        hasProperty("value",
                                containsInAnyOrder(mapSet(MOBILE_INSTALLED_APPS_URLS,
                                        url -> beanDiffer(new MobileInstalledApp()
                                                .withStoreUrl(url)).useCompareStrategy(onlyExpectedFields()))))),
                createDefaultTargetingMatcher(adGroupId, IsDefaultYandexSearchAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(adGroupId, SidsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(SIDS))),
                createDefaultTargetingMatcher(adGroupId, UuidsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(UUIDS))),
                createDefaultTargetingMatcher(adGroupId, DeviceIdsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(DEVICE_IDS))),
                createDefaultTargetingMatcher(adGroupId, PlusUserSegmentsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(PLUS_USER_SEGMENTS))),
                createDefaultTargetingMatcher(adGroupId, SearchTextAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(SEARCH_TEXT))),
                createDefaultTargetingMatcher(adGroupId, TimeAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(TIME_TARGETS)))
        )));
    }

    @Test
    public void addInternalAdGroup_WithRetargetingCondition() {
        var goal = defaultGoalByType(GoalType.INTERESTS);
        var request = createRequest(createCorrectAddItem(campaignId)
                .withRetargetingConditions(List.of(getRetargetingCondition(goal))));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, request, operator);
        validateResponseSuccessful(payload);

        Long groupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();

        RetargetingCondition expectedRetargetingCondition = (RetargetingCondition) new RetargetingCondition()
                .withType(ConditionType.interests)
                .withRules(List.of(
                        new Rule()
                                .withType(RuleType.OR)
                                .withInterestType(CryptaInterestType.all)
                                .withGoals(List.of(
                                        (Goal) new Goal()
                                                .withId(goal.getId())
                                                .withTime(goal.getTime())
                                ))
                ))
                .withClientId(clientInfo.getClientId().asLong())
                .withDeleted(false)
                .withAutoRetargeting(false);

        checkInternalAdGroupRetargetingConditionInDb(groupId, expectedRetargetingCondition);
    }


    @Test
    public void addInternalAdGroup_WithBadRetargetingCondition_ReturnsValidationError() {
        var goal1 = defaultGoalByType(GoalType.INTERESTS);
        var goal2 = defaultGoalByType(GoalType.GOAL);
        var request = createRequest(createCorrectAddItem(campaignId)
                .withRetargetingConditions(List.of(getRetargetingCondition(goal1, goal2))));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, request, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAdGroups.ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdGroupsItem.RETARGETING_CONDITIONS.name()),
                        index(0),
                        field(GdUpdateInternalAdRetargetingConditionItem.CONDITION_RULES.name()),
                        index(0),
                        field(GdRetargetingConditionRuleItemReq.GOALS.name())
                ),
                RetargetingDefects.allGoalsMustBeEitherFromMetrikaOrCrypta())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    private GdUpdateInternalAdRetargetingConditionItem getRetargetingCondition(Goal... goals) {
        var ruleGoals = Arrays.stream(goals)
                .map(goal -> new GdGoalMinimal()
                        .withId(goal.getId())
                        .withTime(goal.getTime()))
                .collect(Collectors.toList());
        return new GdUpdateInternalAdRetargetingConditionItem().withConditionRules(List.of(
                new GdRetargetingConditionRuleItemReq()
                        .withType(OR)
                        .withInterestType(all)
                        .withGoals(ruleGoals)
        ));
    }

    @Test
    public void addInternalAdGroup_PositiveAndNegativeTargetings() {
        GdAdditionalTargetingUnion positiveUnion = new GdAdditionalTargetingUnion()
                .withTargetingYandexUids(createYandexUidsTargeting(List.of("1021110101545123184")));
        GdAdditionalTargetingUnion negativeUnion = new GdAdditionalTargetingUnion()
                .withTargetingYandexUids(createYandexUidsTargeting(List.of("2021110101545123184"))
                        .withTargetingMode(GdAdditionalTargetingMode.FILTERING));
        GdAddInternalAdGroupsItem item = createCorrectAddItem(campaignId)
                .withTargetings(List.of(positiveUnion, negativeUnion));
        GdAddInternalAdGroups input = createRequest(item);

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        List<AdGroupAdditionalTargeting> targetingList =
                adGroupAdditionalTargetingRepository.getByAdGroupId(shard, adGroupId);

        assertThat(targetingList.get(0)).is(matchedBy(createYaUidTargetingMatcher(adGroupId, List.of(
                "1021110101545123184"))));
    }

    private GdAddInternalAdGroups createRequest(GdAddInternalAdGroupsItem... inputItems) {
        return new GdAddInternalAdGroups().withAddItems(asList(inputItems));
    }

    private GdAddInternalAdGroupsItem createCorrectAddItem(Long campaignId) {
        return new GdAddInternalAdGroupsItem()
                .withCampaignId(campaignId)
                .withName(GROUP_NAME)
                .withLevel(GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD_GD)
                .withStartTime(START_TIME)
                .withFinishTime(FINISH_TIME)
                .withRegionIds(GROUP_REGION_IDS);
    }

    private GdAddInternalAdGroupsItem createAddItemWithOneTargeting(Long campaignId, List<String> value) {
        var targetingUnion = new GdAdditionalTargetingUnion().withTargetingYandexUids(createYandexUidsTargeting(value));
        return createCorrectAddItem(campaignId).withTargetings(List.of(targetingUnion));
    }

    private GdAdditionalTargetingYandexUidsRequest createYandexUidsTargeting(List<String> value) {
        return new GdAdditionalTargetingYandexUidsRequest()
                .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                .withJoinType(GdAdditionalTargetingJoinType.ANY)
                .withValue(value);
    }

    private GdAddInternalAdGroupsItem createCorrectAddItemWithTargeting(Long campaignId) {
        var targetings = List.of(
                new GdAdditionalTargetingUnion()
                        .withTargetingHasPassportId(createDefaultTargeting(GdAdditionalTargetingHasPassportIdRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingIsVirused(createDefaultTargeting(GdAdditionalTargetingIsVirusedRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingHasLCookie(createDefaultTargeting(GdAdditionalTargetingHasLCookieRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingInternalNetwork(createDefaultTargeting(GdAdditionalTargetingInternalNetworkRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingIsMobile(createDefaultTargeting(GdAdditionalTargetingIsMobileRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingIsTablet(createDefaultTargeting(GdAdditionalTargetingIsTabletRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingIsTouch(createDefaultTargeting(GdAdditionalTargetingIsTouchRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingYandexUids(createDefaultTargeting(GdAdditionalTargetingYandexUidsRequest::new)
                                .withValue(YAUID_LIST)),
                new GdAdditionalTargetingUnion()
                        .withTargetingQueryReferers(createDefaultTargeting(GdAdditionalTargetingQueryReferersRequest::new)
                                .withValue(REFERERS_LIST)),
                new GdAdditionalTargetingUnion()
                        .withTargetingCallerReferrers(createDefaultTargeting(GdAdditionalTargetingCallerReferrersRequest::new)
                                .withValue(REFERERS_LIST)),
                new GdAdditionalTargetingUnion()
                        .withTargetingInterfaceLangs(createDefaultTargeting(GdAdditionalTargetingInterfaceLangsRequest::new)
                                .withValue(INTERFACE_LANGS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingUserAgents(createDefaultTargeting(GdAdditionalTargetingUserAgentsRequest::new)
                                .withValue(USER_AGENTS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingBrowserEngines(createDefaultTargeting(GdAdditionalTargetingBrowserEnginesRequest::new)
                                .withValue(singletonList(new GdAdditionalTargetingVersioned().withTargetingValueEntryId(1L)
                                        .withMinVersion("1.1").withMaxVersion("2.2")))),
                new GdAdditionalTargetingUnion()
                        .withTargetingBrowserNames(createDefaultTargeting(GdAdditionalTargetingBrowserNamesRequest::new)
                                .withValue(singletonList(new GdAdditionalTargetingVersioned().withTargetingValueEntryId(1L)
                                        .withMinVersion("1.1").withMaxVersion("2.2")))),
                new GdAdditionalTargetingUnion()
                        .withTargetingOsFamilies(createDefaultTargeting(GdAdditionalTargetingOsFamiliesRequest::new)
                                .withValue(asList(
                                        new GdAdditionalTargetingVersioned().withTargetingValueEntryId(2L)
                                                .withMinVersion("1.1").withMaxVersion("2.2"),
                                        new GdAdditionalTargetingVersioned().withTargetingValueEntryId(3L)))),
                new GdAdditionalTargetingUnion()
                        .withTargetingOsNames(createDefaultTargeting(GdAdditionalTargetingOsNamesRequest::new)
                                .withValue(asList(
                                        new GdAdditionalTargetingUatraits().withTargetingValueEntryId(1L),
                                        new GdAdditionalTargetingUatraits().withTargetingValueEntryId(2L)))),
                new GdAdditionalTargetingUnion()
                        .withTargetingDeviceVendors(createDefaultTargeting(GdAdditionalTargetingDeviceVendorsRequest::new)
                                .withValue(singletonList(new GdAdditionalTargetingUatraits().withTargetingValueEntryId(1L)))),
                new GdAdditionalTargetingUnion()
                        .withTargetingDeviceNames(createDefaultTargeting(GdAdditionalTargetingDeviceNamesRequest::new)
                                .withValue(DEVICE_NAMES)),
                new GdAdditionalTargetingUnion()
                        .withTargetingShowDates(createDefaultTargeting(GdAdditionalTargetingShowDatesRequest::new)
                                .withValue(SHOW_DATES)),
                new GdAdditionalTargetingUnion()
                        .withTargetingDesktopInstalledApps(createDefaultTargeting(GdAdditionalTargetingDesktopInstalledAppsRequest::new)
                                .withValue(DESKTOP_INSTALLED_APPS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingClidTypes(createDefaultTargeting(GdAdditionalTargetingClidTypesRequest::new)
                                .withValue(CLID_TYPES)),
                new GdAdditionalTargetingUnion()
                        .withTargetingClids(createDefaultTargeting(GdAdditionalTargetingClidsRequest::new)
                                .withValue(CLIDS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingQueryOptions(createDefaultTargeting(GdAdditionalTargetingQueryOptionsRequest::new)
                                .withValue(QUERY_OPTIONS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingTestIds(createDefaultTargeting(GdAdditionalTargetingTestIdsRequest::new)
                                .withValue(TEST_IDS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingYsCookies(createDefaultTargeting(GdAdditionalTargetingYsCookiesRequest::new)
                                .withValue(YS_COOKIES)),
                new GdAdditionalTargetingUnion()
                        .withTargetingFeaturesInPP(createDefaultTargeting(GdAdditionalTargetingFeaturesInPPRequest::new)
                                .withValue(FEATURES_IN_PP)),
                new GdAdditionalTargetingUnion()
                        .withTargetingYpCookies(createDefaultTargeting(GdAdditionalTargetingYpCookiesRequest::new)
                                .withValue(YP_COOKIES)),
                new GdAdditionalTargetingUnion()
                        .withTargetingIsYandexPlus(createDefaultTargeting(GdAdditionalTargetingIsYandexPlusRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingIsPPLoggedIn(createDefaultTargeting(GdAdditionalTargetingIsPPLoggedInRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingMobileInstalledApps(createDefaultTargeting(GdAdditionalTargetingMobileInstalledAppsRequest::new)
                                .withValue(MOBILE_INSTALLED_APPS_URLS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingIsDefaultYandexSearch(createDefaultTargeting(GdAdditionalTargetingIsDefaultYandexSearchRequest::new)),
                new GdAdditionalTargetingUnion()
                        .withTargetingSids(createDefaultTargeting(GdAdditionalTargetingSidsRequest::new)
                                .withValue(SIDS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingUuids(createDefaultTargeting(GdAdditionalTargetingUuidsRequest::new)
                                .withValue(UUIDS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingDeviceIds(createDefaultTargeting(GdAdditionalTargetingDeviceIdsRequest::new)
                                .withValue(DEVICE_IDS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingPlusUserSegments(createDefaultTargeting(GdAdditionalTargetingPlusUserSegmentsRequest::new)
                                .withValue(PLUS_USER_SEGMENTS)),
                new GdAdditionalTargetingUnion()
                        .withTargetingSearchText(createDefaultTargeting(GdAdditionalTargetingSearchTextRequest::new)
                                .withValue(SEARCH_TEXT)),
                new GdAdditionalTargetingUnion()
                        .withTargetingTime(createDefaultTargeting(GdAdditionalTargetingTimeRequest::new)
                                .withValue(new GdTimeTarget()
                                        .withTimeBoard(createTimeBoardOneHourEachDay())
                                        .withEnabledHolidaysMode(false)
                                        .withUseWorkingWeekends(false)
                                        .withIdTimeZone(-1L) // таймзона не задаётся для timeTarget на группах
                                )
                        )
        );
        return createCorrectAddItem(campaignId).withTargetings(targetings);
    }

    private <T extends GdAdditionalTargetingRequest> T createDefaultTargeting(Supplier<T> targetingCreator) {
        T targeting = targetingCreator.get();
        targeting
                .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                .withJoinType(GdAdditionalTargetingJoinType.ANY);
        return targeting;
    }

    private Matcher<Object> createDefaultTargetingMatcher(
            Long adGroupId, Class<? extends AdGroupAdditionalTargeting> targeting,
            @Nullable Matcher<Object> valueMatcher) {
        Matcher<Object> commonMatcher = allOf(
                instanceOf(targeting),
                hasProperty("adGroupId", equalTo(adGroupId)),
                hasProperty("targetingMode", equalTo(AdGroupAdditionalTargetingMode.TARGETING)),
                hasProperty("joinType", equalTo(AdGroupAdditionalTargetingJoinType.ANY))
        );

        if (valueMatcher == null) {
            return commonMatcher;
        }

        return allOf(commonMatcher, valueMatcher);
    }

    private Matcher<Object> createYaUidTargetingMatcher(Long adGroupId) {
        return createYaUidTargetingMatcher(adGroupId, YAUID_LIST);
    }

    private Matcher<Object> createYaUidTargetingMatcher(Long adGroupId, List<String> value) {
        return createDefaultTargetingMatcher(adGroupId, YandexUidsAdGroupAdditionalTargeting.class,
                hasProperty("value", equalTo(value)));
    }

    void checkInternalAdGroupRetargetingConditionInDb(Long adGroupId, RetargetingCondition expected) {
        var retargetingConditions = retargetingConditionRepository.getRetConditionsByAdGroupIds(
                shard, singletonList(adGroupId)).getOrDefault(adGroupId, List.of());

        MatcherAssert.assertThat(retargetingConditions, contains(
                beanDiffer(expected).useCompareStrategy(
                        allFieldsExcept(
                                newPath("id"),
                                newPath("name"),
                                newPath("lastChangeTime")))
        ));
    }

    private static List<List<Integer>> createTimeBoardOneHourEachDay() {
        List<List<Integer>> result = new ArrayList<>(DAYS_PER_WEEK);
        for (int i = 0; i < DAYS_PER_WEEK; i++) {
            List<Integer> day = new ArrayList<>(Collections.nCopies(HOURS_PER_DAY, 0));
            day.set(0, 100);
            result.add(i, day);
        }
        return result;
    }

}
