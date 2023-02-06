package ru.yandex.market.bidding.engine.model;

import ru.yandex.market.bidding.model.Place;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 25.11.15
 * Time: 16:41
 */
class FatBid {
    private final Slot[] slots = new Slot[Place.ALL.size()];

    FatBid() {
    }

    FatBid(BidBuilder builder) {
        for (Place place : Place.values()) {
            set(place, newSlot(builder.getPlaceBid(place)));
        }
    }

    static Slot newSlot() {
        return new Slot();
    }

    static Slot newSlot(int value) {
        return new Slot((short) value);
    }

    static Slot newSlot(BidBuilder.PlaceBid bid) {
        return bid != null ? new Slot(bid) : null;
    }

    void set(Place place, Slot slot) {
        if (slot != null) {
            slots[place.ordinal()] = slot;
        }
    }

    void add(Place place) {
        set(place, newSlot());
    }

    void add(Place place, int value) {
        set(place, newSlot(value));
    }

    Slot get(Place place) {
        return slots[place.ordinal()];
    }

    static class Slot {
        private final short value;
        private final int mtime;
        private final byte status;
        private final short pvalue;
        private final int ptime;

        Slot() {
            this((short) 0);
        }

        Slot(short value) {
            this(value, 0, (byte) 0, (short) 0, 0);
        }

        Slot(BidBuilder.PlaceBid bid) {
            this(bid.value(), bid.modTime(), (byte) bid.status().getCode(), bid.pubValue(), bid.pubTime());
        }

        Slot(short value, int mtime, byte status, short pvalue, int ptime) {
            this.value = value;
            this.mtime = mtime;
            this.status = status;
            this.pvalue = pvalue;
            this.ptime = ptime;
        }

        public short value() {
            return value;
        }

        public int mtime() {
            return mtime;
        }

        public byte status() {
            return status;
        }

        public short pvalue() {
            return pvalue;
        }

        public int ptime() {
            return ptime;
        }
    }
}
