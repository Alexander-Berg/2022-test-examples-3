package ru.yandex.market.checkout.helpers;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.InspectInconsistentReceiptPartitionTaskV2Factory;

public class ReceiptRepairHelper {
    @Autowired
    private InspectInconsistentReceiptPartitionTaskV2Factory inspectInconsistentReceiptPartitionTaskV2Factory;
    @Autowired
    private ReceiptService receiptService;
    @Value("${receipt.inspector.notification.timeout:48}")
    private int notificationTimeoutHours;

    public void repairReceipts() {
        inspectInconsistentReceiptPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });
        var receipts = receiptService.getInconsistentReceipts(notificationTimeoutHours, 15);
        Assertions.assertTrue(receipts.isEmpty(), receipts.toString());
    }
}
