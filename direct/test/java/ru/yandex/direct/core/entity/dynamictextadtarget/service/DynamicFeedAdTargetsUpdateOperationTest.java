package ru.yandex.direct.core.entity.dynamictextadtarget.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerWithStatusBsSynced;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getHashForDynamicFeedRules;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicFeedAdTargetsUpdateOperationTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerTypedRepository bannerRepository;

    private int shard;
    private long operatorUid;
    private ClientId clientId;
    private DynamicFeedAdTarget dynamicFeedAdTarget;
    private DynamicFeedAdTarget suspendedDynamicAdTarget;
    private AdGroupInfo dynamicFeedAdGroup;
    private Long bannerId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();

        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        dynamicFeedAdGroup = steps.adGroupSteps().createDynamicFeedAdGroup(clientInfo,
                activeDynamicFeedAdGroup(null, feedId)
                        .withLastChange(LocalDateTime.now().minusHours(5).truncatedTo(ChronoUnit.SECONDS))
                        .withStatusBLGenerated(StatusBLGenerated.NO)
                        .withStatusModerate(StatusModerate.YES)
                        .withStatusBsSynced(StatusBsSynced.YES));

        dynamicFeedAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(dynamicFeedAdGroup);

        suspendedDynamicAdTarget = dynamicFeedAdTargetWithRandomRules(dynamicFeedAdGroup)
                .withIsSuspended(true);
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(dynamicFeedAdGroup, suspendedDynamicAdTarget);

        bannerId = steps.bannerSteps().createActiveDynamicBanner(dynamicFeedAdGroup).getBannerId();
    }

    @Test
    public void updateDynamicFeedAdTargets_whenNameChanged() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4));

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicFeedAdTarget actual = getDynamicFeedAdTarget(newDynamicAdTarget.getId());
        assertThat(actual)
                .isEqualToIgnoringNullFields(newDynamicAdTarget);
    }

    @Test
    public void updateDynamicFeedAdTargets_whenPriceChanged() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withPrice(BigDecimal.valueOf(15))
                .withPriceContext(BigDecimal.valueOf(25))
                .withAutobudgetPriority(5);

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicFeedAdTarget actual = getDynamicFeedAdTarget(newDynamicAdTarget.getId());
        assertThat(actual)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(newDynamicAdTarget);
    }

    @Test
    public void updateDynamicFeedAdTargets_whenConditionChanged() {
        List<DynamicFeedRule> condition = dynamicFeedAdTargetWithRandomRules(dynamicFeedAdGroup)
                .getCondition();
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(getHashForDynamicFeedRules(condition))
                .withTab(DynamicAdTargetTab.CONDITION);

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicFeedAdTarget actual = getDynamicFeedAdTarget(newDynamicAdTarget.getId());
        assertThat(actual).isEqualToIgnoringNullFields(newDynamicAdTarget);
    }

    @Test
    public void updateDynamicFeedAdTargets_whenIsSuspendedChanged() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(suspendedDynamicAdTarget.getId())
                .withIsSuspended(false);

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicFeedAdTarget actual = getDynamicFeedAdTarget(newDynamicAdTarget.getId());
        assertThat(actual).isEqualToIgnoringNullFields(newDynamicAdTarget);
    }

    @Test
    public void updateAdGroupStatuses_whenNameChanged() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4));

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicFeedAdGroup adGroup = getDynamicFeedAdGroup(dynamicFeedAdGroup.getAdGroupId());
        StatusBsSynced bannerStatusBsSynced = getBannerStatusBsSynced(bannerId);

        assertSoftly(softly -> {
            softly.assertThat(adGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.YES);
            softly.assertThat(bannerStatusBsSynced).isEqualTo(StatusBsSynced.YES);
            softly.assertThat(adGroup.getStatusBLGenerated()).isEqualTo(StatusBLGenerated.NO);
            softly.assertThat(adGroup.getLastChange()).is(matchedBy(approximatelyNow()));
        });
    }

    @Test
    public void updateAdGroupStatuses_whenPriceChanged() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withPrice(BigDecimal.valueOf(15))
                .withTab(DynamicAdTargetTab.CONDITION);

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicFeedAdGroup adGroup = getDynamicFeedAdGroup(dynamicFeedAdGroup.getAdGroupId());
        StatusBsSynced bannerStatusBsSynced = getBannerStatusBsSynced(bannerId);

        assertSoftly(softly -> {
            softly.assertThat(adGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.YES);
            softly.assertThat(bannerStatusBsSynced).isEqualTo(StatusBsSynced.YES);
            softly.assertThat(adGroup.getStatusBLGenerated()).isEqualTo(StatusBLGenerated.NO);
            softly.assertThat(adGroup.getLastChange()).isEqualTo(dynamicFeedAdGroup.getAdGroup().getLastChange());
        });
    }

    @Test
    public void updateAdGroupStatuses_whenConditionChanged() {
        List<DynamicFeedRule> condition = dynamicFeedAdTargetWithRandomRules(dynamicFeedAdGroup)
                .getCondition();
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(getHashForDynamicFeedRules(condition));

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicFeedAdGroup adGroup = getDynamicFeedAdGroup(dynamicFeedAdGroup.getAdGroupId());
        StatusBsSynced bannerStatusBsSynced = getBannerStatusBsSynced(bannerId);

        assertSoftly(softly -> {
            softly.assertThat(adGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            softly.assertThat(bannerStatusBsSynced).isEqualTo(StatusBsSynced.NO);
            softly.assertThat(adGroup.getStatusBLGenerated()).isEqualTo(StatusBLGenerated.PROCESSING);
            softly.assertThat(adGroup.getLastChange()).is(matchedBy(approximatelyNow()));
        });
    }

    @Test
    public void updateAdGroupStatuses_whenIsSuspendedChanged() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(suspendedDynamicAdTarget.getId())
                .withIsSuspended(false);

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicFeedAdGroup adGroup = getDynamicFeedAdGroup(dynamicFeedAdGroup.getAdGroupId());
        StatusBsSynced bannerStatusBsSynced = getBannerStatusBsSynced(bannerId);

        assertSoftly(softly -> {
            softly.assertThat(adGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            softly.assertThat(bannerStatusBsSynced).isEqualTo(StatusBsSynced.NO);
            softly.assertThat(adGroup.getStatusBLGenerated()).isEqualTo(StatusBLGenerated.PROCESSING);
            softly.assertThat(adGroup.getLastChange()).is(matchedBy(approximatelyNow()));
        });
    }

    private void updateDynamicAdTarget(DynamicFeedAdTarget newDynamicAdTarget) {
        ModelChanges<DynamicFeedAdTarget> modelChanges = toModelChanges(newDynamicAdTarget);

        MassResult<Long> result = dynamicTextAdTargetService.updateDynamicFeedAdTargets(
                clientId, operatorUid, List.of(modelChanges));
        assumeThat(result, isSuccessful(true));
    }

    private ModelChanges<DynamicFeedAdTarget> toModelChanges(DynamicFeedAdTarget newDynamicAdTarget) {

        ModelChanges<DynamicFeedAdTarget> modelChanges =
                new ModelChanges<>(newDynamicAdTarget.getId(), DynamicFeedAdTarget.class);

        modelChanges.processNotNull(newDynamicAdTarget.getCondition(), DynamicFeedAdTarget.CONDITION);
        modelChanges.processNotNull(newDynamicAdTarget.getConditionHash(), DynamicAdTarget.CONDITION_HASH);
        modelChanges.processNotNull(newDynamicAdTarget.getTab(), DynamicAdTarget.TAB);

        modelChanges.processNotNull(newDynamicAdTarget.getConditionName(), DynamicAdTarget.CONDITION_NAME);
        modelChanges.processNotNull(newDynamicAdTarget.getPrice(), DynamicAdTarget.PRICE);
        modelChanges.processNotNull(newDynamicAdTarget.getPriceContext(), DynamicAdTarget.PRICE_CONTEXT);
        modelChanges.processNotNull(newDynamicAdTarget.getAutobudgetPriority(), DynamicAdTarget.AUTOBUDGET_PRIORITY);
        modelChanges.processNotNull(newDynamicAdTarget.getIsSuspended(), DynamicAdTarget.IS_SUSPENDED);

        return modelChanges;
    }

    private DynamicFeedAdTarget getDynamicFeedAdTarget(Long id) {
        List<DynamicFeedAdTarget> dynamicAdTargets = dynamicTextAdTargetRepository
                .getDynamicFeedAdTargetsByIds(shard, clientId, List.of(id));

        assumeThat(dynamicAdTargets, hasSize(1));
        return dynamicAdTargets.get(0);
    }

    private DynamicFeedAdGroup getDynamicFeedAdGroup(Long adGroupId) {
        return (DynamicFeedAdGroup) adGroupRepository.getAdGroups(shard, List.of(adGroupId)).get(0);
    }

    private StatusBsSynced getBannerStatusBsSynced(Long bannerId) {
        return bannerRepository.getSafely(shard, List.of(bannerId), BannerWithStatusBsSynced.class)
                .get(0)
                .getStatusBsSynced();
    }
}
