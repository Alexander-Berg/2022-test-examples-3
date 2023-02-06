package ru.yandex.mail.cerberus.dao.user;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.mail.cerberus.GroupId;
import ru.yandex.mail.cerberus.GroupKey;
import ru.yandex.mail.cerberus.GroupType;
import ru.yandex.mail.cerberus.RoleId;
import ru.yandex.mail.cerberus.RoleName;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.UserType;
import ru.yandex.mail.cerberus.dao.group.GroupEntity;
import ru.yandex.mail.cerberus.dao.group.GroupRepository;
import ru.yandex.mail.cerberus.dao.role.RoleEntity;
import ru.yandex.mail.cerberus.dao.role.RoleRepository;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.mail.micronaut.common.CerberusUtils.mapToList;
import static ru.yandex.mail.cerberus.dao.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.dao.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.dao.user.UserRepositoryTest.DB_NAME;
import static ru.yandex.mail.micronaut.common.CerberusUtils.mapToMap;
import static ru.yandex.mail.micronaut.common.CerberusUtils.mapToSet;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
class UserRepositoryTest {
    static final String DB_NAME = "user_repository_db";

    private static final GroupType GROUP_TYPE = new GroupType("type");

    private static final List<UserEntity> ENTITIES = List.of(
        new UserEntity(new Uid(42L), UserType.BASIC, "usr", Optional.of("{\"info\": \"inf\"}")),
        new UserEntity(new Uid(100500L), UserType.BASIC, "usr2", Optional.of("{\"info\": \"fni\"}"))
    );

    private static final List<GroupEntity> GROUP_ENTITIES = List.of(
        new GroupEntity(new GroupId(100L), GROUP_TYPE, "g1", true, Optional.empty()),
        new GroupEntity(new GroupId(200L), GROUP_TYPE, "g2", true, Optional.empty()),
        new GroupEntity(new GroupId(300L), GROUP_TYPE, "g3", true, Optional.empty()),
        new GroupEntity(new GroupId(400L), GROUP_TYPE, "g4", true, Optional.empty())
    );

    private static final List<RoleEntity> ROLE_ENTITIES = List.of(
        new RoleEntity(new RoleId(101L), new RoleName("r1"), true, Optional.empty()),
        new RoleEntity(new RoleId(102L), new RoleName("r2"), true, Optional.empty())
    );

    private static <T> Comparator<T> randomComparator() {
        return (o1, o2) -> ThreadLocalRandom.current().nextInt(-1, 2);
    }

    @Inject
    private UserRepository userRepository;

    @Inject
    private GroupRepository groupRepository;

    @Inject
    private RoleRepository roleRepository;

    private static List<UserEntity> entities = emptyList();
    private static List<GroupId> groupIds = emptyList();
    private static List<RoleId> roleIds = emptyList();
    private static List<Uid> uids = emptyList();

    @BeforeEach
    void init() {
        if (!entities.isEmpty()) {
            return;
        }

        entities = userRepository.insertAll(ENTITIES);
        val groupEntities = groupRepository.insertAll(GROUP_ENTITIES);
        val roleEntities = roleRepository.insertAll(ROLE_ENTITIES);

        uids = mapToList(entities, UserEntity::getUid);
        groupIds = mapToList(groupEntities, GroupEntity::getId);
        roleIds = mapToList(roleEntities, RoleEntity::getId);
    }

    @Test
    @DisplayName("Verify that users could be added/removed to/from groups")
    void testGroupsManagement() {
        val groupsPerUser = 2;
        final var groupMapping = StreamEx.of(uids)
            .toMap(uid -> {
                return StreamEx.of(groupIds)
                    .sorted(randomComparator())
                    .limit(groupsPerUser)
                    .toImmutableSet();
            });

        userRepository.addToGroup(GROUP_TYPE, groupMapping);

        assertThat(userRepository.findUserGroupsByType(uids, GROUP_TYPE).getSetMapping())
            .containsExactlyEntriesOf(groupMapping);

        assertThat(userRepository.findUsersGroupKeys(uids).getSetMapping())
            .containsExactlyEntriesOf(
                EntryStream.of(groupMapping)
                    .mapValues(ids -> mapToSet(ids, id -> new GroupKey(id, GROUP_TYPE)))
                    .toImmutableMap()
            );

        userRepository.removeFromGroup(GROUP_TYPE, groupMapping);

        assertThat(userRepository.findUserGroupsByType(uids, GROUP_TYPE).getMapping())
            .containsExactlyEntriesOf(
                EntryStream.of(groupMapping)
                    .mapValues(ignored -> Collections.<GroupId>emptyList())
                    .toMap()
            );
    }

    @Test
    @DisplayName("Verify that role could be assigned/detached to/from user")
    void testRolesManagement() {
        val rolesPerUser = 2;
        final var roleMapping = StreamEx.of(uids)
            .toMap(uid -> {
                return StreamEx.of(roleIds)
                    .sorted(randomComparator())
                    .limit(rolesPerUser)
                    .toImmutableSet();
            });

        userRepository.attachRoles(roleMapping);
        assertThat(userRepository.findUserRoles(uids).getSetMapping())
            .containsExactlyEntriesOf(roleMapping);

        userRepository.detachRoles(roleMapping);
        assertThat(userRepository.findUserRoles(uids).getSetMapping())
            .containsExactlyEntriesOf(
                mapToMap(uids, identity(), uid -> emptySet())
            );
    }
}
