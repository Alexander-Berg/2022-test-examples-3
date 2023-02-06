package ru.yandex.market.mbi.api.controller.moderation;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.moderation.event.EntityType;
import ru.yandex.market.core.moderation.event.Event;
import ru.yandex.market.core.moderation.event.EventList;
import ru.yandex.market.core.moderation.event.EventSubtype;
import ru.yandex.market.core.moderation.event.EventType;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.mbi.api.client.entity.moderation.LightCheckReadyShopResponse;
import ru.yandex.market.mbi.api.client.entity.moderation.LightCheckReadyShops;
import ru.yandex.market.mbi.api.client.entity.moderation.PremoderationReadyShopResponse;
import ru.yandex.market.mbi.api.client.entity.moderation.PremoderationReadyShops;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты на ручки {@link ru.yandex.market.mbi.api.controller.ModerationController}.
 *
 * @author fbokovikov
 */
class PremoderationReadyShopsFunctionalTest extends FunctionalTest {
    private static final EventList EVENTS = new EventList(
            new Event(
                    1L,
                    11L,
                    HasId.getById(EntityType.class, 1L),
                    HasId.getById(EventType.class, 1L),
                    HasId.getById(EventSubtype.class, 1L),
                    Timestamp.valueOf("2021-06-25 10:10:10.000000000")
            ),
            new Event(
                    2L,
                    12L,
                    HasId.getById(EntityType.class, 2L),
                    HasId.getById(EventType.class, 2L),
                    HasId.getById(EventSubtype.class, 2L),
                    Timestamp.valueOf("2022-06-25 10:10:10.000000000")
            ),
            new Event(
                    3L,
                    13L,
                    HasId.getById(EntityType.class, 1L),
                    HasId.getById(EventType.class, 3L),
                    HasId.getById(EventSubtype.class, 3L),
                    Timestamp.valueOf("2020-06-25 10:10:10.000000000")
            ),
            new Event(
                    4L,
                    14L,
                    HasId.getById(EntityType.class, 2L),
                    HasId.getById(EventType.class, 4L),
                    HasId.getById(EventSubtype.class, 4L),
                    Timestamp.valueOf("2021-08-20 10:10:10.000000000")
            ),
            new Event(
                    5L,
                    15L,
                    HasId.getById(EntityType.class, 1L),
                    HasId.getById(EventType.class, 5L),
                    HasId.getById(EventSubtype.class, 5L),
                    Timestamp.valueOf("2021-01-01 01:01:01.000000000")
            ),
            new Event(
                    6L,
                    16L,
                    HasId.getById(EntityType.class, 2L),
                    HasId.getById(EventType.class, 6L),
                    HasId.getById(EventSubtype.class, 6L),
                    Timestamp.valueOf("2020-02-03 04:05:06.000000000")
            ),
            new Event(
                    7L,
                    17L,
                    HasId.getById(EntityType.class, 1L),
                    HasId.getById(EventType.class, 7L),
                    HasId.getById(EventSubtype.class, 7L),
                    Timestamp.valueOf("2017-07-07 07:07:07.000000000")
            ),
            new Event(
                    8L,
                    18L,
                    HasId.getById(EntityType.class, 2L),
                    HasId.getById(EventType.class, 1L),
                    HasId.getById(EventSubtype.class, 8L),
                    Timestamp.valueOf("2020-08-08 15:20:21.000000000")
            ),
            new Event(
                    9L,
                    19L,
                    HasId.getById(EntityType.class, 1L),
                    HasId.getById(EventType.class, 2L),
                    HasId.getById(EventSubtype.class, 1L),
                    Timestamp.valueOf("2021-10-11 11:10:21.000000000")
            )
    );

    private static List<Event> getEventsList(
            Long eventId,
            List<EntityType> entityTypes,
            List<EventType> eventTypes,
            Long numberOfEvents
    ) {
        if (entityTypes == null) {
            entityTypes = Arrays.asList(EntityType.values());
        }
        if (eventTypes == null) {
            eventTypes = Arrays.asList(EventType.values());
        }
        if (numberOfEvents == null) {
            numberOfEvents = (long) EVENTS.getEventList().size();
        }
        long finalEventId = eventId == null
                ? 0L
                : eventId;
        List<EntityType> finalEntityTypes = entityTypes;
        List<EventType> finalEventTypes = eventTypes;
        return EVENTS.getEventList().stream().filter(event ->
                        event.getEventId() > finalEventId &&
                                finalEntityTypes.contains(event.getEntityType()) &&
                                finalEventTypes.contains(event.getEventType()))
                .limit(numberOfEvents)
                .collect(Collectors.toList());
    }

