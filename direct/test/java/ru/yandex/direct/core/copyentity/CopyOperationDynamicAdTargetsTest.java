package ru.yandex.direct.core.copyentity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithDynamicAdTargetsFilterService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetsQueryFilter;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.DynamicTextAdTargetService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.entity.showcondition.Constants.DEFAULT_AUTOBUDGET_PRIORITY;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicFeedAdTarget;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTarget;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CopyOperationDynamicAdTargetsTest {
    private static final RecursiveComparisonConfiguration DYNAMIC_AD_TARGET_COMPARE_STRATEGY =
            RecursiveComparisonConfiguration.builder()
            .withIgnoredFields("id", "dynamicConditionId", "adGroupId", "campaignId")
            .build();

    @Autowired
    private Steps steps;
    @Autowired
    private CopyOperationFactory factory;
    @Autowired
    private CopyOperationAssert asserts;
    @Autowired
    private AdGroupWithDynamicAdTargetsFilterService adGroupWithDynamicAdTargetsService;
    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;

    private Long operatorUid;
    private ClientId clientId;
    private int shard;
    private ClientInfo clientInfo;
    private Long campaignIdFrom;
    private AdGroupInfo dynamicTextAdGroup;
    private AdGroupInfo dynamicFeedAdGroup;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        operatorUid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        CampaignInfo campaignInfoFrom = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        campaignIdFrom = campaignInfoFrom.getCampaignId();

        dynamicTextAdGroup = steps.adGroupSteps().createAdGroup(activeDynamicTextAdGroup(null), campaignInfoFrom);

        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        dynamicFeedAdGroup = steps.adGroupSteps().createAdGroup(
                activeDynamicFeedAdGroup(null, feedId), campaignInfoFrom);

        asserts.init(clientId, clientId, operatorUid);
    }

    @Test
    public void adGroupWithDynamicTextAdTarget() {
        DynamicTextAdTarget dynamicTextAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(dynamicTextAdGroup)
                .getDynamicTextAdTarget();

        Set<Long> copiedAdGroupIds = copyAdGroups(List.of(dynamicTextAdGroup.getAdGroupId()), campaignIdFrom);

        List<DynamicAdTarget> copiedDynamicAdTargets = getDynamicAdTargetsByAdGroupIds(clientId, copiedAdGroupIds);
        assertDynamicAdTarget(dynamicTextAdTarget, copiedDynamicAdTargets);
    }

    @Test
    public void adGroupWithDynamicFeedAdTarget() {
        DynamicFeedAdTarget dynamicFeedAdTarget = defaultDynamicFeedAdTarget(dynamicFeedAdGroup.getAdGroupId())
                .withAutobudgetPriority(3);
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(dynamicFeedAdGroup, dynamicFeedAdTarget);

        Set<Long> copiedAdGroupIds = copyAdGroups(List.of(dynamicFeedAdGroup.getAdGroupId()), campaignIdFrom);

        List<DynamicAdTarget> copiedDynamicAdTargets = getDynamicAdTargetsByAdGroupIds(clientId, copiedAdGroupIds);
        assertDynamicAdTarget(dynamicFeedAdTarget, copiedDynamicAdTargets);
    }

    @Test
    public void twoAdGroups_withDynamicTextAdTarget_andDynamicFeedAdTarget() {
        DynamicTextAdTarget dynamicTextAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(dynamicTextAdGroup)
                .getDynamicTextAdTarget();

        DynamicFeedAdTarget dynamicFeedAdTarget = defaultDynamicFeedAdTarget(dynamicFeedAdGroup.getAdGroupId())
                .withAutobudgetPriority(3);
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(dynamicFeedAdGroup, dynamicFeedAdTarget);
        List<Long> adGroupIdsFrom = List.of(dynamicTextAdGroup.getAdGroupId(), dynamicFeedAdGroup.getAdGroupId());

        Set<Long> copiedAdGroupIds = copyAdGroups(adGroupIdsFrom, campaignIdFrom);
        Set<Long> copiedIds = adGroupWithDynamicAdTargetsService
                .getChildEntityIdsByParentIds(clientId, operatorUid, copiedAdGroupIds);

        asserts.assertEntitiesAreCopied(DynamicAdTarget.class, copiedIds,
                List.of(dynamicTextAdTarget, dynamicFeedAdTarget), COPIED);
    }

    @Test
    public void campaignWithDynamicTextAdTarget_whenCurrencyChanges() {
        ClientInfo clientInfoFrom = steps.clientSteps().createClient(
                defaultClient().withWorkCurrency(CurrencyCode.RUB));

        ClientInfo clientInfoTo = steps.clientSteps().createClient(
                defaultClient().withWorkCurrency(CurrencyCode.YND_FIXED));
        ClientId clientIdTo = clientInfoTo.getClientId();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfoFrom);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(activeDynamicTextAdGroup(null), campaignInfo);

        DynamicTextAdTarget dynamicTextAdTarget = defaultDynamicTextAdTarget(adGroupInfo)
                .withPrice(BigDecimal.valueOf(150L))
                .withPriceContext(BigDecimal.valueOf(300L));
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(adGroupInfo, dynamicTextAdTarget);

        // копируем динамические условия клиенту с другой валютой
        CopyConfig copyConfig = CopyEntityTestUtils.campaignsBetweenClientsCopyConfig(
                clientInfoFrom, clientInfoTo, campaignInfo.getCampaignId(), operatorUid);
        Set<Long> copiedAdGroupIds = copyAdGroups(copyConfig);

        List<DynamicAdTarget> copiedDynamicAdTargets = getDynamicAdTargetsByAdGroupIds(clientIdTo, copiedAdGroupIds);
        assertThat(copiedDynamicAdTargets).hasSize(1);

        // проверяем, что цены сконвертированы
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(copiedDynamicAdTargets.get(0).getPrice())
                    .isEqualByComparingTo(BigDecimal.valueOf(5L));
            softly.assertThat(copiedDynamicAdTargets.get(0).getPriceContext())
                    .isEqualByComparingTo(BigDecimal.valueOf(10L));
        });
    }

    @Test
    public void campaignWithDynamicTextAdTarget_nullFieldsTest() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(activeDynamicTextAdGroup(null), campaignInfo);

        DynamicTextAdTarget expectedDynamicTextAdTarget = defaultDynamicTextAdTarget(adGroupInfo)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(adGroupInfo, expectedDynamicTextAdTarget);

        Set<Long> copiedAdGroupIds = copyAdGroups(List.of(adGroupInfo.getAdGroupId()), campaignIdFrom);

        List<DynamicAdTarget> copiedDynamicAdTargets = getDynamicAdTargetsByAdGroupIds(clientId, copiedAdGroupIds);

        Currency currency = Currencies.getCurrency(clientInfo.getClient().getWorkCurrency());

        expectedDynamicTextAdTarget
                .withPrice(currency.getMinPrice())
                .withPriceContext(currency.getMinPrice())
                .withAutobudgetPriority(DEFAULT_AUTOBUDGET_PRIORITY);

        assertDynamicAdTarget(expectedDynamicTextAdTarget, copiedDynamicAdTargets);
    }

    private Set<Long> copyAdGroups(List<Long> adGroupIdsFrom, Long campaignId) {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdsFrom, campaignId, operatorUid);
        return copyAdGroups(copyConfig);
    }

    private Set<Long> copyAdGroups(CopyConfig copyConfig) {
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Map<Object, Object> entityMapping = copyResult.getEntityMapping(AdGroup.class);
        return StreamEx.of(entityMapping.values()).select(Long.class).toSet();
    }

    private List<DynamicAdTarget> getDynamicAdTargetsByAdGroupIds(ClientId clientId, Set<Long> adGroupIds) {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withAdGroupIds(adGroupIds)
                .withIncludeDeleted(false);
        return dynamicTextAdTargetService.getDynamicAdTargets(clientId, queryFilter);
    }

    private void assertDynamicAdTarget(
            DynamicAdTarget expectedDynamicTextAdTarget, List<? extends DynamicAdTarget> actualDynamicTextAdTargets) {
        assertThat(actualDynamicTextAdTargets)
                .hasSize(1)
                .element(0)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .usingRecursiveComparison(DYNAMIC_AD_TARGET_COMPARE_STRATEGY)
                .isEqualTo(expectedDynamicTextAdTarget);
    }
}
