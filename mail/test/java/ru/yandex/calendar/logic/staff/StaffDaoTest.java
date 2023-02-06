package ru.yandex.calendar.logic.staff;

import com.fasterxml.jackson.core.JsonProcessingException;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.calendar.logic.staff.dao.*;
import ru.yandex.calendar.micro.yt.entity.*;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.mail.cerberus.GroupId;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.ResourceId;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.yt.data.YtDepartmentInfo;
import ru.yandex.mail.cerberus.yt.data.YtOfficeInfo;
import ru.yandex.mail.cerberus.yt.data.YtRoomInfo;
import ru.yandex.mail.cerberus.yt.data.YtUserInfo;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffLocalizedString;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser;

import java.time.ZoneId;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;

public class StaffDaoTest extends AbstractConfTest {

    @Autowired
    UsersDao usersDao;
    @Autowired
    GroupsDao groupsDao;
    @Autowired
    OfficesDao officesDao;
    @Autowired
    RoomsDao roomsDao;

    @Test
    public void testPersons() throws JsonProcessingException {
        assertTrue(usersDao.getAll().isEmpty());

        List<YtUserWithDepartmentIds> users = List.of(
                new YtUserWithDepartmentIds(new YtUser(new Uid(1120000000045832L), "robot-calendar", new YtUserInfo(1, StaffUser.Affiliation.YANDEX, "robot-calendar@yandex-team.ru", ZoneId.of("Europe/Moscow"), new LocationId(1), OptionalInt.empty(), "ru", "Хранитель", "Календаря", "Calendar", "Keeper", Optional.empty(), "гуру", "guru", StaffUser.Gender.MALE, Optional.empty(), emptyList(), EnumSet.noneOf(YtUserInfo.Trait.class), emptyList(), Optional.empty(), Optional.empty())),
                        Set.of(new GroupId(1), new GroupId(2))),
                new YtUserWithDepartmentIds(new YtUser(new Uid(1120000000004717L), "calendartestuser", new YtUserInfo(2, StaffUser.Affiliation.YANDEX, "calendartestuser@yandex-team.ru", ZoneId.of("Europe/Moscow"), new LocationId(1), OptionalInt.empty(), "ru", "Тест", "Календарев", "Test", "Calendar", Optional.empty(), "подмастерье", "scholar", StaffUser.Gender.MALE, Optional.empty(), emptyList(), EnumSet.noneOf(YtUserInfo.Trait.class), emptyList(), Optional.empty(), Optional.empty())),
                        Set.of(new GroupId(2), new GroupId(3)))
        );
        assertEquals(2, usersDao.update(users));
        var usersReceived = usersDao.getAll();
        assertEquals(Set.copyOf(users), Set.copyOf(usersReceived));

        List<YtUserWithDepartmentIds> update = List.of(
                new YtUserWithDepartmentIds(new YtUser(new Uid(1120000000045832L), "robot-calendar11111",
                        new YtUserInfo(1, StaffUser.Affiliation.YANDEX, "robot-calendar@yandex-team.ru", ZoneId.of("Europe/Moscow"), new LocationId(1), OptionalInt.empty(), "ru", "Хранитель", "Календаря", "Calendar", "Keeper", Optional.empty(), "гуру", "guru", StaffUser.Gender.MALE, Optional.empty(), emptyList(), EnumSet.noneOf(YtUserInfo.Trait.class), emptyList(), Optional.empty(), Optional.empty())),
                        Set.of(new GroupId(1), new GroupId(2))),
                new YtUserWithDepartmentIds(new YtUser(new Uid(1120000000004718L), "calendartestuser", new YtUserInfo(2, StaffUser.Affiliation.YANDEX, "calendartestuser@yandex-team.ru", ZoneId.of("Europe/Moscow"), new LocationId(1), OptionalInt.empty(), "ru", "Тест", "Календарев", "Test", "Calendar", Optional.empty(), "подмастерье", "scholar", StaffUser.Gender.MALE, Optional.empty(), emptyList(), EnumSet.noneOf(YtUserInfo.Trait.class), emptyList(), Optional.empty(), Optional.empty())),
                        Set.of(new GroupId(2), new GroupId(3)))
        );
        assertEquals(1, usersDao.update(update));
        usersReceived = usersDao.getAll();
        var usersExpected = StreamEx.of(update).append(users.get(1)).toImmutableSet();
        assertEquals(Set.copyOf(usersExpected), Set.copyOf(usersReceived));

        assertEquals(3, usersDao.getAll().size());
    }

