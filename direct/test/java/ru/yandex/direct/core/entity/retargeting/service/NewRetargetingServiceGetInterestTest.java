package ru.yandex.direct.core.entity.retargeting.service;

import java.math.BigInteger;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.RetargetingSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingServiceGetInterestTest {
    private static final TargetingCategory TARGETING_CATEGORY =
            new TargetingCategory(11L, null, "", "", BigInteger.valueOf(11111111), true);

    @Autowired
    public AdGroupSteps adGroupSteps;

    @Autowired
    public RetargetingSteps retargetingSteps;

    @Autowired
    private RetargetingService serviceUnderTest;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private TestTargetingCategoriesRepository testTargetingCategoriesRepository;

    private ClientId clientId;
    private long clientUid;
    private long uid;

    private Long targetInterestId;
    private Long retargetingId;

    @Before
    public void before() {
        RetargetingInfo retargetingInfo = retargetingSteps.createDefaultRetargeting();
        retargetingId = retargetingInfo.getRetargetingId();
        clientId = retargetingInfo.getClientId();
        clientUid = rbacService.getChiefByClientId(clientId);
        uid = retargetingInfo.getUid();

        testTargetingCategoriesRepository.addTargetingCategory(TARGETING_CATEGORY);
        targetInterestId = createTargetInterest(retargetingInfo.getClientInfo());
    }

    @Test
    public void getRetargetings_SelectionIsValid_ReturnsSuccessfulResult() {
        RetargetingSelection selection = new RetargetingSelection()
                .withIds(asList(retargetingId, targetInterestId));
        List<TargetInterest> result = serviceUnderTest.getRetargetings(selection, clientId, uid, maxLimited());
        assertThat("результат содержит ожидаемое число ретаргетингов", result, hasSize(2));
        assertThat(mapList(result, TargetInterest::getId), containsInAnyOrder(targetInterestId, retargetingId));
    }

    @Test
    public void getRetargetings_SelectionContainsInterestId_ReturnsOnlyTargetInterestOnRequestedId() {
        RetargetingSelection selection = new RetargetingSelection()
                .withInterestIds(singletonList(TARGETING_CATEGORY.getTargetingCategoryId()))
                .withIds(asList(retargetingId, targetInterestId));
        List<TargetInterest> result = serviceUnderTest.getRetargetings(selection, clientId, uid, maxLimited());
        assertThat("результат содержит ожидаемое число ретаргетингов", result, hasSize(1));
        assertThat(mapList(result, TargetInterest::getId), containsInAnyOrder(targetInterestId));
    }

    @Test
    public void getRetargetings_GetOnlyTargetInterest_CheckInterestId() {
        RetargetingSelection selection = new RetargetingSelection()
                .withIds(singletonList(targetInterestId));
        List<TargetInterest> result = serviceUnderTest.getRetargetings(selection, clientId, uid, maxLimited());
        assertThat("результат содержит ожидаемое число ретаргетингов", result, hasSize(1));
        assertThat(result.get(0).getInterestId(), is(TARGETING_CATEGORY.getTargetingCategoryId()));
    }

    private Long createTargetInterest(ClientInfo clientInfo) {
        AdGroupInfo mcAdGroupInfo = adGroupSteps.createActiveMobileContentAdGroup(clientInfo);
        List<TargetInterest> targetInterests = singletonList(
                targetInterestOnInterestId(mcAdGroupInfo.getCampaignId(), mcAdGroupInfo.getAdGroupId(),
                        TARGETING_CATEGORY.getTargetingCategoryId()));
        MassResult<Long> result = serviceUnderTest
                .createAddOperation(Applicability.PARTIAL, targetInterests, clientInfo.getUid(), clientId, clientUid)
                .prepareAndApply();
        assumeThat(result.get(0).getValidationResult(), hasNoDefectsDefinitions());
        assumeThat(result, isFullySuccessful());
        return result.get(0).getResult();
    }

    private TargetInterest targetInterestOnInterestId(Long campaignId, Long adGroupId, Long interestId) {
        return defaultTargetInterest()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withRetargetingConditionId(null)
                .withInterestId(interestId);
    }
}
