package ru.yandex.direct.core.entity.dynamictextadtarget.service;

import java.math.BigDecimal;
import java.util.List;

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
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestAutobudgetAlertRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.notAcceptableAdGroupType;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicFeedAdTarget;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicFeedAdTargetServiceAddTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private TestAutobudgetAlertRepository testAutobudgetAlertRepository;

    private long operatorUid;
    private ClientId clientId;
    private int shard;

    private AdGroupInfo activeAdGroupInfo;
    private Long activeAdGroupId;

    private AdGroupInfo draftAdGroupInfo;
    private Long draftAdGroupId;

    @Before
    public void before() {
        var clientInfo = steps.clientSteps().createDefaultClient();

        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        var feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        var adGroup = TestGroups.activeDynamicFeedAdGroup(null, feedInfo.getFeedId())
                .withStatusBLGenerated(StatusBLGenerated.NO);
        activeAdGroupInfo = steps.adGroupSteps().createDynamicFeedAdGroup(clientInfo, adGroup);
        draftAdGroupInfo = steps.adGroupSteps().createDraftDynamicFeedAdGroup(clientInfo);
        activeAdGroupId = activeAdGroupInfo.getAdGroupId();
        draftAdGroupId = draftAdGroupInfo.getAdGroupId();
    }

    @Test
    public void addDynamicFeedAdTargetsServiceTest() {
        DynamicFeedAdTarget dynamicFeedAdTarget = defaultDynamicFeedAdTarget(activeAdGroupId);
        Long dynamicConditionId = addDynamicFeedAdTargets(dynamicFeedAdTarget);

        List<DynamicFeedAdTarget> actual = dynamicTextAdTargetRepository.getDynamicFeedAdTargets(
                shard, clientId, List.of(dynamicConditionId));

        assertThat(actual)
                .hasSize(1)
                .element(0)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(dynamicFeedAdTarget);
    }

    @Test
    public void processReuseDeletedConditionTest() {
        Long dynamicConditionId = addDynamicFeedAdTargets(defaultDynamicFeedAdTarget(activeAdGroupId));

        dynamicTextAdTargetRepository.deleteDynamicTextAdTargets(shard, List.of(dynamicConditionId));

        Long newDynamicConditionId = addDynamicFeedAdTargets(defaultDynamicFeedAdTarget(activeAdGroupId));
        assertThat(newDynamicConditionId).isEqualTo(dynamicConditionId);
    }

    @Test
    public void updateAdGroupStatusesTest() {
        testAutobudgetAlertRepository.addAutobudgetAlert(activeAdGroupInfo.getCampaignInfo());
        addDynamicFeedAdTargets(defaultDynamicFeedAdTarget(activeAdGroupId));

        DynamicFeedAdGroup expectedAdGroup = new DynamicFeedAdGroup()
                .withStatusModerate(StatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING);

        DynamicFeedAdGroup actualAdGroup = (DynamicFeedAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(activeAdGroupId)).get(0);

        assertThat(actualAdGroup).isEqualToIgnoringNullFields(expectedAdGroup);
        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(activeAdGroupInfo);
    }

    @Test
    public void updateDraftAdGroupStatusesTest() {
        testAutobudgetAlertRepository.addAutobudgetAlert(draftAdGroupInfo.getCampaignInfo());
        addDynamicFeedAdTargets(defaultDynamicFeedAdTarget(draftAdGroupId));

        DynamicFeedAdGroup expectedAdGroup = new DynamicFeedAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusBLGenerated(StatusBLGenerated.NO);

        DynamicFeedAdGroup actualAdGroup = (DynamicFeedAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(draftAdGroupId)).get(0);

        assertThat(actualAdGroup).isEqualToIgnoringNullFields(expectedAdGroup);
        testAutobudgetAlertRepository.assertAutobudgetAlertNotFrozen(draftAdGroupInfo);
    }

    @Test
    public void updateAdGroupStatusesTest_whenSuspendedDynamicAdTarget() {
        addDynamicFeedAdTargets(defaultDynamicFeedAdTarget(activeAdGroupId).withIsSuspended(true));

        DynamicFeedAdGroup expectedAdGroup = new DynamicFeedAdGroup()
                .withStatusModerate(StatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusBLGenerated(StatusBLGenerated.NO);

        DynamicFeedAdGroup actualAdGroup = (DynamicFeedAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(activeAdGroupId)).get(0);

        assertThat(actualAdGroup).isEqualToIgnoringNullFields(expectedAdGroup);
    }

    @Test
    public void addDynamicFeedAdTargets_failure_whenDynamicTextAdGroup() {
        Long dynamicTextAdGroupId = steps.adGroupSteps()
                .createActiveDynamicTextAdGroup(activeAdGroupInfo.getClientInfo())
                .getAdGroupId();

        DynamicFeedAdTarget dynamicFeedAdTarget = defaultDynamicFeedAdTarget(dynamicTextAdGroupId);

        MassResult<Long> result = dynamicTextAdTargetService.addDynamicFeedAdTargets(clientId, operatorUid,
                singletonList(dynamicFeedAdTarget));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(index(0), field(DynamicAdTarget.AD_GROUP_ID)),
                                notAcceptableAdGroupType()))));
    }

    @Test
    public void addDynamicFeedAdTargets_whenTwoItems() {
        List<DynamicFeedAdTarget> dynamicFeedAdTargets = List.of(
                dynamicFeedAdTargetWithRandomRules(activeAdGroupInfo),
                dynamicFeedAdTargetWithRandomRules(activeAdGroupInfo)
        );

        MassResult<Long> result = dynamicTextAdTargetService.addDynamicFeedAdTargets(clientId, operatorUid,
                dynamicFeedAdTargets);

        assertThat(result).is(matchedBy(isSuccessful(true, true)));
    }

    @Test
    public void addDynamicFeedAdTargets_whenNullTab() {
        DynamicFeedAdTarget dynamicFeedAdTarget = defaultDynamicFeedAdTarget(activeAdGroupId).withTab(null);
        Long dynamicConditionId = addDynamicFeedAdTargets(dynamicFeedAdTarget);

        List<DynamicFeedAdTarget> actual = dynamicTextAdTargetRepository.getDynamicFeedAdTargets(
                shard, clientId, List.of(dynamicConditionId));

        assertThat(actual)
                .element(0)
                .isEqualToIgnoringNullFields(new DynamicFeedAdTarget().withTab(DynamicAdTargetTab.CONDITION));
    }

    private Long addDynamicFeedAdTargets(DynamicFeedAdTarget dynamicFeedAdTarget) {
        MassResult<Long> result = dynamicTextAdTargetService.addDynamicFeedAdTargets(clientId, operatorUid,
                singletonList(dynamicFeedAdTarget));
        assumeThat(result, isSuccessful(true));
        return result.getResult().get(0).getResult();
    }
}
