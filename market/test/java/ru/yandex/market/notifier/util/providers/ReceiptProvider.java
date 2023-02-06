package ru.yandex.market.notifier.util.providers;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;

public class ReceiptProvider {

    public static Receipt getReceipt(ReceiptType receiptType) {
        Receipt receipt = new Receipt();

        receipt.setId(123L);
        receipt.setType(receiptType);
        receipt.setPaymentId(456L);
        receipt.setRefundId(789L);
        receipt.setStatus(ReceiptStatus.NEW);

        receipt.setCreatedAt((new Date(111111111111000L)).toInstant());
        receipt.setStatusUpdatedAt((new Date(111111111133000L)).toInstant());

        receipt.setItems(Collections.singletonList(getReceiptItem()));
        return receipt;
    }

    public static ReceiptItem getReceiptItem() {
        ReceiptItem item = new ReceiptItem();
        item.setOrderId(1L);
        item.setReceiptId(123L);
        item.setItemId(2L);
        item.setItemServiceId(4L);
        item.setDeliveryId(3L);
        item.setItemTitle("top item");
        item.setCount(666);
        item.setPrice(new BigDecimal("322"));
        item.setAmount(new BigDecimal("123456"));
        return item;
    }
}
