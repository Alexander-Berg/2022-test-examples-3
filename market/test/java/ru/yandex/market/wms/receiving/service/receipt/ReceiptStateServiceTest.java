package ru.yandex.market.wms.receiving.service.receipt;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

public class ReceiptStateServiceTest extends ReceivingIntegrationTest {

    private static final String USER = "TEST";
    private static final String SOURCE = "TEST";

    @Autowired
    private ReceiptStateService receiptStateService;

    /**
     * Отмена поставки
     */
    @Test
    @DatabaseSetup("/service/receipt-state/cancel/1/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/cancel/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCancel() {
        receiptStateService.cancelReceipt("0000000001", USER, SOURCE);
    }

    /**
     * Отмена поставки, есть принятые товары
     */
    @Test
    @DatabaseSetup("/service/receipt-state/cancel/2/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/cancel/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCancelSomeItemsReceived() {
        assertions.assertThatThrownBy(() -> receiptStateService.cancelReceipt("0000000001", USER, SOURCE))
                .hasMessageContaining("Inbound has received items");
    }

    /**
     * Первичная приемка возвратов. Реестр получен.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-container/1/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-container/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveContainerReturnRegistryReceived() {
        receiptStateService.receiveContainer("0000000001", USER, SOURCE);
    }

    /**
     * Первичная приемка возвратов. Реестра нет.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-container/2/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-container/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveContainerReturnNoRegistry() {
        assertions.assertThatThrownBy(() -> receiptStateService.receiveContainer("0000000002", USER, SOURCE))
                .hasMessageContaining("Inbound registry is missing");
    }

    /**
     * Первичная приемка обычной поставки. Реестра нет.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-container/3/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-container/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveContainerDefaultNoRegistry() {
        receiptStateService.receiveContainer("0000000003", USER, SOURCE);
    }

    /**
     * Окончание первичной приемки возврата.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/finish-receive-container/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/finish-receive-container/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testFinishReceiveContainerReturn() {
        receiptStateService.finishReceiveContainer("0000000001", USER, SOURCE);
    }

    /**
     * Вторичная приемка обычной поставки.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-item/1/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-item/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveItemDefault() {
        receiptStateService.receiveItem("0000000001", USER, SOURCE);
    }

    /**
     * Вторичная приемка возврата после завершения первичной приемки.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-item/2/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-item/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveItemReturn() {
        receiptStateService.receiveItem("0000000001", USER, SOURCE);
    }

    /**
     * Вторичная приемка возврата до завершения первичной приемки.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-item/3/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-item/3/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveItemReturnError() {
        assertions.assertThatThrownBy(() -> receiptStateService.receiveItem("0000000001", USER, SOURCE))
                .hasMessageContaining("is not supported for receipt type");
    }

    /**
     * Вторичная приемка обычной поставки. Реестра нет.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-item/4/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-item/4/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveItemDefaultNoRegistry() {
        assertions.assertThatThrownBy(() -> receiptStateService.receiveItem("0000000001", USER, SOURCE))
                .hasMessageContaining("Inbound registry is missing");
    }

    /**
     * Вторичная приемка айтема с типом INVENTARIZATION из lost
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-item/5/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-item/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveItemInventarization() {
        receiptStateService.receiveItem("0000000001", USER, SOURCE);
    }

    /**
     * internal transition IN_RECEIVING
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-item/6/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-item/6/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveItemDefaultInternal() {
        receiptStateService.receiveItem("0000000001", USER, SOURCE);
    }

    /**
     * Завершение вторичной приемки обычной поставки. Без расхождений.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/finish-receive-item/1/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/finish-receive-item/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testFinishReceiveItemNoDiscr() {
        receiptStateService.finishReceiveItem("0000000001", USER, SOURCE);
    }

    /**
     * Завершение вторичной приемки обычной поставки. С расхожденями.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/finish-receive-item/2/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/finish-receive-item/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testFinishReceiveItemHasDiscr() {
        receiptStateService.finishReceiveItem("0000000001", USER, SOURCE);
    }

    /**
     * Завершение вторичной приемки возвратной поставки из статуса RECEIVED_COMPLETE. С расхождениями
     */
    @Test
    @DatabaseSetup("/service/receipt-state/finish-receive-item/3/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/finish-receive-item/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testFinishReceiveItemReceivedCompleteHasDiscr() {
        receiptStateService.finishReceiveItem("0000000001", USER, SOURCE);
    }

    /**
     * Возобновление приемки поставки завершенной без расхождений.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/resume-receive-item/1/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/resume-receive-item/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testResumeReceiveItemReceived() {
        receiptStateService.resumeReceiveItem("0000000001", USER, SOURCE);
    }

    /**
     * Возобновление приемки поставки завершенной с расхождениями.
     */
    @Test
    @DatabaseSetup("/service/receipt-state/resume-receive-item/2/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/resume-receive-item/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testResumeReceiveItemReceivedWDiscr() {
        receiptStateService.resumeReceiveItem("0000000001", USER, SOURCE);
    }

    /**
     * Закрытие с проверкой поставки завершенной без расхождений
     */
    @Test
    @DatabaseSetup("/service/receipt-state/close-with-verification/1/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/close-with-verification/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCloseWithVerificationReceived() {
        receiptStateService.closeReceiptWithVerification("0000000001", USER, SOURCE);
    }

    /**
     * Закрытие с проверкой поставки завершенной с расхождениями
     */
    @Test
    @DatabaseSetup("/service/receipt-state/close-with-verification/2/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/close-with-verification/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCloseWithVerificationReceivedWDiscr() {
        receiptStateService.closeReceiptWithVerification("0000000001", USER, SOURCE);
    }

    /**
     * Вторичная приемка допоставки
     */
    @Test
    @DatabaseSetup("/service/receipt-state/receive-item/5/before.xml")
    @ExpectedDatabase(value = "/service/receipt-state/receive-item/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReceiveItemAdditional() {
        receiptStateService.receiveItem("0000000001", USER, SOURCE);
    }
}
