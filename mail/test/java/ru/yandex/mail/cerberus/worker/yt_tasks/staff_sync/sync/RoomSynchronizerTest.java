package ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.LocationKey;
import ru.yandex.mail.cerberus.ResourceId;
import ru.yandex.mail.cerberus.client.dto.Location;
import ru.yandex.mail.cerberus.client.dto.Resource;
import ru.yandex.mail.cerberus.client.dto.ResourceType;
import ru.yandex.mail.cerberus.core.CollisionStrategy;
import ru.yandex.mail.cerberus.core.location.LocationManager;
import ru.yandex.mail.cerberus.yt.staff.dto.Meta;
import ru.yandex.mail.micronaut.common.JsonMapper;
import ru.yandex.mail.cerberus.core.resource.ResourceManager;
import ru.yandex.mail.cerberus.ReadTarget;
import ru.yandex.mail.cerberus.dao.resource.ResourceEntity;
import ru.yandex.mail.cerberus.dao.resource.RoResourceRepository;
import ru.yandex.mail.cerberus.worker.api.TaskExecutionContext;
import ru.yandex.mail.cerberus.yt.data.YtOfficeInfo;
import ru.yandex.mail.cerberus.yt.data.YtRoomInfo;
import ru.yandex.mail.cerberus.yt.staff.StaffConstants;
import ru.yandex.mail.cerberus.yt.staff.client.StaffClient;
import ru.yandex.mail.cerberus.yt.staff.client.StaffResult;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffRoom;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;
import javax.inject.Named;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.BC;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.TODAY;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.TOMORROW;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.YESTERDAY;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.sync.RoomSynchronizerTest.DB_NAME;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
@ExtendWith(MockitoExtension.class)
class RoomSynchronizerTest {
    static final String DB_NAME = "room_synchronizer_test_db";

    @Inject
    @Named("room")
    private Synchronizer synchronizer;

    @Inject
    private RoResourceRepository roResourceRepository;

    @Inject
    private ResourceManager resourceManager;

    @Inject
    private LocationManager locationManager;

    @Inject
    private JsonMapper jsonMapper;

    private static final StaffClient staffClientMock = Mockito.mock(StaffClient.class);

    @MockBean(StaffClient.class)
    public StaffClient mockStaffClient() {
        return staffClientMock;
    }

    @BeforeEach
    void reset() {
        Mockito.reset(staffClientMock);
    }

    private static final YtRoomInfo.Equipment EQUIPMENT = new YtRoomInfo.Equipment(
        true, true, "PS4", true, false, 1, 1, 2, "meh", true
    );
    private static final String PHONE = "23435";

    private static Resource<YtRoomInfo> resource(long id, ResourceType type, String name, String displayName, long officeId,
                                                 boolean active, boolean bookable) {
        val info = new YtRoomInfo(22L, OptionalInt.of(2), displayName, displayName, displayName, displayName, "", 42, EQUIPMENT, PHONE, bookable);
        return new Resource<>(new ResourceId(id), type, name, Optional.of(location(officeId)), active, Optional.of(info));
    }

    private StaffRoom room(OffsetDateTime modifiedAt, long id, long officeId, boolean isDeleted, boolean isBookable, String name) {
        val floor = new StaffRoom.Floor(22L, new StaffRoom.Floor.Office(new LocationId(officeId)), OptionalInt.of(2));
        return new StaffRoom(new Meta(modifiedAt), new ResourceId(id), isDeleted, isBookable, StaffRoom.Type.CONFERENCE,
            new StaffRoom.Name(name, name, name, name, name), floor, "", Optional.of("42"), EQUIPMENT, PHONE);
    }

    private static LocationKey location(long id) {
        return new LocationKey(new LocationId(id), StaffConstants.YT_OFFICE_TYPE);
    }

    private List<ResourceEntity> getAllResources() {
        val resources = roResourceRepository.findAll();
        resources.sort(Comparator.comparing(entity -> entity.getId().getValue()));
        return resources;
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that nothing changes if nothing to sync")
    void testEmptyChanges(@Mock TaskExecutionContext context) {
        when(staffClientMock.rooms(anyInt(), any(), anyInt(), any(), any(), any()))
            .thenReturn(completedFuture(StaffResult.empty()));

        val resourcesBefore = getAllResources();

        val result = synchronizer.synchronize(context, Optional.empty()).block();
        assertThat(result)
            .isNull();

        verify(staffClientMock, only())
            .rooms(anyInt(), any(), anyInt(), any(), any(), any());

        assertThat(resourcesBefore)
            .containsExactlyElementsOf(getAllResources());

        verify(context, never())
            .getExecutor();
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that rooms synchronization correctly applies changes from staff")
    void testSynchronization(@Mock TaskExecutionContext context) {
        val type = resourceManager.getByNameOrCreateType(StaffConstants.YT_ROOM_RESOURCE_TYPE_DATA).get();

        val syncedOffices = List.<Location<YtOfficeInfo>>of(
            new Location<>(new LocationId(10L), StaffConstants.YT_OFFICE_TYPE, "office0", Optional.empty()),
            new Location<>(new LocationId(11L), StaffConstants.YT_OFFICE_TYPE, "office1", Optional.empty()),
            new Location<>(new LocationId(12L), StaffConstants.YT_OFFICE_TYPE, "office2", Optional.empty()),
            new Location<>(new LocationId(13L), StaffConstants.YT_OFFICE_TYPE, "office3", Optional.empty())
        );

        locationManager.insert(syncedOffices).get();

        val existingResources = List.of(
            resource(100L, type, "room1", "room1", 10L, true, true),
            resource(101L, type, "room2", "room2", 11L, true, true),
            resource(102L, type, "room3", "room3", 12L, false, false)
        );
        resourceManager.insertResources(CollisionStrategy.FAIL, existingResources).get();

        val newStaffRoom = room(TOMORROW, 0L, 13L, false, true, "new");
        val updatedStaffRoom = room(YESTERDAY,100L, 10L, false, false, "room11");
        val deletedStaffRoom = room(BC,101L, 11L, true, true, "room2");
        val upToDateStaffRoom = room(TODAY,102L, 12L, false, false, "room3");
        val items = List.of(deletedStaffRoom, upToDateStaffRoom, newStaffRoom, updatedStaffRoom);

        when(staffClientMock.rooms(anyInt(), any(), anyInt(), any(), any(), any()))
            .thenAnswer(new StaffResultAnswer<>(items, jsonMapper));

        val maxModificationTime = synchronizer.synchronize(context, Optional.empty()).block();
        assertThat(maxModificationTime)
            .isAtSameInstantAs(TOMORROW);

        assertThat(roResourceRepository.count())
            .isEqualTo(4);

        val ids = Set.of(new ResourceId(0L), new ResourceId(100L), new ResourceId(101L), new ResourceId(102L));
        val resources = resourceManager.findResources(type.getName(), ids, YtRoomInfo.class, ReadTarget.MASTER).get();
        assertThat(resources)
            .containsExactlyInAnyOrder(
                resource(0L, type, "new", "new", 13L, true, true),
                resource(100L, type, "room1", "room11", 10L, false, false),
                resource(101L, type, "room2", "room2", 11L, false, true),
                resource(102L, type, "room3", "room3", 12L, false, false)
            );

        verify(staffClientMock, times(3))
            .rooms(anyInt(), any(), anyInt(), any(), any(), any());
        verifyNoMoreInteractions(staffClientMock);

        verify(context, never())
            .getExecutor();
    }
}
