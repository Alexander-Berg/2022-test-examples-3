package ru.yandex.direct.core.entity.bs.resync.queue.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncItem;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncQueueStat;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.exception.RollbackException;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static ru.yandex.direct.dbschema.ppc.tables.BsResyncQueue.BS_RESYNC_QUEUE;

@CoreTest
@ExtendWith(SpringExtension.class)
class BsResyncQueueRepositoryGetQueueStatTest {
    private static final String AGE_FILED = "maximumAge";
    private static final int TEST_SHARD = 2;

    @Autowired
    private BsResyncQueueRepository bsResyncQueueRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private final Long priority = nextLong(0, 100);
    private final SoftAssertions softAssertions = new SoftAssertions();

    @Test
    void singleCampaignTest() {
        Collection<BsResyncItem> items = singleton(new BsResyncItem(priority, cid(), 0L, 0L));
        BsResyncQueueStat expected = emptyStat().withCampaignsNum(1L);

        testBase(items, expected);
    }

    @Test
    void singleAdgroupTest() {
        Collection<BsResyncItem> items = singleton(new BsResyncItem(priority, cid(), 0L, id()));
        BsResyncQueueStat expected = emptyStat().withContextsNum(1L);

        testBase(items, expected);
    }

    @Test
    void singleBannerTest() {
        Collection<BsResyncItem> items = singleton(new BsResyncItem(priority, cid(), id(), 0L));
        BsResyncQueueStat expected = emptyStat().withBannersNum(1L);

        testBase(items, expected);
    }

    @Test
    void singleAdgroupAndBannerTest() {
        Collection<BsResyncItem> items = singleton(new BsResyncItem(priority, cid(), id(), id()));
        BsResyncQueueStat expected = emptyStat().withContextsNum(1L).withBannersNum(1L);

        testBase(items, expected);
    }

    @Test
    void campaignAndAdgroupTest() {
        Collection<BsResyncItem> items = asList(new BsResyncItem(priority, cid(), 0L, 0L),
                new BsResyncItem(priority, cid(), 0L, id()));
        BsResyncQueueStat expected = emptyStat().withCampaignsNum(1L).withContextsNum(1L);

        testBase(items, expected);
    }

    @Test
    void campaignAndBannerTest() {
        Collection<BsResyncItem> items = asList(new BsResyncItem(priority, cid(), 0L, 0L),
                new BsResyncItem(priority, cid(), id(), 0L));
        BsResyncQueueStat expected = emptyStat().withCampaignsNum(1L).withBannersNum(1L);

        testBase(items, expected);
    }

    @Test
    void adgroupAndCampaignAndBannerTest() {
        Collection<BsResyncItem> items = asList(new BsResyncItem(priority, cid(), 0L, 0L),
                new BsResyncItem(priority, cid(), id(), id()));
        BsResyncQueueStat expected = emptyStat().withCampaignsNum(1L).withContextsNum(1L).withBannersNum(1L);

        testBase(items, expected);
    }

    @Test
    void twoPrioritiesSmokeTest() {
        Long secondPriority = -15L;
        Collection<BsResyncItem> items = asList(
                new BsResyncItem(priority, cid(), 0L, 0L),
                new BsResyncItem(priority, cid(), id(), id()),
                new BsResyncItem(priority, cid(), id(), 0L),
                new BsResyncItem(secondPriority, cid(), 0L, 0L),
                new BsResyncItem(secondPriority, cid(), id(), id()),
                new BsResyncItem(secondPriority, cid(), 0L, id())
        );
        BsResyncQueueStat expected1 = emptyStat().withCampaignsNum(1L).withContextsNum(1L).withBannersNum(2L);
        BsResyncQueueStat expected2 = emptyStat().withCampaignsNum(1L).withContextsNum(2L).withBannersNum(1L)
                .withPriority(secondPriority);

        testBase(items, expected1, expected2);
    }

    @Test
    void maximumAgeTest() {
        Duration age = Duration.ofMinutes(nextLong(100, Short.MAX_VALUE));
        BsResyncQueueStat expected = emptyStat().withCampaignsNum(0L).withContextsNum(1L).withBannersNum(1L);

        runWithEmptyBsResyncTable(dsl -> {
            dsl.insertInto(BS_RESYNC_QUEUE)
                    .set(BS_RESYNC_QUEUE.PRIORITY, priority)
                    .set(BS_RESYNC_QUEUE.CID, cid())
                    .set(BS_RESYNC_QUEUE.PID, id())
                    .set(BS_RESYNC_QUEUE.BID, id())
                    .set(BS_RESYNC_QUEUE.SEQUENCE_TIME, LocalDateTime.now().minus(age))
                    .execute();

            List<BsResyncQueueStat> queueStat = bsResyncQueueRepository.getQueueStat(dsl);
            softAssertions.assertThat(queueStat)
                    .usingElementComparatorIgnoringFields(AGE_FILED)
                    .contains(expected);
            softAssertions.assertThat(queueStat)
                    .are(ageBetween(age.minusSeconds(10), age.plusSeconds(10)));

            softAssertions.assertAll();
        });
    }

    private void testBase(Collection<BsResyncItem> items, BsResyncQueueStat... expected) {
        runWithEmptyBsResyncTable(dsl -> {
            bsResyncQueueRepository.addToResync(dsl, items);

            List<BsResyncQueueStat> queueStat = bsResyncQueueRepository.getQueueStat(dsl);
            softAssertions.assertThat(queueStat)
                    .usingElementComparatorIgnoringFields(AGE_FILED)
                    .contains(expected);
            softAssertions.assertThat(queueStat)
                    .are(ageInGap());

            softAssertions.assertAll();
        });
    }

    private void runWithEmptyBsResyncTable(Consumer<DSLContext> test) {
        try {
            dslContextProvider.ppcTransaction(TEST_SHARD, configuration -> {
                DSLContext dsl = configuration.dsl();

                dsl.deleteFrom(BS_RESYNC_QUEUE).execute();

                test.accept(dsl);

                throw new RollbackException();
            });
        } catch (RollbackException ignored) {
        }
    }

    private BsResyncQueueStat emptyStat() {
        return new BsResyncQueueStat()
                .withCampaignsNum(0L)
                .withBannersNum(0L)
                .withContextsNum(0L)
                .withPriority(priority);
    }

    private static Condition<BsResyncQueueStat> ageInGap() {
        return ageBetween(Duration.ZERO, Duration.ofSeconds(10));
    }

    private static Condition<BsResyncQueueStat> ageBetween(Duration from, Duration to) {
        return new Condition<>(o -> {
            Duration maximumAge = o.getMaximumAge();
            return maximumAge.compareTo(from) >= 0 && maximumAge.compareTo(to) <= 0;
        }, "maximumAge in [" + from + " , " + to + "]");
    }

    private static Long cid() {
        return nextLong(1, Integer.MAX_VALUE);
    }

    private static Long id() {
        return nextLong(1, Long.MAX_VALUE);
    }
}
