package ru.yandex.calendar.logic.resource.center;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import lombok.SneakyThrows;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.ResourceTestManager;
import ru.yandex.calendar.logic.staff.dao.OfficesDao;
import ru.yandex.calendar.logic.staff.dao.RoomsDao;
import ru.yandex.calendar.micro.yt.entity.YtOffice;
import ru.yandex.calendar.micro.yt.entity.YtRoom;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.blackbox.PassportDomain;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.ResourceId;
import ru.yandex.mail.cerberus.yt.data.YtOfficeInfo;
import ru.yandex.mail.cerberus.yt.data.YtRoomInfo;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffLocalizedString;
import ru.yandex.misc.test.Assert;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;

public class CenterResourceUpdaterTest extends AbstractConfTest {
    @Autowired
    private CenterResourceUpdater centerResourceUpdater;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ResourceTestManager resourceTestManager;

    private StaffDaoMock mock;

    @Before
    public void cleanBeforeTest() {
        resourceTestManager.clearResourcesAndOffices(PassportDomain.YANDEX_TEAM_RU);
        mock = new StaffDaoMock();
        centerResourceUpdater.setMicroContextForTest(mock.officesDaoMock, mock.roomsDaoMock);
    }

    @Test
    public void addRoom() {
        mock.addOffice(1, "Office 1");

        mock.addRoom(1, "Room 1", "conf_rr_1");
        centerResourceUpdater.update();

        mock.addRoom(1, "Room 2", "conf_rr_2");
        centerResourceUpdater.update();

        Assert.some(findOfficeO(1));
        Assert.some("Room 1", findResource("conf_rr_1").getName());
        Assert.some("Room 2", findResource("conf_rr_2").getName());
    }

    @Test
    public void removeRoom() {
        mock.addOffice(1, "Office 1");

        mock.addRoom(1, "Room 1", "conf_rr_1");
        mock.addRoom(1, "Room 2", "conf_rr_2");
        centerResourceUpdater.update();

        Assert.isTrue(findResource("conf_rr_2").getIsActive());

        mock.removeLastRoom();
        centerResourceUpdater.update();

        Assert.isFalse(findResource("conf_rr_2").getIsActive());
    }

    @Test
    public void ignoreOfficesWithoutRooms() {
        mock.addOffice(1, "Office 1");
        mock.addOffice(2, "Office 2");

        centerResourceUpdater.update();

        Assert.none(findOfficeO(1));
        Assert.none(findOfficeO(2));
    }

    @Test
    public void removeOffice() {
        mock.addOffice(1, "Office 1");
        mock.addRoom(1, "Room 1", "conf_rr_1");

        centerResourceUpdater.update();
        Assert.isTrue(findOfficeO(1).get().getIsActive());

        mock.clear();
        centerResourceUpdater.update();

        Assert.isFalse(findOfficeO(1).get().getIsActive());
    }

    @Test
    public void addRoomAndThenDeactivateIt() {
        mock.addOffice(1, "Office 1");

        mock.addRoom(1, "Room 1", "conf_rr_1");
        centerResourceUpdater.update();

        Assert.some(findOfficeO(1));
        Assert.some("Room 1", findResource("conf_rr_1").getName());
        Assert.isTrue(findResource("conf_rr_1").getIsActive());

        mock.deactivateRoom(1, "Room 1", "conf_rr_1");
        centerResourceUpdater.update();
        Assert.some("Room 1", findResource("conf_rr_1").getName());
        Assert.isFalse(findResource("conf_rr_1").getIsActive());

    }

    private Resource findResource(String exchangeName) {
        return resourceRoutines.findByExchangeName(exchangeName).get();
    }

    private Option<ru.yandex.calendar.logic.beans.generated.Office> findOfficeO(int officeCenterId) {
        return resourceDao.findOfficeByCenterId(officeCenterId);
    }

    private static class StaffDaoMock {
        private List<YtOffice> offices = emptyList();
        private List<YtRoom> rooms = emptyList();

        private long lastInsertedRoomId = 1;

        RoomsDao roomsDaoMock = Mockito.mock(RoomsDao.class);
        OfficesDao officesDaoMock = Mockito.mock(OfficesDao.class);

        StaffDaoMock() {
            this.mock();
        }

        public void clear() {
            offices = emptyList();
            rooms = emptyList();
            mock();
        }

        void addOffice(long officeId, String name) {
            val info = new YtOfficeInfo(
                    new StaffLocalizedString(name, name),
                    "code",
                    new StaffLocalizedString("city", "city"),
                    ZoneId.of("Europe/Moscow"),
                    false
            );
            val office = new YtOffice(new LocationId(officeId), name, info);

            offices = StreamEx.of(offices)
                    .append(office)
                    .toImmutableList();
            mock();
        }

        void addRoom(long officeId, String name, String exchangeName) {
            val info = new YtRoomInfo(
                    1,
                    OptionalInt.of(1),
                    name,
                    name,
                    name,
                    "",
                    "",
                    0,
                    new YtRoomInfo.Equipment(false, false, "", false, false, 0, 0, 0, "", false),
                    "",
                    true
            );
            val room = new YtRoom(
                    new ResourceId(lastInsertedRoomId++),
                    exchangeName,
                    Optional.of(new LocationId(officeId)),
                    true,
                    info
            );

            rooms = StreamEx.of(rooms)
                    .append(room)
                    .toImmutableList();
            mock();
        }

        void deactivateRoom(long officeId, String name, String exchangeName) {
            rooms = StreamEx.of(rooms).filter(room1 -> !room1.getName().equals(name)).toImmutableList();
            val info = new YtRoomInfo(
                    1,
                    OptionalInt.of(1),
                    name,
                    name,
                    name,
                    "",
                    "",
                    0,
                    new YtRoomInfo.Equipment(false, false, "", false, false, 0, 0, 0, "", false),
                    "",
                    false
            );
            val room = new YtRoom(
                    new ResourceId(1),
                    exchangeName,
                    Optional.of(new LocationId(officeId)),
                    false,
                    info
            );

            rooms = StreamEx.of(rooms)
                    .append(room)
                    .toImmutableList();
            mock();
        }

        void removeLastRoom() {
            rooms = StreamEx.of(rooms)
                    .limit(rooms.size() - 1)
                    .toImmutableList();
            mock();
        }

        @SneakyThrows
        private void mock() {

            when(roomsDaoMock.getAll())
                    .thenReturn(rooms);


            when(officesDaoMock.getAll())
                    .thenReturn(offices);

        }
    }
}
