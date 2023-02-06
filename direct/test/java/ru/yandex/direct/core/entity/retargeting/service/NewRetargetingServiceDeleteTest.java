package ru.yandex.direct.core.entity.retargeting.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingServiceDeleteTest extends BaseTestWithCtreatedRetargetings {

    @Test
    public void deleteRetargetings_OneItem_ResultIsFullySuccessful() {
        deleteItemsAndCheckResult(singletonList(retargetingId1), singletonList(true));
    }

    @Test
    public void deleteRetargetings_OneItem_DeletesOneItem() {
        deleteItemsAndCheckDeletion(singletonList(retargetingId1), singletonList(true), singletonList(retargetingId2));
    }

    @Test
    public void deleteRetargetings_BothItemsExists_ResultIsFullySuccessful() {
        deleteItemsAndCheckResult(retargetingIds, asList(true, true));
    }

    @Test
    public void deleteRetargetings_BothItemsExists_DeletesBothItems() {
        deleteItemsAndCheckDeletion(retargetingIds, asList(true, true), new ArrayList<>());
    }

    @Test
    public void deleteRetargetings_OneItemExists_ResultIsPartlySuccessful() {
        deleteItemsAndCheckResult(asList(1234L, retargetingId2), asList(false, true));
    }

    @Test
    public void deleteRetargetings_OneItemExists_DeletesOneItem() {
        deleteItemsAndCheckDeletion(asList(1234L, retargetingId2), asList(false, true), singletonList(retargetingId1));
    }

    @Test
    public void deleteRetargetings_NoItemsExists_ElementsResultsIsBroken() {
        deleteItemsAndCheckResult(asList(1234L, 5678L), asList(false, false));
    }

    @Test
    public void deleteRetargetings_NoItemsExists_DeletesNothing() {
        deleteItemsAndCheckDeletion(asList(1234L, 5678L), asList(false, false), retargetingIds);
    }

    @Test
    public void deleteRetargetings_EmptyList_ResultIsSuccessful() {
        MassResult<Long> result = serviceUnderTest.deleteRetargetings(new ArrayList<>(), clientId, uid);
        assertThat("результат операции положительный", result.isSuccessful(), is(true));
    }

    @Test
    public void deleteRetargetings_DropsAdGroupStatusBsSynced() {
        Assert.state(retargetingInfo1.getAdGroupInfo().getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "невозможно провести тест: статус группы statusBsSynced сброшен");

        MassResult<Long> result = serviceUnderTest
                .deleteRetargetings(singletonList(retargetingId1), clientId, uid);
        assumeThat(result, isSuccessful(true));

        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(retargetingInfo1.getAdGroupId())).get(0);
        assertThat("статус statusBsSynced группы сброшен", adGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    private void deleteItemsAndCheckResult(List<Long> retIds, List<Boolean> elementsResults) {
        MassResult<Long> result = serviceUnderTest.deleteRetargetings(retIds, clientId, uid);
        assertThat(result, isSuccessful(elementsResults));
    }

    private void deleteItemsAndCheckDeletion(List<Long> retIds, List<Boolean> elementsResults,
                                             List<Long> expectedLeftRetIds) {
        MassResult<Long> result = serviceUnderTest.deleteRetargetings(retIds, clientId, uid);
        assumeThat(result, isSuccessful(elementsResults));

        List<Retargeting> existingRetargetings =
                retargetingRepository.getRetargetingsByIds(shard, retargetingIds, maxLimited());
        List<Long> actualRetargetingIds = mapList(existingRetargetings, Retargeting::getId);

        assertThat("остались только не удаленные ретаргетинги", actualRetargetingIds,
                containsInAnyOrder(expectedLeftRetIds.toArray()));
    }
}
