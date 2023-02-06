package ru.yandex.market.checkout.checkouter.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.RefundItem;
import ru.yandex.market.checkout.checkouter.pay.RefundItems;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundableItems;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.CashParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

@Disabled("использовалось для миграции данных со старого кода на новый, староый код выпиливается")
public class FillPurchaseTokenCommandTest extends AbstractPaymentTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FillPurchaseTokenCommandTest.class);

    @Autowired
    private FillPurchaseTokenCommand command;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;

    @Test
    public void testPaymentsMigrate() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();

        update("update payment set purchase_token = null");
        Assertions.assertNotEquals(0L, queryLong("select count(1) from payment " +
                "where purchase_token is null"));
        executeCommand("payments");
        Assertions.assertEquals(0L, queryLong("select count(1) from payment " +
                "where purchase_token is null"));
    }

    @Test
    public void testRefundsMigrate() {
        makeRefund();

        update("update refund set cash_refund_purchase_token = null");
        Assertions.assertNotEquals(0L, queryLong("select count(1) from refund " +
                "where trust_refund_id is null and " +
                "cash_refund_purchase_token is null"));
        executeCommand("cashRefunds");
        Assertions.assertEquals(0L, queryLong("select count(1) from refund" +
                " where trust_refund_id is null and " +
                "cash_refund_purchase_token is null"));
    }


    private void makeRefund() {
        Parameters parameters = CashParametersProvider.createCashParameters(true);
        Order order = orderCreateHelper.createOrder(parameters);

        shopService.updateMeta(123, ShopSettingsHelper.createCustomNewPrepayMeta(123));
        shopService.updateMeta(FulfilmentProvider.FF_SHOP_ID, ShopSettingsHelper.createCustomNewPrepayMeta(
                FulfilmentProvider.FF_SHOP_ID.intValue()));

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        RefundableItems refundableItems = refundService.getRefundableItems(order);
        List<RefundItem> refundItemsList = refundableItems.getItems().stream()
                .map(RefundItem::of)
                .collect(Collectors.toList());
        refundableItems.getItemServices().stream()
                .map(RefundItem::of)
                .forEach(refundItemsList::add);

        RefundItems refundItems = new RefundItems(refundItemsList);
        refundService.createRefund(
                order.getId(),
                null,
                "Возвращаем деньги за сломанный айфон",
                new ClientInfo(ClientRole.REFEREE, 135135L),
                RefundReason.USER_RETURNED_ITEM,
                PaymentGoal.ORDER_POSTPAY,
                false,
                refundItems,
                false,
                null,
                false
        );
    }

    private void update(String sql) {
        transactionTemplate.execute(st -> masterJdbcTemplate.update(sql));
    }

    private long queryLong(String sql) {
        return masterJdbcTemplate.queryForObject(sql, Long.class);
    }

    private void executeCommand(String name) {
        String[] arguments = new String[]{name, "0"};
        CommandInvocation invocation = new CommandInvocation("fill-purchase-token", arguments,
                Collections.emptyMap());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        TestTerminal terminal = new TestTerminal(new ByteArrayInputStream(new byte[0]), output);
        command.executeCommand(invocation, terminal);
        terminal.getWriter().flush();

        LOG.info("Command output: \n{}", output.toString().trim());
    }

}
