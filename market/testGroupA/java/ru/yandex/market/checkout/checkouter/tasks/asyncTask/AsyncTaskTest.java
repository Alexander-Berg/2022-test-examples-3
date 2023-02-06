package ru.yandex.market.checkout.checkouter.tasks.asyncTask;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.async.AsyncTask;
import ru.yandex.market.checkout.checkouter.async.AsyncTaskStatus;
import ru.yandex.market.checkout.checkouter.async.CreateAsyncTaskResponse;
import ru.yandex.market.checkout.checkouter.async.changeStatus.SingleOrderStatusChangePayload;
import ru.yandex.market.checkout.checkouter.async.changeStatus.SingleOrderStatusChangeResult;
import ru.yandex.market.checkout.checkouter.async.changeStatus.StatusChangePayload;
import ru.yandex.market.checkout.checkouter.async.changeStatus.StatusChangeResult;
import ru.yandex.market.checkout.checkouter.client.AsyncTaskClient;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.tasks.async.AsyncTaskCleanerJob;
import ru.yandex.market.checkout.checkouter.tasks.async.statusChange.StatusChangeTaskV2Factory;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskResult;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AsyncTaskTest extends AbstractWebTestBase {

    @Autowired
    private AsyncTaskCleanerJob asyncTaskCleanerJob;

    @Autowired
    private AsyncTaskClient asyncTaskClient;

    @Autowired
    private StatusChangeTaskV2Factory statusChangeTaskV2Factory;

    @Test
    public void testAsyncTaskRun() {
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.SHOP).withId(1L).build();

        CreateAsyncTaskResponse createAsyncTaskResponse = asyncTaskClient.changeStatus(new StatusChangePayload(
                List.of(new SingleOrderStatusChangePayload(
                        1L, OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND
                ))
        ), clientInfo);

        runAsyncTaskChangeOrderStatus();

        AsyncTask<StatusChangePayload, StatusChangeResult> result =
                asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), clientInfo);
        assertEquals(AsyncTaskStatus.PROCESSED, result.getStatus());
        assertEquals(1L, result.getRunCount());
    }

    @Test
    public void testAsyncTaskRunOnlyOnce() {
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.SHOP).withId(1L).build();

        CreateAsyncTaskResponse createAsyncTaskResponse = asyncTaskClient.changeStatus(new StatusChangePayload(
                List.of(new SingleOrderStatusChangePayload(
                        1L, OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND
                ))
        ), clientInfo);

        runAsyncTaskChangeOrderStatus();
        runAsyncTaskChangeOrderStatus();
        runAsyncTaskChangeOrderStatus();

        AsyncTask<StatusChangePayload, StatusChangeResult> result =
                asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), clientInfo);
        assertEquals(AsyncTaskStatus.PROCESSED, result.getStatus());
        assertEquals(1L, result.getRunCount());
    }

    @Test
    public void testAsyncTaskAuth() {
        ClientInfo shopClientInfo = ClientInfo.builder(ClientRole.SHOP).withId(1L).build();
        ClientInfo shopClientInfo2 = ClientInfo.builder(ClientRole.SHOP).withId(2L).build();

        ClientInfo clientInfo = ClientInfo.builder(ClientRole.SHOP).withId(1L).build();

        CreateAsyncTaskResponse createAsyncTaskResponse = asyncTaskClient.changeStatus(new StatusChangePayload(
                List.of(new SingleOrderStatusChangePayload(
                        1L, OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND
                ))
        ), clientInfo);

        runAsyncTaskChangeOrderStatus();

        AsyncTask<StatusChangePayload, StatusChangeResult> result =
                asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), shopClientInfo);
        assertEquals(AsyncTaskStatus.PROCESSED, result.getStatus());

        assertThrows(RuntimeException.class, () -> {
            asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), shopClientInfo2);
        });
    }

    @Test
    public void testAsyncTaskAuthBySystem() {
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.SHOP).withId(1L).build();

        CreateAsyncTaskResponse createAsyncTaskResponse = asyncTaskClient.changeStatus(new StatusChangePayload(
                List.of(new SingleOrderStatusChangePayload(
                        1L, OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND
                ))
        ), clientInfo);

        runAsyncTaskChangeOrderStatus();

        AsyncTask<StatusChangePayload, StatusChangeResult> result =
                asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), ClientInfo.SYSTEM);

        assertEquals(AsyncTaskStatus.PROCESSED, result.getStatus());
    }


    @Test
    public void testSuccessAsyncOrderStatusChange() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        ClientInfo clientInfo = order.getUserClientInfo();

        setFixedTime(Instant.now());

        CreateAsyncTaskResponse createAsyncTaskResponse = asyncTaskClient.changeStatus(new StatusChangePayload(
                List.of(
                        new SingleOrderStatusChangePayload(
                                order.getId(),
                                OrderStatus.CANCELLED,
                                OrderSubstatus.USER_CHANGED_MIND
                        )
                )
        ), clientInfo);

        runAsyncTaskChangeOrderStatus();

        AsyncTask<StatusChangePayload, StatusChangeResult> statusChangeStatus =
                asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), clientInfo);

        assertEquals(
                SingleOrderStatusChangeResult.SingleOrderStatusChangeStatus.SUCCESS,
                statusChangeStatus.getResult().orElseThrow().getResults().iterator().next().getStatus()
        );

        asyncTaskCleanerJob.run(TaskRunType.ONCE);
        assertNotNull(asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), clientInfo));


        setFixedTime(Instant.now().plus(4, ChronoUnit.DAYS));
        asyncTaskCleanerJob.run(TaskRunType.ONCE);
        assertNotNull(asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), clientInfo));

        setFixedTime(Instant.now().plus(6, ChronoUnit.DAYS));
        asyncTaskCleanerJob.run(TaskRunType.ONCE);
        assertThrows(RuntimeException.class,
                () -> asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), clientInfo));
    }

    @Test
    public void testFailAsyncOrderStatusChange() {
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.SHOP).withId(1L).build();

        CreateAsyncTaskResponse createAsyncTaskResponse = asyncTaskClient.changeStatus(new StatusChangePayload(
                List.of(new SingleOrderStatusChangePayload(
                        2222222L, OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND
                ))
        ), clientInfo);

        runAsyncTaskChangeOrderStatus();

        AsyncTask<StatusChangePayload, StatusChangeResult> statusChangeStatus =
                asyncTaskClient.getStatusChangeTask(createAsyncTaskResponse.getId(), clientInfo);

        assertEquals(
                SingleOrderStatusChangeResult.SingleOrderStatusChangeStatus.FAIL,
                statusChangeStatus.getResult().orElseThrow().getResults().iterator().next().getStatus()
        );

        assertEquals(
                "Order not found: 2222222",
                statusChangeStatus.getResult().orElseThrow().getResults().iterator().next().getErrorMessage()
        );
    }

    private void runAsyncTaskChangeOrderStatus() {
        statusChangeTaskV2Factory.getTasks().forEach((k, v) -> {
            TaskResult run = v.run(TaskRunType.ONCE);
            assertEquals(TaskStageType.SUCCESS, run.getStage());
        });
    }
}
