package ru.yandex.market.checkout.helpers;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.tasks.v2.AbstractTask;
import ru.yandex.market.checkout.checkouter.tasks.v2.ExpireOrderTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.RegisterDeliveryTrackReturnTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.RegisterDeliveryTrackTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.archiving.ArchivedMultiOrdersMovingTaskV2Factory;
import ru.yandex.market.checkout.checkouter.tasks.v2.archiving.ArchivedSingleOrdersMovingTaskV2Factory;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.CheckPaymentStatusPartitionTaskV2Factory;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.ProcessHeldPaymentsPartitionTaskV2Factory;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.ProcessReturnPaymentsPartitionTaskV2Factory;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.SyncRefundWithBillingPartitionTaskV2Factory;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentBnplTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentCashbackEmitTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentCreditTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentPostpayTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentPrepayTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentRetryableTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentStationSubscriptionTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentSubsidyTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentTinkoffCreditTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentVirtualBnplTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.preorder.ProcessPreorderTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.removing.RemoveIdempotentOperationTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.removing.RemoveShootingOrdersTaskV2;
import ru.yandex.market.checkout.checkouter.tasks.v2.returns.ReturnApplicationFallbackGeneratorTaskV2;
import ru.yandex.market.checkout.common.TestHelper;

import static ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType.ONCE;

@TestHelper
public class TmsTaskHelper {
    private final Consumer<AbstractTask<?>> consumer = value -> {
        var anotherResult = value.run(ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
    };

    @Autowired
    private ProcessHeldPaymentsPartitionTaskV2Factory processHeldPaymentsPartitionTaskV2Factory;
    @Autowired
    private ExpireOrderTaskV2 expireOrderTaskV2;
    @Autowired
    private CheckPaymentStatusPartitionTaskV2Factory checkPaymentStatusPartitionTaskV2Factory;
    @Autowired
    private RemoveShootingOrdersTaskV2 removeShootingOrdersTaskV2;

    @Autowired
    private ProcessReturnPaymentsPartitionTaskV2Factory processReturnPaymentsPartitionTaskV2Factory;
    @Autowired
    private SyncRefundWithBillingPartitionTaskV2Factory syncRefundWithBillingPartitionTaskV2Factory;

    @Autowired
    private InspectExpiredPaymentBnplTaskV2 inspectExpiredPaymentBnplTaskV2;
    @Autowired
    private InspectExpiredPaymentCashbackEmitTaskV2 inspectExpiredPaymentCashbackEmitTaskV2;
    @Autowired
    private InspectExpiredPaymentCreditTaskV2 inspectExpiredPaymentCreditTaskV2;
    @Autowired
    private InspectExpiredPaymentPostpayTaskV2 inspectExpiredPaymentPostpayTaskV2;
    @Autowired
    private InspectExpiredPaymentPrepayTaskV2 inspectExpiredPaymentPrepayTaskV2;
    @Autowired
    private InspectExpiredPaymentRetryableTaskV2 inspectExpiredPaymentRetryableTaskV2;
    @Autowired
    private InspectExpiredPaymentStationSubscriptionTaskV2 inspectExpiredPaymentStationSubscriptionTaskV2;
    @Autowired
    private InspectExpiredPaymentSubsidyTaskV2 inspectExpiredPaymentSubsidyTaskV2;
    @Autowired
    private InspectExpiredPaymentTinkoffCreditTaskV2 inspectExpiredPaymentTinkoffCreditTaskV2;
    @Autowired
    private InspectExpiredPaymentVirtualBnplTaskV2 inspectExpiredPaymentVirtualBnplTaskV2;

    @Autowired
    private RegisterDeliveryTrackTaskV2 registerDeliveryTrackTaskV2;
    @Autowired
    private RegisterDeliveryTrackReturnTaskV2 registerDeliveryTrackReturnTaskV2;

    @Autowired
    private RemoveIdempotentOperationTaskV2 removeIdempotentOperationTaskV2;

    @Autowired
    private ReturnApplicationFallbackGeneratorTaskV2 returnApplicationFallbackGeneratorTaskV2;

    @Autowired
    private ProcessPreorderTaskV2 processPreorderTaskV2;

    @Autowired
    private ArchivedSingleOrdersMovingTaskV2Factory archivedSingleOrdersMovingTaskV2Factory;
    @Autowired
    private ArchivedMultiOrdersMovingTaskV2Factory archivedMultiOrdersMovingTaskV2Factory;

    public void runProcessHeldPaymentsTaskV2() {
        runPartitionedTaskOnce(processHeldPaymentsPartitionTaskV2Factory.getTasks());
    }

    public void runCheckPaymentStatusTaskV2() {
        runPartitionedTaskOnce(checkPaymentStatusPartitionTaskV2Factory.getTasks());
    }

    public void runExpireOrderTaskV2() {
        List.of(expireOrderTaskV2).forEach(consumer);
    }

    public void runRemoveShootingOrderTaskV2() {
        List.of(removeShootingOrdersTaskV2).forEach(consumer);
    }

    public void runInspectExpiredPaymentTaskV2() {
        List.of(inspectExpiredPaymentBnplTaskV2, inspectExpiredPaymentCashbackEmitTaskV2,
                inspectExpiredPaymentCreditTaskV2, inspectExpiredPaymentPostpayTaskV2,
                inspectExpiredPaymentPrepayTaskV2, inspectExpiredPaymentRetryableTaskV2,
                inspectExpiredPaymentStationSubscriptionTaskV2, inspectExpiredPaymentSubsidyTaskV2,
                inspectExpiredPaymentTinkoffCreditTaskV2, inspectExpiredPaymentVirtualBnplTaskV2).forEach(consumer);
    }

    public void runProcessReturnPaymentsPartitionTaskV2() {
        runPartitionedTaskOnce(processReturnPaymentsPartitionTaskV2Factory.getTasks());
    }

    public void runSyncRefundWithBillingPartitionTaskV2() {
        runPartitionedTaskOnce(syncRefundWithBillingPartitionTaskV2Factory.getTasks());
    }

    public void runRegisterDeliveryTrackTaskV2() {
        List.of(registerDeliveryTrackTaskV2, registerDeliveryTrackReturnTaskV2).forEach(consumer);
    }

    public void runReturnApplicationFallbackGeneratorTaskV2() {
        List.of(returnApplicationFallbackGeneratorTaskV2).forEach(consumer);
    }

    public void runRemoveIdempotentOperationTaskV2() {
        List.of(removeIdempotentOperationTaskV2).forEach(consumer);
    }

    public void runProcessPreorderTaskV2() {
        List.of(processPreorderTaskV2).forEach(consumer);
    }

    public void runArchiveOrderTasks() {
        runPartitionedTaskOnce(archivedSingleOrdersMovingTaskV2Factory.getTasks());
        runPartitionedTaskOnce(archivedMultiOrdersMovingTaskV2Factory.getTasks());
    }

    private void runPartitionedTaskOnce(Map<Integer, AbstractTask<?>> tasks) {
        tasks.values().forEach(consumer);
    }
}
