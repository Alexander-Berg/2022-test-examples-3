package ru.yandex.mail.cerberus.core.user;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.mail.cerberus.GroupType;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.UserType;
import ru.yandex.mail.cerberus.client.dto.User;
import ru.yandex.mail.cerberus.core.CollisionStrategy;
import ru.yandex.mail.cerberus.exception.UserAlreadyExistsException;
import ru.yandex.mail.cerberus.core.group.GroupManager;
import ru.yandex.mail.cerberus.dao.user.RoUserRepository;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.mail.cerberus.core.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.core.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.core.user.UserManagerTest.DB_NAME;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false, propertySources = "classpath:application_with_database.yml")
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
public class UserManagerTest {
    static final String DB_NAME = "user_manager_db";

    @Inject
    UserManager userManager;

    @Inject
    GroupManager groupManager;

    @Inject
    RoUserRepository userRepository;

    @Test
    @SneakyThrows
    @DisplayName("Verify that new user is a member of default group")
    void testNewUserIsAMemberOfDefaultGroup() {
        val defaultGroupId = groupManager.findDefaultGroup().get().getId();

        val uid = new Uid(100020202L);
        val user = new User<>(uid, UserType.BASIC, "vasya", Optional.empty());
        userManager.insert(user).get();

        val groupsMap = userManager.findUsersGroupsByType(singleton(uid), GroupType.INTERNAL).get();

        assertThat(groupsMap)
            .containsOnly(
                entry(uid, singleton(defaultGroupId))
            );
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that duplicate user cannot be added")
    void testDuplicateUserInsertion() {
        val uid = new Uid(100030303L);
        val user = new User<>(uid, UserType.BASIC, "vasya", Optional.<Void>empty());

        val userCountBefore = userRepository.count();

        userManager.insert(user).get();

        val dupUser = new User<>(uid, UserType.YT, "pupkin", Optional.empty());
        assertThatThrownBy(() -> userManager.insert(dupUser).get())
            .hasRootCauseExactlyInstanceOf(UserAlreadyExistsException.class);

        assertThat(userManager.find(uid, void.class).get())
            .contains(user);
        assertThat(userRepository.count())
            .isEqualTo(userCountBefore + 1);
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that duplicate user cannot be added while inserting batch")
    void testDuplicateBatchUserInsertion() {
        val uid = new Uid(100040404L);
        val user = new User<>(uid, UserType.BASIC, "vasya", Optional.<Void>empty());

        val userCountBefore = userRepository.count();

        userManager.insert(user).get();

        val newUid = new Uid(100050505L);
        val newUser = new User<>(newUid, UserType.BASIC, "aysav", Optional.<Void>empty());
        assertThatThrownBy(() -> userManager.insert(CollisionStrategy.FAIL, List.of(user, newUser)).get())
            .hasRootCauseExactlyInstanceOf(UserAlreadyExistsException.class);

        assertThat(userManager.find(uid, void.class).get())
            .contains(user);
        assertThat(userManager.find(newUid, void.class).get())
            .isEmpty();
        assertThat(userRepository.count())
            .isEqualTo(userCountBefore + 1);
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that duplicate user cannot be added while inserting batch using ignore_duplicates flag")
    void testDuplicateBatchUserInsertionIgnoreDuplicates() {
        val uid = new Uid(100060606L);
        val user = new User<>(uid, UserType.BASIC, "vasya", Optional.<Void>empty());

        val userCountBefore = userRepository.count();

        userManager.insert(user).get();

        val newUid = new Uid(100070707L);
        val newUser = new User<>(newUid, UserType.BASIC, "aysav", Optional.<Void>empty());
        userManager.insert(CollisionStrategy.SKIP_EXISTING, List.of(user, newUser)).get();

        assertThat(userManager.find(uid, void.class).get())
            .contains(user);
        assertThat(userManager.find(newUid, void.class).get())
            .contains(newUser);
        assertThat(userRepository.count())
            .isEqualTo(userCountBefore + 2);
    }
}
