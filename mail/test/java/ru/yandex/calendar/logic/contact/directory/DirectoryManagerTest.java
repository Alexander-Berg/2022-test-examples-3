package ru.yandex.calendar.logic.contact.directory;

import lombok.val;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.contact.directory.search.DirectorySearchField;
import ru.yandex.calendar.logic.contact.directory.search.DirectorySearchFieldOperator;
import ru.yandex.calendar.logic.contact.directory.search.DirectorySearchPredicate;
import ru.yandex.calendar.logic.user.StaffCacheStub;
import ru.yandex.calendar.logic.user.TestUsers;
import ru.yandex.calendar.micro.yt.StaffCache;
import ru.yandex.calendar.micro.yt.entity.YtUser;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.test.generic.TestBaseContextConfiguration;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.yt.data.YtUserInfo;
import ru.yandex.mail.cerberus.yt.data.YtUserInfo.Trait;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Affiliation;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Gender;
import ru.yandex.misc.email.Email;

import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = DirectoryManagerTest.MockedStaffCacheConfiguration.class)
@TestExecutionListeners
public class DirectoryManagerTest extends AbstractConfTest {
    private static final List<YtUser> USERS = List.of(
        new YtUser(new Uid(1120000000045832L), "robot-calendar", new YtUserInfo(
            1, Affiliation.YANDEX, "robot-calendar@yandex-team.ru", ZoneId.of("Europe/Moscow"),
            new LocationId(1), OptionalInt.empty(), "ru", "Хранитель", "Календаря",
            "Calendar", "Keeper", Optional.empty(), "гуру", "guru", Gender.MALE,
            Optional.empty(), emptyList(), EnumSet.noneOf(Trait.class), emptyList(), Optional.empty(), Optional.empty()
        )),
        new YtUser(new Uid(1120000000004717L), "calendartestuser", new YtUserInfo(
            2, Affiliation.YANDEX, "calendartestuser@yandex-team.ru", ZoneId.of("Europe/Moscow"),
            new LocationId(1), OptionalInt.empty(), "ru", "Тест", "Календарев",
            "Test", "Calendar", Optional.empty(), "подмастерье", "scholar", Gender.MALE,
            Optional.empty(), emptyList(), EnumSet.noneOf(Trait.class), emptyList(), Optional.empty(), Optional.empty()
        ))
    );

    @Autowired
    private TestManager testManager;
    @Autowired
    private DirectoryManager directoryManager;

    @BeforeClass
    public static void beforeAnyTest() {
        ru.yandex.calendar.util.conf.Configuration.initializeEnvironment(false);
    }

    public static DirectorySearchPredicate predicate(String text) {
        DirectorySearchPredicate fnPredicate = DirectorySearchPredicate.fieldPredicate(
                DirectorySearchField.DISPLAY_NAME, DirectorySearchFieldOperator.CONTAINS, text);
        DirectorySearchPredicate emailPredicate = DirectorySearchPredicate.fieldPredicate(
                DirectorySearchField.EMAIL, DirectorySearchFieldOperator.STARTS_WITH, text);

        return DirectorySearchPredicate.anyPredicate(Cf.list(fnPredicate, emailPredicate));
    }

    @Test
    public void someUsersNotFound() {
        val user = testManager.prepareYandexUser(TestManager.createAkirakozov());

        assertThat(directoryManager.getDirectory(user.getUid()))
            .isNotEmpty();

        val robotCalendar =
                directoryManager.findContacts(user.getUid(), predicate("robot-calendar"), false, 50).firstO();
        val calendarTestUser =
                directoryManager.findContacts(user.getUid(), predicate("calendartestuser"), false, 50).firstO();
        assertThat(robotCalendar.toOptional())
            .isNotEmpty();
        assertThat(calendarTestUser.toOptional())
            .isNotEmpty();

        assertThat(robotCalendar.get().getDisplayName())
            .isNotBlank();
        assertThat(calendarTestUser.get().getDisplayName())
            .isNotBlank();
    }

    @Test
    public void roomFoundByNames() {
        val uid = TestUsers.AKIRAKOZOV;

        testManager.cleanAndCreateResource("conf_st_5_6", "Камчатка");
        val email = new Email("conf_st_5_6@yandex-team.ru");

        assertThat(directoryManager.findContacts(uid, predicate("Камчатка"), false, 50).single().getEmail())
            .isEqualTo(email);
        assertThat(directoryManager.findContacts(uid, predicate("Kamchatka"), false, 50).single().getEmail())
            .isEqualTo(email);
        assertThat(directoryManager.findContacts(uid, predicate("conf_st_5_6"), false, 50).single().getEmail())
            .isEqualTo(email);
    }

    @ContextConfiguration
    @Import(TestBaseContextConfiguration.class)
    public static class MockedStaffCacheConfiguration {
        @Bean
        public StaffCache staffCache() {
            return new StaffCacheStub() {
                @Override
                public List<YtUser> getUsers() {
                    return USERS;
                }
            };
        }
    }
}
