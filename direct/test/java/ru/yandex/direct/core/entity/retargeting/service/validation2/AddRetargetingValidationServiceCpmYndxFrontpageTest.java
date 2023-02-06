package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
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
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesCache;
import ru.yandex.direct.core.entity.retargeting.service.validation2.cpmprice.RetargetingConditionsCpmPriceValidationDataFactory;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
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
import static ru.yandex.direct.core.entity.retargeting.service.validation2.MockServices.emptyRetargetingConditionsCpmPriceValidationDataFactory;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.currency.CurrencyCode.RUB;
import static ru.yandex.direct.feature.FeatureName.CPM_YNDX_FRONTPAGE_PROFILE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AddRetargetingValidationServiceCpmYndxFrontpageTest {

    private static final int SHARD = 4;
    private static final ClientId DEFAULT_CLIENT_ID = ClientId.fromLong(1L);
    private static final Long DEFAULT_AD_GROUP_ID = 10L;
    private static final ClientId CPM_YNDX_FRONTPAGE_PROFILE_FEATURE_CLIENT_ID = ClientId.fromLong(2L);
    private static final Long AD_GROUP_ID_FOR_FEATURE_CLIENT = 11L;
    private static final Long OPERATOR_ID = 1000L;
    private static final Long FAKE_RET_CONDITION_ID = 1235813L;
    private static final Currency CURRENCY = RUB.getCurrency();

    private CampaignRepository campaignRepository;
    private RetargetingConditionRepository retargetingConditionRepository;
    private AddRetargetingValidationService addRetargetingValidationService;
    private FeatureService featureService;


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

    private static CpmYndxFrontpageCurrencyService mockedCpmYndxFrontpageCurrencyService() {
        CpmYndxFrontpageCurrencyService cpmYndxFrontpageCurrencyService = mock(CpmYndxFrontpageCurrencyService.class);
        CpmYndxFrontpageAdGroupPriceRestrictions defaultCpmYndxFrontpageAdGroupRestrictions =
                new CpmYndxFrontpageAdGroupPriceRestrictions(BigDecimal.valueOf(0),
                        BigDecimal.valueOf(Double.MAX_VALUE))
                        .withClientCurrency(CURRENCY);
        when(cpmYndxFrontpageCurrencyService
                .getAdGroupIdsToPriceDataMapByAdGroups(any(Collection.class), anyInt(), any(ClientId.class)))
                .thenReturn(ImmutableMap.of(AD_GROUP_ID_FOR_FEATURE_CLIENT, defaultCpmYndxFrontpageAdGroupRestrictions,
                        DEFAULT_AD_GROUP_ID, defaultCpmYndxFrontpageAdGroupRestrictions));
        when(cpmYndxFrontpageCurrencyService
                .getAdGroupIdsToPriceDataMapByAdGroups(any(Collection.class), anyInt(), any(Currency.class)))
                .thenReturn(ImmutableMap.of(AD_GROUP_ID_FOR_FEATURE_CLIENT, defaultCpmYndxFrontpageAdGroupRestrictions,
                        DEFAULT_AD_GROUP_ID, defaultCpmYndxFrontpageAdGroupRestrictions));
        when(cpmYndxFrontpageCurrencyService
                .getAdGroupIndexesToPriceDataMapByAdGroups(any(List.class), anyInt(), any(ClientId.class)))
                .thenReturn(ImmutableMap.of(
                        0, defaultCpmYndxFrontpageAdGroupRestrictions, 1, defaultCpmYndxFrontpageAdGroupRestrictions));
        when(cpmYndxFrontpageCurrencyService
                .getAdGroupIndexesToPriceDataMapByAdGroups(any(List.class), anyInt(), any(Currency.class)))
                .thenReturn(ImmutableMap.of(
                        0, defaultCpmYndxFrontpageAdGroupRestrictions, 1, defaultCpmYndxFrontpageAdGroupRestrictions));
        return cpmYndxFrontpageCurrencyService;
    }

    private FeatureService mockedFeatureService() {
        FeatureService featureService = mock(FeatureService.class);
        when(featureService.getEnabledForClientId(any(ClientId.class)))
                .thenAnswer((InvocationOnMock invocation) ->
//                        (invocation.getArguments().length > 1
//                                && invocation.getArgument(1) == CPM_YNDX_FRONTPAGE_PROFILE
//                                && invocation.getArgument(0) == CPM_YNDX_FRONTPAGE_PROFILE_FEATURE_CLIENT_ID)
                        (invocation.getArgument(0) == CPM_YNDX_FRONTPAGE_PROFILE_FEATURE_CLIENT_ID)
                                ? Set.of(CPM_YNDX_FRONTPAGE_PROFILE.getName()) : emptySet());

        return featureService;
    }

    private void returnCampaignsWithType(CampaignType campaignType) {
        when(campaignRepository.getCampaignsSimple(anyInt(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Collection<Long> cids = (Collection<Long>) invocation.getArguments()[1];
            return listToMap(mapList(cids, id -> new Campaign()
                            .withId(id)
                            .withType(campaignType)
                            .withStatusArchived(false)),
                    CampaignSimple::getId);
        });
    }

    private void returnFrontpageDefaultRule() {
        returnRetCondWithRule(new Rule().withType(RuleType.ALL));
    }

    private void returnPriceDefaultRule() {
        returnRetCondWithRule(new Rule()
                .withType(RuleType.OR)
                .withGoals(List.of(
                        defaultGoalByType(GoalType.SOCIAL_DEMO)
                )));
    }

    private void returnRetCondWithRule(Rule rule) {
        when(retargetingConditionRepository
                .getFromRetargetingConditionsTable(anyInt(), any(ClientId.class),
                        ArgumentMatchers.<Collection<Long>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<Long> ids = (Collection<Long>) invocation.getArguments()[2];
                    return mapList(ids, id -> new RetargetingCondition()
                            .withId(id)
                            .withRules(singletonList(rule))
                            .withType(interests)
                    );
                });
    }

    @Before
    public void setup() {
        ClientService clientService = mock(ClientService.class);
        when(clientService.getWorkCurrency(any(ClientId.class)))
                .thenReturn(CURRENCY);

        featureService = mockedFeatureService();


        RbacService rbacService = acceptAllCampaignsRbacService();

        CpmYndxFrontpageCurrencyService cpmYndxFrontpageCurrencyService = mockedCpmYndxFrontpageCurrencyService();

        AdGroupRepository adGroupRepository = mock(AdGroupRepository.class);

        TargetingCategoriesCache targetingCategoriesCache = mock(TargetingCategoriesCache.class);

        RetargetingsWithAdsValidator retargetingsWithAdsValidator = mock(RetargetingsWithAdsValidator.class);
        when(retargetingsWithAdsValidator.validateInterconnectionsWithAds(
                anyInt(), any(ClientId.class), any(List.class), any(List.class)))
                .thenAnswer((InvocationOnMock invocation) -> {
                    return ListValidationBuilder.of(invocation.getArgument(2)).getResult();
                });

        campaignRepository = mock(CampaignRepository.class);
        retargetingConditionRepository = mock(RetargetingConditionRepository.class);

        RetargetingConditionsCpmPriceValidationDataFactory cpmPriceValidationDataFactory =
                emptyRetargetingConditionsCpmPriceValidationDataFactory();

        addRetargetingValidationService =
                new AddRetargetingValidationService(
                        clientService, rbacService, campaignRepository,
                        retargetingConditionRepository, adGroupRepository, targetingCategoriesCache,
                        retargetingsWithAdsValidator, featureService, cpmYndxFrontpageCurrencyService,
                        cpmPriceValidationDataFactory);
    }

    private ValidationResult<List<TargetInterest>, Defect> validateAddWithDefaultParameters(
            ClientId clientId, Long adGroupId, List<TargetInterest> targetInterests) {
        Map<Long, AdGroupSimple> adGroupsById =
                listToMap(asList(new TextAdGroup().withId(adGroupId).withCampaignId(10L)
                        .withType(AdGroupType.CPM_YNDX_FRONTPAGE)), AdGroupSimple::getId);
        return addRetargetingValidationService
                .validate(new ValidationResult<>(targetInterests), emptyList(),
                        adGroupsById, OPERATOR_ID, clientId, SHARD);
    }

    @Test
    public void validate_CpmYndxFrontpageCampaign_NonEmptyRetCondition_NoError() {
        returnCampaignsWithType(CampaignType.CPM_YNDX_FRONTPAGE);
        returnFrontpageDefaultRule();

        List<TargetInterest> retargetings =
                asList(new TargetInterest().withAdGroupId(AD_GROUP_ID_FOR_FEATURE_CLIENT)
                        .withRetargetingConditionId(FAKE_RET_CONDITION_ID));
        ValidationResult<List<TargetInterest>, Defect> actual =
                validateAddWithDefaultParameters(CPM_YNDX_FRONTPAGE_PROFILE_FEATURE_CLIENT_ID,
                        AD_GROUP_ID_FOR_FEATURE_CLIENT, retargetings);
        assertThat(actual, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_CpmYndxFrontpageCampaign_NonEmptyRetCondition_Error() {
        returnCampaignsWithType(CampaignType.CPM_YNDX_FRONTPAGE);
        returnFrontpageDefaultRule();

        List<TargetInterest> retargetings =
                asList(new TargetInterest().withAdGroupId(DEFAULT_AD_GROUP_ID)
                        .withRetargetingConditionId(FAKE_RET_CONDITION_ID));

        ValidationResult<List<TargetInterest>, Defect> actual =
                validateAddWithDefaultParameters(DEFAULT_CLIENT_ID, DEFAULT_AD_GROUP_ID, retargetings);
        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field(Retargeting.RETARGETING_CONDITION_ID.name())),
                        RetargetingDefects.cpmYndxFrontpageRetargetingsNotAllowed())));
    }

    @Test
    public void validate_CpmPriceCampaign_UnsupportedFields_Error() {
        returnCampaignsWithType(CampaignType.CPM_PRICE);
        returnPriceDefaultRule();

        List<TargetInterest> retargetings =
                asList(new TargetInterest().withAdGroupId(DEFAULT_AD_GROUP_ID)
                        .withPriceContext(BigDecimal.TEN)
                        .withRetargetingConditionId(FAKE_RET_CONDITION_ID));
        ValidationResult<List<TargetInterest>, Defect> actual =
                validateAddWithDefaultParameters(DEFAULT_CLIENT_ID, DEFAULT_AD_GROUP_ID, retargetings);
        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(field(Retargeting.PRICE_CONTEXT)), isNull())
        ));
    }

}
