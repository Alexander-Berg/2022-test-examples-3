package ru.yandex.market.crm.core.domain.coins;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.crm.core.domain.trigger.EventResult;
import ru.yandex.market.crm.external.loyalty.Coin;
import ru.yandex.market.crm.external.loyalty.CoinStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static ru.yandex.market.crm.core.domain.coins.CoinApi.BB3_FILTER;
import static ru.yandex.market.crm.core.domain.coins.CoinApi.BB3_TAG;
import static ru.yandex.market.crm.core.domain.coins.CoinApi.PLUS_FILTER;
import static ru.yandex.market.crm.core.domain.coins.CoinApi.PLUS_TAG;
import static ru.yandex.market.crm.core.domain.coins.CoinApi.USUAL_FILTER;
import static ru.yandex.market.crm.core.domain.coins.CoinApi.USUAL_TAG;

public class CoinsTableTest {

    static class TestCoinsTable extends CoinsTable {

        final LocalDateTime from;

        TestCoinsTable(LocalDateTime from) {
            this.from = from;
        }

        @Override
        public LocalDateTime fromTime() {
            return this.from;
        }
    }

    /**
     * В таблицу должны загружаться только активные монеты
     */
    @Test
    public void testUpdate() {
        LocalDateTime now = CoinApi.now();
        CoinsTable table = new TestCoinsTable(now);
        table.update(Arrays.asList(
                createCoin(1, CoinStatus.ACTIVE, now.minusDays(2), now.minusDays(1)),
                createCoin(2, CoinStatus.ACTIVE, now.minusDays(2), now.plusDays(2)),
                createCoin(3, CoinStatus.ACTIVE, now.plusDays(2), now.plusDays(4)),
                createCoin(11, CoinStatus.INACTIVE, now.minusDays(2), now.minusDays(1)),
                createCoin(12, CoinStatus.INACTIVE, now.minusDays(2), now.plusDays(2)),
                createCoin(13, CoinStatus.INACTIVE, now.plusDays(2), now.plusDays(4)),
                createCoin(21, CoinStatus.EXPIRED, now.minusDays(2), now.minusDays(1)),
                createCoin(22, CoinStatus.EXPIRED, now.minusDays(2), now.plusDays(2)),
                createCoin(23, CoinStatus.EXPIRED, now.plusDays(2), now.plusDays(4)),
                createCoin(31, CoinStatus.USED, now.minusDays(2), now.minusDays(1)),
                createCoin(32, CoinStatus.USED, now.minusDays(2), now.plusDays(2)),
                createCoin(33, CoinStatus.USED, now.plusDays(2), now.plusDays(4)),
                createCoin(41, CoinStatus.REVOKED, now.minusDays(2), now.minusDays(1)),
                createCoin(42, CoinStatus.REVOKED, now.minusDays(2), now.plusDays(2)),
                createCoin(43, CoinStatus.REVOKED, now.plusDays(2), now.plusDays(4))
        ));

        EventResult<Coin> nextEvent = table.getNextEvent("end-push", 0, 100, coin -> true);
        assertNotNull(nextEvent);
        assertEquals(Set.of(2L, 3L, 12L, 13L), nextEvent.getItems().stream().map(Coin::getId).collect(Collectors.toSet()));
    }

    /**
     * Проверяем работу фильтров по значению mergeTag
     */
    @Test
    public void testSelectByTags() {
        CoinsTable table = new CoinsTable();
        Coin coin1 = createActiveCoin(1, BB3_TAG);
        Coin coin2 = createActiveCoin(2, PLUS_TAG);
        Coin coin3 = createActiveCoin(3, USUAL_TAG);
        table.update(List.of(coin1, coin2, coin3));

        EventResult<Coin> nextEvent = table.getNextEvent(0, 100, BB3_FILTER);
        assertNotNull(nextEvent);
        assertSetEquals(List.of(coin1), nextEvent.getItems());

        nextEvent = table.getNextEvent(0, 100, PLUS_FILTER);
        assertNotNull(nextEvent);
        assertSetEquals(List.of(coin2), nextEvent.getItems());

        nextEvent = table.getNextEvent(0, 100, USUAL_FILTER);
        assertNotNull(nextEvent);
        assertSetEquals(List.of(coin3), nextEvent.getItems());

        nextEvent = table.getNextEvent(0, 100, USUAL_FILTER.or(PLUS_FILTER));
        assertNotNull(nextEvent);
        assertSetEquals(List.of(coin2, coin3), nextEvent.getItems());

        nextEvent = table.getNextEvent(0, 100, USUAL_FILTER.and(PLUS_FILTER));
        assertSame(null, nextEvent);

        nextEvent = table.getNextEventForCoins("start-push", 0, 100, BB3_TAG);
        assertNotNull(nextEvent);
        assertSetEquals(List.of(coin1), nextEvent.getItems());

        nextEvent = table.getNextEventForCoins("start-push", 0, 100, USUAL_TAG);
        assertNotNull(nextEvent);
        assertSetEquals(List.of(coin3), nextEvent.getItems());

        nextEvent = table.getNextEventForCoins("start-push", 0, 100, USUAL_TAG, PLUS_TAG);
        assertNotNull(nextEvent);
        assertSetEquals(List.of(coin3, coin2), nextEvent.getItems());
    }

