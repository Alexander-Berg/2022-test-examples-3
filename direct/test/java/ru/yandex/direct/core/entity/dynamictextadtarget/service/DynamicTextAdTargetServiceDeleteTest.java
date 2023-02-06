package ru.yandex.direct.core.entity.dynamictextadtarget.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.result.MassResult;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicTextAdTargetServiceDeleteTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;

    private Long dynamicFeedAdGroupId;
    private Long dynamicTextAdGroupId;
    private Long dynamicFeedAdTargetId;
    private Long dynamicTextAdTargetId;
    private long operatorUid;
    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        AdGroupInfo dynamicFeedAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo);
        dynamicFeedAdGroupId = dynamicFeedAdGroup.getAdGroupId();

        AdGroupInfo dynamicTextAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        dynamicTextAdGroupId = dynamicTextAdGroup.getAdGroupId();

        dynamicFeedAdTargetId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(dynamicFeedAdGroup)
                .getDynamicConditionId();

        dynamicTextAdTargetId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(dynamicTextAdGroup)
                .getDynamicConditionId();
    }

    @Test
    public void deleteDynamicFeedAdTargets_success() {
        MassResult<Long> result = deleteDynamicAdTargets(List.of(dynamicFeedAdTargetId));
        assumeThat(result, isSuccessful(true));

        List<DynamicFeedAdTarget> actual = dynamicTextAdTargetRepository.getDynamicFeedAdTargets(
                shard, clientId, List.of(dynamicFeedAdTargetId));

        assertThat(actual, hasSize(1));
        assertThat("проверяем, что условие удалено - нет записи в bids_dynamic", actual.get(0).getId(), nullValue());
    }

    @Test
    public void deleteDynamicFeedAdTargets_updateAdGroupStatuses() {
        MassResult<Long> result = deleteDynamicAdTargets(List.of(dynamicFeedAdTargetId));
        assumeThat(result, isSuccessful(true));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, List.of(dynamicFeedAdGroupId)).get(0);
        assertThat(actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    public void deleteDynamicFeedAdTargets_validationError() {
        AdGroupInfo anotherClientAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup();

        Long dynamicConditionId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(anotherClientAdGroup)
                .getDynamicConditionId();

        MassResult<Long> result = deleteDynamicAdTargets(List.of(dynamicConditionId));

        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0)), objectNotFound())));
    }

    @Test
    public void deleteDynamicTextAdTargets_success() {
        MassResult<Long> result = deleteDynamicAdTargets(List.of(dynamicTextAdTargetId));
        assumeThat(result, isSuccessful(true));

        List<DynamicTextAdTarget> actual = dynamicTextAdTargetRepository.getDynamicTextAdTargetsWithDomainType(
                shard, clientId, List.of(dynamicTextAdTargetId), true, LimitOffset.maxLimited());

        assertThat(actual, hasSize(1));
        assertThat("проверяем, что условие удалено - нет записи в bids_dynamic", actual.get(0).getId(), nullValue());
    }

    @Test
    public void deleteDynamicTextAdTargets_updateAdGroupStatuses() {
        MassResult<Long> result = deleteDynamicAdTargets(List.of(dynamicTextAdTargetId));
        assumeThat(result, isSuccessful(true));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, List.of(dynamicTextAdGroupId)).get(0);
        assertThat(actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    public void deleteDynamicTextAdTargets_validationError() {
        AdGroupInfo anotherClientAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup();

        Long dynamicConditionId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(anotherClientAdGroup)
                .getDynamicConditionId();

        MassResult<Long> result = deleteDynamicAdTargets(List.of(dynamicConditionId));

        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0)), objectNotFound())));
    }

    @Test
    public void deleteDynamicAdTargets_twoAdTargets() {
        MassResult<Long> result = deleteDynamicAdTargets(List.of(dynamicFeedAdTargetId, dynamicTextAdTargetId));
        assertThat(result, isSuccessful(true, true));
    }

    private MassResult<Long> deleteDynamicAdTargets(List<Long> dynamicConditionIds) {
        return dynamicTextAdTargetService.deleteDynamicAdTargets(operatorUid, clientId, dynamicConditionIds);
    }
}
