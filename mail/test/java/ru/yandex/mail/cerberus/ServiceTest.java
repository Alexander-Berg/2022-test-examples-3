package ru.yandex.mail.cerberus;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import lombok.SneakyThrows;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.mail.cerberus.client.CerberusClient;
import ru.yandex.mail.cerberus.client.dto.AllowedActions;
import ru.yandex.mail.cerberus.client.dto.Grant;
import ru.yandex.mail.cerberus.client.dto.Group;
import ru.yandex.mail.cerberus.client.dto.Location;
import ru.yandex.mail.cerberus.client.dto.ResourceData;
import ru.yandex.mail.cerberus.client.dto.ResourceType;
import ru.yandex.mail.cerberus.client.dto.RoleData;
import ru.yandex.mail.cerberus.client.dto.User;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.mail.cerberus.ServiceTest.DB_NAME;
import static ru.yandex.mail.cerberus.ServiceTest.MIGRATIONS;
import static ru.yandex.mail.cerberus.TestAction.DIG;
import static ru.yandex.mail.cerberus.TestAction.DO_NOT_DIG;
import static ru.yandex.mail.cerberus.TestAction.SLEEP;
import static ru.yandex.mail.cerberus.TestAction.WRITING_PRETTY_LONG_NAMES_WITHOUT_A_REASON;

enum TestAction {
    DIG,
    DO_NOT_DIG,
    SLEEP,
    WRITING_PRETTY_LONG_NAMES_WITHOUT_A_REASON
}

@ExtendWith(MockitoExtension.class)
@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = "test.database.name", value = DB_NAME)
class ServiceTest {
    static final String MIGRATIONS = "migrations";
    static final String DB_NAME = "cerberus_service_db";

    private static final Uid UID = new Uid(100500L);
    private static final GroupType GROUP_TYPE = new GroupType("AAA");
    private static final LocationType LOCATION_TYPE = new LocationType("RedRose");

    private static Set<String> actionsOf(TestAction... actions) {
        return StreamEx.of(actions)
            .map(Enum::name)
            .toImmutableSet();
    }

    @Inject
    CerberusClient client;

    @Test
    @SneakyThrows
    @DisplayName("Verify that permissions correctly calculates from user grants, groups and roles")
    void testPermissions() {
        val firstResourceTypeName = client.createResourceType(new ResourceType(new ResourceTypeName("resource_type"), TestAction.class)).block().getName();
        val secondResourceTypeName = client.createResourceType(new ResourceType(new ResourceTypeName("rtype"), TestAction.class)).block().getName();
        val officeKey = client.createLocation(new Location<>(new LocationId(1L), LOCATION_TYPE, "test office", Optional.empty())).block().extractKey();
        val firstResource = client.createResource(new ResourceData<>(firstResourceTypeName, "res1", Optional.of(officeKey), true)).block();
        val secondResource = client.createResource(new ResourceData<>(secondResourceTypeName, "res2", Optional.of(officeKey), true)).block();
        val role = client.createRole(new RoleData(new RoleName("lord"), Optional.of("Calendar lord"), true)).block();
        val groupId = client.addGroup(new Group<>(new GroupId(1), GROUP_TYPE, "maildev", true)).block().getId();
        client.addUser(new User<>(UID, UserType.BASIC, "login")).block();
        client.addUserToGroup(groupId, GROUP_TYPE, UID).block();
        client.assignRoleToUser(role.getId(), UID).block();

        client.setGroupGrant(groupId, GROUP_TYPE, new Grant(actionsOf(DIG, WRITING_PRETTY_LONG_NAMES_WITHOUT_A_REASON), firstResourceTypeName)).block();
        client.setRoleGrant(role.getId(), new Grant(actionsOf(DO_NOT_DIG, SLEEP), secondResourceTypeName, secondResource.getId())).block();

        client.setUserGrant(UID, new Grant(actionsOf(DIG, SLEEP), firstResourceTypeName, firstResource.getId())).block();
        client.setUserGrant(UID, new Grant(actionsOf(SLEEP), secondResourceTypeName, secondResource.getId())).block();

        val expectedFirstResourceActions = actionsOf(DIG, WRITING_PRETTY_LONG_NAMES_WITHOUT_A_REASON, SLEEP);
        val expectedSecondResourceActions = actionsOf(DO_NOT_DIG, SLEEP);

        val keys = List.of(
            firstResource.extractKey(),
            secondResource.extractKey()
        );

        assertThat(client.actions(UID, Set.copyOf(keys), ReadTarget.MASTER).block())
            .satisfies(actions -> {
                assertThat(actions.getResources())
                    .containsExactlyInAnyOrder(
                        new AllowedActions.ResourceInfo(keys.get(0), expectedFirstResourceActions),
                        new AllowedActions.ResourceInfo(keys.get(1), expectedSecondResourceActions)
                    );
            });
    }
}
