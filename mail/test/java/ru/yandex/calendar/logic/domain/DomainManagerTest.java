package ru.yandex.calendar.logic.domain;

import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.calendar.micro.yt.entity.YtUser;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.yt.data.YtUserInfo;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser;

import static java.util.Collections.emptyList;

public class DomainManagerTest {
    @Test
    public void testParse() {
        final YtUser ytUser = new YtUser(new Uid(1120000000045832L), "robot-calendar", new YtUserInfo(
                1, StaffUser.Affiliation.YANDEX, "robot-calendar@yandex-team.ru", ZoneId.of("Europe/Moscow"),
                new LocationId(1), OptionalInt.empty(), "ru", "Хранитель", "Календаря",
                "Calendar", "Keeper", Optional.empty(), "гуру", "guru", StaffUser.Gender.MALE,
                Optional.empty(), emptyList(), EnumSet.noneOf(YtUserInfo.Trait.class), emptyList(), Optional.empty(), Optional.empty()
        ));
        final Optional<String> phone = DomainManager.getPhone(Optional.of(ytUser));
        Assert.assertTrue(phone.isEmpty());
    }
}
