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
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerWithStatusBsSynced;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils;
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
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicTextAdTargetsUpdateOperationTest {

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
    private DynamicTextAdTarget dynamicTextAdTarget;
    private AdGroupInfo dynamicTextAdGroup;
    private Long bannerId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();

        dynamicTextAdGroup = steps.adGroupSteps().createDynamicTextAdGroup(clientInfo,
                activeDynamicTextAdGroup(null)
                        .withLastChange(LocalDateTime.now().minusHours(5).truncatedTo(ChronoUnit.SECONDS))
                        .withStatusBLGenerated(StatusBLGenerated.NO)
                        .withStatusModerate(StatusModerate.YES)
                        .withStatusBsSynced(StatusBsSynced.YES));

        dynamicTextAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(dynamicTextAdGroup)
                .getDynamicTextAdTarget();

        bannerId = steps.bannerSteps().createActiveDynamicBanner(dynamicTextAdGroup).getBannerId();
    }

    @Test
    public void updateDynamicTextAdTargets_whenNameChanged() {
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4))
                .withIsSuspended(true);

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicTextAdTarget actual = getDynamicTextAdTarget(newDynamicAdTarget.getId());
        assertThat(actual).isEqualToIgnoringNullFields(newDynamicAdTarget);
    }

    @Test
    public void updateDynamicTextAdTargets_whenPriceChanged() {
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withPrice(BigDecimal.valueOf(15))
                .withPriceContext(BigDecimal.valueOf(25))
                .withAutobudgetPriority(5);

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicTextAdTarget actual = getDynamicTextAdTarget(newDynamicAdTarget.getId());
        assertThat(actual)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(newDynamicAdTarget);
    }

    @Test
    public void updateDynamicTextAdTargets_whenConditionChanged() {
        List<WebpageRule> condition = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                .getCondition();
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(condition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(condition));

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicTextAdTarget actual = getDynamicTextAdTarget(newDynamicAdTarget.getId());
        assertThat(actual).isEqualToIgnoringNullFields(newDynamicAdTarget);
    }

    @Test
    public void updateAdGroupStatuses_whenNameChanged() {
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4));

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicTextAdGroup adGroup = getDynamicTextAdGroup(dynamicTextAdGroup.getAdGroupId());
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
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withPrice(BigDecimal.valueOf(15));

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicTextAdGroup adGroup = getDynamicTextAdGroup(dynamicTextAdGroup.getAdGroupId());
        StatusBsSynced bannerStatusBsSynced = getBannerStatusBsSynced(bannerId);

        assertSoftly(softly -> {
            softly.assertThat(adGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.YES);
            softly.assertThat(bannerStatusBsSynced).isEqualTo(StatusBsSynced.YES);
            softly.assertThat(adGroup.getStatusBLGenerated()).isEqualTo(StatusBLGenerated.NO);
            softly.assertThat(adGroup.getLastChange()).isEqualTo(dynamicTextAdGroup.getAdGroup().getLastChange());
        });
    }

    @Test
    public void updateAdGroupStatuses_whenConditionChanged() {
        List<WebpageRule> condition = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                .getCondition();
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(condition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(condition));

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicTextAdGroup adGroup = getDynamicTextAdGroup(dynamicTextAdGroup.getAdGroupId());
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
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withIsSuspended(true);

        updateDynamicAdTarget(newDynamicAdTarget);

        DynamicTextAdGroup adGroup = getDynamicTextAdGroup(dynamicTextAdGroup.getAdGroupId());
        StatusBsSynced bannerStatusBsSynced = getBannerStatusBsSynced(bannerId);

        assertSoftly(softly -> {
            softly.assertThat(adGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            softly.assertThat(bannerStatusBsSynced).isEqualTo(StatusBsSynced.NO);
            softly.assertThat(adGroup.getStatusBLGenerated()).isEqualTo(StatusBLGenerated.NO);
            softly.assertThat(adGroup.getLastChange()).is(matchedBy(approximatelyNow()));
        });
    }

    private void updateDynamicAdTarget(DynamicTextAdTarget newDynamicAdTarget) {
        ModelChanges<DynamicTextAdTarget> modelChanges = toModelChanges(newDynamicAdTarget);

        MassResult<Long> result = dynamicTextAdTargetService.updateDynamicTextAdTargets(
                clientId, operatorUid, List.of(modelChanges));
        assumeThat(result, isSuccessful(true));
    }

    private ModelChanges<DynamicTextAdTarget> toModelChanges(DynamicTextAdTarget newDynamicAdTarget) {

        ModelChanges<DynamicTextAdTarget> modelChanges =
                new ModelChanges<>(newDynamicAdTarget.getId(), DynamicTextAdTarget.class);

        modelChanges.processNotNull(newDynamicAdTarget.getCondition(), DynamicTextAdTarget.CONDITION);
        modelChanges.processNotNull(newDynamicAdTarget.getConditionHash(), DynamicAdTarget.CONDITION_HASH);
        modelChanges.processNotNull(newDynamicAdTarget.getConditionUniqHash(), DynamicTextAdTarget.CONDITION_UNIQ_HASH);

        modelChanges.processNotNull(newDynamicAdTarget.getConditionName(), DynamicAdTarget.CONDITION_NAME);
        modelChanges.processNotNull(newDynamicAdTarget.getPrice(), DynamicAdTarget.PRICE);
        modelChanges.processNotNull(newDynamicAdTarget.getPriceContext(), DynamicAdTarget.PRICE_CONTEXT);
        modelChanges.processNotNull(newDynamicAdTarget.getAutobudgetPriority(), DynamicAdTarget.AUTOBUDGET_PRIORITY);
        modelChanges.processNotNull(newDynamicAdTarget.getIsSuspended(), DynamicAdTarget.IS_SUSPENDED);

        return modelChanges;
    }

    private DynamicTextAdTarget getDynamicTextAdTarget(Long id) {
        List<DynamicTextAdTarget> dynamicAdTargets = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsByIds(shard, clientId, List.of(id));

        assumeThat(dynamicAdTargets, hasSize(1));
        return dynamicAdTargets.get(0);
    }

    private DynamicTextAdGroup getDynamicTextAdGroup(Long adGroupId) {
        return (DynamicTextAdGroup) adGroupRepository.getAdGroups(shard, List.of(adGroupId)).get(0);
    }

    private StatusBsSynced getBannerStatusBsSynced(Long bannerId) {
        return bannerRepository.getSafely(shard, List.of(bannerId), BannerWithStatusBsSynced.class)
                .get(0)
                .getStatusBsSynced();
    }
}
