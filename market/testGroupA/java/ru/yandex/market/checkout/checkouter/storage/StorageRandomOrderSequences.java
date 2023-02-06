package ru.yandex.market.checkout.checkouter.storage;

import java.util.Iterator;
import java.util.Random;

import ru.yandex.market.checkout.storage.StorageSequence;

/**
 * @author mmetlov
 */
public class StorageRandomOrderSequences implements OrderSequences {
    private final Random random = new Random();

    @Override
    public long getNextOrderId() {
        return random.nextLong();
    }

    @Override
    public long getNextDeliveryId() {
        return random.nextLong();
    }

    @Override
    public long getNextAddressId() {
        return random.nextLong();
    }

    @Override
    public long getNextBuyerId() {
        return random.nextLong();
    }

    @Override
    public long getNextHistoryId() {
        return random.nextLong();
    }

    @Override
    public long getNextTrackId() {
        return random.nextLong();
    }

    @Override
    public StorageSequence getEventIdSequence() {
        return newRandomSequence();
    }

    @Override
    public long getNextParcelId() {
        return random.nextLong();
    }

    @Override
    public long getNextTrackCheckpointId() {
        return random.nextLong();
    }

    @Override
    public long getNextTrackHistoryId() {
        return random.nextLong();
    }

    @Override
    public long getNextTrackCheckpointHistoryId() {
        return random.nextLong();
    }

    @Override
    public long getNextOrderPromoId() {
        return random.nextLong();
    }

    @Override
    public StorageSequence getOrderItemSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getItemServiceSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getOrderPromoSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getOrderDeliveryPromoSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getOrderItemPromoSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getTrackCheckpointSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getParcelBoxSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getParcelBoxItemSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getShopScheduleLineSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getQueuedCallSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getChangeRequestSequence() {
        return newRandomSequence();
    }

    @Override
    public StorageSequence getRefundItemSequence() {
        return newRandomSequence();
    }

    private StorageSequence newRandomSequence() {
        return new StorageSequence() {
            @Override
            public long nextValue() {
                return random.nextLong();
            }

            @Override
            public Iterable<Long> nextValues(int i) {
                return () -> new Iterator<Long>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Long next() {
                        return random.nextLong();
                    }
                };
            }
        };
    }
}
