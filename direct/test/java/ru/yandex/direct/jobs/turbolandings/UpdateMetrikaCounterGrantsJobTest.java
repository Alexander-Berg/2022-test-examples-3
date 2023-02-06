package ru.yandex.direct.jobs.turbolandings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParams;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParamsItem;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsResult;
import ru.yandex.direct.core.entity.turbolanding.service.UpdateCounterGrantsService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.model.DbQueueJobStatus;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbqueue.service.DbQueueService;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.metrika.client.MetrikaApiError;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.model.response.MetrikaErrorResponse;
import ru.yandex.direct.metrika.client.model.response.UpdateCounterGrantsResponse;
import ru.yandex.direct.rbac.PpcRbac;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.UPDATE_COUNTER_GRANTS_JOB;
import static ru.yandex.direct.core.entity.turbolanding.service.UpdateCounterGrantsService.ERROR_TYPES_WITHOUT_RETRY;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.isAfter;


@JobsTest
@ExtendWith(SpringExtension.class)
class UpdateMetrikaCounterGrantsJobTest {
    private static final int SHARD = 1;
    private static final Long COUNTER_ID_1 = 1234L;
    private static final Long COUNTER_ID_2 = 5678L;

    @Autowired
    private Steps steps;
    @Autowired
    private DbQueueSteps dbQueueSteps;

    @Autowired
    private DbQueueService dbQueueService;
    @Autowired
    private UserService userService;
    @Autowired
    private ClientService clientService;

    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private DbQueueRepository dbQueueRepository;
    @Autowired
    private PpcRbac ppcRbac;

    private MetrikaClient metrikaClient;
    private UpdateCounterGrantsService updateCounterGrantsService;
    private UpdateMetrikaCounterGrantsJob job;

    private UserInfo userInfo1;
    private UserInfo userInfo2;
    private Long operatorUid;

    @BeforeEach
    void before() {
        operatorUid = steps.clientSteps().createClient(new ClientInfo().withShard(SHARD)).getUid();
        userInfo1 = steps.clientSteps().createClient(new ClientInfo().withShard(1)).getChiefUserInfo();
        userInfo2 = steps.clientSteps().createClient(new ClientInfo().withShard(2)).getChiefUserInfo();

        metrikaClient = mock(MetrikaClient.class);

        updateCounterGrantsService = new UpdateCounterGrantsService(
                shardHelper, metrikaClient, dbQueueRepository, ppcRbac, clientService);

        job = new UpdateMetrikaCounterGrantsJob(SHARD, dbQueueService, updateCounterGrantsService, userService);

        dbQueueSteps.registerJobType(UPDATE_COUNTER_GRANTS_JOB);
        dbQueueSteps.clearQueue(UPDATE_COUNTER_GRANTS_JOB);
    }

    @Test
    void addCountersToResyncQueueTest() {
        List<Long> userIds1 = asList(userInfo1.getUid(), userInfo2.getUid());
        List<Long> userIds2 = singletonList(userInfo2.getUid());

        Map<Long, List<Long>> userIdsByCounterId = ImmutableMap.of(
                COUNTER_ID_1, userIds1, COUNTER_ID_2, userIds2);

        Long jobId = updateCounterGrantsService.addCountersToResyncQueue(operatorUid, userIdsByCounterId).get(0);

        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> addedJob =
                dbQueueRepository.findJobById(SHARD, UPDATE_COUNTER_GRANTS_JOB, jobId);
        checkState(addedJob != null);

        UpdateCounterGrantsParamsItem expectedItem1 = new UpdateCounterGrantsParamsItem()
                .withCounterId(COUNTER_ID_1)
                .withUserIds(userIds1);
        UpdateCounterGrantsParamsItem expectedItem2 = new UpdateCounterGrantsParamsItem()
                .withCounterId(COUNTER_ID_2)
                .withUserIds(userIds2);

        assertThat(addedJob.getArgs().getItems(),
                containsInAnyOrder(asList(beanDiffer(expectedItem1), beanDiffer(expectedItem2))));
    }


