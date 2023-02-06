package ru.yandex.mail.cerberus.dao.group;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import ru.yandex.mail.cerberus.GroupId;
import ru.yandex.mail.cerberus.GroupType;
import ru.yandex.mail.cerberus.RoleId;
import ru.yandex.mail.cerberus.RoleName;
import ru.yandex.mail.cerberus.dao.role.RoleEntity;
import ru.yandex.mail.cerberus.dao.role.RoleRepository;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.mail.cerberus.dao.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.dao.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.dao.group.GroupRepositoryTest.DB_NAME;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
class GroupRepositoryTest {
    static final String DB_NAME = "group_repository_db";

    private static final GroupType GROUP_TYPE = new GroupType("group-type");
    private static final List<GroupEntity> NEW_ENTITIES = List.of(
        new GroupEntity(new GroupId(100L), GROUP_TYPE, "g2", true, Optional.empty()),
        new GroupEntity(new GroupId(200L), GROUP_TYPE, "g9", false, Optional.empty())
    );

    @Inject
    private GroupRepository groupRepository;

    @Inject
    RoleRepository roleRepository;

    private static List<GroupEntity> entities = emptyList();

    @BeforeEach
    void cleanup() {
        if (!entities.isEmpty()) {
            return;
        }

        entities = groupRepository.insertAll(NEW_ENTITIES);
    }

    @Test
    @DisplayName("Verify that role could be assigned to a group or detached from a group")
    void testRoleManagement() {
        val entity = entities.get(0);
        val roleId = new RoleId(1L);
        val unexistingGroupId = new GroupId(100500L);

        roleRepository.insert(new RoleEntity(roleId, new RoleName("role"), true, Optional.empty()));

        groupRepository.attachRole(entity.getId(), entity.getType(), roleId);
        assertThat(groupRepository.findGroupRoleIds(GROUP_TYPE, entity.getId()))
            .containsExactlyInAnyOrder(roleId);

        assertThatThrownBy(() -> groupRepository.attachRole(unexistingGroupId, GROUP_TYPE, roleId))
            .isExactlyInstanceOf(DataIntegrityViolationException.class);

        groupRepository.detachRole(entity.getId(), entity.getType(), roleId);
        assertThat(groupRepository.findGroupRoleIds(GROUP_TYPE, entity.getId()))
            .isEmpty();

        assertThatCode(() -> groupRepository.detachRole(unexistingGroupId, GROUP_TYPE, roleId))
            .doesNotThrowAnyException();
    }
}
