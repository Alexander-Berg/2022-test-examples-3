package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.storage.receipt.ReceiptDao;
import ru.yandex.market.checkout.common.tasks.ZooTask;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author gelvy
 * Created on: 09.03.2021
 **/
public class CashbackErpEventExportTest extends AbstractPaymentTestBase {

    @Autowired
    private ZooTask firstPartyOrderEventExportTask;
    @Autowired
    private ReceiptDao receiptDao;
    @Autowired
    private JdbcTemplate erpJdbcTemplate;

    @Test
    public void subtractCashbackFromItemTest() throws Exception {
        orderServiceTestHelper.createUnpaidBlue1POrder(order -> {
            order.getItems().forEach(item -> {
                item.setPrice(item.getBuyerPrice());
            });
        });
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.markupPayment(createDefaultPartitions());
        paymentTestHelper.notifyPaymentSucceeded(receiptId, false);
        paymentTestHelper.clearPayment();

        //Проверка чтения чеков из базы (https://st.yandex-team.ru/MARKETCHECKOUT-10253)
        Map<Long, ReceiptItem> map = receiptDao.fetchReceiptsItemForOrderItems(singletonList(order().getId()),
                ReceiptType.INCOME, PaymentGoal.ORDER_PREPAY);
        ReceiptItem receiptItemFromDB = map.values().iterator().next();
        assertThat(receiptItemFromDB.getPartitions(), notNullValue());

        firstPartyOrderEventExportTask.runOnce();
        verifyCashbackSubtracted(receiptItemFromDB.getItemId(),
                receiptItemFromDB.getPrice().longValue(),
                receiptItemFromDB.amountByAgent(PaymentAgent.YANDEX_CASHBACK).longValue());
    }

    private PaymentPartitions createDefaultPartitions() {
        BigDecimal total = order.get().getBuyerItemsTotal();
        PaymentPartitions partitions = new PaymentPartitions();
        IncomingPaymentPartition cashbackPart = new IncomingPaymentPartition(PaymentAgent.YANDEX_CASHBACK,
                total.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_EVEN));
        partitions.setPartitions(singletonList(cashbackPart));
        return partitions;
    }

    private void verifyCashbackSubtracted(long itemId, long price, long cashback) {
        Long res = erpJdbcTemplate.queryForObject(
                "SELECT PRICE FROM COOrderItem WHERE ITEM_ID=? LIMIT 1",
                (rs, rowNum) -> rs.getLong(1),
                itemId
        );

        long expectedPrice = price - cashback;
        assertThat(res, equalTo(expectedPrice));
    }
}
