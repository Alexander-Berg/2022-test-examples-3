package ru.yandex.direct.core.entity.retargeting.service;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.retargeting.container.SwitchRetargeting;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.RetargetingSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SwitchRetargetingsOnBannersTest {
    @Autowired
    protected RetargetingSteps retargetingSteps;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private RetargetingService retargetingService;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private RetargetingRepository retargetingRepository;

    private ClientId clientId;
    private RetConditionInfo retConditionInfo;
    private RetargetingInfo retargetingInfo;

    @Before
    public void before() {
        bannerSteps.createActiveTextBanner();
        retargetingInfo = retargetingSteps.createDefaultRetargeting();
        retConditionInfo = retargetingInfo.getRetConditionInfo();
        clientId = retConditionInfo.getClientId();
        AdGroup group = adGroupRepository
                .getAdGroups(retargetingInfo.getShard(), Collections.singletonList(retargetingInfo.getAdGroupId()))
                .get(0);
        checkState(group.getStatusBsSynced().equals(StatusBsSynced.YES), "статус statusBsSynced группы Yes");
    }

    @Test
    public void switchRetargetingOffOnOneBanner() {
        boolean isSuspended = true;
        List<SwitchRetargeting> switchRetargetings =
                Collections
                        .singletonList(new SwitchRetargeting().withRetCondId(retConditionInfo.getRetConditionId())
                                .withSuspended(isSuspended));
        retargetingService.switchRetargetingConditions(switchRetargetings, clientId, Applicability.FULL);
        Retargeting actualRetargeting = retargetingRepository
                .getRetargetingsByIds(retargetingInfo.getShard(), Collections.singletonList(retargetingInfo.getRetargetingId()),
                        maxLimited()).get(0);
        assertThat("статус isSuspended соответсвует ожиданиям", actualRetargeting.getIsSuspended(), is(isSuspended));

    }

    @Test
    public void switchRetargetingOffOnOneBanner_CheckStatusBsSync() {
        List<SwitchRetargeting> switchRetargetings =
                Collections
                        .singletonList(new SwitchRetargeting().withRetCondId(retConditionInfo.getRetConditionId())
                                .withSuspended(true));
        retargetingService.switchRetargetingConditions(switchRetargetings, clientId, Applicability.FULL);
        AdGroup actualAdGroup = adGroupRepository
                .getAdGroups(retargetingInfo.getShard(), Collections.singletonList(retargetingInfo.getAdGroupId()))
                .get(0);
        assertThat("статус statusBsSynced группы сброшен", actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

}