    @Test
    void updateCounterGrantsTest_success() {
        Map<Long, List<Long>> userIdsByCounterId = ImmutableMap.of(
                COUNTER_ID_1, asList(userInfo1.getUid(), userInfo2.getUid()),
                COUNTER_ID_2, singletonList(userInfo2.getUid()));

        Long jobId = updateCounterGrantsService.addCountersToResyncQueue(operatorUid, userIdsByCounterId).get(0);

        setMetrikaResponse(COUNTER_ID_1, asSet(userInfo1.getUser().getLogin(), userInfo2.getUser().getLogin()), true);
        setMetrikaResponse(COUNTER_ID_2, singleton(userInfo2.getUser().getLogin()), true);

        executeJob();

        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> updateJob =
                dbQueueRepository.findJobById(SHARD, UPDATE_COUNTER_GRANTS_JOB, jobId);

        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> expectedJob =
                new DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult>()
                        .withStatus(DbQueueJobStatus.FINISHED)
                        .withTryCount(1L);

        assertThat(updateJob, beanDiffer(expectedJob).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    void updateCounterGrantsTest_fail() {
        Map<Long, List<Long>> userIdsByCounterId = ImmutableMap.of(
                COUNTER_ID_1, asList(userInfo1.getUid(), userInfo2.getUid()),
                COUNTER_ID_2, singletonList(userInfo2.getUid()));

        Long jobId = updateCounterGrantsService.addCountersToResyncQueue(operatorUid, userIdsByCounterId).get(0);

        setMetrikaResponse(COUNTER_ID_1, asSet(userInfo1.getUser().getLogin(), userInfo2.getUser().getLogin()), true);
        setMetrikaResponse(COUNTER_ID_2, singleton(userInfo2.getUser().getLogin()), false,
                new MetrikaErrorResponse(
                        List.of(new MetrikaApiError("new error type", "", "")),
                        404,
                        ""
                ));

        executeJob();

        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> updateJob =
                dbQueueRepository.findJobById(SHARD, UPDATE_COUNTER_GRANTS_JOB, jobId);

        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> expectedJob =
                new DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult>()
                        .withStatus(DbQueueJobStatus.NEW)
                        .withTryCount(1L)
                        .withGrabbedBy("");

        assertThat(updateJob, beanDiffer(expectedJob).useCompareStrategy(
                onlyFields(newPath("status"), newPath("tryCount"), newPath("grabbedBy"), newPath("runAfter"))
                        .forFields(newPath("runAfter")).useMatcher(isAfter(LocalDateTime.now()))));
    }

    @Test
    void updateCounterGrantsTest_notRetry() {
        Map<Long, List<Long>> userIdsByCounterId = ImmutableMap.of(
                COUNTER_ID_1, asList(userInfo1.getUid(), userInfo2.getUid()),
                COUNTER_ID_2, singletonList(userInfo2.getUid()));

        Long jobId = updateCounterGrantsService.addCountersToResyncQueue(operatorUid, userIdsByCounterId).get(0);

        setMetrikaResponse(COUNTER_ID_1, asSet(userInfo1.getUser().getLogin(), userInfo2.getUser().getLogin()), true);
        setMetrikaResponse(COUNTER_ID_2, singleton(userInfo2.getUser().getLogin()), false,
                new MetrikaErrorResponse(
                        List.of(new MetrikaApiError(ERROR_TYPES_WITHOUT_RETRY.stream().findAny().get(), "", "")),
                        404,
                        ""
                ));

        executeJob();

        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> updateJob =
                dbQueueRepository.findJobById(SHARD, UPDATE_COUNTER_GRANTS_JOB, jobId);

        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> expectedJob =
                new DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult>()
                        .withStatus(DbQueueJobStatus.FINISHED)
                        .withTryCount(1L);

        assertThat(updateJob, beanDiffer(expectedJob).useCompareStrategy(onlyExpectedFields()));    }

    private void executeJob() {
        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    private void setMetrikaResponse(long counterId, Set<String> logins, boolean isSuccessful) {
        setMetrikaResponse(counterId, logins, isSuccessful, null);
    }

    private void setMetrikaResponse(long counterId, Set<String> logins, boolean isSuccessful,
                                    @Nullable MetrikaErrorResponse errors) {
        when(metrikaClient.updateCounterGrants(eq(counterId), eq(logins))).thenReturn(new UpdateCounterGrantsResponse()
                .withSuccessful(isSuccessful).withMetrikaErrorResponse(errors));
    }
}
