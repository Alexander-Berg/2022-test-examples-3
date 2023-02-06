package ru.yandex.direct.core.entity.retargeting.service;

import java.util.ArrayList;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingServiceResumeTest extends BaseTestWithCtreatedRetargetings {

    @Before
    public void before() {
        super.before();
        List<Retargeting> retargetingsWithSuspended =
                retargetingRepository.getRetargetingsByIds(shard, retargetingIds, maxLimited());
        List<AppliedChanges<Retargeting>> changes = StreamEx.of(retargetingsWithSuspended)
                .map(retargetingWithSuspended -> new ModelChanges<>(retargetingWithSuspended.getId(), Retargeting.class)
                        .process(true, Retargeting.IS_SUSPENDED)
                        .applyTo(retargetingWithSuspended)
                ).toList();
        retargetingRepository.setSuspended(shard, changes);
        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByIds(shard, retargetingIds, maxLimited());
        List<Boolean> suspendedFlags = mapList(retargetings, Retargeting::getIsSuspended);
        Assert.state(!suspendedFlags.contains(false), "перед началом теста все ретаргетинги должны быть остановлены");
    }

    @Test
    public void resumeRetargetings_OneItem_ResultIsFullySuccessful() {
        resumeItemsAndCheckResult(singletonList(retargetingId1), singletonList(true));
    }

    @Test
    public void resumeRetargetings_OneItem_ResumesOneItem() {
        resumeItemsAndCheckSuspendedFlag(singletonList(retargetingId1), singletonList(true), asList(false, true));
    }

    @Test
    public void resumeRetargetings_BothItemsExists_ResultIsFullySuccessful() {
        resumeItemsAndCheckResult(retargetingIds, asList(true, true));
    }

    @Test
    public void resumeRetargetings_BothItemsExists_ResumesBothItems() {
        resumeItemsAndCheckSuspendedFlag(retargetingIds, asList(true, true), asList(false, false));
    }

    @Test
    public void resumeRetargetings_OneItemExists_ResultIsPartlySuccessful() {
        resumeItemsAndCheckResult(asList(1234L, retargetingId2), asList(false, true));
    }

    @Test
    public void resumeRetargetings_OneItemExists_ResumesOneItem() {
        resumeItemsAndCheckSuspendedFlag(asList(1234L, retargetingId2), asList(false, true), asList(true, false));
    }

    @Test
    public void resumeRetargetings_NoItemsExists_ElementsResultsIsBroken() {
        resumeItemsAndCheckResult(asList(1234L, 5678L), asList(false, false));
    }

    @Test
    public void resumeRetargetings_NoItemsExists_ResumesNothing() {
        resumeItemsAndCheckSuspendedFlag(asList(1234L, 5678L), asList(false, false), asList(true, true));
    }

    @Test
    public void resumeRetargetings_DropsAdGroupStatusBsSynced() {
        Assert.state(retargetingInfo1.getAdGroupInfo().getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "невозможно провести тест: статус группы statusBsSynced сброшен");

        MassResult<Long> result = serviceUnderTest
                .resumeRetargetings(singletonList(retargetingId1), clientId, uid);
        assumeThat("результат операции положительный", result.isSuccessful(), is(true));
        assumeThat("поэлементные результаты соответствуют ожидаемым",
                mapList(result.getResult(), Result::isSuccessful), contains(true));

        AdGroup adGroup = adGroupRepository
                .getAdGroups(shard, singletonList(retargetingInfo1.getAdGroupId())).get(0);
        assertThat("статус statusBsSynced группы сброшен",
                adGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void resumeRetargetings_EmptyList_ResultIsSuccessful() {
        MassResult<Long> result = serviceUnderTest.resumeRetargetings(new ArrayList<>(), clientId, uid);
        assertThat(result, isSuccessful());
    }

    private void resumeItemsAndCheckResult(List<Long> retIds, List<Boolean> elementsResults) {
        MassResult<Long> result = serviceUnderTest.resumeRetargetings(retIds, clientId, uid);
        assertThat(result, isSuccessful(elementsResults));
    }

    private void resumeItemsAndCheckSuspendedFlag(List<Long> retIds, List<Boolean> elementsResults,
                                                  List<Boolean> expectedSuspendedFlags) {
        MassResult<Long> result = serviceUnderTest.resumeRetargetings(retIds, clientId, uid);
        assumeThat(result, isSuccessful(elementsResults));

        List<Retargeting> allRetargetings =
                retargetingRepository.getRetargetingsByIds(shard, retargetingIds, maxLimited());
        List<Boolean> suspendedFlags = mapList(allRetargetings, Retargeting::getIsSuspended);

        assertThat("все указанные ретаргетинги запущены",
                suspendedFlags, contains(expectedSuspendedFlags.toArray()));
    }
}
