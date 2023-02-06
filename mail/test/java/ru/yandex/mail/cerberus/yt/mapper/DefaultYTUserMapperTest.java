package ru.yandex.mail.cerberus.yt.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import javax.inject.Inject;

import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.mail.cerberus.GroupId;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.UserType;
import ru.yandex.mail.cerberus.client.dto.User;
import ru.yandex.mail.cerberus.yt.data.YtUserInfo;
import ru.yandex.mail.cerberus.yt.data.YtUserInfo.Trait;
import ru.yandex.mail.cerberus.yt.staff.dto.Meta;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffLocalizedString;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Affiliation;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Car;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.DepartmentAncestor;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.DepartmentGroup;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Environment;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Language;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Location;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Location.Office;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Location.Table;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Location.Table.Floor;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Name;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Official;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Personal;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Phone;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.PhoneType;
import ru.yandex.mail.micronaut.common.CerberusUtils;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class DefaultYTUserMapperTest {
    @Inject
    private YtUserMapper mapper;

    private static final long ID = 42L;
    private static final Uid UID = new Uid(100500L);
    private static final GroupId USER_DEPARTMENT_GROUP_ID = new GroupId(99L);
    private static final Set<GroupId> USER_DEPARTMENT_GROUP_ANCESTOR_IDS = Set.of(
        new GroupId(101L), new GroupId(102L), new GroupId(103L)
    );
    private static final String EMAIL = "email@email.com";
    private static final ZoneId TZ = ZoneId.systemDefault();
    private static final LocationId OFFICE_ID = new LocationId(22);
    private static final OptionalInt FLOOR = OptionalInt.of(2);
    private static final String LANG = "la lango";
    private static final List<Phone> PHONES = List.of(
        new Phone(PhoneType.MOBILE, "88005553535", true),
        new Phone(PhoneType.HOME, "101010", false)
    );
    private static final List<Car> CARS = List.of(
        new Car("A001AA", "Jeep Grand Cherokee green"),
        new Car("T361AA", "tesla")
    );
    private static final Meta META = new Meta(OffsetDateTime.MAX);
    private static final StaffUser STAFF_USER = new StaffUser(
        META,
        ID,
        UID.toString(),
        "login",
        false,
        new Official(
            false,
            false,
            true,
            Affiliation.EXTERNAL,
            new StaffLocalizedString("позиция", "position")
        ),
        new Personal(StaffUser.Gender.MALE),
        new DepartmentGroup(
            USER_DEPARTMENT_GROUP_ID,
            CerberusUtils.mapToList(USER_DEPARTMENT_GROUP_ANCESTOR_IDS, id -> new DepartmentAncestor(id, false)),
            false
        ),
        Collections.emptyList(),
        EMAIL,
        Optional.of(2223322L),
        PHONES,
        new Environment(TZ),
        new Location(new Office(OFFICE_ID), new Table(new Floor(FLOOR))),
        new Language(LANG),
        new Name(
            new StaffLocalizedString("имя", "name"),
            new StaffLocalizedString("фамилие", "surname"),
            Optional.empty()
        ),
        CARS,
        Optional.empty()
    );

    @Test
    @DisplayName("Verify that 'mapToUser' return correct user data")
    void testMapToUser() {
        val expectedInfo = new YtUserInfo(
            ID,
            Affiliation.EXTERNAL,
            EMAIL,
            TZ,
            OFFICE_ID,
            FLOOR,
            LANG,
            "имя",
            "фамилие",
            "name",
            "surname",
            Optional.empty(),
            "позиция",
            "position",
            StaffUser.Gender.MALE,
            Optional.of(2223322L),
            PHONES,
            EnumSet.of(Trait.HOMEWORKER),
            CARS,
            Optional.of(USER_DEPARTMENT_GROUP_ID),
            Optional.empty()
        );
        val expectedUser = new User<>(UID, UserType.YT, "login", expectedInfo);

        val userData = mapper.mapToUser(STAFF_USER);
        assertThat(userData).isEqualTo(expectedUser);
    }
}