    /**
     * Проверяем корректность формирования времени нотификации
     * Время нотификации должно быть в интервале 10-21 час дня
     */
    @Test
    public void testCheckNotificationTime() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        CoinsTable table = new TestCoinsTable(from);

        LocalDate nowDate = LocalDate.now();
        LocalDateTime midnight = LocalDateTime.of(nowDate, LocalTime.of(0, 0, 0));
        LocalDateTime beforeWork = LocalDateTime.of(nowDate, LocalTime.of(9, 59, 59));
        LocalDateTime startWork = LocalDateTime.of(nowDate, LocalTime.of(10, 0, 0));
        LocalDateTime work = LocalDateTime.of(nowDate, LocalTime.of(17, 11, 19));
        LocalDateTime endWork = LocalDateTime.of(nowDate, LocalTime.of(21, 0, 0));
        LocalDateTime afterWork = LocalDateTime.of(nowDate, LocalTime.of(21, 0, 1));

        Coin coin1 = createCoin(1, CoinStatus.ACTIVE, midnight, work, BB3_TAG);
        Coin coin2 = createCoin(2, CoinStatus.ACTIVE, beforeWork, endWork, PLUS_TAG);
        Coin coin3 = createCoin(3, CoinStatus.ACTIVE, startWork, afterWork, USUAL_TAG);
        table.update(List.of(coin1, coin2, coin3));

        CoinEventResult nextEvent = (CoinEventResult) table.getNextEvent("start-push", BB3_FILTER);
        assertNotNull(nextEvent);
        assertEquals(startWork, nextEvent.getNotificationTime());

        nextEvent = (CoinEventResult) table.getNextEvent("end-push", BB3_FILTER);
        assertNotNull(nextEvent);
        assertEquals(work, nextEvent.getNotificationTime());

        nextEvent = (CoinEventResult) table.getNextEvent("start-push", PLUS_FILTER);
        assertNotNull(nextEvent);
        assertEquals(startWork, nextEvent.getNotificationTime());

        nextEvent = (CoinEventResult) table.getNextEvent("end-push", PLUS_FILTER);
        assertNotNull(nextEvent);
        assertEquals(endWork, nextEvent.getNotificationTime());

        nextEvent = (CoinEventResult) table.getNextEvent("start-push", USUAL_FILTER);
        assertNotNull(nextEvent);
        assertEquals(startWork, nextEvent.getNotificationTime());

        nextEvent = (CoinEventResult) table.getNextEvent("end-push", USUAL_FILTER);
        assertNotNull(nextEvent);
        assertEquals(startWork.plusDays(1), nextEvent.getNotificationTime());
    }

    /**
     * Бонусы Плюса выбираются по тегу формата "yandex_plus_*", где * — любое кол-во символов
     */
    @Test
    public void testSelectPlusByNewTagFormat() {
        CoinsTable table = new CoinsTable();
        Coin coin = createActiveCoin(1, PLUS_TAG + "_new-format");
        table.update(List.of(coin));

        EventResult<Coin> nextEvent = table.getNextEvent(0, 100, PLUS_FILTER);
        assertNotNull(nextEvent);
        assertSetEquals(List.of(coin), nextEvent.getItems());
    }

    private static Coin createActiveCoin(long id, String mergeTag) {
        return createCoin(
                id,
                CoinStatus.ACTIVE,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                mergeTag
        );
    }

    private static Coin createCoin(long id, CoinStatus status, LocalDateTime start, LocalDateTime end) {
        return createCoin(id, status, start, end, null);
    }

    private static Coin createCoin(long id, CoinStatus status, LocalDateTime start, LocalDateTime end, String mergeTag) {
        return new Coin(id, null, null, null, null, null, null, null, null, start, end, null, Map.of(), null,
                status.name(), false, null, null, null, null, mergeTag);
    }

    private static <T> void assertSetEquals(Collection<T> one, Collection<T> other) {
        assertEquals(new HashSet<>(one), new HashSet<>(other));
    }
}
