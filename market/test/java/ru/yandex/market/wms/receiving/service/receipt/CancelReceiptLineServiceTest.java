package ru.yandex.market.wms.receiving.service.receipt;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

class CancelReceiptLineServiceTest extends ReceivingIntegrationTest {

    @Autowired
    private CancelReceiptLineService service;


    @Test
    @DatabaseSetup("/service/receipt-state/cancel-bbxd-task/1/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/cancel-bbxd-task/1/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void cancelBbxdTasksIfExistsNoActiveTasks() {
        service.cancelBbxdTasksIfExists(Collections.emptyMap());
    }

    @Test
    @DatabaseSetup("/service/receipt-state/cancel-bbxd-task/2/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/cancel-bbxd-task/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void cancelBbxdTasksIfExistsDifferentTaskReceipt() {
        Map<String, Set<String>> receiptLines = Map.of("0000000001", Set.of("00001"));
        service.cancelBbxdTasksIfExists(receiptLines);
    }

    @Test
    @DatabaseSetup("/service/receipt-state/cancel-bbxd-task/3/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/cancel-bbxd-task/3/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void cancelBbxdTasksIfExistsDifferentTaskSku() {
        Map<String, Set<String>> receiptLines = Map.of("0000000001", Set.of("00001"));
        service.cancelBbxdTasksIfExists(receiptLines);
    }

    @Test
    @DatabaseSetup("/service/receipt-state/cancel-bbxd-task/4/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/cancel-bbxd-task/4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void cancelBbxdTasksIfExistsCancelledOk() {
        Map<String, Set<String>> receiptLines = Map.of("0000000001", Set.of("00001"));
        service.cancelBbxdTasksIfExists(receiptLines);
    }
}