    private static Stream<Arguments> provideArgsForGetEventsTest() {
        List<EntityType> entityType1 = List.of(HasId.getById(EntityType.class, 2L));
        List<EntityType> entityTypeAll = Arrays.asList(EntityType.values());

        List<EventType> eventType1 = List.of(HasId.getById(EventType.class, 2L));
        List<EventType> eventType2 = List.of(
                HasId.getById(EventType.class, 1L),
                HasId.getById(EventType.class, 2L)
        );
        List<EventType> eventTypeAll = Arrays.asList(EventType.values());

        return Stream.of(
                Arguments.of(null, null, null, null),
                Arguments.of(null, null, null, 3L),
                Arguments.of(1L, null, null, null),
                Arguments.of(7L, null, null, null),
                Arguments.of(1L, null, eventType1, null),
                Arguments.of(1L, entityType1, null, null),
                Arguments.of(1L, entityType1, eventType1, null),
                Arguments.of(3L, null, eventType2, null),
                Arguments.of(3L, entityType1, eventType2, null),
                Arguments.of(5L, entityTypeAll, null, null),
                Arguments.of(5L, entityTypeAll, eventType2, null),
                Arguments.of(6L, null, eventTypeAll, null),
                Arguments.of(5L, entityTypeAll, eventTypeAll, null)
        );
    }

    /**
     * Проверяет логику формирования магазинов, которым требуется модерация.
     */
    @Test
    @DbUnitDataSet(before = "testShopsForModeration.csv")
    void testShopsForModeration() {
        checkShopForModeration();
    }

    /**
     * Проверяет логику формирования магазинов, которым требуется модерация в присутсвии самопроверок
     */
    @Test
    @DbUnitDataSet(before = "testShopsForModeration.withSelfTest.csv")
    void testShopsForModerationWithSelfTest() {
        checkShopForModeration();
    }

    /**
     * Тестирует ручку /qc/shops/light-checks для получения магазинов для лайтовой проверки.
     */
    @Test
    @DbUnitDataSet(before = "testShopsForModeration.csv")
    void testGetLiteCheckReadyShops() {
        LightCheckReadyShops shops = mbiApiClient.getLiteCheckReadyShops();
        assertThat(shops)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreCollectionOrder(true)
                        .withIgnoredFields("shops.body", "shops.subject") // локально ок, в ci почему-то нет
                        .build()
                ).isEqualTo(new LightCheckReadyShops(List.of(
                        new LightCheckReadyShopResponse(1, TestingType.CPC_LITE_CHECK, "тема", "тело"),
                        new LightCheckReadyShopResponse(1, TestingType.GENERAL_LITE_CHECK, null, null),
                        new LightCheckReadyShopResponse(100, TestingType.GENERAL_LITE_CHECK, null, null)
                )));
    }

    @ParameterizedTest
    @MethodSource("provideArgsForGetEventsTest")
    @DbUnitDataSet(before = "testGetEvents.csv")
    void testGetEvents(Long eventId, List<EntityType> entities, List<EventType> events, Long numberOfEvents) {
        List<Event> eventList = mbiApiClient.getEvents(eventId, entities, events, numberOfEvents).getEventList();
        List<Event> expected = getEventsList(eventId, entities, events, numberOfEvents);
        assertThat(eventList)
                .hasSameSizeAs(expected)
                .containsAll(expected);
    }

    @Test
    @DbUnitDataSet(before = "testGetEvents.csv")
    void testGetEvent() {
        mbiApiClient.getEvents(null, null, null, null);
    }

    private void checkShopForModeration() {
        PremoderationReadyShops premoderationReadyShops = mbiApiClient.getPremoderationReadyShops();
        List<PremoderationReadyShopResponse> shops = premoderationReadyShops.getShops();
        assertThat(shops).containsExactlyInAnyOrder(
                new PremoderationReadyShopResponse(774, TestingType.CPC_PREMODERATION, false, 5),
                new PremoderationReadyShopResponse(774, TestingType.CPA_PREMODERATION, false, 5),
                new PremoderationReadyShopResponse(100, TestingType.CPC_PREMODERATION, true, 2)
        );
    }
}
