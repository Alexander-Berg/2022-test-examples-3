package ru.yandex.market.checkout.checkouter.storage.payment;

import java.util.Random;

public class StorageRandomPaymentSequences implements PaymentSequences {
    private final Random random = new Random();

    @Override
    public long getNextPaymentId() {
        return random.nextLong();
    }

    @Override
    public long getNextHistoryId() {
        return random.nextLong();
    }

    @Override
    public long getNextRefundId() {
        return random.nextLong();
    }

    @Override
    public long getNextRefundItemId() {
        return random.nextLong();
    }

    @Override
    public long getNextRefundHistoryId() {
        return random.nextLong();
    }

    @Override
    public long getNextServiceProductCacheId() {
        return random.nextLong();
    }
}
