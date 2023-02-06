package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
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
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdAgeType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdGenderType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographics;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographicsAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupInterestItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupKeywordItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRelevanceMatchItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRetargetingItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMobileContentAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMobileContentAdGroupItem;
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
import static ru.yandex.direct.core.entity.adgroup.container.ComplexMobileContentAdGroup.TARGET_INTERESTS;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.deviceTargetingIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.invalidMinOsVersion;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.minOsVersionIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.networkTargetingIsNotSet;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.illegalCharacters;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.audienceTargetNotFound;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.inconsistentStateTargetingCategoryUnavailable;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.notFoundRetargeting;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentFromStoreUrl;
import static ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType.DEMOGRAPHY_MULTIPLIER;
import static ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMobileContentCampaign.NETWORK_TARGETING;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupDeviceTypeTargeting.PHONE;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupDeviceTypeTargeting.TABLET;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupNetworkTargeting.CELLULAR;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupNetworkTargeting.WI_FI;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdAddMobileContentAdGroupItem.INTERESTS;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMobileContentAdGroup.UPDATE_ITEMS;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMobileContentAdGroupItem.DEVICE_TYPE_TARGETING;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMobileContentAdGroupItem.KEYWORDS;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
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
public class AdGroupGraphQlServiceUpdateMobileContentTest {
    private static final String AD_GROUP_NAME = RandomStringUtils.randomAlphanumeric(16);
    private static final List<Integer> CAMPAIGN_GEO = List.of((int) MOSCOW_REGION_ID, (int) SAINT_PETERSBURG_REGION_ID);
    private static final List<String> MINUS_KEYWORDS = List.of("minus1", "minus2");
    private static final String CURRENT_MINIMAL_OS_VERSION = "8.0";
    private static final Long CATEGORY_ID1 = 254L;
    private static final BigInteger IMPORT_ID1 = BigInteger.valueOf(5200L);
    private static final Long CATEGORY_ID2 = 255L;
    private static final BigInteger IMPORT_ID2 = BigInteger.valueOf(5205L);
    private static final Long CATEGORY_ID_NOT_AVAILABLE = 256L;
    private static final BigInteger IMPORT_ID_NOT_AVAILABLE = BigInteger.valueOf(5255L);
    private static final BigDecimal PRICE = BigDecimal.valueOf(111).setScale(2, CEILING);
    private static final String KEYWORD = "keyword";
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.ya.test";
    public static final List<String> PAGE_GROUP_TAGS = List.of("aaa");
    public static final List<String> TARGET_TAGS = List.of("bbb");
    private static final String UPDATE_MUTATION_NAME = "updateMobileContentAdGroup";
    private static final String UPDATE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedAdGroupItems {\n"
            + "         adGroupId,\n"
            + "     }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateMobileContentAdGroup, GdUpdateAdGroupPayload> UPDATE_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(UPDATE_MUTATION_NAME, UPDATE_MUTATION_TEMPLATE,
                    GdUpdateMobileContentAdGroup.class, GdUpdateAdGroupPayload.class);

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
    private Long campaignId1;
    private Long campaignId2;
    private Long adGroupId1;
    private Long adGroupId2;
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

        AdGroupInfo adGroupInfo1 =
                steps.adGroupSteps().createActiveMobileContentAdGroup(mobileAppInfo.getMobileContentInfo());
        adGroupId1 = adGroupInfo1.getAdGroupId();
        AdGroupInfo adGroupInfo2 =
                steps.adGroupSteps().createActiveMobileContentAdGroup(mobileAppInfo.getMobileContentInfo());
        adGroupId2 = adGroupInfo2.getAdGroupId();
        campaignId1 = adGroupInfo1.getCampaignId();
        campaignId2 = adGroupInfo2.getCampaignId();

        campaignRepository.setMobileAppIds(shard, Map.of(campaignId1, mobileAppId, campaignId2, mobileAppId));

        TargetingCategory targetingCategory1 =
                new TargetingCategory(CATEGORY_ID1, null, "", "", IMPORT_ID1, true);
        TargetingCategory targetingCategory2 =
                new TargetingCategory(CATEGORY_ID2, CATEGORY_ID_NOT_AVAILABLE, "", "", IMPORT_ID2, true);
        TargetingCategory targetingCategoryNotAvailable =
                new TargetingCategory(CATEGORY_ID_NOT_AVAILABLE, null, "", "", IMPORT_ID_NOT_AVAILABLE, false);
        testTargetingCategoriesRepository.addTargetingCategory(targetingCategory1);
        testTargetingCategoriesRepository.addTargetingCategory(targetingCategory2);
        testTargetingCategoriesRepository.addTargetingCategory(targetingCategoryNotAvailable);

