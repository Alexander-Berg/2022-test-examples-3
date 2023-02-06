package ru.yandex.market.bidding.engine;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.bidding.engine.model.OfferBid;

import static java.lang.Math.max;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

public class BasicPartnerFullScanTest extends BasicShopTestCase {

    public static final int SAMPLE_BIDS_COUNT = 10;
    public static final AllPredicate ALL_PREDICATE = new AllPredicate();
    public static final DummyTransformer DUMMY_TRANSFORMER = new DummyTransformer();
    public static final NonePredicate NONE_PREDICATE = new NonePredicate();
    public static final OddPredicate ODD_PREDICATE = new OddPredicate();
    public static final CountAccumulator COUNT_ACCUMULATOR = new CountAccumulator();

    private BasicPartner shop;

    @Before
    public void setUp() throws Exception {
        shop = sampleBidsForFulScanTests();

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testFullScanAll() {
        List<OfferBid> bids = shop.fullScan(
                ALL_PREDICATE, DUMMY_TRANSFORMER,
                true, -1, -1);

        assertEquals(SAMPLE_BIDS_COUNT, bids.size());
        for (int i = 0; i < SAMPLE_BIDS_COUNT; i++) {
            assertEquals("bid" + i, bids.get(i).id());
        }
    }

    @Test
    public void testFullScanAllWithReduce() {
        int count = shop.fullScan(
                ALL_PREDICATE, COUNT_ACCUMULATOR,
                true, -1, -1);

        assertEquals(SAMPLE_BIDS_COUNT, count);
    }

    /**
     * Ставки считаются с единицы. То есть, 10 ставок в тестовых данных имеют номера с 1 по 10, а не с 0 по 9.
     * From 1 - должны быть все ставки
     * From 11 - ни одной
     */
    @Test
    public void testFullScanFrom() {
        for (int from = 1; from <= 11; from++) {
            testFullScanFrom(from);
        }
    }

    protected void testFullScanFrom(int from) {
        List<OfferBid> bids = shop.fullScan(
                ALL_PREDICATE, DUMMY_TRANSFORMER,
                true, from, -1);

        String msg = format("from=%d;total=%d", from, SAMPLE_BIDS_COUNT);
        assertEquals(msg, SAMPLE_BIDS_COUNT - from + 1, bids.size());
    }

    /**
     * Ставки считаются с единицы. То есть, 10 ставок в тестовых данных имеют номера с 1 по 10, а не с 0 по 9.
     * To 0 - ни одной
     * To 10 - все
     */
    @Test
    public void testFullScanTo() {
        for (int to = 0; to <= 10; to++) {
            testFullScanTo(to);
        }
    }

    protected void testFullScanTo(int to) {
        List<OfferBid> bids = shop.fullScan(
                ALL_PREDICATE, DUMMY_TRANSFORMER,
                true, -1, to);

        String msg = format("to=%d;total=%d", to, SAMPLE_BIDS_COUNT);
        assertEquals(msg, to, bids.size());
    }

    @Test
    public void testFullScanFromTo() {
        for (int from = 1; from <= 11; from++) {
            for (int to = 0; to <= 10; to++) {
                testFullScanFromTo(from, to);
            }
        }
    }

    protected void testFullScanFromTo(int from, int to) {
        List<OfferBid> bids = shop.fullScan(
                ALL_PREDICATE, DUMMY_TRANSFORMER,
                true, from, to);

        String msg = format("from=%d;to=%d;total=%d", from, to, SAMPLE_BIDS_COUNT);
        assertEquals(msg, max(to - from + 1, 0), bids.size());
    }

    @Test
    public void testFullScanNone() {
        List<OfferBid> bids = shop.fullScan(
                NONE_PREDICATE, DUMMY_TRANSFORMER,
                true, -1, -1);

        assertEquals(0, bids.size());
    }

    @Test
    public void testFullScanSelectOdd() {
        List<OfferBid> bids = shop.fullScan(
                ODD_PREDICATE, DUMMY_TRANSFORMER,
                true, -1, -1);
        assertEquals(SAMPLE_BIDS_COUNT / 2 + SAMPLE_BIDS_COUNT % 2, bids.size());

    }


    protected BasicPartner sampleBidsForFulScanTests() {
        BasicPartner.Builder shopBuilder = shop(1L);
        for (int i = 0; i < SAMPLE_BIDS_COUNT; i++) {
            shopBuilder.addOfferBid(titleBid("bid" + i, 0, null));
        }
        return shopBuilder.build();
    }

    protected static class AllPredicate implements Predicate<OfferBid> {
        @Override
        public boolean test(OfferBid input) {
            return true;
        }
    }

    protected static class NonePredicate implements Predicate<OfferBid> {
        @Override
        public boolean test(OfferBid input) {
            return false;
        }
    }

    protected static class OddPredicate implements Predicate<OfferBid> {
        @Override
        public boolean test(OfferBid input) {
            String idStr = String.valueOf(input.id());
            Integer number = Integer.valueOf(idStr.substring(3));
            return number % 2 == 0;
        }
    }

    protected static class DummyTransformer implements Function<OfferBid, OfferBid> {
        @Override
        public OfferBid apply(OfferBid input) {
            return input;
        }
    }

    private static class CountAccumulator implements BasicPartner.Accumulator<OfferBid, Integer> {
        private int counter = 0;

        @Override
        public void append(OfferBid offerBid) {
            counter++;
        }

        @Override
        public Integer result() {
            return counter;
        }
    }

}
