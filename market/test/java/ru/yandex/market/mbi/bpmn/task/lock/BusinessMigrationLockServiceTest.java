package ru.yandex.market.mbi.bpmn.task.lock;

import java.util.List;

import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.bpmn.function.DelegateExecutionUtils;
import ru.yandex.market.mbi.bpmn.model.enums.ExternalSystem;
import ru.yandex.market.mbi.bpmn.task.lock.business.BusinessMigrationLockService;
import ru.yandex.market.mbi.bpmn.task.lock.business.impl.MbiBusinessLocker;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BusinessMigrationLockServiceTest {

    private LockTaskDelegate lockTaskDelegate;
    private MbiBusinessLocker mbiBusinessLocker;
    private BusinessMigrationLockService businessMigrationLockService;

    @BeforeEach
    void init() {
        mbiBusinessLocker = mock(MbiBusinessLocker.class);
        doReturn(ExternalSystem.MBI)
                .when(mbiBusinessLocker).getExternalSystemName();

        businessMigrationLockService = new BusinessMigrationLockService(List.of(mbiBusinessLocker));
        DelegateExecutionUtils delegateExecutionUtils = new DelegateExecutionUtils();
        lockTaskDelegate = new LockTaskDelegate(businessMigrationLockService, null, delegateExecutionUtils);
    }

    @Test
    void testLockAndUnlock() throws Exception {
        ExecutionEntity execution = mock(ExecutionEntity.class);
        doReturn("1")
                .when(execution).getVariable("srcBusinessId");
        doReturn("2")
                .when(execution).getVariable("dstBusinessId");
        doReturn("777")
                .when(execution).getVariable("serviceId");
        doReturn("MBI")
                .when(execution).getVariable("externalSystemName");
        doReturn("business_migration")
                .when(execution).getVariable("lockType");

        doReturn("12345678")
                .when(execution).getRootProcessInstanceId();
        doReturn("12345678")
                .when(execution).getProcessInstanceId();

        // Lock direction
        BusinessMigration.LockBusinessRequest lockRequest = BusinessMigration.LockBusinessRequest.newBuilder()
                .setSrcBusinessId(1L)
                .setDstBusinessId(2L)
                .setShopId(777L)
                .setProcessId(execution.getProcessInstanceId())
                .build();
        doReturn("LOCK")
                .when(execution).getVariable("direction");
        BusinessMigration.LockBusinessResponse lockResponse = BusinessMigration.LockBusinessResponse.newBuilder()
                .setMessage(GenericCallResponse.ok().getMessage())
                .setStatus(BusinessMigration.Status.SUCCESS)
                .build();
        doReturn(lockResponse)
                .when(mbiBusinessLocker).lock(lockRequest);
        lockTaskDelegate.execute(execution);
        verify(mbiBusinessLocker).lock(lockRequest);
        verify(execution).setVariable(eq("status"), eq("SUCCESS"));

        // unlock direction
        BusinessMigration.UnlockBusinessRequest unlockRequest = BusinessMigration.UnlockBusinessRequest.newBuilder()
                .setSrcBusinessId(1L)
                .setDstBusinessId(2L)
                .setShopId(777L)
                .setProcessId(execution.getProcessInstanceId())
                .build();
        doReturn("UNLOCK")
                .when(execution).getVariable("direction");
        GenericCallResponse unlockApiResponse = GenericCallResponse.exception(new RuntimeException());
        BusinessMigration.UnlockBusinessResponse unlockResponse = BusinessMigration.UnlockBusinessResponse.newBuilder()
                .setMessage(unlockApiResponse.getMessage())
                .setStatus(BusinessMigration.Status.FAIL)
                .build();

        doReturn(unlockResponse)
                .when(mbiBusinessLocker).unlock(unlockRequest);
        lockTaskDelegate.execute(execution);
        verify(mbiBusinessLocker).unlock(unlockRequest);
        verify(execution).setVariable(eq("status"), eq("FAIL"));
    }
}
