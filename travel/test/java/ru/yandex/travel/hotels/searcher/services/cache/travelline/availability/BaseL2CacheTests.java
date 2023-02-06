package ru.yandex.travel.hotels.searcher.services.cache.travelline.availability;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Tuple3;
import ru.yandex.travel.commons.proto.TJson;
import ru.yandex.travel.hotels.proto.TInventoryList;
import ru.yandex.travel.hotels.proto.TVersionList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@Ignore
public abstract class BaseL2CacheTests {
    protected L2Cache cache;

    private CachedHotelInventory buildInventory(String id, long version) {
        return CachedHotelInventory.builder()
                .hotelId(id)
                .version(version)
                .inventory(TInventoryList.newBuilder().build())
                .actualizationTimestamp(Instant.now())
                .build();
    }

    private CachedOfferAvailability buildAvailability(String id, LocalDate checkin, LocalDate checkout) {
        return CachedOfferAvailability.builder()
                .hotelId(id)
                .checkinDate(checkin)
                .checkoutDate(checkout)
                .availabilityResponse(TJson.newBuilder().build())
                .versions(TVersionList.newBuilder().build())
                .actualizationTimestamp(Instant.now())
                .build();
    }


    @Before
    public abstract void prepare();

    @Test
    public void testSimplePopulateAndGetInventory() {
        cache.putInventory(buildInventory("1", 1L)).join();
        cache.putInventory(buildInventory("2", 1L)).join();
        cache.putInventory(buildInventory("3", 1L)).join();
        cache.putInventory(buildInventory("1", 2L)).join();
        cache.putInventory(buildInventory("2", 2L)).join();
        cache.putInventory(buildInventory("2", 3L)).join();

        assertThat(cache.getInventory("1").join().getHotelId()).isEqualTo("1");
        assertThat(cache.getInventory("1").join().getVersion()).isEqualTo(2L);
        assertThat(cache.getInventory("2").join().getHotelId()).isEqualTo("2");
        assertThat(cache.getInventory("2").join().getVersion()).isEqualTo(3L);
        assertThat(cache.getInventory("3").join().getHotelId()).isEqualTo("3");
        assertThat(cache.getInventory("3").join().getVersion()).isEqualTo(1L);
        assertThat(cache.getInventory("4").join()).isNull();

        assertThat(cache.getInventoryVersions().join()).isEqualTo(
                Map.of(
                        "1", 2L,
                        "2", 3L,
                        "3", 1L
                )
        );
    }

