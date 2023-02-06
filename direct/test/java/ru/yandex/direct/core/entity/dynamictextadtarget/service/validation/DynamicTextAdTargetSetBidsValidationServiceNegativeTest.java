package ru.yandex.direct.core.entity.dynamictextadtarget.service.validation;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bids.container.SetBidItem;
import ru.yandex.direct.core.entity.bids.service.CommonSetBidsValidationService;
import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.repository.CampaignAccessCheckRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.DynamicTextAdTargetSetBidsService;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.RequestSetBidType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.DynamicTextAdTargetSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adGroupNotFound;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.mixedTypes;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.oneOfFieldsShouldBeSpecified;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.dynamicTextAdTargetNotFoundInAdGroup;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.dynamicTextAdTargetNotFoundInCampaign;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetSetBidsValidationService.BIDS_FIELDS;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotGreaterThan;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_DYNAMIC;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.JsonUtils.toJson;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicTextAdTargetSetBidsValidationServiceNegativeTest {

    private DynamicTextAdTargetSetBidsValidationService setBidsValidationService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignAccessCheckRepository campaignAccessCheckRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    @Autowired
    protected DslContextProvider dslContextProvider;

    @Autowired
    private DynamicTextAdTargetSetBidsService dynamicTextAdTargetSetBidsService;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private DynamicTextAdTargetSteps dynamicTextAdTargetSteps;

    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Autowired
    private FeatureService featureService;

    private long operatorUid;
    private ClientId clientId;
    private int shard;
    private DynamicTextAdTargetInfo dynamicTextAdTarget;
    private AdGroupInfo defaultAdGroup;
    private AdGroupInfo anotherAdGroup;

    @Before
    public void before() {
        defaultAdGroup = adGroupSteps.createActiveDynamicTextAdGroup();
        anotherAdGroup = adGroupSteps.createActiveDynamicTextAdGroup(defaultAdGroup.getClientInfo());
        operatorUid = defaultAdGroup.getUid();
        clientId = defaultAdGroup.getClientId();
        dynamicTextAdTarget = dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(defaultAdGroup);
        shard = defaultAdGroup.getShard();

        setBidsValidationService = new DynamicTextAdTargetSetBidsValidationService(
                new CommonSetBidsValidationService(),
                new CampaignSubObjectAccessCheckerFactory(
                        shardHelper, rbacService, campaignAccessCheckRepository, new AffectedCampaignIdsContainer(),
                        requestAccessibleCampaignTypes, featureService),
                clientService,
                campaignRepository
        );
    }

    private ValidationResult<List<SetBidItem>, Defect> validate(List<SetBidItem> setBids,
                                                                RequestSetBidType requestType) {
        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetSetBidsService.getDynamicTextAdTargetsByBid(shard, clientId, setBids, requestType);

        return setBidsValidationService
                .validateForTextAdTargets(shard, operatorUid, clientId, setBids, requestType, dynamicTextAdTargets);
    }

    @Test
    public void validate_IdNegative() {
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(singletonList(new SetBidItem()
                        .withId(-1L)), RequestSetBidType.ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.ID.name())), validId()))));
    }

    @Test
    public void validate_AdGroupIdNegative() {
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(singletonList(new SetBidItem()
                        .withAdGroupId(-1L)), RequestSetBidType.ADGROUP_ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.AD_GROUP_ID.name())), validId()))));
    }

    @Test
    public void validate_CampaignIdNegative() {
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(singletonList(new SetBidItem()
                        .withCampaignId(-1L)), RequestSetBidType.CAMPAIGN_ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CAMPAIGN_ID.name())), validId()))));
    }

    @Test
    public void validate_MixedTypes() {
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(asList(
                        new SetBidItem()
                                .withCampaignId(1L),
                        new SetBidItem()
                                .withAdGroupId(1L)),
                        RequestSetBidType.CAMPAIGN_ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(), mixedTypes()))));
    }

    @Test
    public void validate_IdNotFound() {
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(singletonList(new SetBidItem()
                        .withId(123L)
                        .withPriceSearch(BigDecimal.valueOf(100))), RequestSetBidType.ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), objectNotFound()))));
    }

    @Test
    public void validate_AdGroupIdNotFound() {
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(singletonList(new SetBidItem()
                        .withAdGroupId(123L)
                        .withPriceSearch(BigDecimal.valueOf(100))), RequestSetBidType.ADGROUP_ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), adGroupNotFound()))));
    }

    @Test
    public void validate_EmptyAdGroup() {
        Long adGroupId = anotherAdGroup.getAdGroupId();
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(singletonList(new SetBidItem()
                        .withAdGroupId(adGroupId)
                        .withPriceSearch(BigDecimal.valueOf(100))), RequestSetBidType.ADGROUP_ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), dynamicTextAdTargetNotFoundInAdGroup(adGroupId)))));
    }

    @Test
    public void validate_EmptyCampaign() {
        Long campaignId = anotherAdGroup.getCampaignId();
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(singletonList(new SetBidItem()
                        .withCampaignId(campaignId)
                        .withPriceSearch(BigDecimal.valueOf(100))), RequestSetBidType.CAMPAIGN_ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), dynamicTextAdTargetNotFoundInCampaign(campaignId)))));
    }

    @Test
    public void validate_NotAllowedCampaignType() {
        AdGroupInfo activeTextAdGroup = adGroupSteps.createActiveTextAdGroup(defaultAdGroup.getClientInfo());

        Long campaignId = activeTextAdGroup.getCampaignId();
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(singletonList(new SetBidItem()
                        .withCampaignId(campaignId)
                        .withPriceSearch(BigDecimal.valueOf(100))), RequestSetBidType.CAMPAIGN_ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), dynamicTextAdTargetNotFoundInCampaign(campaignId)))));
    }

    @Test
    public void validate_NotAllowedAdGroupType() {
        convertGroupToFeedType(defaultAdGroup);

        Long adGroupId = defaultAdGroup.getAdGroupId();
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(singletonList(new SetBidItem()
                        .withAdGroupId(adGroupId)
                        .withPriceSearch(BigDecimal.valueOf(100))), RequestSetBidType.ADGROUP_ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), dynamicTextAdTargetNotFoundInAdGroup(adGroupId)))));
    }

    @Test
    public void validate_WithoutPrices() {
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(asList(
                        new SetBidItem()
                                .withId(dynamicTextAdTarget.getDynamicConditionId())),
                        RequestSetBidType.ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), oneOfFieldsShouldBeSpecified(BIDS_FIELDS)))));
    }

    @Test
    public void validate_PriorityForManualStrategy() {
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(asList(
                        new SetBidItem()
                                .withId(dynamicTextAdTarget.getDynamicConditionId())
                                .withAutobudgetPriority(2)),
                        RequestSetBidType.ID);

        assertThat(actual).is(matchedBy(hasWarningWithDefinition(
                validationError(path(index(0), field(SetBidItem.AUTOBUDGET_PRIORITY.name())), new Defect<>(
                        BidsDefects.Ids.PRIORITY_WONT_BE_ACCEPTED_IN_CASE_OF_NOT_AUTO_BUDGET_STRATEGY)))));
    }

    @Test
    public void validate_PriceSearchForAutobudgetStrategy() {
        makeCampaignAutobudget(dslContextProvider, dynamicTextAdTarget.getAdGroupInfo().getCampaignInfo());
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(asList(
                        new SetBidItem()
                                .withId(dynamicTextAdTarget.getDynamicConditionId())
                                .withPriceSearch(BigDecimal.valueOf(123L))),
                        RequestSetBidType.ID);

        assertThat(actual).is(matchedBy(hasWarningWithDefinition(
                validationError(path(index(0), field(SetBidItem.PRICE_SEARCH.name())), new Defect<>(
                        BidsDefects.Ids.BID_FOR_SEARCH_WONT_BE_ACCEPTED_IN_CASE_OF_AUTOBUDGET_STRATEGY)))));
    }

    @Test
    public void validate_PriceContextForAutobudgetStrategy() {
        makeCampaignAutobudget(dslContextProvider, dynamicTextAdTarget.getAdGroupInfo().getCampaignInfo());
        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(asList(
                        new SetBidItem()
                                .withId(dynamicTextAdTarget.getDynamicConditionId())
                                .withPriceContext(BigDecimal.valueOf(123L))),
                        RequestSetBidType.ID);

        assertThat(actual).is(matchedBy(hasWarningWithDefinition(
                validationError(path(index(0), field(SetBidItem.PRICE_CONTEXT.name())), new Defect<>(
                        BidsDefects.Ids.BID_FOR_CONTEXT_WONT_BE_ACCEPTED_IN_CASE_OF_AUTOBUDGET_STRATEGY)))));
    }

    @Test
    public void validate_PriceSearchLessThanMin() {
        Currency currency = clientService.getWorkCurrency(clientId);
        BigDecimal price = currency.getMinPrice().subtract(BigDecimal.ONE);
        CurrencyCode code = currency.getCode();

        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(asList(
                        new SetBidItem()
                                .withId(dynamicTextAdTarget.getDynamicConditionId())
                                .withPriceSearch(price)),
                        RequestSetBidType.ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(SetBidItem.PRICE_SEARCH.name())), invalidValueNotLessThan(
                        Money.valueOf(currency.getMinPrice(), code))))));
    }

    @Test
    public void validate_PriceSearchGreaterThanMax() {
        Currency currency = clientService.getWorkCurrency(clientId);
        BigDecimal price = currency.getMaxPrice().add(BigDecimal.ONE);
        CurrencyCode code = currency.getCode();

        ValidationResult<List<SetBidItem>, Defect> actual =
                validate(asList(
                        new SetBidItem()
                                .withId(dynamicTextAdTarget.getDynamicConditionId())
                                .withPriceSearch(price)),
                        RequestSetBidType.ID);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(SetBidItem.PRICE_SEARCH.name())),
                        invalidValueNotGreaterThan(Money.valueOf(currency.getMaxPrice(), code))))));
    }

    protected static void makeCampaignAutobudget(DslContextProvider dslContextProvider, CampaignInfo campaignInfo) {
        StrategyData strategyData = new StrategyData()
                .withSum(new BigDecimal("100000"))
                .withLimitClicks(1000L)
                .withVersion(1L);
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.Yes)
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.autobudget_avg_click)
                .set(CAMPAIGNS.STRATEGY_DATA, toJson(strategyData))
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }

    private void convertGroupToFeedType(AdGroupInfo adGroupInfo) {
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        dslContextProvider.ppc(adGroupInfo.getShard())
                .update(ADGROUPS_DYNAMIC)
                .set(ADGROUPS_DYNAMIC.MAIN_DOMAIN_ID, (Long) null)
                .set(ADGROUPS_DYNAMIC.FEED_ID, feedId)
                .where(ADGROUPS_DYNAMIC.PID.eq(adGroupInfo.getAdGroupId()))
                .execute();
    }
}
