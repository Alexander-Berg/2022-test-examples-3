package ru.yandex.direct.core.entity.dynamictextadtarget.service;

import java.util.List;

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
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestAutobudgetAlertRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.notAcceptableAdGroupType;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTarget;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules;
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
public class DynamicTextAdTargetServiceAddTest {

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

    private AdGroupInfo activeAdGroup;
    private AdGroupInfo draftAdGroup;

    @Before
    public void before() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        var adGroup = TestGroups.activeDynamicTextAdGroup(null)
                .withStatusBLGenerated(StatusBLGenerated.NO);
        activeAdGroup = steps.adGroupSteps().createDynamicTextAdGroup(clientInfo, adGroup);
        draftAdGroup = steps.adGroupSteps().createDraftDynamicTextAdGroup(clientInfo);
    }

    @Test
    public void addDynamicTextAdTargetsServiceTest() {
        DynamicTextAdTarget dynamicTextAdTarget = defaultDynamicTextAdTarget(activeAdGroup);

        MassResult<Long> result =
                dynamicTextAdTargetService
                        .addDynamicTextAdTargets(clientId, operatorUid, singletonList(dynamicTextAdTarget));

        assertThat(result).is(matchedBy(isSuccessful(true)));
    }

    @Test
    public void addDynamicTextAdTargetsDraftServiceTest() {
        DynamicTextAdTarget dynamicTextAdTarget = defaultDynamicTextAdTarget(draftAdGroup);

        MassResult<Long> result =
                dynamicTextAdTargetService
                        .addDynamicTextAdTargets(clientId, operatorUid, singletonList(dynamicTextAdTarget));

        assertThat(result).is(matchedBy(isSuccessful(true)));
    }

    @Test
    public void addDynamicTextAdTargets_whenTwoItems() {
        List<DynamicTextAdTarget> dynamicTextAdTargets = List.of(
                defaultDynamicTextAdTargetWithRandomRules(activeAdGroup),
                defaultDynamicTextAdTargetWithRandomRules(activeAdGroup)
        );

        MassResult<Long> result =
                dynamicTextAdTargetService
                        .addDynamicTextAdTargets(clientId, operatorUid, dynamicTextAdTargets);

        assertThat(result).is(matchedBy(isSuccessful(true, true)));
    }

    @Test
    public void addDynamicTextAdTargets_whenNullTab() {
        DynamicTextAdTarget dynamicTextAdTarget = defaultDynamicTextAdTarget(activeAdGroup).withTab(null);
        Long dynamicConditionId = addDynamicTextAdTargets(dynamicTextAdTarget);

        List<DynamicTextAdTarget> actual = dynamicTextAdTargetRepository.getDynamicTextAdTargetsWithDomainType(
                shard, clientId, List.of(dynamicConditionId), false, LimitOffset.maxLimited());

        assertThat(actual)
                .element(0)
                .isEqualToIgnoringNullFields(new DynamicTextAdTarget().withTab(DynamicAdTargetTab.CONDITION));

    }

    @Test
    public void processReuseDeletedConditionTest() {
        Long dynamicConditionId = addDynamicTextAdTargets(defaultDynamicTextAdTarget(activeAdGroup));

        MassResult<Long> result = dynamicTextAdTargetService.deleteDynamicAdTargets(operatorUid, clientId,
                singletonList(dynamicConditionId));
        assumeThat(result, isSuccessful(true));

        Long newDynamicConditionId = addDynamicTextAdTargets(defaultDynamicTextAdTarget(activeAdGroup));
        assertThat(newDynamicConditionId).isEqualTo(dynamicConditionId);
    }

    @Test
    public void updateAdGroupStatusesTest() {
        testAutobudgetAlertRepository.addAutobudgetAlert(activeAdGroup.getCampaignInfo());
        addDynamicTextAdTargets(defaultDynamicTextAdTarget(activeAdGroup));

        DynamicTextAdGroup expectedAdGroup = new DynamicTextAdGroup()
                .withStatusModerate(StatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING);

        DynamicTextAdGroup actualAdGroup = (DynamicTextAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(activeAdGroup.getAdGroupId())).get(0);

        assertThat(actualAdGroup).isEqualToIgnoringNullFields(expectedAdGroup);
        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(activeAdGroup);
    }

    @Test
    public void updateDraftAdGroupStatusesTest() {
        testAutobudgetAlertRepository.addAutobudgetAlert(draftAdGroup.getCampaignInfo());
        addDynamicTextAdTargets(defaultDynamicTextAdTarget(draftAdGroup));

        DynamicTextAdGroup expectedAdGroup = new DynamicTextAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusBLGenerated(StatusBLGenerated.NO);

        DynamicTextAdGroup actualAdGroup = (DynamicTextAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(draftAdGroup.getAdGroupId())).get(0);

        assertThat(actualAdGroup).isEqualToIgnoringNullFields(expectedAdGroup);
        testAutobudgetAlertRepository.assertAutobudgetAlertNotFrozen(draftAdGroup);
    }

    @Test
    public void updateAdGroupStatusesTest_whenSuspendedDynamicAdTarget() {
        addDynamicTextAdTargets(defaultDynamicTextAdTarget(activeAdGroup).withIsSuspended(true));

        DynamicTextAdGroup expectedAdGroup = new DynamicTextAdGroup()
                .withStatusModerate(StatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusBLGenerated(StatusBLGenerated.NO);

        DynamicTextAdGroup actualAdGroup = (DynamicTextAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(activeAdGroup.getAdGroupId())).get(0);

        assertThat(actualAdGroup).isEqualToIgnoringNullFields(expectedAdGroup);
    }

    @Test
    public void addDynamicTextAdTargets_failure_whenDynamicFeedAdGroup() {
        AdGroupInfo dynamicFeedAdGroup = steps.adGroupSteps()
                .createActiveDynamicFeedAdGroup(activeAdGroup.getClientInfo());

        DynamicTextAdTarget dynamicTextAdTarget = defaultDynamicTextAdTarget(dynamicFeedAdGroup);

        MassResult<Long> result = dynamicTextAdTargetService.addDynamicTextAdTargets(clientId, operatorUid,
                singletonList(dynamicTextAdTarget));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(index(0), field(DynamicAdTarget.AD_GROUP_ID)),
                                notAcceptableAdGroupType()))));
    }

    private Long addDynamicTextAdTargets(DynamicTextAdTarget dynamicTextAdTarget) {
        MassResult<Long> result = dynamicTextAdTargetService.addDynamicTextAdTargets(clientId, operatorUid,
                singletonList(dynamicTextAdTarget));
        assumeThat(result, isSuccessful(true));
        return result.getResult().get(0).getResult();
    }
}