    @Test
    public void testListInventoryByActualization() throws InterruptedException {
        cache.putInventory(buildInventory("1", 1L)).join();
        cache.putInventory(buildInventory("2", 1L)).join();
        Thread.sleep(1);
        Instant now = Instant.now();
        cache.putInventory(buildInventory("3", 1L)).join();
        cache.putInventory(buildInventory("4", 1L)).join();
        var res = cache.listHotelsActualizedBefore(now).join();
        assertThat(res).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void testListAvailabilitiesByCheckout() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        cache.putAvailability(buildAvailability("1", yesterday, today)).join();
        cache.putAvailability(buildAvailability("2", yesterday, today)).join();
        cache.putAvailability(buildAvailability("3", today, tomorrow)).join();
        var res = cache.listAvailabilityKeysForOffersBeforeDate(tomorrow).join();
        assertThat(res).hasSize(2);
        assertThat(res).extracting(Tuple3::get1).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void testDeleteInventories() throws InterruptedException {
        cache.putInventory(buildInventory("1", 1L)).join();
        cache.putInventory(buildInventory("2", 1L)).join();
        cache.removeInventories(List.of("1", "2")).join();
        Thread.sleep(1);
        Instant now = Instant.now();
        var res = cache.listHotelsActualizedBefore(now).join();
        assertThat(res).isEmpty();
    }

    @Test
    public void testDeleteAvailabilities() throws InterruptedException {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        cache.putAvailability(buildAvailability("1", yesterday, today)).join();
        cache.putAvailability(buildAvailability("2", yesterday, today)).join();
        cache.putAvailability(buildAvailability("3", today, tomorrow)).join();
        cache.removeAvailabilities(List.of(
                Tuple3.tuple("1", yesterday, today),
                Tuple3.tuple("2", yesterday, today)
        )).join();
        var res = cache.listAvailabilityKeysForOffersBeforeDate(tomorrow).join();
        assertThat(res).isEmpty();
    }

    @Test
    public void testSimplePopulateAndGetAvailability() {
        LocalDate now = LocalDate.now();
        LocalDate tomorrow = now.plusDays(1);
        cache.putAvailability(buildAvailability("1", now, tomorrow)).join();
        assertThat(cache.getAvailability(null, "1", now, tomorrow).join()).isNotNull();
    }

    @Test
    public void testUncommittedIsNotAccessibleWithinTransaction() {
        cache.transactionally(t ->
                cache.putInventory(buildInventory("1", 1L), t).thenCompose(put ->
                        cache.getInventory("1", t).thenAccept(r -> assertThat(r).isNull()))).join();
    }


    @Test
    public void testTransactionalPopulateAndGet() {
        var res = cache.transactionally(t ->
                cache.getInventory("1", t).thenCompose(i -> {
                            assertThat(i).isNull();
                            return cache.putInventory(buildInventory("1", 1L), t);
                        }
                )).join();
        assertThat(res).isNotNull();
        assertThat(cache.getInventory("1").join().getHotelId()).isEqualTo(res.getHotelId());
    }

    @Test
    public void testRollbackOnException() {
        cache.putInventory(buildInventory("1", 1L)).join();
        assertThat(cache.getInventory("1").join().getVersion()).isEqualTo(1L);
        assertThatThrownBy(() -> cache.transactionally(transaction ->
                cache.putInventory(buildInventory("1", 2L), transaction)
                        .thenCompose(ignored -> cache.getInventory("1", transaction))
                        .whenComplete((r, t) -> {
                            throw new RuntimeException("Error, will roll back");
                        })).join()).isInstanceOf(RuntimeException.class);
        assertThat(cache.getInventory("1").join().getVersion()).isEqualTo(1L);
    }


    @Test
    public void testIsolatedTransactionsDoNoSeeEachOther() {
        final CountDownLatch itemsToPut = new CountDownLatch(1);
        final CountDownLatch itemsToGet = new CountDownLatch(1);
        var asyncPut =
                cache.transactionally(transaction1 -> {
                    log.info("Will put item to cache");
                    return cache.putInventory(buildInventory("1", 1L), transaction1)
                            .whenComplete((r, t) -> {
                                log.info("Item put");
                                itemsToPut.countDown();
                                log.info("Waiting for item to be read");
                                try {
                                    itemsToGet.await();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });
                });

        var asyncGet =
                cache.transactionally(transaction2 -> {
                    try {
                        itemsToPut.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return cache.getInventory("1", transaction2).whenComplete((r, t) -> itemsToGet.countDown());
                });

        var put = asyncPut.join();
        var get = asyncGet.join();
        assertThat(put).isNotNull();
        assertThat(get).isNull();
    }

    @Test
    public void testRowConflicts() throws InterruptedException {
        final CountDownLatch bothAreStarted = new CountDownLatch(2);
        final CountDownLatch firstIsCommitted = new CountDownLatch(1);
        var asyncPut1 = cache.transactionally(
                transaction -> {
                    log.info("Started first transaction");
                    bothAreStarted.countDown();
                    try {
                        bothAreStarted.await();
                    } catch (InterruptedException e) {
                    }
                    log.info("Both are started, will do puts");
                    return cache.putInventory(buildInventory("1", 1L), transaction)
                            .whenComplete((r, t) -> log.info("Done putting first, will commit"));
                })
                .whenComplete((r, t) -> {
                    log.info("First one is committed, the second will proceed");
                    firstIsCommitted.countDown();
                });

        var asyncPut2 = cache.transactionally(
                transaction -> {
                    log.info("Started second transaction");
                    bothAreStarted.countDown();
                    try {
                        bothAreStarted.await();
                    } catch (InterruptedException e) {
                    }
                    log.info("Both are started, will do puts");
                    return cache.putInventory(buildInventory("1", 2L), transaction)
                            .thenApply(inventory -> {
                                log.info("Done putting second, will wait for the first one to commit");
                                try {
                                    firstIsCommitted.await();
                                } catch (InterruptedException e) {
                                }
                                return inventory;
                            });
                });

        CompletableFuture.allOf(asyncPut1, asyncPut2).handle((r, t) -> {
            assertThat(asyncPut1.join().getVersion()).isEqualTo(1L);
            assertThatThrownBy(asyncPut2::join).hasCauseInstanceOf(ConcurrentUpdateException.class);
            return null;
        }).join();
    }

    @Test
    public void testUpdateTwoRepos() {
        LocalDate now = LocalDate.now();
        LocalDate tomorrow = now.plusDays(1);
        cache.transactionally(transaction -> {
            log.info("Transaction started, will update inventory");
            return cache.putInventory(buildInventory("1", 1L), transaction)
                    .thenCompose(ignored -> {
                        log.info("First update done, will do the second");
                        return cache.putAvailability(buildAvailability("1", now, tomorrow),
                                transaction);
                    })
                    .thenCompose(ignored -> {
                        log.info("Second update done, check state");
                        return CompletableFuture.allOf(
                                cache.getInventory("1", transaction).thenAccept(res -> assertThat(res).isNull()),
                                cache.getAvailability(null, "1", now, tomorrow, transaction).thenAccept(res -> assertThat(res).isNull()));
                    });
        }).join();
        assertThat(cache.getInventory("1").join()).isNotNull();
        assertThat(cache.getAvailability(null, "1", now, tomorrow).join()).isNotNull();
    }

    @Test
    public void testUpdateRemoveAsTwoTransactions() {
        cache.putInventory(buildInventory("1", 1L)).join();
        cache.removeInventory("1").join();
        assertThat(cache.getInventory("1").join()).isNull();
    }

    @Test
    public void testUpdateRemoveAsASingleTransaction() {
        cache.transactionally(transaction ->
                cache.putInventory(buildInventory("1", 1L), transaction).thenCompose(ignored ->
                        cache.removeInventory("1", transaction))).join();
        assertThat(cache.getInventory("1").join()).isNull();

    }
}
