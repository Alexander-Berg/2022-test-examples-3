package ru.yandex.direct.core.entity.retargeting.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.RetargetingSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.utils.FunctionalUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingServiceGetTest {

    @Autowired
    public RetargetingSteps retargetingSteps;

    @Autowired
    private RetargetingService serviceUnderTest;

    private long adGroupId;
    private ClientId clientId;
    private long uid;
    private List<Long> retargetingIds;

    @Before
    public void before() {
        RetargetingInfo retargetingInfo1 = retargetingSteps.createDefaultRetargeting();
        RetargetingInfo retargetingInfo2 = retargetingSteps.createDefaultRetargeting(retargetingInfo1.getAdGroupInfo());
        adGroupId = retargetingInfo1.getAdGroupId();
        clientId = retargetingInfo1.getClientId();
        uid = retargetingInfo1.getUid();
        retargetingIds = asList(retargetingInfo1.getRetargetingId(), retargetingInfo2.getRetargetingId());
    }

    @Test
    public void getRetargetings_SelectionIsValid_ReturnsSuccessfulResult() {
        RetargetingSelection selection = new RetargetingSelection()
                .withAdGroupIds(singletonList(adGroupId));
        List<TargetInterest> result = serviceUnderTest.getRetargetings(selection, clientId, uid, maxLimited());
        assertThat("результат содержит ожидаемое число ретаргетингов", result, hasSize(2));
    }

    @Test
    public void getRetargetings_SelectionContainsInvisibleRetargetingId_ReturnsOnlyVisibleRetargetings() {
        RetargetingInfo secondClientRetargeting = retargetingSteps
                .createDefaultRetargetingInActiveTextAdGroup(new ClientInfo());
        RetargetingSelection selection = new RetargetingSelection()
                .withAdGroupIds(asList(adGroupId, secondClientRetargeting.getAdGroupId()));
        List<TargetInterest> result = serviceUnderTest.getRetargetings(selection, clientId, uid, maxLimited());
        List<Long> actualRetargetingIds = FunctionalUtils.mapList(result, TargetInterest::getId);
        assertThat("сервис вернул только ретаргетинги из видимых клиенту кампаний",
                actualRetargetingIds, contains(retargetingIds.toArray()));
    }
}
