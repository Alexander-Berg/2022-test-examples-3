package ru.yandex.market.mbi.bpmn.process.polling;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import io.grpc.stub.StreamObserver;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.business.migration.BusinessMigrationServiceGrpc;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.model.enums.ProcessType;
import ru.yandex.market.mbi.bpmn.task.polling.PollingTaskType;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.times;

public class AsyncMigrationFinishTaskTest extends FunctionalTest {

    private static final long PARTNER_ID = 777;
    private static final long SRC_BUSINESS_ID = 100;
    private static final long DST_BUSINESS_ID = 101;
    private static final long OPERATION_ID = 1L;

    @Autowired
    @Qualifier("mbocBusinessMigrationService")
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mboService;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(mboService);
    }

    @Test
    @DisplayName("Запрашиваем проверку индексации. Первый раз ждем, второй получаем результат")
    void migrationFailedUpdateSucceed() throws InterruptedException {
        //given
        willAnswer(invocation -> asyncFinishAnswer(invocation, pendingResponse()))
                .willAnswer(invocation -> asyncFinishAnswer(invocation, successResponse()))
                .given(mboService).asyncFinish(any(), any());
        //when
        Map<String, Object> params = Map.of(
                "params", Map.of("entityId", PARTNER_ID,
                        "srcBusinessId", SRC_BUSINESS_ID,
                        "dstBusinessId", DST_BUSINESS_ID
                ),
                "pollingTaskType", PollingTaskType.ASYNC_MIGRATION_FINISH,
                "timerDuration", Duration.of(30, ChronoUnit.SECONDS).toString(),
                "operationId", OPERATION_ID
        );
        ProcessInstance processInstance =
                CamundaTestUtil.invoke(processEngine, ProcessType.POLLING_TASK.getId(), params);
        assertNotNull(processInstance);

        //then
        //Проверили, что все выполнение завершилось
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));
        then(mboService).should(times(2)).asyncFinish(refEq(
                BusinessMigration.AsyncFinishBusinessRequest.newBuilder()
                        .setShopId(PARTNER_ID)
                        .setSrcBusinessId(SRC_BUSINESS_ID)
                        .setDstBusinessId(DST_BUSINESS_ID)
                        .build()), any());
    }

    @Test
    @DisplayName("Инцидент при превышении Ttl")
    void migrationTimeout() throws InterruptedException {
        //given
        willAnswer(invocation -> asyncFinishAnswer(invocation, pendingResponse()))
                .given(mboService).asyncFinish(any(), any());
        //when
        Map<String, Object> params = Map.of(
                "params", Map.of("entityId", PARTNER_ID,
                        "srcBusinessId", SRC_BUSINESS_ID,
                        "dstBusinessId", DST_BUSINESS_ID
                ),
                "pollingTaskType", PollingTaskType.ASYNC_MIGRATION_FINISH,
                "timerDuration", Duration.of(10, ChronoUnit.SECONDS).toString(),
                "operationId", OPERATION_ID,
                "pollingTaskTtl", "1"
        );
        ProcessInstance processInstance =
                CamundaTestUtil.invoke(processEngine, ProcessType.POLLING_TASK.getId(), params);

        CamundaTestUtil.checkIncidents(processEngine, processInstance, "get_result");
    }

    public Object asyncFinishAnswer(InvocationOnMock invocation,
                                    BusinessMigration.AsyncFinishBusinessResponse response) {
        StreamObserver<BusinessMigration.AsyncFinishBusinessResponse> asyncFinishBusinessResponseStreamObserver =
                invocation.getArgument(1);
        asyncFinishBusinessResponseStreamObserver.onNext(response);
        asyncFinishBusinessResponseStreamObserver.onCompleted();
        return null;
    }

    public BusinessMigration.AsyncFinishBusinessResponse successResponse() {
        return BusinessMigration.AsyncFinishBusinessResponse.newBuilder()
                .setStatus(BusinessMigration.Status.SUCCESS)
                .build();
    }

    public BusinessMigration.AsyncFinishBusinessResponse pendingResponse() {
        return BusinessMigration.AsyncFinishBusinessResponse.newBuilder()
                .setStatus(BusinessMigration.Status.IN_PROGRESS)
                .build();
    }
}