        retargetingConditionId = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo1.getCampaignInfo())
                .getRetConditionId();
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
     * Проверка обновления группы при разных значениях таргетинга на тип подключения к сети (networkTargeting)
     */
    @Test
    @Parameters(method = "networkTargetingParameters")
    @TestCaseName("{0}")
    public void updateAdGroups_CheckNetworkTargeting(@SuppressWarnings("unused") String description,
                                                     Set<GdMobileContentAdGroupNetworkTargeting> networkTargeting,
                                                     @Nullable Defect expectedDefect) {
        GdUpdateMobileContentAdGroup gdUpdateMobileContentAdGroup = getMobileContentAdGroupForSending(campaignId1,
                adGroupId1, networkTargeting, Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor
                .doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateMobileContentAdGroup, operator);

        if (expectedDefect == null) {
            validateResponseSuccessful(payload);
            assumeThat(payload.getUpdatedAdGroupItems(), iterableWithSize(1));

            MobileContentAdGroup expectedAdGroup =
                    getExpectedMobileContentAdGroup(campaignId1, adGroupId1, networkTargeting, Set.of(PHONE));

            checkUpdatedAdGroup(campaignId1, adGroupId1, expectedAdGroup, List.of(IMPORT_ID1));
        } else {
            checkErrors(payload,
                    path(field(UPDATE_ITEMS.name()), index(0), field(NETWORK_TARGETING.name())),
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
     * Проверка обновления группы при разных значениях таргетинга на мобильное устройство (DeviceTypeTargeting)
     */
    @Test
    @Parameters(method = "deviceTypeTargetingParameters")
    @TestCaseName("{0}")
    public void updateAdGroups_CheckDeviceTargeting(@SuppressWarnings("unused") String description,
                                                    Set<GdMobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings,
                                                    @Nullable Defect expectedDefect) {
        GdUpdateMobileContentAdGroup gdUpdateMobileContentAdGroup = getMobileContentAdGroupForSending(campaignId1,
                adGroupId1, Set.of(WI_FI), deviceTypeTargetings, CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateMobileContentAdGroup, operator);

        if (expectedDefect == null) {
            validateResponseSuccessful(payload);
            assumeThat(payload.getUpdatedAdGroupItems(), iterableWithSize(1));

            MobileContentAdGroup expectedAdGroup =
                    getExpectedMobileContentAdGroup(campaignId1, adGroupId1, Set.of(WI_FI), deviceTypeTargetings);

            checkUpdatedAdGroup(campaignId1, adGroupId1, expectedAdGroup, List.of(IMPORT_ID1));
        } else {
            checkErrors(payload,
                    path(field(UPDATE_ITEMS.name()), index(0), field(DEVICE_TYPE_TARGETING.name())),
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
     * Проверка что группа не обновляется при отправке неправильных значений минимальной версии ОС
     */
    @Test
    @Parameters(method = "currentMinimalOsVersionParameters")
    @TestCaseName("{0}")
    public void updateAdGroups_WithWrongCurrentMinimalOsVersion(@SuppressWarnings("unused") String description,
                                                                String currentMinimalOsVersion,
                                                                @Nullable Defect expectedDefect) {
        GdUpdateMobileContentAdGroup gdUpdateMobileContentAdGroup = getMobileContentAdGroupForSending(campaignId1,
                adGroupId1, Set.of(WI_FI), Set.of(PHONE), currentMinimalOsVersion, Set.of(CATEGORY_ID1));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateMobileContentAdGroup, operator);

        checkErrors(payload,
                path(field(UPDATE_ITEMS.name()), index(0), field(MINIMAL_OPERATING_SYSTEM_VERSION.name())),
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
     * Проверка обновления группы при отправке разных значений категории таргетинга (targeting_categories.category_id)
     */
    @Test
    @Parameters(method = "targetingCategoriesParameters")
    @TestCaseName("{0}")
    public void updateAdGroups_CheckTargetingCategories(@SuppressWarnings("unused") String description,
                                                        Map<Long, BigInteger> categoryIdToImportId,
                                                        @Nullable Defect expectedDefect) {
        GdUpdateMobileContentAdGroup gdUpdateMobileContentAdGroup = getMobileContentAdGroupForSending(campaignId1,
                adGroupId1, Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, categoryIdToImportId.keySet());

        if (expectedDefect == null) {
            GdUpdateAdGroupPayload payload = graphQlTestExecutor
                    .doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateMobileContentAdGroup, operator);

            validateResponseSuccessful(payload);
            assumeThat(payload.getUpdatedAdGroupItems(), iterableWithSize(1));

            MobileContentAdGroup expectedAdGroup =
                    getExpectedMobileContentAdGroup(campaignId1, adGroupId1, Set.of(WI_FI), Set.of(PHONE));

            checkUpdatedAdGroup(campaignId1, adGroupId1, expectedAdGroup, categoryIdToImportId.values());
        } else {
            thrown.expect(GridValidationException.class);
            thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(
                    path(field(UPDATE_ITEMS.name()), index(0), field(INTERESTS.name()), index(0), field("categoryId")),
                    expectedDefect))));

            adGroupMutationService
                    .updateMobileContentAdGroup(clientId, operator.getUid(), gdUpdateMobileContentAdGroup);
        }
    }

    /**
     * Проверка обновления двух групп в одном запросе с одинаковой категорией таргетинга
     */
    @Test
    public void updateAdGroups_TwoGroupsWithSameTargetingCategory() {
        GdUpdateMobileContentAdGroupItem mobileContentAdGroupItem1 = getMobileContentAdGroupItemForSending();
        GdUpdateMobileContentAdGroupItem mobileContentAdGroupItem2 = getMobileContentAdGroupItemForSending(campaignId2,
                adGroupId2, Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));
        GdUpdateMobileContentAdGroup gdUpdateMobileContentAdGroup = new GdUpdateMobileContentAdGroup()
                .withUpdateItems(List.of(mobileContentAdGroupItem1, mobileContentAdGroupItem2));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateMobileContentAdGroup, operator);

        validateResponseSuccessful(payload);
        assumeThat(payload.getUpdatedAdGroupItems(), iterableWithSize(2));

        MobileContentAdGroup expectedAdGroup1 =
                getExpectedMobileContentAdGroup(campaignId1, adGroupId1, Set.of(WI_FI), Set.of(PHONE));
        MobileContentAdGroup expectedAdGroup2 =
                getExpectedMobileContentAdGroup(campaignId2, adGroupId2, Set.of(WI_FI), Set.of(PHONE));

        checkUpdatedAdGroup(campaignId1, adGroupId1, expectedAdGroup1, List.of(IMPORT_ID1));
        checkUpdatedAdGroup(campaignId2, adGroupId2, expectedAdGroup2, List.of(IMPORT_ID1));
    }

    /**
     * Проверка обновления двух групп в одном запросе с разными категориями таргетинга
     */
    @Test
    public void updateAdGroups_TwoGroupsWithDifferentTargetingCategories() {
        GdUpdateMobileContentAdGroupItem mobileContentAdGroupItem1 = getMobileContentAdGroupItemForSending();
        GdUpdateMobileContentAdGroupItem mobileContentAdGroupItem2 = getMobileContentAdGroupItemForSending(campaignId2,
                adGroupId2, Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID2));
        GdUpdateMobileContentAdGroup gdUpdateMobileContentAdGroup = new GdUpdateMobileContentAdGroup()
                .withUpdateItems(List.of(mobileContentAdGroupItem1, mobileContentAdGroupItem2));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateMobileContentAdGroup, operator);

        validateResponseSuccessful(payload);
        assumeThat(payload.getUpdatedAdGroupItems(), iterableWithSize(2));

        MobileContentAdGroup expectedAdGroup1 =
                getExpectedMobileContentAdGroup(campaignId1, adGroupId1, Set.of(WI_FI), Set.of(PHONE));
        MobileContentAdGroup expectedAdGroup2 =
                getExpectedMobileContentAdGroup(campaignId2, adGroupId2, Set.of(WI_FI), Set.of(PHONE));

        checkUpdatedAdGroup(campaignId1, adGroupId1, expectedAdGroup1, List.of(IMPORT_ID1));
        checkUpdatedAdGroup(campaignId2, adGroupId2, expectedAdGroup2, List.of(IMPORT_ID2));
    }

    /**
     * Проверка обновления двух групп в разных запросах с однаковой категорией таргетинга
     */
    @Test
    public void updateAdGroups_TwoGroupsWithSameTargetingCategoryInDifferentRequests() {
        MobileContentAdGroup expectedAdGroup1 =
                getExpectedMobileContentAdGroup(campaignId1, adGroupId1, Set.of(WI_FI), Set.of(PHONE));
        MobileContentAdGroup expectedAdGroup2 =
                getExpectedMobileContentAdGroup(campaignId2, adGroupId2, Set.of(WI_FI), Set.of(PHONE));

        GdUpdateMobileContentAdGroup mobileContentAdGroup1 = getMobileContentAdGroupForSending(campaignId1,
                adGroupId1, Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdUpdateAdGroupPayload payload1 =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, mobileContentAdGroup1, operator);

        validateResponseSuccessful(payload1);
        assumeThat(payload1.getUpdatedAdGroupItems(), iterableWithSize(1));

        checkUpdatedAdGroup(campaignId1, adGroupId1, expectedAdGroup1, List.of(IMPORT_ID1));

        GdUpdateMobileContentAdGroup mobileContentAdGroup2 = getMobileContentAdGroupForSending(campaignId2,
                adGroupId2, Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdUpdateAdGroupPayload payload2 =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, mobileContentAdGroup2, operator);
        assumeThat(payload2.getUpdatedAdGroupItems(), iterableWithSize(1));

        checkUpdatedAdGroup(campaignId2, adGroupId2, expectedAdGroup2, List.of(IMPORT_ID1));
    }

    /**
     * Проверяем что при успешном обновлении интереса было корректно заполнено поле condition_json (без лишних полей)
     */
    @Test
    public void updateAdGroups_CheckThatConditionJsonIsFilledCorrectly() {
        GdUpdateMobileContentAdGroup mobileContentAdGroup1 = getMobileContentAdGroupForSending(campaignId1,
                adGroupId1, Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, mobileContentAdGroup1, operator);

        validateResponseSuccessful(payload);
        assumeThat(payload.getUpdatedAdGroupItems(), iterableWithSize(1));
        Long adGroupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();

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
    public void updateAdGroups_CheckKeywordError() {
        String wrongKeyword = "//wrong_keyword";
        GdUpdateMobileContentAdGroupItem mobileContentAdGroupItem = getMobileContentAdGroupItemForSending()
                .withKeywords(List.of(new GdUpdateAdGroupKeywordItem()
                        .withPhrase(wrongKeyword)));
        GdUpdateMobileContentAdGroup gdUpdateMobileContentAdGroup = new GdUpdateMobileContentAdGroup()
                .withUpdateItems(singletonList(mobileContentAdGroupItem));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateMobileContentAdGroup, operator);

        GdDefect expectedGdDefect = toGdDefect(
                path(field(UPDATE_ITEMS.name()), index(0), field(KEYWORDS.name()), index(0), field("phrase")),
                illegalCharacters(singletonList(wrongKeyword)),
                true);
        assertThat(payload.getValidationResult().getErrors()).containsExactly(expectedGdDefect);
    }

    /**
     * Проверяем ошибки валидации для интереса и ретаргетинга вместе.
     * Проверяем порядок следования (сначала ретаргетинг затем интерес)
     */
    @Test
    public void updateAdGroups_CheckInterestAndRetargetingIdsError() {
        Long worngRetargetingInterestId = 5L;
        Long worngRetargetingId = 6L;
        GdUpdateMobileContentAdGroupItem mobileContentAdGroupItem = getMobileContentAdGroupItemForSending()
                .withInterests(singletonList(new GdUpdateAdGroupInterestItem()
                        .withCategoryId(CATEGORY_ID1)
                        .withId(worngRetargetingInterestId)))
                .withRetargetings(singletonList(new GdUpdateAdGroupRetargetingItem()
                        .withRetCondId(retargetingConditionId)
                        .withId(worngRetargetingId)));
        GdUpdateMobileContentAdGroup gdUpdateMobileContentAdGroup = new GdUpdateMobileContentAdGroup()
                .withUpdateItems(singletonList(mobileContentAdGroupItem));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateMobileContentAdGroup, operator);

        toGdDefect(path(field(UPDATE_ITEMS.name()), index(0), field(TARGET_INTERESTS.name()), index(0), field("id")),
                notFoundRetargeting(), false);

        GdValidationResult expectValidationResult = new GdValidationResult()
                .withErrors(List.of(
                        toGdDefect(path(field(UPDATE_ITEMS.name()), index(0), field(TARGET_INTERESTS.name()),
                                index(0), field("id")), notFoundRetargeting(), false),
                        toGdDefect(path(field(UPDATE_ITEMS.name()), index(0), field(TARGET_INTERESTS.name()),
                                index(1), field("id")), audienceTargetNotFound(), false)
                ))
                .withWarnings(null);

        assertThat(payload.getValidationResult())
                .isNotNull()
                .is(matchedBy(beanDiffer(expectValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateAdGroups_PiTags() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.TARGET_TAGS_ALLOWED, true);

        GdUpdateMobileContentAdGroupItem mobileContentAdGroupItem = getMobileContentAdGroupItemForSending()
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withTargetTags(TARGET_TAGS);
        GdUpdateMobileContentAdGroup gdUpdateMobileContentAdGroup = new GdUpdateMobileContentAdGroup()
                .withUpdateItems(singletonList(mobileContentAdGroupItem));

        GdUpdateAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateMobileContentAdGroup, operator);

        validateResponseSuccessful(payload);
        assumeThat(payload.getUpdatedAdGroupItems(), iterableWithSize(1));

        MobileContentAdGroup expectedAdGroup =
                getExpectedMobileContentAdGroup(campaignId1, adGroupId1, Set.of(WI_FI), Set.of(PHONE))
                        .withPageGroupTags(PAGE_GROUP_TAGS)
                        .withTargetTags(TARGET_TAGS);

        checkUpdatedAdGroup(campaignId1, adGroupId1, expectedAdGroup, List.of(IMPORT_ID1));

    }

    /**
     * Проверка обновленной группы
     */
    private void checkUpdatedAdGroup(Long campaignId,
                                     Long adGroupId,
                                     AdGroup expectedMobileContentAdGroup,
                                     Collection<BigInteger> importIds) {
        BidModifier expectedBidModifier = getExpectedBidModifier(campaignId);
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
    private void checkErrors(GdUpdateAdGroupPayload payload, Path path, Defect expectedDefect) {
        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path, expectedDefect)
                .withWarnings(null);

        assertThat(payload.getValidationResult())
                .isNotNull()
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    private GdUpdateMobileContentAdGroup getMobileContentAdGroupForSending(
            Long campaignId,
            Long adGroupId,
            Set<GdMobileContentAdGroupNetworkTargeting> networkTargetings,
            Set<GdMobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings,
            String currentMinimalOsVersion,
            Set<Long> targetingCategoryIds) {
        return new GdUpdateMobileContentAdGroup()
                .withUpdateItems(singletonList(getMobileContentAdGroupItemForSending(campaignId, adGroupId,
                        networkTargetings, deviceTypeTargetings, currentMinimalOsVersion, targetingCategoryIds)));
    }

    private GdUpdateMobileContentAdGroupItem getMobileContentAdGroupItemForSending() {
        return getMobileContentAdGroupItemForSending(campaignId1,
                adGroupId1, Set.of(WI_FI), Set.of(PHONE), CURRENT_MINIMAL_OS_VERSION, Set.of(CATEGORY_ID1));
    }

    private GdUpdateMobileContentAdGroupItem getMobileContentAdGroupItemForSending(
            Long campaignId,
            Long adGroupId,
            Set<GdMobileContentAdGroupNetworkTargeting> networkTargetings,
            Set<GdMobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings,
            String currentMinimalOsVersion,
            Set<Long> targetingCategoryIds) {
        return new GdUpdateMobileContentAdGroupItem()
                .withAdGroupName(AD_GROUP_NAME)
                .withAdGroupId(adGroupId)
                .withAdGroupMinusKeywords(MINUS_KEYWORDS)
                .withCurrentMinimalOsVersion(currentMinimalOsVersion)
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
                        categoryId -> new GdUpdateAdGroupInterestItem().withCategoryId(categoryId)))
                .withBidModifiers(new GdUpdateBidModifiers()
                        .withBidModifierDemographics(new GdUpdateBidModifierDemographics()
                                .withCampaignId(campaignId)
                                .withAdGroupId(adGroupId)
                                .withAdjustments(singletonList(new GdUpdateBidModifierDemographicsAdjustmentItem()
                                        .withAge(GdAgeType._0_17)
                                        .withGender(GdGenderType.FEMALE)
                                        .withPercent(10)))
                                .withEnabled(true)
                                .withType(DEMOGRAPHY_MULTIPLIER)));
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
            Long campaignId,
            Long adGroupId,
            Set<GdMobileContentAdGroupNetworkTargeting> networkTargetings,
            Set<GdMobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings) {
        return new MobileContentAdGroup()
                .withId(adGroupId)
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

    private BidModifier getExpectedBidModifier(Long campaignId) {
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
