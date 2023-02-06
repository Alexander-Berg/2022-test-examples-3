package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATE_2017_01_01;

/**
 * Тесты {@link GetOrderEventsStrategy}, затрагивающие логику обновления полей транзакции при событии
 * {@link HistoryEventType#ITEMS_UPDATED}.
 */
@DbUnitDataSet(before = {"db/datasource.csv", "db/environmentZeroDelay.csv"})
@ActiveProfiles("goe-processing")
class DbGetOrdersExecutorTransactionsItemsUpdateTest extends FunctionalTest
        implements ResourceHttpUtilitiesMixin, ResourceUtilitiesMixin {

    private static final String RESOURCE_PREFIX = "resources/checkouter_response/events/items_update/";

    @Autowired
    private RestTemplate checkouterRestTemplate;

    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private EventProcessorSupportFactory eventProcessorSupportFactory;

    private GetOrderEventsStrategy strategy;

    @BeforeEach
    void onBefore() {
        this.strategy = new GetOrderEventsStrategy(eventProcessorSupportFactory.createSupport());
    }

    /**
     * Тестируется только логика метода {@link GetOrderEventsStrategy#processOrderTransactions} для события
     * {@link HistoryEventType#ITEMS_UPDATED}.
     * <p>
     * Непосредственно само изменение состава заказа, тестируется отдельно в
     * {@link GetOrderEventsStrategyUpdateItemsEventTest}.
     * <p>
     * Случай, когда удалось получить информацию о платеже.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_ChangeFee_ChangeTotal_PaidWithYandex.before" +
                            ".csv",
                    "db/OrderTransactionInfo_ItemsUpdate.before.csv"
            },
            after = "db/OrderTransactionInfo_ItemsUpdate.after.csv"
    )
    void test_executor_when_prepaidWithPaymentInfo_should_updateTransactionInfo() throws IOException {
        runTest("processingToProcessing-with-itemsCountDecrementWithDelete-prepaid-withPaymentId.json");
    }

    /**
     * Тоже что и
     * {@link DbGetOrdersExecutor_Transactions_ItemsUpdate_Test#test_executor_when_eventIsItemsUpdated_should_updateTransactionInfo},
     * но для случая, когда НЕ удалось получить информацию о платеже.
     * <p>
     * В таком случае ничего не делаем.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_ChangeFee_ChangeTotal_PaidWithYandex.before" +
                            ".csv",
                    "db/OrderTransactionInfo_ItemsUpdate.before.csv"
            },
            after = "db/OrderTransactionInfo_ItemsUpdate.before.csv"
    )
    void test_executor_when_prepaidOrderWithoutPayment_should_updateTransactionWithActualTotalItemsFromOrder()
            throws IOException {
        runTest("processingToProcessing-with-itemsCountDecrementWithDelete-prepaid-withoutPaymentId.json");
    }

    /**
     * Тест, проверяющий, что для типа оплаты отличного от
     * {@link ru.yandex.market.checkout.checkouter.pay.PaymentType#PREPAID}, не происходит модификация полей транзакции.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_ChangeFee_ChangeTotal_PaidWithYandex.before" +
                            ".csv",
                    "db/OrderTransactionInfo_ItemsUpdate.before.csv"
            },
            after = "db/OrderTransactionInfo_ItemsUpdate.before.csv"
    )
    void test_executor_when_postpaidOrder_then_doNotAffectTransactions() throws IOException {
        runTest("processingToProcessing-with-itemsCountDecrementWithDelete-postpaid-withPaymentId.json");
    }

    /**
     * Тест для случая BNPL заказа
     * <p>
     * Случай, когда удалось получить информацию о платеже.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_ChangeFee_ChangeTotal_PaidWithYandex.before" +
                            ".csv",
                    "db/OrderTransactionInfo_ItemsUpdate.before.csv"
            },
            after = "db/OrderTransactionInfo_ItemsUpdateBnpl.after.csv"
    )
    void test_executor_when_BnplWithPaymentInfo_should_updateTransactionInfo() throws IOException {
        runTest("processingToProcessing-with-itemsCountDecrementWithDelete-bnpl-withPaymentId.json");
    }

    /**
     * Тест для случая Предоплата счетом {@link PaymentMethod#B2B_ACCOUNT_PREPAYMENT}, {@link PaymentGoal#ORDER_ACCOUNT_PAYMENT}
     */
    @Test
    @DbUnitDataSet(before = {
            "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_ChangeFee_ChangeTotal_PaidWithYandex.before.csv",
            "db/OrderTransactionInfo_ItemsUpdateB2b.before.csv"
    },
            after = "db/OrderTransactionInfo_ItemsUpdateB2b.after.csv"
    )
    void test_executor_when_B2BPayment_should_updateTransactionInfo() throws IOException {
        runTest("processingToProcessing-b2b-withPaymentId.json");
    }

    private void runTest(String filename) throws IOException {
        mockClientWithResource(filename);
        List<OrderHistoryEvent> events = events("events/" + filename);
        strategy.process(events, DATE_2017_01_01);
    }

    @Override
    public RestTemplate getRestTemplate() {
        return checkouterRestTemplate;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public String getResourcePrefix() {
        return RESOURCE_PREFIX;
    }
}
