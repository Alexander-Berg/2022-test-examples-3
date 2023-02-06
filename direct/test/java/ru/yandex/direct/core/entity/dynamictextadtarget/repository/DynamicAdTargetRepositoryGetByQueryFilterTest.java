package ru.yandex.direct.core.entity.dynamictextadtarget.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetBaseStatus;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetsQueryFilter;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTarget;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicAdTargetRepositoryGetByQueryFilterTest {

    private static final Long INCORRECT_ID = 555555L;

    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    private ClientInfo clientInfo;
    private int shard;
    private DynamicTextAdTarget dynamicTextAdTarget;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        AdGroupInfo dynamicTextAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);

        dynamicTextAdTarget = defaultDynamicTextAdTarget(dynamicTextAdGroup)
                .withConditionName("test")
                .withPrice(BigDecimal.valueOf(20))
                .withPriceContext(BigDecimal.valueOf(30))
                .withAutobudgetPriority(5)
                .withIsSuspended(false);

        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, dynamicTextAdTarget);
    }

    @Test
    public void getDynamicAdTargets_success() {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withCampaignIds(Set.of(dynamicTextAdTarget.getCampaignId()))
                .withAdGroupIds(Set.of(dynamicTextAdTarget.getAdGroupId()))
                .withIds(Set.of(dynamicTextAdTarget.getId()))
                .withIncludeDeleted(false)
                .withBaseStatuses(Set.of(DynamicAdTargetBaseStatus.ACTIVE))
                .withNameContains("test")
                .withNameNotContains("test2")
                .withMinPrice(BigDecimal.valueOf(15))
                .withMaxPrice(BigDecimal.valueOf(25))
                .withMinPriceContext(BigDecimal.valueOf(27))
                .withMaxPriceContext(BigDecimal.valueOf(35))
                .withAutobudgetPriorities(Set.of(5));

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(1);
    }

    @Test
    public void getDynamicAdTargets_emptyQueryFilter() {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter();

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(1);
    }

    @Test
    public void getDynamicAdTargets_incorrectCampaignId() {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withCampaignIds(Set.of(INCORRECT_ID));

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(0);
    }

    @Test
    public void getDynamicAdTargets_incorrectAdGroupId() {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withAdGroupIds(Set.of(INCORRECT_ID));

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(0);
    }

    @Test
    public void getDynamicAdTargets_incorrectId() {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withIds(Set.of(INCORRECT_ID));

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(0);
    }

    @Test
    public void getDynamicAdTargets_withDeleted() {
        dynamicTextAdTargetRepository.deleteDynamicTextAdTargets(
                shard, List.of(dynamicTextAdTarget.getDynamicConditionId()));

        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withIncludeDeleted(true);

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(1);
    }

    @Test
    public void getDynamicAdTargets_withoutDeleted() {
        dynamicTextAdTargetRepository.deleteDynamicTextAdTargets(
                shard, List.of(dynamicTextAdTarget.getDynamicConditionId()));

        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withIncludeDeleted(false);

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(0);
    }

    @Test
    public void getDynamicAdTargets_incorrectNameContains() {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withNameContains("test2");

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(0);
    }

    @Test
    public void getDynamicAdTargets_incorrectNameNotContains() {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withNameNotContains("test");

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(0);
    }

    @Test
    public void getDynamicAdTargets_incorrectMinPrice() {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withMinPrice(BigDecimal.valueOf(1000));

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(0);
    }

    @Test
    public void getDynamicAdTargets_dynamicFeedAdTarget() {
        AdGroupInfo dynamicFeedAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo);

        DynamicFeedAdTarget dynamicFeedAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(dynamicFeedAdGroup);

        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withIds(Set.of(dynamicFeedAdTarget.getId()));

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(1);
    }

    @Test
    public void getDynamicAdTargets_incorrectBaseStatuses() {
        DynamicAdTargetsQueryFilter queryFilter = new DynamicAdTargetsQueryFilter()
                .withBaseStatuses(Set.of(DynamicAdTargetBaseStatus.SUSPENDED));

        List<DynamicAdTarget> dynamicAdTargets = getDynamicAdTargets(queryFilter);
        assertThat(dynamicAdTargets).hasSize(0);
    }

    private List<DynamicAdTarget> getDynamicAdTargets(DynamicAdTargetsQueryFilter queryFilter) {
        return dynamicTextAdTargetRepository.getDynamicAdTargetsByQueryFilter(
                shard, clientInfo.getClientId(), queryFilter);
    }
}
