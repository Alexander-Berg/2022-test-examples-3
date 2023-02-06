package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.MockClientHttpRequestFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link OrderPaymentTransactionHelper}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class OrderPaymentTransactionHelperTest extends FunctionalTest {

    private static final String RESOURCE_PREFIX = "ru/yandex/market/billing/checkout/resources/checkouter_response/refunds/";
    private static final LocalDate localDate = LocalDate.of(2018, 12, 12);
    private static final Date DATE_2018_12_12 = new Date(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    private static final long ORDER_ID_101 = 101L;
    private static final long SHOP_ID_774 = 774L;
    private static final long PAYMENT_ID_666 = 666L;
    private static final String BALANCE_ORDER_ID = "balance_order_id_100500";

    @Autowired
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private EventProcessorSupportFactory eventProcessorSupportFactory;

    @Mock
    private Order orderAfter;

    private EventContext context;

    @BeforeEach
    void beforeEach() {
        when(orderAfter.getId())
                .thenReturn(ORDER_ID_101);

        when(orderAfter.getBalanceOrderId())
                .thenReturn(BALANCE_ORDER_ID);

        when(orderAfter.getShopId())
                .thenReturn(SHOP_ID_774);

        when(orderAfter.getPaymentId()).thenReturn(PAYMENT_ID_666);
        Buyer buyer = mock(Buyer.class);
        when(buyer.getUid()).thenReturn(900L);
        when(orderAfter.getBuyer()).thenReturn(buyer);

        final OrderHistoryEvent event = new OrderHistoryEvent();
        event.setOrderAfter(orderAfter);
        event.setFromDate(DATE_2018_12_12);

        final EventProcessorSupport support = eventProcessorSupportFactory.createSupport();
        context = new EventContext(event, support, DATE_2018_12_12);
    }

    @DisplayName("Сохранение refund'а")
    @Test
    @DbUnitDataSet(
            before = "db/OrderPaymentTransactionHelperTest.refunds.before.csv",
            after = "db/OrderPaymentTransactionHelperTest.refunds.after.csv")
    void test_registerRefund() throws IOException {
        mockCheckouterClientWithResource("refund_1_item.json");
        OrderPaymentTransactionHelper.registerRefund(context, PaymentGoal.SUBSIDY);
    }

    @DisplayName("Сохранение refund'а с shop_manager_id = null")
    @Test
    @DbUnitDataSet(
            before = "db/OrderPaymentTransactionHelperTest.refunds.before.csv",
            after = "db/OrderPaymentTransactionHelperTest.refunds.after.csv")
    void test_registerRefund_noShopManagerId() throws IOException {
        mockCheckouterClientWithResource("refund_1_item_no_manager_id.json");
        OrderPaymentTransactionHelper.registerRefund(context, PaymentGoal.SUBSIDY);
    }

    @DisplayName("Smoke тест на перезапись refund'а")
    @Test
    @DbUnitDataSet(
            before = "db/OrderPaymentTransactionHelperTest.refunds_rewrite.before.csv",
            after = "db/OrderPaymentTransactionHelperTest.refunds_rewrite.after.csv"
    )
    void test_registerRefund_rewrite() throws IOException {
        mockCheckouterClientWithResource("refund_1_item_no_manager_id.json");
        OrderPaymentTransactionHelper.registerRefund(context, PaymentGoal.SUBSIDY);
    }

    @DisplayName("Сохранение refund'а с cession=true")
    @Test
    @DbUnitDataSet(
            before = "db/OrderPaymentTransactionHelperTest.refunds_cession.before.csv",
            after = "db/OrderPaymentTransactionHelperTest.refunds_cession.after.csv")
    void test_registerCessionRefund() throws IOException {
        mockCheckouterClientWithResource("refund_1_item_cession.json");
        OrderPaymentTransactionHelper.registerRefund(context, PaymentGoal.TINKOFF_CREDIT);
    }

    @DisplayName("Сохранение refund'а с полем usingCashRefundService")
    @Test
    @DbUnitDataSet(
            before = "db/OrderPaymentTransactionHelperTest.refunds_613.before.csv",
            after = "db/OrderPaymentTransactionHelperTest.refunds_613.after.csv")
    void test_registerRefundWithUsingCashRefundServiceField() throws IOException {
        mockCheckouterClientWithResource("refund_1_item_613.json");
        OrderPaymentTransactionHelper.registerRefund(context, PaymentGoal.TINKOFF_CREDIT);
    }

    @DisplayName("Сохранение refund'а с serviceFeePartitions")
    @Test
    @DbUnitDataSet(
            before = "db/OrderPaymentTransactionHelperTest.refunds_service_fee_partitions.before.csv",
            after = "db/OrderPaymentTransactionHelperTest.refunds_service_fee_partitions.after.csv")
    void test_registerRefundWithServiceFeePartitions() throws IOException {
        mockCheckouterClientWithResource("refund_with_service_fee_partitions.json");
        OrderPaymentTransactionHelper.registerRefund(context, PaymentGoal.TINKOFF_CREDIT);
    }

    @DisplayName("Сохранение refund'а с обновлением существующих serviceFeePartitions")
    @Test
    @DbUnitDataSet(
            before = "db/OrderPaymentTransactionHelperTest.refunds_existing_service_fee_partitions.before.csv",
            after = "db/OrderPaymentTransactionHelperTest.refunds_existing_service_fee_partitions.after.csv")
    void test_registerRefundWithExistingServiceFeePartitions() throws IOException {
        mockCheckouterClientWithResource("refund_with_existing_service_fee_partitions.json");
        OrderPaymentTransactionHelper.registerRefund(context, PaymentGoal.TINKOFF_CREDIT);
    }

    @DisplayName("Сохранение payment'а с признаком я.карты")
    @Test
    @DbUnitDataSet(
            before = "db/OrderPaymentTransactionHelperTest.payments_ya_card.before.csv",
            after = "db/OrderPaymentTransactionHelperTest.payments_ya_card.after.csv")
    void testYaCardPropertiesPayment() throws IOException {
        mockCheckouterClientWithResource("payment_ya_card.json");
        OrderPaymentTransactionHelper.registerPayment(context, PaymentGoal.TINKOFF_CREDIT);
    }

    @DisplayName("Сохранение refund'а с признаков я.карты")
    @Test
    @DbUnitDataSet(
            before = "db/OrderPaymentTransactionHelperTest.refunds_ya_card.before.csv",
            after = "db/OrderPaymentTransactionHelperTest.refunds_ya_card.after.csv")
    void testYaCardPropertiesRefund() throws IOException {
        mockCheckouterClientWithResource("refund_1_item_ya_card.json");
        OrderPaymentTransactionHelper.registerRefund(context, PaymentGoal.TINKOFF_CREDIT);
    }

    private void mockCheckouterClientWithResource(String fileName) throws IOException {
        checkouterRestTemplate.setRequestFactory(new MockClientHttpRequestFactory(
                new ClassPathResource(RESOURCE_PREFIX + fileName)
        ));
    }
}
