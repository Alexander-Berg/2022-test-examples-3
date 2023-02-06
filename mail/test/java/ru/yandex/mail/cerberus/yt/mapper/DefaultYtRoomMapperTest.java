package ru.yandex.mail.cerberus.yt.mapper;

import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.LocationKey;
import ru.yandex.mail.cerberus.ResourceId;
import ru.yandex.mail.cerberus.client.dto.Resource;
import ru.yandex.mail.cerberus.yt.data.YtRoomInfo;
import ru.yandex.mail.cerberus.yt.staff.StaffConstants;
import ru.yandex.mail.cerberus.yt.staff.dto.Meta;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffRoom;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class DefaultYtRoomMapperTest {
    @Inject
    private YtRoomMapper mapper;

    private static final ResourceId ROOM_ID = new ResourceId(100500L);
    private static final LocationId LOCATION_ID = new LocationId(2245L);
    private static final YtRoomInfo.Equipment EQUIPMENT = new YtRoomInfo.Equipment(true, true, "ps4", true, true, 0, 1, 42, "da", false);
    private static final String PHONE = "4637893";
    private static final Meta META = new Meta(OffsetDateTime.MAX);
    private static final StaffRoom ROOM = new StaffRoom(
        META,
        ROOM_ID,
        false,
        true,
        StaffRoom.Type.CONFERENCE,
        new StaffRoom.Name("name", "exchange_name", "alter", "имяЪ", "name"),
        new StaffRoom.Floor(1, new StaffRoom.Floor.Office(LOCATION_ID), OptionalInt.of(2)),
        "add",
        Optional.of("42"),
        EQUIPMENT,
        PHONE
    );

    @Test
    @DisplayName("Verify that 'mapToResource' returns expected resource object")
    void testMapToResource() {
        val expectedResource = new Resource<YtRoomInfo>(
            ROOM_ID,
            StaffConstants.YT_ROOM_RESOURCE_TYPE_NAME,
            "exchange_name",
            Optional.of(new LocationKey(LOCATION_ID, StaffConstants.YT_OFFICE_TYPE)),
            true,
            Optional.of(new YtRoomInfo(1, OptionalInt.of(2), "name", "имяЪ", "name", "alter", "add", 42, EQUIPMENT, PHONE, true))
        );
        val resource = mapper.mapToResource(ROOM);
        assertThat(resource).isEqualTo(expectedResource);
    }
}
