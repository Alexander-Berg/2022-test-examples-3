package ru.yandex.calendar.logic.user;

import java.util.Arrays;
import java.util.EnumSet;

import lombok.val;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.inside.passport.PassportUid;

import static org.assertj.core.api.Assertions.assertThat;

public class UserInfoTest {
    private static final Integer GROUP = 42;

    private static final Resource ROOM = makeResource("Room", Option.empty(), ResourceType.ROOM);
    private static final Resource GROUPED_ROOM = makeResource("Room", Option.of(GROUP), ResourceType.ROOM);
    private static final Resource PROTECTED_ROOM = makeResource("ProtectedRoom", Option.of(GROUP), ResourceType.PROTECTED_ROOM);
    private static final Resource PRIVATE_ROOM = makeResource("PrivateRoom", Option.of(GROUP), ResourceType.PRIVATE_ROOM);

    private static final UserInfo TOWNEE = makeUser();
    private static final UserInfo RESOURCE_USER = makeUser(GROUP, true, false);
    private static final UserInfo RESOURCE_ADMIN = makeUser(GROUP, false, true);

    private static final UserInfo ROOM_ADMIN = makeSpecialUser(Group.MEETING_ROOM_ADMIN);
    private static final UserInfo SUPER_USER = makeSpecialUser(Group.SUPER_USER);

    private static Resource makeResource(String exchangeName, Option<Integer> accessGroup, ResourceType resourceType) {
        val resource = new Resource();
        resource.setExchangeName(exchangeName);
        resource.setAccessGroup(accessGroup);
        resource.setType(resourceType);
        resource.setIsActive(true);
        return resource;
    }

    private static UserInfo makeUser() {
        return new UserInfo(
                PassportUid.MIN_VALUE,
                EnumSet.noneOf(Group.class),
                Cf.list(),
                Cf.list(),
                false,
                false
        );
    }

    private static UserInfo makeSpecialUser(Group... groups) {
        return new UserInfo(
                PassportUid.MIN_VALUE,
                EnumSet.copyOf(Arrays.asList(groups)),
                Cf.list(),
                Cf.list(),
                false,
                false
        );
    }

    private static UserInfo makeUser(Integer accessGroup, boolean resourceUser, boolean resourceAdmin) {
        return new UserInfo(
                PassportUid.MIN_VALUE,
                EnumSet.noneOf(Group.class),
                Option.when(resourceUser, accessGroup),
                Option.when(resourceAdmin, accessGroup),
                false,
                false
        );
    }

    public static class CheckPermissionsTest {
        private static void assertUserPermissions(
                UserInfo user, Resource resource,
                boolean canView, boolean canBook, boolean canAdmin, boolean canIgnoreMaxEventStart
        ) {
            assertThat(user.canViewResource(resource)).isEqualTo(canView);
            assertThat(user.canBookResource(resource)).isEqualTo(canBook);
            assertThat(user.canAdminResource(resource)).isEqualTo(canAdmin);
            assertThat(user.canIgnoreMaxEventStart(resource)).isEqualTo(canIgnoreMaxEventStart);
        }

        @Test
        public void checkPlainUserPermissions() {
            assertUserPermissions(TOWNEE, ROOM, true, true, false, false);
            assertUserPermissions(TOWNEE, GROUPED_ROOM, true, true, false, false);
            assertUserPermissions(TOWNEE, PROTECTED_ROOM, true, false, false, false);
            assertUserPermissions(TOWNEE, PRIVATE_ROOM, false, false, false, false);
        }

        @Test
        public void checkResourceUserPermissions() {
            assertUserPermissions(RESOURCE_USER, ROOM, true, true, false, false);
            assertUserPermissions(RESOURCE_USER, GROUPED_ROOM, true, true, false, true);
            assertUserPermissions(RESOURCE_USER, PROTECTED_ROOM, true, true, false, true);
            assertUserPermissions(RESOURCE_USER, PRIVATE_ROOM, true, true, false, true);
        }

        @Test
        public void checkResourceAdminPermissions() {
            assertUserPermissions(RESOURCE_ADMIN, ROOM, true, true, false, false);
            assertUserPermissions(RESOURCE_ADMIN, GROUPED_ROOM, true, true, true, true);
            assertUserPermissions(RESOURCE_ADMIN, PROTECTED_ROOM, true, true, true, true);
            assertUserPermissions(RESOURCE_ADMIN, PRIVATE_ROOM, true, true, true, true);
        }

        @Test
        public void checkRoomAdminPermissions() {
            assertUserPermissions(ROOM_ADMIN, ROOM, true, true, true, true);
            assertUserPermissions(ROOM_ADMIN, GROUPED_ROOM, true, true, true, true);
            assertUserPermissions(ROOM_ADMIN, PROTECTED_ROOM, true, false, false, false);
            assertUserPermissions(ROOM_ADMIN, PRIVATE_ROOM, false, false, false, false);
        }

        @Test
        public void checkSuperUserPermissions() {
            assertUserPermissions(SUPER_USER, ROOM, true, true, true, true);
            assertUserPermissions(SUPER_USER, GROUPED_ROOM, true, true, true, true);
            assertUserPermissions(SUPER_USER, PROTECTED_ROOM, true, true, true, true);
            assertUserPermissions(SUPER_USER, PRIVATE_ROOM, true, true, true, true);
        }
    }
}
