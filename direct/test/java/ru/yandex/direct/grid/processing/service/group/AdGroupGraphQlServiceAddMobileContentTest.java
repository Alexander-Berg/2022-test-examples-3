package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingConditionValidationData;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalStatus;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdAgeType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdGenderType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographics;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographicsAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddMobileContentAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddMobileContentAdGroupItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupInterestItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupKeywordItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRelevanceMatchItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRetargetingItem;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.utils.FunctionalUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.math.RoundingMode.CEILING;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.deviceTargetingIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.invalidMinOsVersion;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.minOsVersionIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.networkTargetingIsNotSet;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.inconsistentStateTargetingCategoryUnavailable;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentFromStoreUrl;
import static ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType.DEMOGRAPHY_MULTIPLIER;
import static ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddMobileContentCampaign.NETWORK_TARGETING;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupDeviceTypeTargeting.PHONE;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupDeviceTypeTargeting.TABLET;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupNetworkTargeting.CELLULAR;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupNetworkTargeting.WI_FI;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdAddMobileContentAdGroup.ADD_ITEMS;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdAddMobileContentAdGroupItem.DEVICE_TYPE_TARGETING;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdAddMobileContentAdGroupItem.INTERESTS;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGroupGraphQlServiceAddMobileContentTest {
    private static final String AD_GROUP_NAME = RandomStringUtils.randomAlphanumeric(16);
    private static final List<Integer> CAMPAIGN_GEO = List.of((int) MOSCOW_REGION_ID, (int) SAINT_PETERSBURG_REGION_ID);
    private static final List<String> MINUS_KEYWORDS = List.of("minus1", "minus2");
    private static final String CURRENT_MINIMAL_OS_VERSION = "8.0";
    private static final Long CATEGORY_ID1 = 154L;
    private static final BigInteger IMPORT_ID1 = BigInteger.valueOf(5100L);
    private static final Long CATEGORY_ID2 = 155L;
    private static final BigInteger IMPORT_ID2 = BigInteger.valueOf(5105L);
    private static final Long CATEGORY_ID_NOT_AVAILABLE = 156L;
    private static final BigInteger IMPORT_ID_NOT_AVAILABLE = BigInteger.valueOf(5155L);
    private static final BigDecimal PRICE = BigDecimal.valueOf(111).setScale(2, CEILING);
    private static final String KEYWORD = "keyword";
    public static final List<String> PAGE_GROUP_TAGS = List.of("aaa");
    public static final List<String> TARGET_TAGS = List.of("bbb");
    private static final String ADD_MUTATION_NAME = "addMobileContentAdGroups";
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.ya.test";
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
    private static final GraphQlTestExecutor.TemplateMutation<GdAddMobileContentAdGroup, GdAddAdGroupPayload> ADD_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(ADD_MUTATION_NAME, ADD_MUTATION_TEMPLATE,
                    GdAddMobileContentAdGroup.class, GdAddAdGroupPayload.class);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private Steps steps;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;
    @Autowired
    private TestTargetingCategoriesRepository testTargetingCategoriesRepository;
    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;
    @Autowired
    private RetargetingRepository retargetingRepository;
    @Autowired
    private BidModifierRepository bidModifierRepository;
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private AdGroupMutationService adGroupMutationService;

    private Integer shard;
    private ClientId clientId;
    private User operator;
    private Long campaignId;
    private Long retargetingConditionId;
    private MobileContent mobileContent;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientAndUser();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();

        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        MobileContentInfo mobileContentInfo = steps.mobileContentSteps().createMobileContent(
                new MobileContentInfo()
                        .withClientInfo(clientInfo)
                        .withMobileContent(mobileContentFromStoreUrl(STORE_URL)));
        mobileContent = mobileContentInfo.getMobileContent();

        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, mobileContentInfo, STORE_URL);
        Long mobileAppId = mobileAppInfo.getMobileAppId();

        Campaign campaign = TestCampaigns.activeMobileContentCampaign(clientId, clientInfo.getUid())
                .withGeo(new HashSet<>(CAMPAIGN_GEO));
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        campaignId = campaignInfo.getCampaignId();

        campaignRepository.setMobileAppIds(shard, Map.of(campaignId, mobileAppId));

        TargetingCategory targetingCategory1 =
                new TargetingCategory(CATEGORY_ID1, null, "", "", IMPORT_ID1, true);
        TargetingCategory targetingCategory2 =
                new TargetingCategory(CATEGORY_ID2, CATEGORY_ID_NOT_AVAILABLE, "", "", IMPORT_ID2, true);
        TargetingCategory targetingCategoryNotAvailable =
                new TargetingCategory(CATEGORY_ID_NOT_AVAILABLE, null, "", "", IMPORT_ID_NOT_AVAILABLE, false);
        testTargetingCategoriesRepository.addTargetingCategory(targetingCategory1);
        testTargetingCategoriesRepository.addTargetingCategory(targetingCategory2);
        testTargetingCategoriesRepository.addTargetingCategory(targetingCategoryNotAvailable);

        retargetingConditionId = steps.retargetingSteps().createDefaultRetargeting(campaignInfo).getRetConditionId();
    }

    @SuppressWarnings("unused")
    private static Object[] networkTargetingParameters() {
        return new Object[][]{
                {"Передан список из WI_FI -> нет ошибок", Set.of(WI_FI), null},
                {"Передан список из CELLULAR -> нет ошибок", Set.of(CELLULAR), null},
                {"Передан список из WI_FI,CELLULAR -> нет ошибок", Set.of(WI_FI, CELLULAR), null},
                {"Передан пустой список networkTargeting -> NETWORK_TARGETING_IS_NOT_SET",
                        emptySet(), networkTargetingIsNotSet()},
        };
    }

    /**
     * Проверка добавления группы при разных значениях таргетинга на тип подключения к сети (networkTargeting)
     */
    @Test
    @Parameters(method = "networkTargetingParameters")
    @TestCaseName("{0}")
    public void addAdGroups_CheckNetworkTargeting(@SuppressWarnings("unused") String description,
                                                  Set<GdMobileContentAdGroupNetworkTargeting> networkTargeting,
                                                  @Nullable Defect expectedDefect) {
        GdAddMobileContentAdGroup gdAddMobileContentAdGroup = getMobileContentAdGroupForSending(
                networkTargeting, Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdAddAdGroupPayload payload = graphQlTestExecutor
                .doMutationAndGetPayload(ADD_MUTATION, gdAddMobileContentAdGroup, operator);

        if (expectedDefect == null) {
            validateResponseSuccessful(payload);
            assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));
            Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();

            MobileContentAdGroup expectedAdGroup = getExpectedMobileContentAdGroup(networkTargeting, Set.of(PHONE));

            checkAddedAdGroup(adGroupId, expectedAdGroup, List.of(IMPORT_ID1));
        } else {
            checkErrors(payload,
                    path(field(ADD_ITEMS.name()), index(0), field(NETWORK_TARGETING.name())),
                    expectedDefect);
        }
    }

    @SuppressWarnings("unused")
    private static Object[] deviceTypeTargetingParameters() {
        return new Object[][]{
                {"Передан список из PHONE -> нет ошибок", Set.of(PHONE), null},
                {"Передан список из TABLET -> нет ошибок", Set.of(TABLET), null},
                {"Передан список из PHONE,TABLET -> нет ошибок", Set.of(PHONE, TABLET), null},
                {"Передан пустой список deviceTypeTargeting -> DEVICE_TARGETING_IS_NOT_SET",
                        Set.of(), deviceTargetingIsNotSet()},
        };
    }

    /**
     * Проверка добавления группы при разных значениях таргетинга на мобильное устройство (DeviceTypeTargeting)
     */
    @Test
    @Parameters(method = "deviceTypeTargetingParameters")
    @TestCaseName("{0}")
    public void addAdGroups_CheckDeviceTargeting(@SuppressWarnings("unused") String description,
                                                 Set<GdMobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings,
                                                 @Nullable Defect expectedDefect) {
        GdAddMobileContentAdGroup gdAddMobileContentAdGroup = getMobileContentAdGroupForSending(
                Set.of(WI_FI), deviceTypeTargetings, CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddMobileContentAdGroup, operator);

        if (expectedDefect == null) {
            validateResponseSuccessful(payload);
            assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));
            Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();

            MobileContentAdGroup expectedAdGroup = getExpectedMobileContentAdGroup(Set.of(WI_FI), deviceTypeTargetings);

            checkAddedAdGroup(adGroupId, expectedAdGroup, List.of(IMPORT_ID1));
        } else {
            checkErrors(payload,
                    path(field(ADD_ITEMS.name()), index(0), field(DEVICE_TYPE_TARGETING.name())),
                    expectedDefect);
        }
    }

    @SuppressWarnings("unused")
    private static Object[] currentMinimalOsVersionParameters() {
        return new Object[][]{
                {"Передан не входящий в список доступных minOs", "0.01", invalidMinOsVersion()},
                {"Передан не числовой minOs", "qwerty", invalidMinOsVersion()},
                {"Передан пустой minOs", "", minOsVersionIsNotSet()},
        };
    }

    /**
     * Проверка что группа не создается при отправке неправильных значений минимальной версии ОС
     */
    @Test
    @Parameters(method = "currentMinimalOsVersionParameters")
    @TestCaseName("{0}")
    public void addAdGroups_WithWrongCurrentMinimalOsVersion(@SuppressWarnings("unused") String description,
                                                             String currentMinimalOsVersion,
                                                             @Nullable Defect expectedDefect) {
        GdAddMobileContentAdGroup gdAddMobileContentAdGroup = getMobileContentAdGroupForSending(
                Set.of(WI_FI), Set.of(PHONE), currentMinimalOsVersion, Set.of(CATEGORY_ID1));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddMobileContentAdGroup, operator);

        checkErrors(payload,
                path(field(ADD_ITEMS.name()), index(0), field(MINIMAL_OPERATING_SYSTEM_VERSION.name())),
                expectedDefect);
    }

    @SuppressWarnings("unused")
    private static Object[] targetingCategoriesParameters() {
        return new Object[][]{
                {"Передано две цели -> нет ошибок",
                        Map.of(CATEGORY_ID1, IMPORT_ID1, CATEGORY_ID2, IMPORT_ID2), null},
                {"Цели не переданны -> нет ошибок",
                        emptyMap(), null},
                {"Передана недоступная цель -> INCONSISTENT_STATE_TARGETING_CATEGORY",
                        Map.of(CATEGORY_ID_NOT_AVAILABLE, IMPORT_ID_NOT_AVAILABLE),
                        inconsistentStateTargetingCategoryUnavailable()},
                {"Передана не существующая цель -> INCONSISTENT_STATE_TARGETING_CATEGORY",
                        Map.of(12345L, BigInteger.valueOf(54321L)),
                        inconsistentStateTargetingCategoryUnavailable()},
        };
    }

    /**
     * Проверка создания группы при отправке разных значений категории таргетинга (targeting_categories.category_id)
     * + проверка порядка следования (сначала ретаргетинг затем интерес с индексом = 1)
     */
    @Test
    @Parameters(method = "targetingCategoriesParameters")
    @TestCaseName("{0}")
    public void addAdGroups_CheckTargetingCategories(@SuppressWarnings("unused") String description,
                                                     Map<Long, BigInteger> categoryIdToImportId,
                                                     @Nullable Defect expectedDefect) {
        GdAddMobileContentAdGroup gdAddMobileContentAdGroup = getMobileContentAdGroupForSending(
                Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, categoryIdToImportId.keySet());

        if (expectedDefect == null) {
            GdAddAdGroupPayload payload =
                    graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddMobileContentAdGroup, operator);

            validateResponseSuccessful(payload);
            assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));
            Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();

            MobileContentAdGroup expectedAdGroup = getExpectedMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE));

            checkAddedAdGroup(adGroupId, expectedAdGroup, categoryIdToImportId.values());
        } else {
            thrown.expect(GridValidationException.class);
            thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(
                    path(field(ADD_ITEMS.name()), index(0), field(INTERESTS.name()), index(0), field("categoryId")),
                    expectedDefect))));

            adGroupMutationService.addMobileContentAdGroups(operator, operator.getUid(), gdAddMobileContentAdGroup);
        }
    }

    /**
     * Проверка добавления двух групп в одном запросе с одинаковой категорией таргетинга
     */
    @Test
    public void addAdGroups_AddTwoGroupsWithSameTargetingCategory() {
        GdAddMobileContentAdGroupItem mobileContentAdGroupItem1 = getMobileContentAdGroupItemForSending(
                Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));
        GdAddMobileContentAdGroupItem mobileContentAdGroupItem2 = getMobileContentAdGroupItemForSending(
                Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));
        GdAddMobileContentAdGroup gdAddMobileContentAdGroup = new GdAddMobileContentAdGroup()
                .withAddItems(List.of(mobileContentAdGroupItem1, mobileContentAdGroupItem2));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddMobileContentAdGroup, operator);

        validateResponseSuccessful(payload);
        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(2));
        Long adGroupId1 = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        Long adGroupId2 = payload.getAddedAdGroupItems().get(1).getAdGroupId();

        MobileContentAdGroup expectedAdGroup = getExpectedMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE));

        checkAddedAdGroup(adGroupId1, expectedAdGroup, List.of(IMPORT_ID1));
        checkAddedAdGroup(adGroupId2, expectedAdGroup, List.of(IMPORT_ID1));
    }

    /**
     * Проверка добавления двух групп в одном запросе с разными категориями таргетинга
     */
    @Test
    public void addAdGroups_AddTwoGroupsWithDifferentTargetingCategories() {
        GdAddMobileContentAdGroupItem mobileContentAdGroupItem1 = getMobileContentAdGroupItemForSending(
                Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));
        GdAddMobileContentAdGroupItem mobileContentAdGroupItem2 = getMobileContentAdGroupItemForSending(
                Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID2));
        GdAddMobileContentAdGroup gdAddMobileContentAdGroup = new GdAddMobileContentAdGroup()
                .withAddItems(List.of(mobileContentAdGroupItem1, mobileContentAdGroupItem2));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddMobileContentAdGroup, operator);

        validateResponseSuccessful(payload);
        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(2));
        Long adGroupId1 = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        Long adGroupId2 = payload.getAddedAdGroupItems().get(1).getAdGroupId();

        MobileContentAdGroup expectedAdGroup = getExpectedMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE));

        checkAddedAdGroup(adGroupId1, expectedAdGroup, List.of(IMPORT_ID1));
        checkAddedAdGroup(adGroupId2, expectedAdGroup, List.of(IMPORT_ID2));
    }

    /**
     * Проверка добавления двух групп в разных запросах с однаковой категорией таргетинга
     */
    @Test
    public void addAdGroups_AddTwoGroupsWithSameTargetingCategoryInDifferentRequests() {
        MobileContentAdGroup expectedAdGroup = getExpectedMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE));

        GdAddMobileContentAdGroup mobileContentAdGroup1 = getMobileContentAdGroupForSending(
                Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdAddAdGroupPayload payload1 =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, mobileContentAdGroup1, operator);

        validateResponseSuccessful(payload1);
        assumeThat(payload1.getAddedAdGroupItems(), iterableWithSize(1));
        Long adGroupId1 = payload1.getAddedAdGroupItems().get(0).getAdGroupId();

        checkAddedAdGroup(adGroupId1, expectedAdGroup, List.of(IMPORT_ID1));

        GdAddMobileContentAdGroup mobileContentAdGroup2 = getMobileContentAdGroupForSending(
                Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdAddAdGroupPayload payload2 =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, mobileContentAdGroup2, operator);
        assumeThat(payload2.getAddedAdGroupItems(), iterableWithSize(1));
        Long adGroupId2 = payload2.getAddedAdGroupItems().get(0).getAdGroupId();

        checkAddedAdGroup(adGroupId2, expectedAdGroup, List.of(IMPORT_ID1));
    }

    /**
     * Проверяем что при успешном создании интереса было корректно заполнено поле condition_json (без лишних полей)
     */
    @Test
    public void addAdGroups_CheckThatConditionJsonIsFilledCorrectly() {
        GdAddMobileContentAdGroup mobileContentAdGroup1 = getMobileContentAdGroupForSending(
                Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, mobileContentAdGroup1, operator);

        validateResponseSuccessful(payload);
        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));
        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();

        String expectRulesJson = "[{\"goals\":[{\"goal_id\":" + IMPORT_ID1 + ",\"time\":90}],\"type\":\"all\"}]";

        List<RetargetingCondition> actualRetConditions = retargetingConditionRepository
                .getRetConditionsByAdGroupIds(shard, singletonList(adGroupId)).get(adGroupId);
        List<RetargetingCondition> actualInterestRetConditions =
                filterList(actualRetConditions, rc -> !rc.getId().equals(retargetingConditionId));

        List<RetargetingConditionValidationData> validationData =
                retargetingConditionRepository.getValidationData(shard, clientId);
        List<RetargetingConditionValidationData> validationDataForInterests =
                filterList(validationData, vd -> !retargetingConditionId.equals(vd.getId()));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualInterestRetConditions).as("список условий ретаргетинга интересов")
                    .hasSize(1);

            var expectedValidationData = new RetargetingConditionValidationData(
                    actualInterestRetConditions.get(0).getId(), "", expectRulesJson);

            soft.assertThat(validationDataForInterests).as("created count of interests").hasSize(1);
            soft.assertThat(validationDataForInterests.get(0)).as("interest data")
                    .is(matchedBy(beanDiffer(expectedValidationData).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void addAdGroups_PiTags() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.TARGET_TAGS_ALLOWED, true);
        GdAddMobileContentAdGroupItem mobileContentAdGroupItem1 = getMobileContentAdGroupItemForSending(
                Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1))
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withTargetTags(TARGET_TAGS);
        GdAddMobileContentAdGroup gdAddMobileContentAdGroup = new GdAddMobileContentAdGroup()
                .withAddItems(List.of(mobileContentAdGroupItem1));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddMobileContentAdGroup, operator);

        validateResponseSuccessful(payload);
        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));
        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();

        MobileContentAdGroup expectedAdGroup = getExpectedMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE))
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withTargetTags(TARGET_TAGS);

        checkAddedAdGroup(adGroupId, expectedAdGroup, List.of(IMPORT_ID1));
    }

    /**
     * Проверка добавленной группы
     */
    private void checkAddedAdGroup(Long adGroupId,
                                   AdGroup expectedMobileContentAdGroup,
                                   Collection<BigInteger> importIds) {
        BidModifier expectedBidModifier = getExpectedBidModifier();
        Set<String> expectedKeywords = Set.of(KEYWORD);
        Retargeting expectedInterestRetargeting = new Retargeting().withPriceContext(PRICE);
        List<RetargetingCondition> expectedInterestRetargetingConditions =
                mapList(importIds, this::getExpectedInterestRetargetingCondition);
        List<RetargetingCondition> expectedRetargetingConditions = getExpectedRetargetingConditions();
        Map<Long, RetargetingCondition> expectedImportIdToInterestRetargetingCondition =
                StreamEx.of(expectedInterestRetargetingConditions)
                        .mapToEntry(rc -> rc.getRules().get(0).getGoals().get(0).getId())
                        .invert()
                        .toMap();

        MobileContentAdGroup actualAdGroup =
                (MobileContentAdGroup) adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        List<RetargetingCondition> actualRetConditions = nvl(retargetingConditionRepository
                .getRetConditionsByAdGroupIds(shard, singletonList(adGroupId)).get(adGroupId), emptyList());

        List<Retargeting> actualRetargetings = retargetingRepository
                .getRetargetingsByAdGroups(shard, singletonList(adGroupId));

        List<BidModifier> actualBidModifiers = bidModifierRepository
                .getByAdGroupIds(shard, singletonMap(adGroupId, campaignId),
                        singleton(BidModifierType.DEMOGRAPHY_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP));

        Map<Long, List<Keyword>> allKeywords = keywordService
                .getKeywordsByAdGroupIds(clientId, singleton(adGroupId));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualAdGroup).as("группа")
                    .is(matchedBy(beanDiffer(expectedMobileContentAdGroup)
                            .useCompareStrategy(onlyExpectedFields())));

            soft.assertThat(actualBidModifiers).as("список корректировок ставок")
                    .hasSize(1);
            soft.assertThat(actualBidModifiers.get(0)).as("корректировоки ставок")
                    .is(matchedBy(beanDiffer(expectedBidModifier)
                            .useCompareStrategy(onlyExpectedFields())));

            soft.assertThat(allKeywords).as("список ключевых слов")
                    .hasSize(expectedKeywords.size());
            soft.assertThat(allKeywords.get(adGroupId)).as("список ключевых слов группы")
                    .hasSize(1);
            soft.assertThat(allKeywords.get(adGroupId).get(0).getPhrase()).as("ключевое слово")
                    .isEqualTo(KEYWORD);

            soft.assertThat(actualRetConditions).as("список условий ретаргетинга")
                    .hasSize(expectedImportIdToInterestRetargetingCondition.size() + expectedRetargetingConditions.size());
            soft.assertThat(actualRetargetings).as("список ретаргетингов")
                    .hasSize(expectedImportIdToInterestRetargetingCondition.size() + expectedRetargetingConditions.size());

            for (int i = 0; i < actualRetConditions.size(); i++) {
                RetargetingCondition rc = actualRetConditions.get(i);
                if (!rc.getId().equals(retargetingConditionId)) {
                    soft.assertThat(rc.getRules()).as("rule условия ретаргетинга интереса")
                            .isNotNull().hasSize(1);
                    soft.assertThat(rc.getRules().get(0).getGoals()).as("goal условия ретаргетинга интереса")
                            .isNotNull().hasSize(1);
                    soft.assertThat(expectedImportIdToInterestRetargetingCondition)
                            .as("id цели условия ретаргетинга интереса")
                            .containsKey(rc.getRules().get(0).getGoals().get(0).getId());

                    long importId = rc.getRules().get(0).getGoals().get(0).getId();
                    soft.assertThat(rc).as("условие ретаргетинга интереса")
                            .is(matchedBy(beanDiffer(expectedImportIdToInterestRetargetingCondition.get(importId))
                                    .useCompareStrategy(onlyExpectedFields())));
                } else {
                    soft.assertThat(rc).as("условие ретаргетинга")
                            .is(matchedBy(beanDiffer(expectedRetargetingConditions.get(0))
                                    .useCompareStrategy(onlyExpectedFields())));
                }

                soft.assertThat(actualRetargetings.get(i)).as("ретаргетинг")
                        .is(matchedBy(beanDiffer(expectedInterestRetargeting)
                                .useCompareStrategy(onlyExpectedFields())));
            }
        });
    }

    /**
     * Проверка полученной ошибки
     */
    private void checkErrors(GdAddAdGroupPayload payload, Path path, Defect expectedDefect) {
        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path, expectedDefect)
                .withWarnings(null);

        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    private GdAddMobileContentAdGroup getMobileContentAdGroupForSending(
            Set<GdMobileContentAdGroupNetworkTargeting> networkTargetings,
            Set<GdMobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings,
            String currentMinimalOsVersion,
            Set<Long> targetingCategoryIds) {
        return new GdAddMobileContentAdGroup()
                .withAddItems(singletonList(getMobileContentAdGroupItemForSending(networkTargetings,
                        deviceTypeTargetings, currentMinimalOsVersion, targetingCategoryIds)));
    }

    private GdAddMobileContentAdGroupItem getMobileContentAdGroupItemForSending(
            Set<GdMobileContentAdGroupNetworkTargeting> networkTargetings,
            Set<GdMobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings,
            String currentMinimalOsVersion,
            Set<Long> targetingCategoryIds) {
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers()
                .withBidModifierDemographics(new GdUpdateBidModifierDemographics()
                        .withCampaignId(campaignId)
                        .withAdjustments(singletonList(new GdUpdateBidModifierDemographicsAdjustmentItem()
                                .withAge(GdAgeType._0_17)
                                .withGender(GdGenderType.FEMALE)
                                .withPercent(10)))
                        .withEnabled(true)
                        .withType(DEMOGRAPHY_MULTIPLIER));

        return new GdAddMobileContentAdGroupItem()
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignId)
                .withAdGroupMinusKeywords(MINUS_KEYWORDS)
                .withCurrentMinimalOsVersion(currentMinimalOsVersion)
                .withBidModifiers(bidModifiers)
                .withDeviceTypeTargeting(deviceTypeTargetings)
                .withNetworkTargeting(networkTargetings)
                .withLibraryMinusKeywordsIds(emptyList())
                .withRegionIds(CAMPAIGN_GEO)
                .withKeywords(List.of(new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD)))
                .withGeneralPrice(PRICE)
                .withRelevanceMatch(new GdUpdateAdGroupRelevanceMatchItem().withIsActive(true))
                .withRetargetings(List.of(new GdUpdateAdGroupRetargetingItem()
                        .withRetCondId(retargetingConditionId)))
                .withInterests(mapList(targetingCategoryIds,
                        categoryId -> new GdUpdateAdGroupInterestItem().withCategoryId(categoryId)));
    }

    private RetargetingCondition getExpectedInterestRetargetingCondition(BigInteger importId) {
        Goal goal = (Goal) new Goal()
                .withType(GoalType.GOAL)
                .withStatus(GoalStatus.ACTIVE)
                .withId(importId.longValue());

        RetargetingCondition expectedRetCondition = new RetargetingCondition();
        expectedRetCondition
                .withInterest(true)
                .withRules(singletonList(new ru.yandex.direct.core.entity.retargeting.model.Rule()
                        .withType(RuleType.ALL)
                        .withGoals(singletonList((Goal) new Goal().withId(goal.getId())))));
        return expectedRetCondition;
    }

    private List<RetargetingCondition> getExpectedRetargetingConditions() {
        Goal goalActive = (Goal) new Goal()
                .withType(GoalType.GOAL);
        Goal goalSegment = (Goal) new Goal()
                .withType(GoalType.SEGMENT);
        Goal goalEcommerce = (Goal) new Goal()
                .withType(GoalType.ECOMMERCE);
        Goal goalAudience = (Goal) new Goal()
                .withType(GoalType.AUDIENCE);
        Goal goalAbSegment = (Goal) new Goal()
                .withType(GoalType.AB_SEGMENT);
        Goal goalCdpSegment = (Goal) new Goal()
                .withType(GoalType.CDP_SEGMENT);

        RetargetingCondition expectedRetCondition = new RetargetingCondition();
        expectedRetCondition
                .withId(retargetingConditionId)
                .withRules(singletonList(new ru.yandex.direct.core.entity.retargeting.model.Rule()
                        .withType(RuleType.ALL)
                        .withGoals(List.of(goalActive, goalSegment, goalEcommerce, goalAudience, goalAbSegment,
                                goalCdpSegment))));
        return singletonList(expectedRetCondition);
    }

    private MobileContentAdGroup getExpectedMobileContentAdGroup(
            Set<GdMobileContentAdGroupNetworkTargeting> networkTargetings,
            Set<GdMobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings) {
        return new MobileContentAdGroup()
                .withType(AdGroupType.MOBILE_CONTENT)
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignId)
                .withMinusKeywords(MINUS_KEYWORDS)
                .withMinimalOperatingSystemVersion(CURRENT_MINIMAL_OS_VERSION)
                .withStoreUrl(STORE_URL)
                .withDeviceTypeTargeting(mapSet(deviceTypeTargetings,
                        dt -> MobileContentAdGroupDeviceTypeTargeting.fromTypedValue(dt.getTypedValue())))
                .withNetworkTargeting(mapSet(networkTargetings,
                        nt -> MobileContentAdGroupNetworkTargeting.fromTypedValue(nt.getTypedValue())))
                .withLibraryMinusKeywordsIds(emptyList())
                .withGeo(FunctionalUtils.mapList(CAMPAIGN_GEO, Integer::longValue))
                .withMobileContent(new MobileContent()
                        .withOsType(OsType.ANDROID)
                        .withMinOsVersion(mobileContent.getMinOsVersion())
                        .withOsType(mobileContent.getOsType()));
    }

    private BidModifier getExpectedBidModifier() {
        return new BidModifierDemographics()
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withCampaignId(campaignId)
                .withEnabled(true)
                .withDemographicsAdjustments(List.of(new BidModifierDemographicsAdjustment()
                        .withGender(GenderType.FEMALE)
                        .withAge(AgeType._0_17)
                        .withPercent(10)));
    }
}
