package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.type.pixels.InventoryType;
import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CpmIndoorBannerInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.validation.defects.MoneyDefects;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPlatform;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.noRightsToPixel;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createCpmAdGroupWithForeignInventory;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createDealWithNonYandexPlacements;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.getPrivateRetConditionId;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.getPublicRetConditionId;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.audienceTargetAllowedOnlyInMobileContentCampaign;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionIsInvalidForRetargeting;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.BIG_PLACEMENT_PAGE_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.dcmPixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmIndoorBanner;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueCpmNotGreaterThan;
import static ru.yandex.direct.dbschema.ppc.tables.Campaigns.CAMPAIGNS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRetargetingValidationServiceValidateTest {

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private PlacementsRepository placementsRepository;

    @Autowired
    private UpdateRetargetingValidationService updateRetargetingValidationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private TestTargetingCategoriesRepository testTargetingCategoriesRepository;

    @Autowired
    private TestAdGroupRepository testAdGroupRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    private ClientInfo clientInfo;
    private ClientInfo agencyClientInfo;
    private CampaignInfo campaignInfo;
    private RetargetingInfo retargetingInfo;
    private RetargetingInfo cpmRetargetingInfo;
    private RetargetingInfo cpmRetargetingInfo2;
    private ClientId clientId;
    private int shard;

    private Long privateRetargetingConditionId;
    private Long publicRetargetingConditionId;
    private AdGroupInfo cpmAdGroupInfo;
    private AdGroupInfo cpmAdGroupInfo2;
    private List<DealInfo> dealInfos;

    @Before
    public void before() {
        agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        clientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyClientInfo);
        retargetingInfo =
                steps.retargetingSteps().createRetargeting(new RetargetingInfo().withClientInfo(clientInfo));
        clientInfo = retargetingInfo.getClientInfo();
        clientId = retargetingInfo.getClientId();
        shard = retargetingInfo.getShard();

        privateRetargetingConditionId = getPrivateRetConditionId(steps, retargetingInfo.getClientInfo());
        publicRetargetingConditionId = getPublicRetConditionId(steps, retargetingInfo.getClientInfo());

        dealInfos = createDealWithNonYandexPlacements(steps, placementsRepository, agencyClientInfo);

        cpmAdGroupInfo = createCpmAdGroupWithForeignInventory(dealInfos, steps, retargetingInfo.getClientInfo());
        cpmRetargetingInfo = steps.retargetingSteps().createDefaultRetargeting(cpmAdGroupInfo);

        cpmAdGroupInfo2 = createCpmAdGroupWithForeignInventory(dealInfos, steps, retargetingInfo.getClientInfo());
        cpmRetargetingInfo2 = steps.retargetingSteps().createDefaultRetargeting(cpmAdGroupInfo2);

        campaignInfo = cpmAdGroupInfo.getCampaignInfo();
    }

    @After
    public void after() {
        steps.dealSteps().unlinkDeals(shard, mapList(dealInfos, DealInfo::getDealId));
        steps.dealSteps().deleteDeals(mapList(dealInfos, DealInfo::getDeal), cpmAdGroupInfo.getClientInfo());
        placementsRepository.deletePlacementsBy(ImmutableList.of(BIG_PLACEMENT_PAGE_ID + 1));
    }

    @Test
    public void validate_ValidBean_NoErrors() {
        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargetingInfo.getRetargeting().withAutobudgetPriority(null)), clientId, shard,
                        List.of(campaignInfo.getCampaignId()));

        assertThat(vr.hasAnyErrors(), is(false));
        assertThat(vr.hasAnyWarnings(), is(false));
    }

    @Test
    public void validate_LowPriceContext_InvalidValueNotLessThanDefectDefinition() {
        Retargeting retargeting = retargetingInfo.getRetargeting().withPriceContext(BigDecimal.ZERO);

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        Currency currency = clientService.getWorkCurrency(clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(Retargeting.PRICE_CONTEXT.name())),
                MoneyDefects.invalidValueNotLessThan(Money.valueOf(currency.getMinPrice(), currency.getCode())))));
    }

    @Test
    public void validate_CpmMaxPriceContext_InvalidValueCpmNotGreaterThan() {
        testAdGroupRepository.updateAdGroupType(shard, retargetingInfo.getAdGroupId(), AdGroupType.CPM_BANNER);
        Retargeting retargeting = retargetingInfo.getRetargeting().withPriceContext(BigDecimal.valueOf(1_000_000));

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        Currency currency = clientService.getWorkCurrency(clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(Retargeting.PRICE_CONTEXT.name())),
                invalidValueCpmNotGreaterThan(Money.valueOf(currency.getMaxCpmPrice(), currency.getCode())))));
    }

    @Test
    public void validate_CpmGeoproductMaxPriceContext_InvalidValueCpmNotGreaterThan() {
        testAdGroupRepository.updateAdGroupType(shard, retargetingInfo.getAdGroupId(), AdGroupType.CPM_GEOPRODUCT);
        Retargeting retargeting = retargetingInfo.getRetargeting().withPriceContext(BigDecimal.valueOf(1_000_000));

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        Currency currency = clientService.getWorkCurrency(clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(Retargeting.PRICE_CONTEXT.name())),
                invalidValueCpmNotGreaterThan(Money.valueOf(currency.getMaxCpmPrice(), currency.getCode())))));
    }

    @Test
    public void validate_EmptyPriceContextWithCampaignManualStrategry_ContextPriceNotNullForManualStrategy() {
        Retargeting retargeting = retargetingInfo.getRetargeting().withPriceContext(null);

        dslContextProvider.ppc(retargetingInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.No)
                .set(CAMPAIGNS.PLATFORM, CampaignsPlatform.context)
                .where(CAMPAIGNS.CID.eq(retargeting.getCampaignId()))
                .execute();

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(Retargeting.PRICE_CONTEXT.name())),
                new Defect<>(BidsDefects.Ids.CONTEXT_PRICE_IS_NOT_SET_FOR_MANUAL_STRATEGY))));
    }

    @Test
    public void validate_IncorrectAutobudgetPriority_InvalidValueDefectType() {
        Retargeting retargeting = retargetingInfo.getRetargeting().withAutobudgetPriority(2);

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        assertThat(vr,
                hasDefectDefinitionWith(validationError(path(index(0), field(Retargeting.AUTOBUDGET_PRIORITY.name())),
                        CommonDefects.invalidValue())));
    }

    @Test
    public void validate_EmptyAutobudgetPriorityWithCampaignAutobudget_PriorityIsNotSetForeAutobudgetStrategyDefect() {
        Retargeting retargeting = retargetingInfo.getRetargeting().withAutobudgetPriority(null);

        dslContextProvider.ppc(retargetingInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.Yes)
                .where(CAMPAIGNS.CID.eq(retargeting.getCampaignId()))
                .execute();

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        assertThat(vr,
                hasDefectDefinitionWith(validationError(path(index(0), field(Retargeting.AUTOBUDGET_PRIORITY.name())),
                        RetargetingDefects.autobudgetPriorityNotMatchStrategy())));
    }

    @Test
    public void validate_DuplicateRetargetings_DuplicatedObjectDefect() {
        Retargeting retargeting = retargetingInfo.getRetargeting();

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(asList(retargeting, retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), CollectionDefects.duplicatedObject())));
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(1)), CollectionDefects.duplicatedObject())));
    }

    @Test
    public void validate_UpdateToInterestInTextCampaign_AudienceTargetAllowedOnlyInMobileContentCampaign() {
        assumeThat(retargetingInfo.getCampaignInfo().getCampaign().getType(), is(CampaignType.TEXT));

        Long interestRetargetingConditionId = interestRetargetingConditionId(retargetingInfo);

        Retargeting retargeting =
                retargetingInfo.getRetargeting().withRetargetingConditionId(interestRetargetingConditionId);

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0),
                field(Retargeting.RETARGETING_CONDITION_ID.name())),
                audienceTargetAllowedOnlyInMobileContentCampaign())));
    }

    private Long interestRetargetingConditionId(RetargetingInfo retargetingInfo) {
        RetargetingCondition retargetingCondition = defaultRetCondition(retargetingInfo.getClientId());
        Goal goal = new Goal();

        TargetingCategory targetingCategory =
                new TargetingCategory(54L, null, "", "", BigInteger.valueOf(10000L), true);
        testTargetingCategoriesRepository.addTargetingCategory(targetingCategory);

        goal.withId(targetingCategory.getImportId().longValue());
        Rule rule = new Rule();
        rule.withGoals(singletonList(goal));
        retargetingCondition.withInterest(true)
                .withRules(singletonList(rule));
        steps.retConditionSteps().createRetCondition(retargetingCondition, retargetingInfo.getClientInfo());

        return retargetingCondition.getId();
    }

    /**
     * Тест проверяет, что корректно валидируются баннеры с пикселями при обновлении ретаргетингов,
     * от которых права на пиксели данных баннеров  зависят
     */
    @Test
    public void validate_Retargetings_BelongingToAdgroupWithBannerPixels_NoErrors() {

        Retargeting retargeting =
                cpmRetargetingInfo.getRetargeting().withRetargetingConditionId(publicRetargetingConditionId);

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));
        Assert.assertThat(vr, hasNoDefectsDefinitions());
    }

    /**
     * Тест проверяет, что корректно валидируются баннеры с пикселями при обновлении ретаргетингов,
     * от которых права на пиксели данных баннеров зависят
     */
    @Test
    public void validate_Retargetings_BelongingToAdgroupWithBannerPixels_Errors() {
        Retargeting retargeting =
                cpmRetargetingInfo.getRetargeting().withRetargetingConditionId(privateRetargetingConditionId);

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(singletonList(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        Assert.assertThat(vr.flattenErrors(),
                contains(validationError(path(index(0)),
                        noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY))));
    }

    /**
     * Тест проверяет, что корректно происходит валидация, когда обновляются ретаргетинги с двумя различными группами
     * объявлений, но одинаковым условием ретаргетинга
     * От предыдущего теста отличается только наличием двух групп
     */
    @Test
    public void validate_Retargetings_DifferentAdGroups_SameRetConditions_PixelError() {

        Retargeting retargeting =
                cpmRetargetingInfo.getRetargeting().withRetargetingConditionId(privateRetargetingConditionId);

        Retargeting retargeting2 =
                cpmRetargetingInfo2.getRetargeting().withRetargetingConditionId(privateRetargetingConditionId);

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(ImmutableList.of(retargeting, retargeting2), clientId, shard,
                        List.of(campaignInfo.getCampaignId()));

        Assert.assertThat(vr.flattenErrors(),
                contains(validationError(path(index(0)),
                        noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY)),
                        validationError(path(index(1)),
                                noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_DEALS,
                                        InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY))));
    }

    @Test
    public void validate_RetargetingsFormDifferentAdGroupType() {
        AdGroupInfo textAdGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        RetargetingInfo retargetingFromTextAdGroup = steps.retargetingSteps().createDefaultRetargeting(textAdGroup);

        Long newRetCondId = steps.retConditionSteps().createDefaultRetCondition(clientInfo).getRetConditionId();
        Retargeting retargetingText =
                retargetingFromTextAdGroup.getRetargeting().withRetargetingConditionId(newRetCondId);
        Retargeting retargetingCpm =
                cpmRetargetingInfo.getRetargeting().withRetargetingConditionId(privateRetargetingConditionId);

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(ImmutableList.of(retargetingText, retargetingCpm), clientId, shard,
                        List.of(campaignInfo.getCampaignId()));

        Assert.assertThat(vr.flattenErrors(),
                contains(validationError(path(index(1)),
                        noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY))));
    }

    @Test
    public void validate_RetargetingFromCpmVideoAdGroup_Error() {
        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, videoCreativeId);
        CpmBannerInfo cpmVideoBanner = steps.bannerSteps()
                .createActiveCpmVideoBanner(activeCpmBanner(campaignInfo.getCampaignId(), null, videoCreativeId)
                                .withPixels(singletonList(dcmPixelUrl())),
                        campaignInfo);

        RetargetingInfo retargetingInfo =
                steps.retargetingSteps().createDefaultRetargeting(cpmVideoBanner.getAdGroupInfo());

        Retargeting retargeting =
                retargetingInfo.getRetargeting().withRetargetingConditionId(privateRetargetingConditionId);

        ValidationResult<List<Retargeting>, Defect> vr =
                validate(ImmutableList.of(retargeting), clientId, shard, List.of(campaignInfo.getCampaignId()));

        Assert.assertThat(vr.flattenErrors(),
                contains(validationError(path(index(0)),
                        noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_BANNER,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY))));
    }

    @Test
    public void validate_RetargetingFromCpmIndoorAdGroup_Error() {
        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmIndoorVideoCreative(clientInfo, videoCreativeId);

        AdGroupInfo cpmIndoorAdGroup = steps.adGroupSteps().createActiveCpmIndoorAdGroup(campaignInfo);
        CpmIndoorBannerInfo cpmIndoorBanner = steps.bannerSteps().createActiveCpmIndoorBanner(
                activeCpmIndoorBanner(cpmIndoorAdGroup.getCampaignId(), cpmIndoorAdGroup.getAdGroupId(),
                        videoCreativeId), cpmIndoorAdGroup);

        RetargetingInfo retargetingInfo =
                steps.retargetingSteps().createDefaultRetargeting(cpmIndoorBanner.getAdGroupInfo());

        Retargeting retargeting =
                retargetingInfo.getRetargeting().withRetargetingConditionId(privateRetargetingConditionId);

        ValidationResult<List<Retargeting>, Defect> vr = validate(ImmutableList.of(retargeting), clientId, shard,
                List.of(campaignInfo.getCampaignId()));

        Assert.assertThat(vr.flattenErrors(),
                contains(validationError(path(index(0), field("retargetingConditionId")),
                        retargetingConditionIsInvalidForRetargeting())));
    }

    private ValidationResult<List<Retargeting>, Defect> validate(List<Retargeting> retargetings,
                                                                 ClientId clientId, int shard,
                                                                 Collection<Long> campaignIds) {
        ValidationResult<List<Retargeting>, Defect> validationResult = new ValidationResult<>(retargetings);
        Map<Long, AdGroupSimple> adGroupsById = adGroupRepository.getAdGroupSimple(shard, clientId,
                mapList(retargetings, Retargeting::getAdGroupId));
        return updateRetargetingValidationService.validate(validationResult, adGroupsById, emptyMap(),
                clientId, shard, false, emptyMap());
    }
}
