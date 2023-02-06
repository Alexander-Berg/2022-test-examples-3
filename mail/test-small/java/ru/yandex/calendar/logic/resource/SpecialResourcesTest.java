package ru.yandex.calendar.logic.resource;

import java.util.List;
import java.util.Map;

import lombok.val;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.resource.SpecialResources.Rooms;
import ru.yandex.calendar.logic.resource.SpecialResources.RoomsDuration;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class SpecialResourcesTest {
    private static final Duration DEFAULT_DURATION = Duration.standardMinutes(60);

    private static Rooms makeRooms(ListF<String> rooms) {
        return new Rooms(rooms, Cf.list(), Cf.list(), Cf.list());
    }

    private static Resource makeResource(String exchangeName) {
        val resource = new Resource();
        resource.setExchangeName(exchangeName);
        resource.setAccessGroup(Option.empty());
        resource.setType(ResourceType.ROOM);
        return resource;
    }

    @Test
    public void findDuration() {
        val resource = makeResource("TheRoom");
        val roomsDurations = singletonList(
            new RoomsDuration(makeRooms(resource.getExchangeName()), DEFAULT_DURATION)
        );
        assertThat(SpecialResources.findDuration(resource, roomsDurations))
            .containsExactly(DEFAULT_DURATION);
    }

    @Test
    public void findDurationReturnsFirst() {
        val resource = makeResource("TheRoom");
        val roomsDurations = List.of(
            new RoomsDuration(makeRooms(resource.getExchangeName()), DEFAULT_DURATION),
            new RoomsDuration(makeRooms(resource.getExchangeName()), 30)
        );
        assertThat(SpecialResources.findDuration(resource, roomsDurations))
            .containsExactly(DEFAULT_DURATION);
    }

    @Test
    public void findDurationNoMatchReturnsEmptyOption() {
        val resource = makeResource("TheRoom");
        val roomsDurations = singletonList(
            new RoomsDuration(makeRooms(Cf.list("AnotherRoom")), DEFAULT_DURATION)
        );
        assertThat(SpecialResources.findDuration(resource, roomsDurations))
            .isEmpty();
    }

    public static class RoomsDurationTest {
        private static final Rooms VALID_ROOMS = makeRooms(Cf.list("RoomName"));

        @Test
        public void emptyRoomsIsNotValid() {
            val emptyRooms = new Rooms(Cf.list(), Cf.list(), Cf.list(), Cf.list());
            val emptyRoomsDuration = new RoomsDuration(emptyRooms, DEFAULT_DURATION);
            assertThat(emptyRoomsDuration)
                .matches(RoomsDuration::isNotValid, "RoomsDuration must not be valid due to empty rooms list");
        }

        @Test
        public void filledRoomsIsValid() {
            val roomsParameters = Map.of(
                "room",          new Rooms(Cf.list("RoomName"), Cf.list(), Cf.list(), Cf.list()),
                "access_group",  new Rooms(Cf.list(), Cf.list(42), Cf.list(), Cf.list()),
                "resource_type", new Rooms(Cf.list(), Cf.list(), Cf.list(ResourceType.ROOM), Cf.list())
            );
            for (val roomsParam: roomsParameters.entrySet()) {
                val roomsDuration = new RoomsDuration(roomsParam.getValue(), DEFAULT_DURATION);
                assertThat(roomsDuration)
                    .matches(RoomsDuration::isValid, "RoomsDuration must be valid because it is filled with " + roomsParam.getKey());
            }
        }

        @Test
        public void nonPositiveDurationIsNotValid() {
            assertThat(new RoomsDuration(VALID_ROOMS, 0))
                .matches(RoomsDuration::isNotValid, "RoomsDuration must not be valid due to zero duration");
            assertThat(new RoomsDuration(VALID_ROOMS, -15))
                .matches(RoomsDuration::isNotValid, "RoomsDuration must not be valid due to negative duration");
        }

        @Test
        public void duplicatingRoomsIsNotValidList() {
            val roomsDurations = List.of(
                new RoomsDuration(makeRooms(Cf.list("first", "second")), DEFAULT_DURATION),
                new RoomsDuration(makeRooms(Cf.list("first", "third")), DEFAULT_DURATION)
            );
            assertThat(RoomsDuration.isValidList(roomsDurations))
                .withFailMessage("RoomsDuration list must not be valid due to unique violation: %s", roomsDurations)
                .isFalse();

        }

        @Test
        public void distinctRoomsIsValidList() {
            val roomsDurations = List.of(
                new RoomsDuration(makeRooms(Cf.list("first*", "second")), DEFAULT_DURATION),
                new RoomsDuration(makeRooms(Cf.list("first", "second*")), DEFAULT_DURATION)
            );
            assertThat(RoomsDuration.isValidList(roomsDurations))
                .withFailMessage("RoomsDuration list must be valid due to being not empty and has only distinct elements: %s", roomsDurations)
                .isTrue();
        }
    }
}
