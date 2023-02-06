package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.CpmYndxFrontpageAdGroupPriceRestrictions;
import ru.yandex.direct.core.entity.currency.service.CpmYndxFrontpageCurrencyService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesCache;
import ru.yandex.direct.core.entity.retargeting.service.validation2.cpmprice.RetargetingConditionsCpmPriceValidationDataFactory;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.interests;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.metrika_goals;
import static ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType.short_term;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.MockServices.emptyRetargetingConditionsCpmPriceValidationDataFactory;
import static ru.yandex.direct.currency.CurrencyCode.YND_FIXED;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(Parameterized.class)
public class AddRetargetingValidationServiceAdGroupTypesTest {

    private static final Matcher<? super Collection<DefectInfo<Defect>>>
            ERROR_MATCHER = contains(validationError(path(field("adGroupId")),
            RetargetingDefects.notEligibleAdGroup()));
    private static final Matcher<? super Collection<DefectInfo<Defect>>> SUCCESS_MATCHER = Matchers.empty();
    private static final int SHARD = 4;
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final long OPERATOR_ID = 1000L;
    private static final long AD_GROUP_ID = 1L;
    private static final long TARGETING_CATEGORY_ID = 10L;
    private static final BigInteger IMPORT_ID = BigInteger.valueOf(1000000L);
    private static final long RET_CONDITION_ID_METRIKA = 10L;
    private static final long RET_CONDITION_ID_INTERESTS = 11L;
    private static final long RET_CONDITION_NO_RULES = 12L;
    private static final Currency CURRENCY = YND_FIXED.getCurrency();

    private final AdGroupType adGroupType;
    private final CampaignType campaignType;
    private final BigDecimal priceContext;
    private final Matcher<? super Collection<DefectInfo<Defect>>> retCondMetrikaMatcher;
    private final Matcher<? super Collection<DefectInfo<Defect>>> interestMatcher;
    private final Matcher<? super Collection<DefectInfo<Defect>>> retCondInterestsMatcher;
    private final Matcher<? super Collection<DefectInfo<Defect>>> retCondInterestsWithNoRulesMatcher;
    private AddRetargetingValidationService addRetargetingValidationService;

    public AddRetargetingValidationServiceAdGroupTypesTest(AdGroupType adGroupType,
                                                           CampaignType campaignType,
                                                           BigDecimal priceContext,
                                                           Matcher<? super Collection<DefectInfo<Defect>>> retCondMetrikaMatcher,
                                                           Matcher<? super Collection<DefectInfo<Defect>>> interestMatcher,
                                                           Matcher<? super Collection<DefectInfo<Defect>>> retCondInterestsMatcher,
                                                           Matcher<? super Collection<DefectInfo<Defect>>> retCondInterestsWithNoRulesMatcher) {
        this.adGroupType = adGroupType;
        this.campaignType = campaignType;
        this.priceContext = priceContext;
        this.retCondMetrikaMatcher = retCondMetrikaMatcher;
        this.interestMatcher = interestMatcher;
        this.retCondInterestsMatcher = retCondInterestsMatcher;
        this.retCondInterestsWithNoRulesMatcher = retCondInterestsWithNoRulesMatcher;
    }