    @Test
    public void testGroups() throws JsonProcessingException {
        assertTrue(groupsDao.getAll().isEmpty());

        List<YtDepartment> groups = List.of(new YtDepartment(new GroupId(123456789L), "NAME1", true,
                        new YtDepartmentInfo(Set.of(1L, 2L), "https://url", "на русском", "in english")),
                new YtDepartment(new GroupId(987654321L), "NAME1", true,
                        new YtDepartmentInfo(Set.of(1L, 2L), "https://url", "на русском", "in english")));
        assertEquals(2, groupsDao.update(groups));
        var received = groupsDao.getAll();
        assertEquals(Set.copyOf(groups), Set.copyOf(received));

        List<YtDepartment> update = List.of(new YtDepartment(new GroupId(123456789L), "NAME2", true,
                        new YtDepartmentInfo(Set.of(1L, 2L), "https://url", "на русском", "in english")),
                new YtDepartment(new GroupId(777L), "NAME1", true,
                        new YtDepartmentInfo(Set.of(1L, 2L), "https://url", "на русском", "in english")));
        assertEquals(1, groupsDao.update(update));
        received = groupsDao.getAll();
        var expected = StreamEx.of(update).append(groups.get(1)).toImmutableSet();
        assertEquals(expected, Set.copyOf(received));

        assertEquals(1, groupsDao.getAll(1, 0).size());
    }


    @Test
    public void testOffices() throws JsonProcessingException {
        assertTrue(officesDao.getAll().isEmpty());

        List<YtOffice> offices = List.of(
                new YtOffice(new LocationId(123454321L), "office name",
                        new YtOfficeInfo(new StaffLocalizedString("нэйм", "name"), "code", new StaffLocalizedString("сити", "city"), ZoneId.of("Europe/Moscow"), false)),
                new YtOffice(new LocationId(99999999999L), "office name",
                        new YtOfficeInfo(new StaffLocalizedString("нэйм", "name"), "code", new StaffLocalizedString("сити", "city"), ZoneId.of("Europe/Moscow"), false))

        );
        assertEquals(2, officesDao.update(offices));
        var received = officesDao.getAll();
        assertEquals(Set.copyOf(offices), Set.copyOf(received));

        List<YtOffice> update = List.of(new YtOffice(new LocationId(123454321L), "new office name",
                        new YtOfficeInfo(new StaffLocalizedString("нэйм", "name"), "code", new StaffLocalizedString("сити", "city"), ZoneId.of("Europe/Moscow"), false)),
                new YtOffice(new LocationId(99999999999L), "office name",
                        new YtOfficeInfo(new StaffLocalizedString("нэйм", "name"), "code", new StaffLocalizedString("сити", "city"), ZoneId.of("Europe/Moscow"), false)));
        assertEquals(0, officesDao.update(update));
        received = officesDao.getAll();
        var expected = StreamEx.of(update).append(offices.get(1)).toImmutableSet();
        assertEquals(expected, Set.copyOf(received));

        assertEquals(1, officesDao.getAll(1, 0).size());
    }

    @Test
    public void testRooms() throws JsonProcessingException {
        assertTrue(roomsDao.getAll().isEmpty());

        officesDao.update(List.of(new YtOffice(new LocationId(44444L), "office name",
                new YtOfficeInfo(new StaffLocalizedString("нэйм", "name"), "code", new StaffLocalizedString("сити", "city"), ZoneId.of("Europe/Moscow"), false))));

        List<YtRoom> rooms = List.of(
                new YtRoom(new ResourceId(111L), "room name", Optional.of(new LocationId(55555L)), true,
                        new YtRoomInfo(1, OptionalInt.of(1), "name", "name", "name", "", "", 0, new YtRoomInfo.Equipment(false, false, "", false, false, 0, 0, 0, "", false), "", true)),
                new YtRoom(new ResourceId(222L), "room name", Optional.of(new LocationId(44444L)), true,
                        new YtRoomInfo(1, OptionalInt.of(1), "name", "name", "name", "", "", 0, new YtRoomInfo.Equipment(false, false, "", false, false, 0, 0, 0, "", false), "", true))
        );
        assertEquals(1, roomsDao.update(rooms));
        officesDao.update(List.of(new YtOffice(new LocationId(55555L), "office name",
                new YtOfficeInfo(new StaffLocalizedString("нэйм", "name"), "code", new StaffLocalizedString("сити", "city"), ZoneId.of("Europe/Moscow"), false))));
        assertEquals(1, roomsDao.update(rooms));

        var received = roomsDao.getAll();
        assertEquals(Set.copyOf(rooms), Set.copyOf(received));

        List<YtRoom> update = List.of(
                new YtRoom(new ResourceId(111L), "new name", Optional.of(new LocationId(55555L)), true,
                        new YtRoomInfo(1, OptionalInt.of(1), "name", "name", "name", "", "", 0, new YtRoomInfo.Equipment(false, false, "", false, false, 0, 0, 0, "", false), "", true)),
                new YtRoom(new ResourceId(333L), "room name", Optional.of(new LocationId(55555L)), true,
                        new YtRoomInfo(1, OptionalInt.of(1), "name", "name", "name", "", "", 0, new YtRoomInfo.Equipment(false, false, "", false, false, 0, 0, 0, "", false), "", true))
        );
        assertEquals(1, roomsDao.update(update));
        received = roomsDao.getAll();
        var expected = StreamEx.of(update).append(rooms.get(1)).toImmutableSet();
        assertEquals(expected, Set.copyOf(received));

        assertEquals(1, roomsDao.getAll(1, 0).size());
    }
}
