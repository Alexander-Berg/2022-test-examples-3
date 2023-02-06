package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.refund.ItemsRefundStrategy;
import ru.yandex.market.checkout.checkouter.pay.refund.SubsidyRefundStrategy;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.common.util.CollectorUtils;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberGreaterThan;

/**
 * @author Nikolai Iusiumbeli
 * date: 20/02/2018
 */
public class SubsidyReceiptsTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "PROMO-CODE";

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private FulfillmentConfigurer fulfillmentConfigurer;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ItemsRefundStrategy itemsRefundStrategy;
    @Autowired
    private SubsidyRefundStrategy subsidyRefundStrategy;

    @Test
    public void testGenerateSubsidyReceipt() {
        Parameters parameters =
                BlueParametersProvider.defaultBlueOrderParametersWithItems(OrderItemProvider.getAnotherOrderItem());
        parameters.setupPromo(PROMO_CODE);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        Collection<QueuedCall> queuedCalls =
                queuedCallService.findQueuedCalls(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        assertEquals(1, queuedCalls.size());
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
        ReceiptItemValidator receiptItemValidator = new ReceiptItemValidator(order);
        long incomeSubsidyReceiptCount = receiptService.findByOrder(order.getId(), ReceiptStatus.GENERATED).stream()
                .filter(r -> r.getType() == ReceiptType.INCOME)
                .filter(r -> paymentService.findPayment(r.getPaymentId(), ClientInfo.SYSTEM).getType() ==
                        PaymentGoal.SUBSIDY)
                .peek(r -> r.getItems().forEach(
                        receiptItemValidator::validate
                ))
                .count();

        assertThat(incomeSubsidyReceiptCount, numberEqualsTo(1L));
    }

    @Test
    public void testGenerateSubsidyRefundReceipt() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setupPromo(PROMO_CODE);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        payHelper.refundAllOrderItems(order);
        trustMockConfigurer.mockCheckBasket(buildPostAuth(), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("First check passed");
        });
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        ReceiptItemValidator receiptItemValidator = new ReceiptItemValidator(order);

        long incomeSubsidyReceiptCount = receiptService.findByOrder(order.getId(), ReceiptStatus.GENERATED).stream()
                .filter(r -> r.getType() == ReceiptType.INCOME_RETURN)
                .filter(r -> r.getRefundId() != null)
                .peek(r -> r.getItems().forEach(
                        receiptItemValidator::validate
                ))
                .map(r -> refundService.getRefund(r.getRefundId()).getPaymentId())
                .filter(Objects::nonNull)
                .filter(p -> paymentService.findPayment(p, ClientInfo.SYSTEM).getType() == PaymentGoal.SUBSIDY)
                .count();

        assertThat(incomeSubsidyReceiptCount, numberGreaterThan(0L));
    }

    /**
     * Выполняет проверку строки в чеке: количество и суммы исходя.
     */
    private class ReceiptItemValidator {

        private final Map<Long, OrderItem> orderItemById;

        ReceiptItemValidator(Order order) {
            orderItemById = orderService.getOrder(order.getId()).getItems().stream()
                    .collect(CollectorUtils.index(OrderItem::getId));
        }

        private void validate(ReceiptItem receiptItem) {
            OrderItem orderItem = orderItemById.get(receiptItem.getItemId());
            BigDecimal singleItemSubsidy = orderItem.getPrices().getSubsidy();

            assertNotNull(orderItem);
            assertNotNull(orderItem.getCount());
            assertEquals((long) orderItem.getCount(), receiptItem.getCount());

            assertThat(receiptItem.getPrice(), comparesEqualTo(singleItemSubsidy));
            assertThat(receiptItem.getAmount(),
                    comparesEqualTo(singleItemSubsidy
                            .multiply(BigDecimal.valueOf(orderItem.getCount()))
                    )
            );
        }
    }

}
