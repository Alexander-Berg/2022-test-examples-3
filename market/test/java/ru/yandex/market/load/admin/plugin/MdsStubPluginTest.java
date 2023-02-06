package ru.yandex.market.load.admin.plugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest;
import ru.yandex.market.load.admin.dao.JobDao;
import ru.yandex.market.load.admin.entity.Job;
import ru.yandex.market.load.admin.entity.StubConfig;
import ru.yandex.market.load.admin.entity.StubConfigType;
import ru.yandex.market.load.admin.entity.TaskType;
import ru.yandex.mj.generated.client.mds.api.MdsApiClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by aproskriakov on 7/1/22
 */
public class MdsStubPluginTest extends AbstractJdbcRecipeTest {

    MdsInitStubPlugin mdsInitStubPlugin;

    MdsGenerateStubPlugin mdsGenerateStubPlugin;

    @Mock
    MdsApiClient mdsApiClient;

    @Mock
    JobDao jobDao;

    @BeforeEach
    void setUp() {
        mdsInitStubPlugin = new MdsInitStubPlugin(mdsApiClient, jobDao);
        mdsGenerateStubPlugin = new MdsGenerateStubPlugin(mdsApiClient, jobDao);
    }

    @Test
    void testGetInitResult() throws ExecutionException, InterruptedException {
        ExecuteCall<String, RetryStrategy> executeCall = mock(ExecuteCall.class);
        when(mdsApiClient.initGet("http://s3.mdst.yandex.net", false)).thenReturn(executeCall);
        CompletableFuture<String> future = mock(CompletableFuture.class);
        when(future.get()).thenReturn("{Test}");
        when(executeCall.schedule()).thenReturn(future);
        String jobParams = "{\n" +
                "    \"urlInitResult\": \"http://s3.mdst.yandex.net\"\n" +
                "}";
        when(jobDao.findJobById(anyLong())).thenReturn(Optional.of(
                Job.builder()
                        .params(jobParams)
                        .build()));
        StubConfig stubConfig = StubConfig.builder()
                .type(StubConfigType.MDS)
                .projectId(1)
                .task(TaskType.INIT)
                .build();

        String result = mdsInitStubPlugin.result(stubConfig, 1L);

        assertEquals("{Test}", result);
    }

    @Test
    void testGetGenerateAmmoResult() throws ExecutionException, InterruptedException {
        ExecuteCall<String, RetryStrategy> executeCall = mock(ExecuteCall.class);
        when(mdsApiClient.generateAmmoJsonGet("http://s3.mdst.yandex.net", false)).thenReturn(executeCall);
        CompletableFuture<String> future = mock(CompletableFuture.class);
        when(future.get()).thenReturn("{Test}");
        when(executeCall.schedule()).thenReturn(future);
        String jobParams = "{\n" +
                "    \"urlGenerateAmmoResult\": \"http://s3.mdst.yandex.net\"\n" +
                "}";
        when(jobDao.findJobById(anyLong())).thenReturn(Optional.of(
                Job.builder()
                        .params(jobParams)
                        .build()));
        StubConfig stubConfig = StubConfig.builder()
                .type(StubConfigType.MDS)
                .projectId(1)
                .task(TaskType.INIT)
                .build();

        String result = mdsGenerateStubPlugin.resultAmmo(stubConfig, 1L);

        assertEquals("{Test}", result);
    }

    @Test
    void testGetGenerateConfigResult() throws ExecutionException, InterruptedException {
        ExecuteCall<String, RetryStrategy> executeCall = mock(ExecuteCall.class);
        when(mdsApiClient.generateConfigJsonGet("http://s3.mdst.yandex.net", false)).thenReturn(executeCall);
        CompletableFuture<String> future = mock(CompletableFuture.class);
        when(future.get()).thenReturn("{Test}");
        when(executeCall.schedule()).thenReturn(future);
        ;
        String jobParams = "{\n" +
                "    \"urlGenerateConfigResult\": \"http://s3.mdst.yandex.net\"\n" +
                "}";
        when(jobDao.findJobById(anyLong())).thenReturn(Optional.of(
                Job.builder()
                        .params(jobParams)
                        .build()));
        StubConfig stubConfig = StubConfig.builder()
                .type(StubConfigType.MDS)
                .projectId(1)
                .task(TaskType.INIT)
                .build();

        String result = mdsGenerateStubPlugin.resultConfig(stubConfig, 1L);

        assertEquals("{Test}", result);
    }

}
