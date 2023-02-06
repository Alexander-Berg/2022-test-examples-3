package ru.yandex.direct.core.entity.retargeting.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.retargeting.AggregatedStatusRetargetingData;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_OK;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingSuspendAggregatedStatusesTest {

    @Autowired
    private Steps steps;
    @Autowired
    private RetargetingService retargetingService;
    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    private ClientId clientId;
    private Long uid;
    private int shard;
    private RetargetingInfo activeRetargeting;
    private RetargetingInfo suspendedRetargeting;

    @Before
    public void before() {
        activeRetargeting = steps.retargetingSteps().createRetargeting(
                defaultRetargeting().withIsSuspended(false));
        suspendedRetargeting = steps.retargetingSteps().createRetargeting(
                defaultRetargeting().withIsSuspended(true),
                activeRetargeting.getAdGroupInfo());

        clientId = activeRetargeting.getClientId();
        uid = activeRetargeting.getUid();
        shard = activeRetargeting.getShard();
    }

    @Test
    public void suspendRetargeting_setAggregatedStatusIsObsolete() {
        createAggregatedStatuses(activeRetargeting);
        suspendRetargeting(activeRetargeting.getRetargetingId());
        checkIsObsolete(activeRetargeting, true);
    }

    @Test
    public void resumeRetargeting_setAggregatedStatusIsObsolete() {
        createAggregatedStatuses(suspendedRetargeting);
        resumeRetargeting(suspendedRetargeting.getRetargetingId());
        checkIsObsolete(suspendedRetargeting, true);
    }

    @Test
    public void suspendRetargeting_dontSetAggregatedStatusIsObsolete_whenNotChanged() {
        createAggregatedStatuses(suspendedRetargeting);
        suspendRetargeting(suspendedRetargeting.getRetargetingId());
        checkIsObsolete(suspendedRetargeting, false);
    }

    @Test
    public void suspendRetargeting_dontSetAggregatedStatusIsObsolete_whenAlreadyUpdated() {
        // устанавливаем время обновления статусов после времени начала операции
        // статусы в этом случае не должны сбрасываться
        LocalDateTime updateTime = LocalDateTime.now().plusHours(1);
        createAggregatedStatuses(activeRetargeting, updateTime);

        suspendRetargeting(activeRetargeting.getRetargetingId());
        checkIsObsolete(activeRetargeting, false);
    }

    private void suspendRetargeting(Long retargetingId) {
        List<Long> retargetingIds = singletonList(retargetingId);
        MassResult<Long> result = retargetingService.suspendRetargetings(retargetingIds, clientId, uid);
        assumeThat(result, isFullySuccessful());
    }

    private void resumeRetargeting(Long retargetingId) {
        List<Long> retargetingIds = singletonList(retargetingId);
        MassResult<Long> result = retargetingService.resumeRetargetings(retargetingIds, clientId, uid);
        assumeThat(result, isFullySuccessful());
    }

    private void checkIsObsolete(RetargetingInfo retargetingInfo, boolean isObsolete) {
        Long retargetingId = retargetingInfo.getRetargetingId();
        Map<Long, Boolean> retargetingStatusesIsObsolete =
                aggregatedStatusesRepository.getRetargetingStatusesIsObsolete(shard, singletonList(retargetingId));
        assertThat(retargetingStatusesIsObsolete.get(retargetingId)).isEqualTo(isObsolete);
    }

    private void createAggregatedStatuses(RetargetingInfo retargetingInfo) {
        LocalDateTime updateTime = LocalDateTime.now().minusSeconds(1);
        createAggregatedStatuses(retargetingInfo, updateTime);
    }

    private void createAggregatedStatuses(RetargetingInfo retargetingInfo, LocalDateTime updateTime) {
        Long retargetingId = retargetingInfo.getRetargetingId();
        AggregatedStatusRetargetingData retargetingStatus = new AggregatedStatusRetargetingData(null, STOP_OK,
                GdSelfStatusReason.SUSPENDED_BY_USER);
        aggregatedStatusesRepository.updateRetargetings(shard, null, Map.of(retargetingId, retargetingStatus));
        aggregatedStatusesRepository.setRetargetingStatusUpdateTime(shard, retargetingId, updateTime);
    }
}
