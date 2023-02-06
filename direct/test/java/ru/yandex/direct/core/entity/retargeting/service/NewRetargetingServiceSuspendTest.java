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
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.warningDuplicatedRetargetingId;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingServiceSuspendTest extends BaseTestWithCtreatedRetargetings {

    @Test
    public void suspendRetargetings_OneItem_ResultIsFullySuccessful() {
        suspendItemsAndCheckResult(singletonList(retargetingId1), singletonList(true));
    }

    @Test
    public void suspendRetargetings_OneItem_SuspendsOneItem() {
        suspendItemsAndCheckSuspendedFlag(singletonList(retargetingId1), singletonList(true), asList(true, false));
    }

    @Test
    public void suspendRetargetings_BothItemsExists_ResultIsFullySuccessful() {
        suspendItemsAndCheckResult(retargetingIds, asList(true, true));
    }

    @Test
    public void suspendRetargetings_BothItemsExists_SuspendsBothItems() {
        suspendItemsAndCheckSuspendedFlag(retargetingIds, asList(true, true), asList(true, true));
    }

    @Test
    public void suspendRetargetings_OneItemExists_ResultIsPartlySuccessful() {
        suspendItemsAndCheckResult(asList(1234L, retargetingId2), asList(false, true));
    }

    @Test
    public void suspendRetargetings_OneItemExists_SuspendsOneItem() {
        suspendItemsAndCheckSuspendedFlag(asList(1234L, retargetingId2), asList(false, true), asList(false, true));
    }

    @Test
    public void suspendRetargetings_NoItemsExists_ElementsResultsIsBroken() {
        suspendItemsAndCheckResult(asList(1234L, 5678L), asList(false, false));
    }

    @Test
    public void suspendRetargetings_NoItemsExists_SuspendsNothing() {
        suspendItemsAndCheckSuspendedFlag(asList(1234L, 5678L), asList(false, false), asList(false, false));
    }

    @Test
    public void suspendRetargetings_EmptyList_ResultIsSuccessful() {
        MassResult<Long> result = serviceUnderTest.suspendRetargetings(new ArrayList<>(), clientId, uid);
        assertThat("результат операции положительный", result.isSuccessful(), is(true));
    }

    @Test
    public void suspendRetargetings_DropsAdGroupStatusBsSynced() {
        Assert.state(retargetingInfo1.getAdGroupInfo().getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "невозможно провести тест: статус группы statusBsSynced сброшен");

        MassResult<Long> result = serviceUnderTest
                .suspendRetargetings(singletonList(retargetingId1), clientId, uid);
        assumeThat("результат операции положительный", result.isSuccessful(), is(true));
        assumeThat("поэлементные результаты соответствуют ожидаемым",
                mapList(result.getResult(), Result::isSuccessful), contains(true));

        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(retargetingInfo1.getAdGroupId())).get(0);
        assertThat("статус statusBsSynced группы сброшен", adGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void suspendRetargetings_ItemExistsAndDuplicates_ResultIsSuccessfulWithWarnings() {
        MassResult<Long> result = serviceUnderTest
                .suspendRetargetings(asList(retargetingId1, retargetingId2, retargetingId1), clientId, uid);
        assumeThat("результат операции положительный", result.isSuccessful(), is(true));
        assumeThat("поэлементные результаты соответствуют ожидаемым",
                mapList(result.getResult(), Result::isSuccessful), contains(asList(true, true, true).toArray()));

        @SuppressWarnings("unchecked")
        ValidationResult<List<Long>, Defect> vr =
                (ValidationResult<List<Long>, Defect>) result.getValidationResult();

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), warningDuplicatedRetargetingId())));
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(2)), warningDuplicatedRetargetingId())));
    }

    private void suspendItemsAndCheckResult(List<Long> retIds, List<Boolean> elementsResults) {
        MassResult<Long> result = serviceUnderTest.suspendRetargetings(retIds, clientId, uid);
        assertThat(result, isSuccessful(elementsResults));
    }

    private void suspendItemsAndCheckSuspendedFlag(List<Long> retIds, List<Boolean> elementsResults,
                                                   List<Boolean> expectedSuspendedFlags) {
        MassResult<Long> result = serviceUnderTest.suspendRetargetings(retIds, clientId, uid);
        assumeThat(result, isSuccessful(elementsResults));

        List<Retargeting> allRetargetings =
                retargetingRepository.getRetargetingsByIds(shard, retargetingIds, maxLimited());
        List<Boolean> suspendedFlags = mapList(allRetargetings, Retargeting::getIsSuspended);

        assertThat("все указанные ретаргетинги остановлены",
                suspendedFlags, contains(expectedSuspendedFlags.toArray()));
    }
}
