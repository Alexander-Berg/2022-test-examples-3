package ru.yandex.direct.core.entity.retargeting.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCampaignInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestRetargetings;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyTextCampaign;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingRepositoryTest {

    private static final CompareStrategy STRATEGY = allFields()
            .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer());

    @Autowired
    private Steps steps;

    @Autowired
    private RetargetingRepository repoUnderTest;

    private RetargetingInfo adGroup1RetInfo1;
    private RetargetingInfo adGroup1RetInfo2Suspended;
    private RetargetingInfo adGroup2RetInfo1;
    private RetargetingInfo campaign2RetInfo;

    private List<Long> allRetIds;

    private int shard;

    @Before
    public void before() {
        // 2 ретаргетинга в первой активной группе в первой кампании
        adGroup1RetInfo1 = steps.retargetingSteps()
                .createDefaultRetargetingInActiveTextAdGroup();
        adGroup1RetInfo2Suspended = steps.retargetingSteps()
                .createRetargeting(
                        TestRetargetings.defaultTargetInterest().withIsSuspended(true),
                        adGroup1RetInfo1.getAdGroupInfo());
        // еще 1 во второй активной группе в первой кампании
        adGroup2RetInfo1 = steps.retargetingSteps()
                .createDefaultRetargetingInActiveTextAdGroup(adGroup1RetInfo1.getCampaignInfo());
        // еще один во второй активной кампании
        campaign2RetInfo = steps.retargetingSteps()
                .createDefaultRetargetingInActiveTextAdGroup(adGroup1RetInfo1.getClientInfo());

        shard = adGroup1RetInfo1.getShard();
        allRetIds = asList(adGroup1RetInfo1.getRetargetingId(),
                adGroup1RetInfo2Suspended.getRetargetingId(),
                adGroup2RetInfo1.getRetargetingId(),
                campaign2RetInfo.getRetargetingId());
    }

    // getRetIdWithCidWithoutLimit
    // todo test interests

    @Test
    public void getRetIdWithCidWithoutLimit_RetargetingIdsExists() {
        List<Long> expectedRetIds = asList(adGroup1RetInfo1.getRetargetingId(), campaign2RetInfo.getRetargetingId());
        RetargetingSelection selection = new RetargetingSelection().withIds(expectedRetIds);
        fetchBySelectionAndCheckRetrievedRetargetingIds(selection, expectedRetIds);
    }

    @Test
    public void getRetIdWithCidWithoutLimit_AdGroupIdsExists() {
        List<Long> expectedRetIds = asList(adGroup1RetInfo1.getRetargetingId(),
                adGroup1RetInfo2Suspended.getRetargetingId(),
                campaign2RetInfo.getRetargetingId());
        List<Long> adGroupIdsToSelect = asList(adGroup1RetInfo1.getAdGroupId(), campaign2RetInfo.getAdGroupId());
        RetargetingSelection selection = new RetargetingSelection()
                .withAdGroupIds(adGroupIdsToSelect);
        fetchBySelectionAndCheckRetrievedRetargetingIds(selection, expectedRetIds);
    }

    @Test
    public void getRetIdWithCidWithoutLimit_CampaignIdsExists() {
        List<Long> expectedRetIds = asList(adGroup1RetInfo1.getRetargetingId(),
                adGroup1RetInfo2Suspended.getRetargetingId(),
                adGroup2RetInfo1.getRetargetingId(),
                campaign2RetInfo.getRetargetingId());
        List<Long> campIdsToSelect = asList(adGroup1RetInfo1.getCampaignId(), campaign2RetInfo.getCampaignId());
        RetargetingSelection selection = new RetargetingSelection()
                .withCampaignIds(campIdsToSelect);
        fetchBySelectionAndCheckRetrievedRetargetingIds(selection, expectedRetIds);
    }

    @Test
    public void getRetIdWithCidWithoutLimit_RetConditionIdsExists() {
        RetargetingInfo retargetingOnExistingCondition =
                steps.retargetingSteps().createRetargeting(
                        TestRetargetings.defaultTargetInterest(),
                        adGroup1RetInfo1.getClientInfo(),
                        adGroup1RetInfo1.getRetConditionInfo());
        List<Long> expectedRetIds = asList(adGroup1RetInfo1.getRetargetingId(),
                campaign2RetInfo.getRetargetingId(),
                retargetingOnExistingCondition.getRetargetingId());
        List<Long> retCondIdsToSelect = asList(retargetingOnExistingCondition.getRetConditionId(),
                campaign2RetInfo.getRetConditionId());
        RetargetingSelection selection = new RetargetingSelection()
                .withRetargetingListIds(retCondIdsToSelect);
        fetchBySelectionAndCheckRetrievedRetargetingIds(selection, expectedRetIds);
    }

    @Test
    public void getRetIdWithCidWithoutLimit_AdGroupIdsWithSuspendedTrue_ReturnsOnlySuspended() {
        List<Long> expectedRetIds = singletonList(adGroup1RetInfo2Suspended.getRetargetingId());
        RetargetingSelection selection = new RetargetingSelection()
                .withAdGroupIds(singletonList(adGroup1RetInfo1.getAdGroupId()))
                .withIsSuspended(true);
        fetchBySelectionAndCheckRetrievedRetargetingIds(selection, expectedRetIds);
    }

    @Test
    public void getRetIdWithCidWithoutLimit_AdGroupIdsWithSuspendedFalse_ReturnsOnlyNotSuspended() {
        List<Long> expectedRetIds = singletonList(adGroup1RetInfo1.getRetargetingId());
        RetargetingSelection selection = new RetargetingSelection()
                .withAdGroupIds(singletonList(adGroup1RetInfo1.getAdGroupId()))
                .withIsSuspended(false);
        fetchBySelectionAndCheckRetrievedRetargetingIds(selection, expectedRetIds);
    }

    @Test
    public void getRetIdWithCidWithoutLimit_ComplexSelection() {
        List<Long> expectedRetIds = asList(adGroup1RetInfo1.getRetargetingId(), campaign2RetInfo.getRetargetingId());
        RetargetingSelection selection = new RetargetingSelection()
                .withCampaignIds(asList(adGroup1RetInfo1.getCampaignId(), campaign2RetInfo.getCampaignId()))
                .withAdGroupIds(asList(adGroup1RetInfo1.getAdGroupId(), campaign2RetInfo.getAdGroupId()))
                .withIsSuspended(false);
        fetchBySelectionAndCheckRetrievedRetargetingIds(selection, expectedRetIds);
    }

    // getRetargetingToCampaignMappingForDelete

    @Test
    public void getRetargetingToCampaignMappingForDelete_AllRetIdsExists_ReturnsAllRequested() {
        List<Long> retIdsToSelect = asList(adGroup1RetInfo1.getRetargetingId(),
                adGroup1RetInfo2Suspended.getRetargetingId(),
                campaign2RetInfo.getRetargetingId());
        Map<Long, RetargetingCampaignInfo> retCampInfos =
                repoUnderTest.getRetargetingToCampaignMappingForDelete(shard, retIdsToSelect);
        Collection<Long> selectedRetIds = retCampInfos.keySet();
        assertThat("метод должен вернуть все запрашиваемые объекты",
                selectedRetIds, contains(retIdsToSelect.toArray()));
    }

    @Test
    public void getRetargetingToCampaignMappingForDelete_ReturnsValidData() {
        List<Long> retIdsToSelect = singletonList(adGroup1RetInfo1.getRetargetingId());
        RetargetingCampaignInfo retCampInfo =
                repoUnderTest.getRetargetingToCampaignMappingForDelete(shard, retIdsToSelect)
                        .get((adGroup1RetInfo1.getRetargetingId()));

        RetargetingCampaignInfo expectedRetCampInfo = new RetargetingCampaignInfo();
        expectedRetCampInfo.setRetargetingId(adGroup1RetInfo1.getRetargetingId());
        expectedRetCampInfo.setRetargetingConditionId(adGroup1RetInfo1.getRetConditionId());
        expectedRetCampInfo.setAdGroupId(adGroup1RetInfo1.getAdGroupId());
        expectedRetCampInfo.setCampaignId(adGroup1RetInfo1.getCampaignId());
        expectedRetCampInfo.setCampaignIsArchived(false);
        expectedRetCampInfo.setCampaignType(adGroup1RetInfo1.getCampaignInfo().getCampaign().getType());

        assertThat(retCampInfo, beanDiffer(expectedRetCampInfo));
    }

    // add

    @Test
    public void add_OneRetargeting_ReturnsId() {
        Retargeting retargeting = retargetingWithNoDefaultValues(
                adGroup1RetInfo1.getCampaignId(),
                adGroup1RetInfo1.getAdGroupId(),
                adGroup1RetInfo1.getRetConditionId());
        List<Long> ids = repoUnderTest.add(shard, singletonList(retargeting));
        assertThat("метод add вернул целое положительное число", ids, contains(greaterThan(0L)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void add_TwoRetargetings_ReturnsIds() {
        Retargeting retargeting1 = retargetingWithNoDefaultValues(
                adGroup1RetInfo1.getCampaignId(),
                adGroup1RetInfo1.getAdGroupId(),
                adGroup1RetInfo1.getRetConditionId());
        Retargeting retargeting2 = retargetingWithNoDefaultValues(
                adGroup1RetInfo1.getCampaignId(),
                adGroup1RetInfo1.getAdGroupId(),
                adGroup1RetInfo1.getRetConditionId());
        List<Long> ids = repoUnderTest.add(shard, asList(retargeting1, retargeting2));
        assertThat("метод add вернул целые положительные числа", ids, contains(greaterThan(0L), greaterThan(0L)));
    }

    @Test
    public void add_OneRetargeting_DataIsSavedCorrectly() {
        Retargeting retargeting = retargetingWithNoDefaultValues(
                adGroup1RetInfo1.getCampaignId(),
                adGroup1RetInfo1.getAdGroupId(),
                adGroup1RetInfo1.getRetConditionId());
        List<Long> ids = repoUnderTest.add(shard, singletonList(retargeting));
        assumeThat("метод add вернул целое положительное число", ids, contains(greaterThan(0L)));

        Retargeting savedRetargeting = repoUnderTest
                .getRetargetingsByIds(shard, ids, maxLimited()).get(0);

        assertThat("сохраненные данные ретаргетинга соответствуют ожидаемым",
                savedRetargeting, beanDiffer(retargeting).useCompareStrategy(STRATEGY));
        assertThat("сохраненная ставка соответствует ожидаемой",
                savedRetargeting.getPriceContext().doubleValue(),
                equalTo(retargeting.getPriceContext().doubleValue()));
    }

    // getRetargetingsByAdGroups

    @Test
    public void getRetargetingsByAdGroups_ReturnsRequestedRetargetings() {
        List<Long> expectedRetIds = asList(adGroup1RetInfo1.getRetargetingId(),
                adGroup1RetInfo2Suspended.getRetargetingId(),
                campaign2RetInfo.getRetargetingId());
        List<Long> adGroupIdsToSelect = asList(adGroup1RetInfo1.getAdGroupId(), campaign2RetInfo.getAdGroupId());

        List<Retargeting> retargetings = repoUnderTest.getRetargetingsByAdGroups(shard, adGroupIdsToSelect);
        List<Long> fetchedRetIds = mapList(retargetings, Retargeting::getId);
        assertThat(fetchedRetIds, contains(expectedRetIds.toArray()));
    }

    // getRetargetingsByCampaigns

    @Test
    public void getRetargetingsByCampaigns_ReturnsRequestedRetargetings() {
        List<Long> expectedRetIds = allRetIds;
        List<Long> campIdsToSelect = asList(adGroup1RetInfo1.getCampaignId(), campaign2RetInfo.getCampaignId());

        List<Retargeting> retargetings = repoUnderTest.getRetargetingsByCampaigns(shard, campIdsToSelect);
        List<Long> fetchedRetIds = mapList(retargetings, Retargeting::getId);
        assertThat(fetchedRetIds, contains(expectedRetIds.toArray()));
    }


    // setSuspend

    @Test
    public void setSuspend() {
        RetargetingInfo adGroup1RetInfo3Suspended = steps.retargetingSteps()
                .createRetargeting(
                        TestRetargetings.defaultTargetInterest().withIsSuspended(true),
                        adGroup1RetInfo1.getAdGroupInfo());
        allRetIds = new ArrayList<>(allRetIds);
        allRetIds.add(adGroup1RetInfo3Suspended.getRetargetingId());

        List<Retargeting> retargetings = new ArrayList<>();
        retargetings.add(new Retargeting().withId(adGroup1RetInfo2Suspended.getRetargetingId()));


        List<AppliedChanges<Retargeting>> changes = StreamEx.of(retargetings)
                .map(retargeting -> new ModelChanges<>(retargeting.getId(), Retargeting.class)
                        .process(false, Retargeting.IS_SUSPENDED)
                        .applyTo(retargeting)
                ).toList();
        repoUnderTest.setSuspended(shard, changes);
        List<Retargeting> allRetargetings = repoUnderTest.getRetargetingsByIds(shard, allRetIds, maxLimited());
        List<Boolean> suspendedValues = mapList(allRetargetings, Retargeting::getIsSuspended);
        assertThat("значения полей suspended у остановленных ретаргетингов соответствуют ожидаемым",
                suspendedValues, contains(false, false, false, false, true));
    }

    // setBids

    @Test
    public void setBids_UpdatesRetargeting() {
        Retargeting retargeting = defaultRetargeting().withStatusBsSynced(StatusBsSynced.YES);
        steps.retargetingSteps().createRetargeting(retargeting, adGroup1RetInfo1.getAdGroupInfo());

        AppliedChanges<Retargeting> appliedChanges = changeRetargeting(retargeting, BigDecimal.valueOf(33.5d), 1);
        repoUnderTest.setBids(shard, singletonList(appliedChanges));

        Retargeting changedRetargeting = repoUnderTest
                .getRetargetingsByIds(shard, singletonList(retargeting.getId()), maxLimited()).get(0);

        assertThat("изменения в ретаргетинге соответствуют ожидаемым",
                changedRetargeting, beanDiffer(retargeting).useCompareStrategy(STRATEGY));
        assertThat("измененная ставка соответствует ожидаемой",
                changedRetargeting.getPriceContext().doubleValue(),
                equalTo(retargeting.getPriceContext().doubleValue()));
    }

    // getAdGroupIds

    @Test
    public void getAdGroupIds_WorksFine() {
        List<Long> retIds = asList(adGroup1RetInfo1.getRetargetingId(), campaign2RetInfo.getRetargetingId());
        Map<Long, Long> adGroupIdByRetargetingIds = repoUnderTest.getAdGroupIdByRetargetingIds(shard, retIds);

        assertThat(adGroupIdByRetargetingIds.get(adGroup1RetInfo1.getRetargetingId()),
                is(adGroup1RetInfo1.getAdGroupId()));
        assertThat(adGroupIdByRetargetingIds.get(campaign2RetInfo.getRetargetingId()),
                is(campaign2RetInfo.getAdGroupId()));
    }

    // getRetIdWithCid

    @Test
    public void getRetIdWithCid_WorksFine() {
        List<Long> retIds = asList(adGroup1RetInfo1.getRetargetingId(), campaign2RetInfo.getRetargetingId());
        Map<Long, Long> adGroupIds = repoUnderTest.getRetIdWithCid(shard, retIds);
        assertThat(adGroupIds.get(retIds.get(0)), is(adGroup1RetInfo1.getCampaignId()));
        assertThat(adGroupIds.get(retIds.get(1)), is(campaign2RetInfo.getCampaignId()));
    }

    // getRetargetingsByIds

    @Test
    public void getRetargetingsByIds_ReturnsRequestedRetargetings() {
        List<Long> retIds = asList(adGroup1RetInfo1.getRetargetingId(), campaign2RetInfo.getRetargetingId());
        List<Retargeting> retargetings = repoUnderTest.getRetargetingsByIds(shard, retIds, maxLimited());
        assertThat(mapList(retargetings, Retargeting::getId), contains(retIds.toArray()));
    }

    @Test
    public void getRetargetingsByIds_ReturnsCorrectRetargetingData() {
        Retargeting expectedRetargeting = adGroup1RetInfo1.getRetargeting();
        Retargeting retargeting = repoUnderTest
                .getRetargetingsByIds(shard, singletonList(expectedRetargeting.getId()), maxLimited()).get(0);
        assertThat(retargeting, beanDiffer(expectedRetargeting).useCompareStrategy(STRATEGY));
        assertThat(retargeting.getPriceContext().doubleValue(),
                equalTo(expectedRetargeting.getPriceContext().doubleValue()));
    }

    @Test
    public void getRetargetingsByIds_OffsetWorksFine() {
        List<Long> retIds = asList(adGroup1RetInfo1.getRetargetingId(), campaign2RetInfo.getRetargetingId());
        List<Retargeting> retargetings = repoUnderTest.getRetargetingsByIds(shard, retIds, limited(10, 1));
        assertThat(mapList(retargetings, Retargeting::getId), contains(retIds.get(1)));
    }

    @Test
    public void getRetargetingsByIds_LimitWorksFine() {
        List<Long> retIds = asList(adGroup1RetInfo1.getRetargetingId(), campaign2RetInfo.getRetargetingId());
        List<Retargeting> retargetings = repoUnderTest.getRetargetingsByIds(shard, retIds, limited(1));
        assertThat(mapList(retargetings, Retargeting::getId), contains(retIds.get(0)));
    }

    @Test
    public void getRetargetingsByIds_OneRetargetingIdExistsAndAnotherInEmptyCampaign() {
        RetargetingInfo emptyCampaignRetInfo = steps.retargetingSteps()
                .createDefaultRetargeting(new CampaignInfo()
                        .withClientInfo(adGroup1RetInfo1.getClientInfo())
                        .withCampaign(emptyTextCampaign(null, null)));
        List<Long> idsToSelect = asList(adGroup1RetInfo1.getRetargetingId(), emptyCampaignRetInfo.getRetargetingId());
        List<Long> expectedRetIds = singletonList(adGroup1RetInfo1.getRetargetingId());

        List<Retargeting> retargetings = repoUnderTest.getRetargetingsByIds(shard, idsToSelect, maxLimited());
        assertThat(mapList(retargetings, Retargeting::getId), contains(expectedRetIds.get(0)));
    }

    // delete

    @Test
    public void delete_DeletesAllSpecifiedRetargetings() {
        List<Long> retIdsToDelete = asList(adGroup1RetInfo1.getRetargetingId(),
                campaign2RetInfo.getRetargetingId());
        List<Long> expectedLeftRetIds = asList(adGroup1RetInfo2Suspended.getRetargetingId(),
                adGroup2RetInfo1.getRetargetingId());

        repoUnderTest.delete(shard, retIdsToDelete);

        List<Retargeting> leftRetargetings = repoUnderTest.getRetargetingsByIds(shard, allRetIds, maxLimited());
        List<Long> leftRetIds = mapList(leftRetargetings, Retargeting::getId);
        assertThat(leftRetIds, contains(expectedLeftRetIds.toArray()));
    }


    // update

    @Test
    public void updateRetargetings_UpdateChangeableFields_FieldsUpdated() {
        RetConditionInfo newRetConditionInfo = steps.retConditionSteps().createDefaultRetCondition();

        RetargetingInfo retargetingInfo = steps.retargetingSteps().createDefaultRetargeting();
        Retargeting retargeting = retargetingInfo.getRetargeting();
        int shard = retargetingInfo.getShard();

        AppliedChanges<Retargeting> appliedChanges = new ModelChanges<>(retargeting.getId(), Retargeting.class)
                .process(newRetConditionInfo.getRetConditionId(), Retargeting.RETARGETING_CONDITION_ID)
                .process(5, Retargeting.AUTOBUDGET_PRIORITY)
                .process(BigDecimal.valueOf(999.99), Retargeting.PRICE_CONTEXT)
                .process(true, Retargeting.IS_SUSPENDED)
                .process(LocalDateTime.now().withNano(0), Retargeting.LAST_CHANGE_TIME)
                .process(StatusBsSynced.SENDING, Retargeting.STATUS_BS_SYNCED)
                .applyTo(retargeting);

        repoUnderTest.updateRetargetings(shard, singletonList(appliedChanges));

        Retargeting retargetingAfterUpdate =
                repoUnderTest.getRetargetingsByIds(shard, singletonList(retargeting.getId()), maxLimited())
                        .iterator().next();
        assertThat(retargetingAfterUpdate, beanDiffer(retargeting).useCompareStrategy(STRATEGY));
    }

    private void fetchBySelectionAndCheckRetrievedRetargetingIds(
            RetargetingSelection selection, List<Long> expectedRetargteingIds) {
        Set<Long> retIds = repoUnderTest.getRetIdWithCidWithoutLimit(shard, selection, new ArrayList<>()).keySet();
        assertThat("список полученных ретаргетингов соответствует ожидаемому",
                new ArrayList<>(retIds),
                containsInAnyOrder(expectedRetargteingIds.toArray()));
    }

    private static AppliedChanges<Retargeting> changeRetargeting(
            Retargeting retargeting, BigDecimal priceContext, Integer autobudgetPriority) {
        ModelChanges<Retargeting> modelChanges = new ModelChanges<>(retargeting.getId(), Retargeting.class);
        if (priceContext != null) {
            modelChanges.process(priceContext, Retargeting.PRICE_CONTEXT);
        }
        if (autobudgetPriority != null) {
            modelChanges.process(autobudgetPriority, Retargeting.AUTOBUDGET_PRIORITY);
        }
        modelChanges.process(StatusBsSynced.NO, Retargeting.STATUS_BS_SYNCED);
        modelChanges.process(LocalDateTime.now().withNano(0), Retargeting.LAST_CHANGE_TIME);
        return modelChanges.applyTo(retargeting);
    }

    private static Retargeting retargetingWithNoDefaultValues(Long campaignId,
                                                              Long groupId, Long retConditionId) {
        return new Retargeting()
                .withCampaignId(campaignId)
                .withAdGroupId(groupId)
                .withRetargetingConditionId(retConditionId)
                .withLastChangeTime(LocalDateTime.now().withNano(0))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withAutobudgetPriority(5)
                .withPriceContext(BigDecimal.valueOf(100))
                .withIsSuspended(false);
    }
}
