package ru.yandex.direct.core.entity.retargeting.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.InterestLink;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddRetargetingsOperationTest {
    private static final TargetingCategory TARGETING_CATEGORY =
            new TargetingCategory(54L, null, "", "", BigInteger.valueOf(10000L), true);

    @Autowired
    public AdGroupSteps adGroupSteps;

    @Autowired
    public RetConditionSteps retConditionSteps;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private RetargetingService serviceUnderTest;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private TestTargetingCategoriesRepository testTargetingCategoriesRepository;

    private AdGroupInfo adGroupInfo;
    private AdGroupInfo mobileContentAdGroupInfo;
    private RetConditionInfo retConditionInfo1;
    private RetConditionInfo retConditionInfo2;
    private int shard;
    private long operatorUid;
    private ClientId clientId;
    private long clientUid;

    @Before
    public void before() {
        adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        mobileContentAdGroupInfo = adGroupSteps.createActiveMobileContentAdGroup(adGroupInfo.getClientInfo());
        retConditionInfo1 = retConditionSteps.createDefaultRetCondition(adGroupInfo.getClientInfo());
        retConditionInfo2 = retConditionSteps.createDefaultRetCondition(adGroupInfo.getClientInfo());
        shard = adGroupInfo.getShard();
        operatorUid = adGroupInfo.getUid();
        clientId = adGroupInfo.getClientId();
        clientUid = rbacService.getChiefByClientId(clientId);
        testTargetingCategoriesRepository.addTargetingCategory(TARGETING_CATEGORY);
    }

    @Test
    public void addRetargeting_OneItemIsValid_ReturnsFullySuccessfulResult() {
        addPartiallyAndCheckResult(singletonList(targetInterest1()), true);
    }

    @Test
    public void addRetargeting_BothItemsAreValid_ReturnsFullySuccessfulResult() {
        addPartiallyAndCheckResult(asList(targetInterest1(), targetInterest2()), true, true);
    }

    @Test
    public void addRetargeting_OneOfItemsIsValid_ReturnsPartlySuccessfulResult() {
        addPartiallyAndCheckResult(
                asList(targetInterest1(),
                        targetInterest2().withPriceContext(BigDecimal.valueOf(-1))),
                true, false);
    }

    @Test
    public void addRetargeting_NoItems_ReturnsSuccessfulResult() {
        MassResult<Long> result = addPartially(new ArrayList<>());
        assertThat(result, isSuccessful());
    }

    @Test
    public void addRetargeting_OneItemIsValid_RetargetingIsSavedAndStatusBsSyncedIsDropped() {
        List<TargetInterest> targetInterests = singletonList(targetInterest1().withStatusBsSynced(StatusBsSynced.YES));
        MassResult<Long> result = addPartially(targetInterests);
        assumeThat(result, isSuccessful(true));

        Long savedRetargetingId = result.getResult().get(0).getResult();
        Retargeting savedRetargeting = retargetingRepository
                .getRetargetingsByIds(shard, singletonList(savedRetargetingId), maxLimited()).get(0);

        assertThat("статус синхронизации у созданного ретаргетинга сброшен",
                savedRetargeting.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void addRetargeting_BothItemsAreValid_RetargetingsIsSaved() {
        List<TargetInterest> targetInterests = asList(targetInterest1(), targetInterest2());
        MassResult<Long> result = addPartially(targetInterests);
        assumeThat(result, isSuccessful(true, true));

        List<Long> savedRetargetingIds = mapList(result.getResult(), Result::getResult);
        List<Retargeting> savedRetargetings = retargetingRepository
                .getRetargetingsByIds(shard, savedRetargetingIds, maxLimited());

        assertThat("по возвращенным id найдены ретаргетинги", savedRetargetings, hasSize(2));
    }

    @Test
    public void addRetargeting_BothItemsAreValid_DropsAdGroupsStatusBsSynced() {
        addAndCheckAdGroupStatusBsSynced(targetInterest1(), targetInterest2(), true, true);
    }

    @Test
    public void addRetargeting_OneOfItemsIsInvalid_DropsOnlyChangedAdGroupsStatusBsSynced() {
        addAndCheckAdGroupStatusBsSynced(targetInterest1(),
                targetInterest2().withAutobudgetPriority(0),
                true, false);
    }

    @Test
    public void addRetargeting_ByInterest_ReturnsSuccessfulResult() {
        List<TargetInterest> targetInterests =
                singletonList(targetInterestOnInterestId(TARGETING_CATEGORY.getTargetingCategoryId()));
        MassResult<Long> result = addPartially(targetInterests);
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void addRetargeting_ByInterest_RetargetingIsSavedProperly() {
        List<TargetInterest> targetInterests =
                singletonList(targetInterestOnInterestId(TARGETING_CATEGORY.getTargetingCategoryId()));
        MassResult<Long> result = addPartially(targetInterests);
        assumeThat(result, isFullySuccessful());

        List<Long> savedRetargetingIds = mapList(result.getResult(), Result::getResult);
        assumeThat(savedRetargetingIds, hasSize(1));

        List<Retargeting> savedRetargetings = retargetingRepository
                .getRetargetingsByIds(shard, savedRetargetingIds, maxLimited());
        assumeThat(savedRetargetings, hasSize(1));
        List<Long> retCondIds = mapList(savedRetargetings, Retargeting::getRetargetingConditionId);
        List<InterestLink> interestLinks =
                retargetingConditionRepository.getInterestByIds(shard, clientId, retCondIds);
        assumeThat(interestLinks, hasSize(1));

        InterestLink actualInterestLink = interestLinks.get(0);
        assertThat("выдаётся неправильное значение идентификатора интереса",
                actualInterestLink.getInterestId(), is(TARGETING_CATEGORY.getTargetingCategoryId()));
        assertThat("в базе записано неправильное значение цели",
                actualInterestLink.getGoalId(),
                is(TARGETING_CATEGORY.getImportId().longValue()));
        assertThat("в базе записано неправильное значение типа retargetingCondition",
                actualInterestLink.asRetargetingCondition().getType(),
                is(ConditionType.metrika_goals));
        assertThat("в базе должна быть записана пустая строка в retargetingCondition.name",
                actualInterestLink.asRetargetingCondition().getName(),
                is(""));
        assertThat("в базе должна быть записана пустая строка в retargetingCondition.description",
                actualInterestLink.asRetargetingCondition().getDescription(),
                is(""));
    }

    private void addPartiallyAndCheckResult(List<TargetInterest> retargetings, Boolean... expectedResults) {
        MassResult<Long> result = addPartially(retargetings);
        assertThat(result, isSuccessful(expectedResults));
    }

    private MassResult<Long> addPartially(List<TargetInterest> targetInterests) {
        return createOperation(Applicability.PARTIAL, targetInterests).prepareAndApply();
    }

    private AddRetargetingsOperation createOperation(Applicability applicability, List<TargetInterest> retargetings) {
        return serviceUnderTest
                .createAddOperation(applicability, retargetings, operatorUid, clientId, clientUid);
    }

    private void addAndCheckAdGroupStatusBsSynced(TargetInterest retargeting1,
                                                  TargetInterest retargeting2, Boolean... elementsResults) {
        AdGroupInfo adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(null), adGroupInfo.getCampaignInfo());
        List<Long> adGroupIds = asList(adGroupInfo.getAdGroupId(), adGroupInfo2.getAdGroupId());

        Assert.state(adGroupInfo.getAdGroup().getStatusBsSynced() == StatusBsSynced.YES &&
                        adGroupInfo2.getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "невозможно провести тест: статус группы statusBsSynced сброшен");

        List<TargetInterest> targetInterests = asList(
                retargeting1,
                retargeting2.withAdGroupId(adGroupInfo2.getAdGroupId()));

        MassResult<Long> result = addPartially(targetInterests);
        assumeThat(result, isSuccessful(elementsResults));

        List<StatusBsSynced> adGroupsStatusBsSynced = getAdGroupsStatusBsSynced(adGroupIds);
        List<StatusBsSynced> expectedAdGroupStatusBsSynced = mapList(asList(elementsResults),
                er -> er ? StatusBsSynced.NO : StatusBsSynced.YES);
        assertThat("статусы групп statusBsSynced сброшены только у затронутых групп",
                adGroupsStatusBsSynced, contains(expectedAdGroupStatusBsSynced.toArray()));
    }

    private List<StatusBsSynced> getAdGroupsStatusBsSynced(List<Long> adGroupIds) {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, adGroupIds);
        return mapList(adGroups, AdGroup::getStatusBsSynced);
    }

    private TargetInterest targetInterest1() {
        return defaultTargetInterest(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), retConditionInfo1.getRetConditionId());
    }

    private TargetInterest targetInterest2() {
        return defaultTargetInterest(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), retConditionInfo2.getRetConditionId())
                .withPriceContext(BigDecimal.valueOf(47.3))
                .withAutobudgetPriority(1)
                .withIsSuspended(true);
    }

    private TargetInterest targetInterestOnInterestId(Long interestId) {
        return defaultTargetInterest()
                .withCampaignId(mobileContentAdGroupInfo.getCampaignId())
                .withAdGroupId(mobileContentAdGroupInfo.getAdGroupId())
                .withRetargetingConditionId(null)
                .withInterestId(interestId);
    }
}
