package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.type.pixels.InventoryType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.InterestLink;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.validation.defects.RightsDefects;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.noRightsToPixel;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingUtils.convertRetargetingsToTargetInterests;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createCpmAdGroupWithForeignInventory;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createDealWithNonYandexPlacements;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.getPrivateRetConditionId;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.getPublicRetConditionId;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.BIG_PLACEMENT_PAGE_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.dcmPixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoals;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotGreaterThan;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddRetargetingValidationServiceTest {

    private static final int MAX_RET_COND_PER_AD_GROUP = 50;
    private static final int MAX_RET_COND_PER_CPM_BANNER_AD_GROUP = 1;

    private static final long TARGETING_CATEGORY_ID = 10L;
    private static final BigInteger IMPORT_ID = BigInteger.valueOf(1000000L);

    private AdGroupInfo defaultAdGroupInfo;
    private AdGroupInfo cpmBannerKeywordsAdGroupInfo;
    private AdGroupInfo cpmAdGroupWithForeignInventoryInfo2;
    private AdGroupInfo cpmAdGroupWithForeignInventoryInfo;
    private AdGroupInfo nonCpmAdGroupFirstInfo;
    private AdGroupInfo nonCpmAdGroupSecondInfo;
    private int shard;
    private long operatorId;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private ClientInfo agencyClientInfo;
    private long defaultAdGroupId;
    private long cpmBannerKeywordsAdGroupId;
    private long retargetingConditionId;
    private long privateRetargetingConditionId;
    private long publicRetargetingConditionId;
    private Currency currency;
    private Map<Long, AdGroupSimple> existingAdGroups;

    private List<DealInfo> dealInfos;

    @Autowired
    private AddRetargetingValidationService addRetargetingValidationService;

    @Autowired
    private Steps steps;

    @Autowired
    private TestTargetingCategoriesRepository testTargetingCategoriesRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private PlacementsRepository placementsRepository;

    private TargetInterest targetInterest(AdGroupInfo adGroupInfo, Long retargetingConditionId) {
        return new TargetInterest()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withRetargetingConditionId(retargetingConditionId);
    }

    @Before
    public void setup() {
        agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        clientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyClientInfo);
        defaultAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        clientInfo = defaultAdGroupInfo.getClientInfo();

        shard = defaultAdGroupInfo.getShard();
        clientId = defaultAdGroupInfo.getClientId();
        operatorId = defaultAdGroupInfo.getUid();

        defaultAdGroupId = defaultAdGroupInfo.getAdGroupId();
        cpmBannerKeywordsAdGroupInfo =
                steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo, CriterionType.KEYWORD);
        cpmBannerKeywordsAdGroupId = cpmBannerKeywordsAdGroupInfo.getAdGroupId();
        retargetingConditionId = steps.retConditionSteps()
                .createDefaultRetCondition(clientInfo)
                .getRetConditionId();
        privateRetargetingConditionId = getPrivateRetConditionId(steps, clientInfo);
        publicRetargetingConditionId = getPublicRetConditionId(steps, clientInfo);

        dealInfos = createDealWithNonYandexPlacements(steps, placementsRepository, agencyClientInfo);

        cpmAdGroupWithForeignInventoryInfo = createCpmAdGroupWithForeignInventory(dealInfos, steps, clientInfo);
        cpmAdGroupWithForeignInventoryInfo2 = createCpmAdGroupWithForeignInventory(dealInfos, steps, clientInfo);

        nonCpmAdGroupFirstInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        nonCpmAdGroupSecondInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);


        existingAdGroups = adGroupRepository.getAdGroupSimple(shard, clientId,
                ImmutableList.of(defaultAdGroupId, cpmBannerKeywordsAdGroupId,
                        cpmAdGroupWithForeignInventoryInfo2.getAdGroupId(),
                        cpmAdGroupWithForeignInventoryInfo.getAdGroupId(),
                        nonCpmAdGroupFirstInfo.getAdGroupId(),
                        nonCpmAdGroupSecondInfo.getAdGroupId()));
        currency = clientService.getWorkCurrency(clientId);
    }

    @After
    public void after() {
        steps.dealSteps().unlinkDeals(shard, mapList(dealInfos, DealInfo::getDealId));
        steps.dealSteps()
                .deleteDeals(mapList(dealInfos, DealInfo::getDeal), cpmAdGroupWithForeignInventoryInfo.getClientInfo());
        placementsRepository.deletePlacementsBy(ImmutableList.of(BIG_PLACEMENT_PAGE_ID + 1));
    }

    private ValidationResult<List<TargetInterest>, Defect> validateWithDefaultParameters(
            List<TargetInterest> retargetings) {
        List<TargetInterest> existTargetInterests =
                getTargetInterestsWithInterestByAdGroupIds(existingAdGroups.keySet(), clientId, shard);

        return addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings),
                        existTargetInterests,
                        existingAdGroups,
                        operatorId, clientId, shard);
    }

    private List<TargetInterest> getTargetInterestsWithInterestByAdGroupIds(
            Collection<Long> adGroupIds, ClientId clientId, int shard) {
        List<Retargeting> retargetings = retargetingRepository.getRetargetingsByAdGroups(shard, adGroupIds);
        List<Long> retCondIds = mapList(retargetings, Retargeting::getRetargetingConditionId);
        List<InterestLink> interestLinks = retargetingConditionRepository.getInterestByIds(shard, clientId, retCondIds);
        return convertRetargetingsToTargetInterests(retargetings, interestLinks);
    }

    @Test
    public void validate_errorDuplicatedObject_whenDuplicatedAdGroupIdAndRetCondIdInRequest() {
        List<TargetInterest> retargetings = asList(
                targetInterest(defaultAdGroupInfo, retargetingConditionId),
                targetInterest(defaultAdGroupInfo, retargetingConditionId));

        ValidationResult<List<TargetInterest>, Defect> actual = validateWithDefaultParameters(retargetings);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(validationError(path(),
                RetargetingDefects.duplicatedRetargetingConditionIdForAdgroupId())));

        assertThat(actual.getSubResults().get(index(1)).flattenErrors(), contains(validationError(path(),
                RetargetingDefects.duplicatedRetargetingConditionIdForAdgroupId())));
    }

    @Test
    public void validate_errorDuplicatedObject_whenDuplicatedAdGroupIdAndRetCondIdInRequestAndDB() {
        final List<TargetInterest> retargetings =
                singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId));
        List<TargetInterest> existingTargetInterests =
                singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId));

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), existingTargetInterests, existingAdGroups,
                        operatorId, clientId, shard);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(),
                        RetargetingDefects.retargetingConditionAlreadyExists())));
    }

    @Test
    public void validate_invalidCollectionSize_whenLimitOfItemsExceededForAdGroup() {
        List<TargetInterest> existingTargetInterests = LongStream.range(0, MAX_RET_COND_PER_AD_GROUP)
                .mapToObj(v -> targetInterest(defaultAdGroupInfo, 10000L + v).withId(v))
                .collect(toList());

        final List<TargetInterest> retargetings =
                singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId));
        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), existingTargetInterests, existingAdGroups,
                        operatorId, clientId, shard);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(),
                        RetargetingDefects.maxCollectionSizeAdGroup(MAX_RET_COND_PER_AD_GROUP))));
    }

    @Test
    public void validate_invalidCollectionSize_whenLimitOfItemsExceededForCpmBannerAdGroup() {
        List<TargetInterest> existingTargetInterests = LongStream.range(0, MAX_RET_COND_PER_CPM_BANNER_AD_GROUP)
                .mapToObj(v -> targetInterest(defaultAdGroupInfo, 10000L + v).withId(v))
                .collect(toList());

        final List<TargetInterest> retargetings =
                singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId));
        existingAdGroups.get(defaultAdGroupId).setType(AdGroupType.CPM_BANNER);
        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), existingTargetInterests, existingAdGroups,
                        operatorId, clientId, shard);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(),
                        RetargetingDefects.maxCollectionSizeCpmAdGroup(MAX_RET_COND_PER_CPM_BANNER_AD_GROUP))));
    }

    @Test
    public void validate_invalidCollectionSize_whenLimitOfItemsExceededForCpmGeoproductAdGroup() {
        List<TargetInterest> existingTargetInterests = LongStream.range(0, MAX_RET_COND_PER_CPM_BANNER_AD_GROUP)
                .mapToObj(v -> targetInterest(defaultAdGroupInfo, 10000L + v).withId(v))
                .collect(toList());

        final List<TargetInterest> retargetings =
                singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId));
        existingAdGroups.get(defaultAdGroupId).setType(AdGroupType.CPM_GEOPRODUCT);
        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), existingTargetInterests, existingAdGroups,
                        operatorId, clientId, shard);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(),
                        RetargetingDefects.maxCollectionSizeCpmAdGroup(MAX_RET_COND_PER_CPM_BANNER_AD_GROUP))));
    }

    @Test
    public void validate_inconsistentState_whenRetConditionIsNegative() {
        Rule negativeRule = new Rule().withType(RuleType.NOT).withGoals(defaultGoals());

        RetargetingCondition rcWithNegative = defaultRetCondition(clientId);
        rcWithNegative.withRules(singletonList(negativeRule));
        RetConditionInfo negativeRetConditionInfo = steps.retConditionSteps()
                .createRetCondition(rcWithNegative, defaultAdGroupInfo.getClientInfo());

        List<TargetInterest> retargetings =
                singletonList(targetInterest(defaultAdGroupInfo, negativeRetConditionInfo.getRetConditionId()));

        ValidationResult<List<TargetInterest>, Defect> actual = validateWithDefaultParameters(retargetings);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(field(Retargeting.RETARGETING_CONDITION_ID.name())),
                        RetargetingDefects.retargetingConditionIsInvalidForRetargeting())));
    }

    @Test
    public void validate_npRights_whenOperatorHasNoRightsForCampaign() {
        operatorId = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER).getUid();

        List<TargetInterest> retargetings = singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId));
        ValidationResult<List<TargetInterest>, Defect> actual = validateWithDefaultParameters(retargetings);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(field(Retargeting.AD_GROUP_ID.name())), RightsDefects.noRightsCantWrite())));
    }

    @Test
    public void validate_notFound_whenCampaignIsNotVisible() {
        operatorId = steps.clientSteps().createDefaultClient().getUid();

        List<TargetInterest> retargetings = singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId));
        ValidationResult<List<TargetInterest>, Defect> actual = validateWithDefaultParameters(retargetings);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(field(Retargeting.AD_GROUP_ID.name())),
                        RetargetingDefects.adGroupNotFound(defaultAdGroupId))));
    }

    @Test
    public void validate_InterestToNotMobileAdGroup_NotEligibleAdGroup() {
        operatorId = agencyClientInfo.getUid();

        TargetingCategory targetingCategory =
                new TargetingCategory(TARGETING_CATEGORY_ID, null, "", "", IMPORT_ID, true);
        testTargetingCategoriesRepository.addTargetingCategory(targetingCategory);

        List<TargetInterest> targetInterests = singletonList(new TargetInterest()
                .withAdGroupId(defaultAdGroupId)
                .withCampaignId(defaultAdGroupInfo.getCampaignId())
                .withInterestId(TARGETING_CATEGORY_ID));

        ValidationResult<List<TargetInterest>, Defect> actual =
                validateWithDefaultParameters(targetInterests);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(field(Retargeting.AD_GROUP_ID.name())),
                        RetargetingDefects.notEligibleAdGroup())));
    }

    private void archiveCampaign(long campaignId, int shard) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ARCHIVED, CampaignsArchived.Yes)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    @Test
    public void validate_badStatus_whenCampaignIsArchived() {
        operatorId = agencyClientInfo.getUid();
        archiveCampaign(defaultAdGroupInfo.getCampaignId(), shard);

        List<TargetInterest> retargetings = singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId));
        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(), existingAdGroups,
                        operatorId, clientId, shard);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field(Retargeting.AD_GROUP_ID.name())),
                        RetargetingDefects.badStatusCampaignArchivedOnAdd(defaultAdGroupInfo.getCampaignId()))));
    }

    @Test
    public void validate_invalidValue_whenAutobudgetPriorityWrong() {
        int wrongAutobudgetPriority = 2;
        List<TargetInterest> retargetings = singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId)
                .withAutobudgetPriority(wrongAutobudgetPriority));

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(), existingAdGroups,
                        operatorId, clientId, shard);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("autobudgetPriority")), CommonDefects.invalidValue())));
    }

    @Test
    public void validate_invalidValue_whenPriceIsTooBig() {
        validatePriceInternal(BigDecimal.valueOf(100000L),
                contains(validationError(path(field(Retargeting.PRICE_CONTEXT.name())),
                        invalidValueNotGreaterThan(Money.valueOf(currency.getMaxPrice(), currency.getCode())))));
    }

    @Test
    public void validate_invalidValue_whenPriceIsTooSmall() {
        validatePriceInternal(BigDecimal.valueOf(-1L), contains(validationError(path(field("priceContext")),
                invalidValueNotLessThan(Money.valueOf(currency.getMinPrice(), currency.getCode())))));
    }

    @Test
    public void validate_success_whenPriceIsMin() {
        operatorId = agencyClientInfo.getUid();
        validatePriceInternal(currency.getMinPrice(), empty());
    }

    @Test
    public void validate_success_whenPriceIsMax() {
        operatorId = agencyClientInfo.getUid();
        validatePriceInternal(currency.getMaxPrice(), empty());
    }

    private void validatePriceInternal(BigDecimal priceContext,
                                       Matcher<? super Collection<DefectInfo<Defect>>> matcher) {
        List<TargetInterest> retargetings =
                singletonList(
                        targetInterest(defaultAdGroupInfo, retargetingConditionId).withPriceContext(priceContext));

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(), existingAdGroups,
                        operatorId, clientId, shard);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), matcher);

    }

    @Test
    public void adGroupWithInvalidId() {
        List<TargetInterest> retargetings =
                singletonList(targetInterest(defaultAdGroupInfo, retargetingConditionId).withAdGroupId(0L));

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(), existingAdGroups,
                        operatorId, clientId, shard);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("adGroupId")), CommonDefects.validId())));
    }

    @Test
    public void validate_RetargetingNotAllowed_WhenCpmGroupWithKeywords() {
        Long retargetingConditionId = steps.retConditionSteps()
                .createRetCondition(
                        (RetargetingCondition) defaultRetCondition(null).withType(ConditionType.interests),
                        cpmBannerKeywordsAdGroupInfo.getClientInfo()
                ).getRetConditionId();
        List<TargetInterest> retargetings =
                singletonList(targetInterest(cpmBannerKeywordsAdGroupInfo, retargetingConditionId));
        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(), existingAdGroups,
                        operatorId, clientId, shard);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field(Retargeting.AD_GROUP_ID.name())),
                        RetargetingDefects.notEligibleAdGroup())));
    }

    /**
     * Тест проверяет, что корректно валидируются баннеры с пикселями при добавлении ретаргетингов,
     * от которых права на пиксели данных баннеров  зависят
     */
    @Test
    public void validate_Retargetings_BelongingToAdgroupWithBannerPixels_NoErrors() {
        operatorId = agencyClientInfo.getUid();

        List<TargetInterest> retargetings =
                singletonList(
                        targetInterest(cpmAdGroupWithForeignInventoryInfo, publicRetargetingConditionId));

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(), existingAdGroups,
                        operatorId, clientId, shard);
        assertThat(actual, hasNoDefectsDefinitions());
    }

    /**
     * Тест проверяет, что корректно валидируются баннеры с пикселями при добавлении ретаргетингов,
     * от которых права на пиксели данных баннеров  зависят
     */
    @Test
    public void validate_Retargetings_BelongingToAdgroupWithBannerPixels_Errors() {
        operatorId = agencyClientInfo.getUid();

        List<TargetInterest> retargetings =
                singletonList(targetInterest(cpmAdGroupWithForeignInventoryInfo, privateRetargetingConditionId));

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(), existingAdGroups,
                        operatorId, clientId, shard);
        assertThat(actual.flattenErrors(),
                contains(validationError(path(index(0)),
                        noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY))));
    }

    /**
     * Тест проверяет, что корректно происходит валидация, когда добавляются ретаргетинги с двумя различными группами
     * объявлений, но одинаковым условием ретаргетинга
     * От предыдущего теста отличается только наличием двух групп
     */
    @Test
    public void validate_Retargetings_DifferentCpmAdGroups_SameRetConditions() {
        operatorId = agencyClientInfo.getUid();

        List<TargetInterest> retargetings =
                ImmutableList.of(targetInterest(cpmAdGroupWithForeignInventoryInfo, privateRetargetingConditionId),
                        targetInterest(cpmAdGroupWithForeignInventoryInfo2, privateRetargetingConditionId));

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(), existingAdGroups,
                        operatorId, clientId, shard);
        assertThat(actual.flattenErrors(),
                contains(validationError(path(index(0)),
                        noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY)),
                        validationError(path(index(1)),
                                noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_DEALS,
                                        InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY))));
    }

    @Test
    public void validate_Retargetings_DifferentAdGroups_SameRetConditions() {

        List<TargetInterest> retargetings =
                ImmutableList.of(targetInterest(nonCpmAdGroupFirstInfo, retargetingConditionId),
                        targetInterest(nonCpmAdGroupSecondInfo, retargetingConditionId));

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(), existingAdGroups,
                        operatorId, clientId, shard);
        assertThat(actual, hasNoErrors());
    }

    @Test
    public void validate_RetargetingsFromCpmVideoAdGroup_PrivateRetargetingsPrivatePixels_Errors() {
        operatorId = agencyClientInfo.getUid();

        CampaignInfo cpmDealsCampaign = steps.campaignSteps().createActiveCpmDealsCampaign(clientInfo);

        Long nextCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, nextCreativeId);
        CpmBannerInfo cpmVideoBanner = steps.bannerSteps().createActiveCpmVideoBanner(
                activeCpmBanner(null, null, nextCreativeId)
                        .withPixels(singletonList(dcmPixelUrl())),
                cpmDealsCampaign);

        Long retConditionId = steps.retConditionSteps().createCpmRetCondition(clientInfo).getRetConditionId();
        List<TargetInterest> retargetings = singletonList(new TargetInterest()
                .withAdGroupId(cpmVideoBanner.getAdGroupId())
                .withRetargetingConditionId(retConditionId));

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(),
                        singletonMap(cpmVideoBanner.getAdGroupId(), cpmVideoBanner.getAdGroupInfo().getAdGroup()),
                        operatorId, clientId, shard);

        assertThat(actual, hasDefectDefinitionWith(validationError(path(index(0)),
                noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_BANNER,
                        InventoryType.YANDEX_INVENTORY))));
    }
}