    private static RbacService acceptAllCampaignsRbacService() {
        RbacService rbacService = mock(RbacService.class);
        when(rbacService.getWritableCampaigns(anyLong(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Collection<Long> ids = (Collection<Long>) invocation.getArguments()[1];
            return ids != null ? new HashSet<>(ids) : emptySet();
        });
        when(rbacService.getVisibleCampaigns(anyLong(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Collection<Long> ids = (Collection<Long>) invocation.getArguments()[1];
            return ids != null ? new HashSet<>(ids) : emptySet();
        });
        return rbacService;
    }

    private CampaignRepository acceptAllCampaignRepository() {
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        when(campaignRepository.getCampaignsSimple(anyInt(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Collection<Long> cids = (Collection<Long>) invocation.getArguments()[1];
            return listToMap(mapList(cids, id -> new Campaign()
                            .withId(id)
                            .withType(campaignType)
                            .withStatusArchived(false)),
                    CampaignSimple::getId);
        });
        return campaignRepository;
    }

    private static TargetingCategoriesCache acceptAllTargetingCategoriesCache() {
        TargetingCategoriesCache targetingCategoriesCache = mock(TargetingCategoriesCache.class);
        when(targetingCategoriesCache.getTargetingCategories())
                .thenReturn(singletonList(new TargetingCategory(TARGETING_CATEGORY_ID, null, "", "", IMPORT_ID, true)));
        return targetingCategoriesCache;
    }

    private static AdGroupRepository acceptAllAdGroupRepository() {
        AdGroupRepository adGroupRepository = mock(AdGroupRepository.class);
        when(adGroupRepository.getCriterionTypeByAdGroupIds(anyInt(), any()))
                .thenReturn(ImmutableMap.of(AD_GROUP_ID, CriterionType.USER_PROFILE));
        return adGroupRepository;
    }

    private static CpmYndxFrontpageCurrencyService mockedCpmYndxFrontpageCurrencyService() {
        CpmYndxFrontpageCurrencyService cpmYndxFrontpageCurrencyService = mock(CpmYndxFrontpageCurrencyService.class);
        CpmYndxFrontpageAdGroupPriceRestrictions defaultCpmYndxFrontpageAdGroupRestrictions =
                new CpmYndxFrontpageAdGroupPriceRestrictions(BigDecimal.valueOf(0),
                        BigDecimal.valueOf(Double.MAX_VALUE))
                        .withClientCurrency(CURRENCY);
        when(cpmYndxFrontpageCurrencyService
                .getAdGroupIdsToPriceDataMapByAdGroups(any(Collection.class), anyInt(), any(ClientId.class)))
                .thenReturn(ImmutableMap.of(AD_GROUP_ID, defaultCpmYndxFrontpageAdGroupRestrictions));
        when(cpmYndxFrontpageCurrencyService
                .getAdGroupIdsToPriceDataMapByAdGroups(any(Collection.class), anyInt(), any(Currency.class)))
                .thenReturn(ImmutableMap.of(AD_GROUP_ID, defaultCpmYndxFrontpageAdGroupRestrictions));
        when(cpmYndxFrontpageCurrencyService
                .getAdGroupIndexesToPriceDataMapByAdGroups(any(List.class), anyInt(), any(ClientId.class)))
                .thenReturn(ImmutableMap.of(0, defaultCpmYndxFrontpageAdGroupRestrictions));
        when(cpmYndxFrontpageCurrencyService
                .getAdGroupIndexesToPriceDataMapByAdGroups(any(List.class), anyInt(), any(Currency.class)))
                .thenReturn(ImmutableMap.of(0, defaultCpmYndxFrontpageAdGroupRestrictions));
        return cpmYndxFrontpageCurrencyService;
    }

    private static RetargetingConditionRepository acceptAllRetCondRepository() {
        RetargetingConditionRepository retargetingConditionRepository = mock(RetargetingConditionRepository.class);
        when(retargetingConditionRepository
                .getFromRetargetingConditionsTable(anyInt(), any(ClientId.class),
                        ArgumentMatchers.<Collection<Long>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<Long> ids = (Collection<Long>) invocation.getArguments()[2];
                    Rule rule = new Rule();
                    rule.withType(RuleType.ALL);
                    rule.withInterestType(short_term);
                    return mapList(ids, id -> new RetargetingCondition()
                            .withId(id)
                            .withRules(id == RET_CONDITION_NO_RULES ? emptyList() : singletonList(rule))
                            .withType(id == RET_CONDITION_ID_INTERESTS || id == RET_CONDITION_NO_RULES ? interests
                                    : metrika_goals)
                    );
                });
        return retargetingConditionRepository;
    }

    @Parameters(name = "validate for adGroup({0}) in campaign({1})")
    public static Collection<Object[]> parameters() {
        return asList(
                new Object[]{AdGroupType.BASE, CampaignType.TEXT, null, SUCCESS_MATCHER, ERROR_MATCHER, ERROR_MATCHER,
                        ERROR_MATCHER},
                new Object[]{AdGroupType.DYNAMIC, CampaignType.DYNAMIC, null, ERROR_MATCHER, ERROR_MATCHER, ERROR_MATCHER,
                        ERROR_MATCHER},
                new Object[]{AdGroupType.MOBILE_CONTENT, CampaignType.MOBILE_CONTENT, null, SUCCESS_MATCHER,
                        SUCCESS_MATCHER, ERROR_MATCHER,
                        ERROR_MATCHER},
                new Object[]{AdGroupType.PERFORMANCE, CampaignType.PERFORMANCE, null, ERROR_MATCHER, ERROR_MATCHER,
                        ERROR_MATCHER, ERROR_MATCHER},
                new Object[]{AdGroupType.CPM_BANNER, CampaignType.CPM_BANNER, null, ERROR_MATCHER, ERROR_MATCHER,
                        SUCCESS_MATCHER, SUCCESS_MATCHER},
                new Object[]{AdGroupType.CPM_GEOPRODUCT, CampaignType.CPM_BANNER, null, ERROR_MATCHER, ERROR_MATCHER,
                        SUCCESS_MATCHER,
                        SUCCESS_MATCHER},
                new Object[]{AdGroupType.CPM_VIDEO, CampaignType.CPM_BANNER, null, ERROR_MATCHER, ERROR_MATCHER,
                        SUCCESS_MATCHER, SUCCESS_MATCHER},
                new Object[]{AdGroupType.CONTENT_PROMOTION_VIDEO, CampaignType.CONTENT_PROMOTION, null, ERROR_MATCHER,
                        ERROR_MATCHER, ERROR_MATCHER,
                        ERROR_MATCHER},
                new Object[]{AdGroupType.CONTENT_PROMOTION, CampaignType.CONTENT_PROMOTION, null, ERROR_MATCHER,
                        ERROR_MATCHER, ERROR_MATCHER,
                        ERROR_MATCHER},
                new Object[]{AdGroupType.CPM_YNDX_FRONTPAGE, CampaignType.CPM_YNDX_FRONTPAGE, null,
                        contains(validationError(path(field(Retargeting.RETARGETING_CONDITION_ID.name())),
                                RetargetingDefects.cpmYndxFrontpageRetargetingsNotAllowed()),
                                validationError(path(field("adGroupId")), RetargetingDefects.notEligibleAdGroup())),
                        contains(validationError(path(field(Retargeting.RETARGETING_CONDITION_ID.name())), notNull()),
                                validationError(path(field("adGroupId")), RetargetingDefects.notEligibleAdGroup())),
                        contains(validationError(path(field(Retargeting.RETARGETING_CONDITION_ID.name())),
                                RetargetingDefects.cpmYndxFrontpageRetargetingsNotAllowed())),
                        SUCCESS_MATCHER},
                new Object[]{AdGroupType.CPM_YNDX_FRONTPAGE, CampaignType.CPM_PRICE, null,
                        ERROR_MATCHER,
                        contains(validationError(path(field(Retargeting.RETARGETING_CONDITION_ID)), notNull()),
                                validationError(path(field("adGroupId")), RetargetingDefects.notEligibleAdGroup())),
                        SUCCESS_MATCHER,
                        SUCCESS_MATCHER}
        );
    }

    @Test
    public void validate_addRetargetingWithRetCondMetrika() {
        List<TargetInterest> targetInterests =
                asList(new TargetInterest().withAdGroupId(AD_GROUP_ID)
                        .withRetargetingConditionId(RET_CONDITION_ID_METRIKA)
                        .withPriceContext(priceContext));
        ValidationResult<List<TargetInterest>, Defect> actual =
                validateWithDefaultParameters(targetInterests);
        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), retCondMetrikaMatcher);
    }

    @Test
    public void validate_addRetargetingWithInterest() {
        List<TargetInterest> targetInterests =
                asList(new TargetInterest().withAdGroupId(AD_GROUP_ID).withInterestId(TARGETING_CATEGORY_ID)
                .withPriceContext(priceContext));
        ValidationResult<List<TargetInterest>, Defect> actual =
                validateWithDefaultParameters(targetInterests);
        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), interestMatcher);
    }

    @Test
    public void validate_addRetargetingWithRetCondInterests() {
        List<TargetInterest> targetInterests =
                asList(new TargetInterest().withAdGroupId(AD_GROUP_ID)
                        .withRetargetingConditionId(RET_CONDITION_ID_INTERESTS)
                        .withPriceContext(priceContext));
        ValidationResult<List<TargetInterest>, Defect> actual =
                validateWithDefaultParameters(targetInterests);
        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), retCondInterestsMatcher);
    }

    @Test
    public void validate_addRetargetingWithRetCondInterestsNoCryptaGoals() {
        List<TargetInterest> targetInterests =
                asList(new TargetInterest().withAdGroupId(AD_GROUP_ID)
                        .withRetargetingConditionId(RET_CONDITION_NO_RULES)
                        .withPriceContext(priceContext));
        ValidationResult<List<TargetInterest>, Defect> actual =
                validateWithDefaultParameters(targetInterests);
        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), retCondInterestsWithNoRulesMatcher);
    }

    private ValidationResult<List<TargetInterest>, Defect> validateWithDefaultParameters(
            List<TargetInterest> targetInterests) {
        Map<Long, AdGroupSimple> adGroupsById =
                listToMap(asList(new TextAdGroup().withId(AD_GROUP_ID).withCampaignId(10L)
                        .withType(adGroupType)), AdGroupSimple::getId);
        return addRetargetingValidationService
                .validate(new ValidationResult<>(targetInterests), emptyList(),
                        adGroupsById, OPERATOR_ID, CLIENT_ID, SHARD);
    }

    @Before
    public void setup() {
        ClientService clientService = mock(ClientService.class);
        when(clientService.getWorkCurrency(any(ClientId.class)))
                .thenReturn(CURRENCY);

        RbacService rbacService = acceptAllCampaignsRbacService();

        CampaignRepository campaignRepository = acceptAllCampaignRepository();

        RetargetingConditionRepository retargetingConditionRepository = acceptAllRetCondRepository();

        AdGroupRepository adGroupRepository = acceptAllAdGroupRepository();

        TargetingCategoriesCache targetingCategoriesCache = acceptAllTargetingCategoriesCache();

        CpmYndxFrontpageCurrencyService cpmYndxFrontpageCurrencyService = mockedCpmYndxFrontpageCurrencyService();

        RetargetingsWithAdsValidator retargetingsWithAdsValidator = mock(RetargetingsWithAdsValidator.class);
        when(retargetingsWithAdsValidator.validateInterconnectionsWithAds(
                anyInt(), any(ClientId.class), any(List.class), any(List.class)))
                .thenAnswer((InvocationOnMock invocation) -> {
                    return ListValidationBuilder.of(invocation.getArgument(2)).getResult();
                });

        FeatureService featureService = mock(FeatureService.class);
        when(featureService.isEnabledForClientId(any(ClientId.class), any(FeatureName.class)))
                .thenAnswer((InvocationOnMock invocation) -> false);

        RetargetingConditionsCpmPriceValidationDataFactory cpmPriceValidationDataFactory =
                emptyRetargetingConditionsCpmPriceValidationDataFactory();

        addRetargetingValidationService =
                new AddRetargetingValidationService(
                        clientService, rbacService, campaignRepository,
                        retargetingConditionRepository, adGroupRepository, targetingCategoriesCache,
                        retargetingsWithAdsValidator, featureService, cpmYndxFrontpageCurrencyService,
                        cpmPriceValidationDataFactory);
    }
}
