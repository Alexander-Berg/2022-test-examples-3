package ru.yandex.market.bidding.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.market.bidding.engine.storage.BasicOracleStorage;
import ru.yandex.market.bidding.service.AdminService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.when;

/**
 * @author snoop
 */
@RunWith(MockitoJUnitRunner.class)
public class DeepDeadBidsCleanerTest {

    private static final Triple<Boolean, Integer, Integer> DEAD_CATEGORY_BID_PARAMETERS = ImmutableTriple.of(true, 0, 0);

    @Mock
    private BasicOracleStorage storage;
    @Mock
    private Tasks tasks;
    @Mock
    private BasicBiddingEngine engine;
    @Mock
    private AdminService adminService;

    private TestCleaner cleaner;

    @Before
    public void setUp() throws Exception {
        cleaner = new TestCleaner(tasks, storage, engine, adminService);
        Set<Long> shopsWithDeadCategoryBids = Sets.newHashSet(1L, 2L, 3L);
        when(storage.findShopsWithDeletedCategoryBids(anySet())).thenReturn(shopsWithDeadCategoryBids);
        Map<Long, Pair<Integer, Integer>> shopsWithDeadOfferBids = new HashMap<Long, Pair<Integer, Integer>>() {
            {
                put(2L, Pair.of(1_000, 200));
                put(7L, Pair.of(10_000, 1_000));
                put(8L, Pair.of(100_000, 1_000));
            }
        };
        when(storage.findShopsWithDeadOfferBids(anySet())).thenReturn(shopsWithDeadOfferBids);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void do_cleaning_all_shops() throws Exception {
        Map<Long, Triple<Boolean, Integer, Integer>> expectedSubmissions =
                new HashMap<Long, Triple<Boolean, Integer, Integer>>() {
                    {
                        put(1L, DEAD_CATEGORY_BID_PARAMETERS);
                        put(2L, Triple.of(true, 200, 1_000));
                        put(3L, DEAD_CATEGORY_BID_PARAMETERS);
                        put(7L, Triple.of(false, 1_000, 10_000));
                        put(8L, Triple.of(false, 1_000, 100_000));
                    }
                };
        cleaner.doCleaning(Collections.emptySet());
        assertEquals(expectedSubmissions, cleaner.getSubmissions());
    }

    @Test
    public void do_cleaning_only_shop_with_dead_category_bids() throws Exception {
        cleaner.doCleaning(Collections.singleton(1L));
        Map<Long, Triple<Boolean, Integer, Integer>> expectedSubmissions =
                Collections.singletonMap(1L, DEAD_CATEGORY_BID_PARAMETERS);
        assertEquals(expectedSubmissions, cleaner.getSubmissions());
    }

    @Test
    public void do_cleaning_only_shop_with_offer_category_bids() throws Exception {
        cleaner.doCleaning(Collections.singleton(7L));
        Map<Long, Triple<Boolean, Integer, Integer>> expectedSubmissions =
                Collections.singletonMap(7L, Triple.of(false, 1_000, 10_000));
        assertEquals(expectedSubmissions, cleaner.getSubmissions());
    }

    @Test
    public void do_cleaning_only_shop_with_all_type_of_bids() throws Exception {
        cleaner.doCleaning(Collections.singleton(2L));
        Map<Long, Triple<Boolean, Integer, Integer>> expectedSubmissions =
                Collections.singletonMap(2L, Triple.of(true, 200, 1_000));
        assertEquals(expectedSubmissions, cleaner.getSubmissions());
    }

    @Test
    public void do_cleaning_shops_without_dead_bids() throws Exception {
        cleaner.doCleaning(Collections.singleton(11L));
        assertTrue(cleaner.getSubmissions().isEmpty());
    }

    private static class TestCleaner extends DeepDeadBidsCleaner {

        private Map<Long, Triple<Boolean, Integer, Integer>> submissions = new HashMap<>();

        TestCleaner(Tasks tasks, BasicOracleStorage storage, BasicBiddingEngine engine, AdminService adminService) {
            super(tasks, storage, engine, adminService);
        }

        @Override
        protected void submitTask(long shopId, boolean needCategoryCleanup, int deadOfferBidsCount,
                                  int totalOfferBidsCount) throws InterruptedException {
            submissions.put(shopId, Triple.of(needCategoryCleanup, deadOfferBidsCount, totalOfferBidsCount));
        }

        public Map<Long, Triple<Boolean, Integer, Integer>> getSubmissions() {
            return submissions;
        }
    }
}