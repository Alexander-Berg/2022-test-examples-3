package ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.sync;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import lombok.SneakyThrows;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.mail.cerberus.GroupId;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.UserType;
import ru.yandex.mail.cerberus.client.dto.Group;
import ru.yandex.mail.cerberus.client.dto.User;
import ru.yandex.mail.cerberus.core.group.GroupManager;
import ru.yandex.mail.cerberus.core.user.UserManager;
import ru.yandex.mail.cerberus.dao.user.RoUserRepository;
import ru.yandex.mail.cerberus.dao.user.UserEntity;
import ru.yandex.mail.cerberus.worker.api.TaskExecutionContext;
import ru.yandex.mail.cerberus.yt.data.YtDepartmentInfo;
import ru.yandex.mail.cerberus.yt.data.YtUserInfo;
import ru.yandex.mail.cerberus.yt.data.YtUserInfo.Trait;
import ru.yandex.mail.cerberus.yt.staff.StaffConstants;
import ru.yandex.mail.cerberus.yt.staff.client.StaffClient;
import ru.yandex.mail.cerberus.yt.staff.client.StaffResult;
import ru.yandex.mail.cerberus.yt.staff.dto.Meta;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffLocalizedString;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Affiliation;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser.Gender;
import ru.yandex.mail.micronaut.common.JsonMapper;
import ru.yandex.mail.micronaut.common.Pageable;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.BC;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.TODAY;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.TOMORROW;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.YESTERDAY;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.sync.UserSynchronizerTest.DB_NAME;
import static ru.yandex.mail.micronaut.common.CerberusUtils.mapToList;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
@ExtendWith(MockitoExtension.class)
class UserSynchronizerTest {
    static final String DB_NAME = "user_synchronizer_test_db";

    @Inject
    @Named("user")
    private Synchronizer synchronizer;

    @Inject
    private RoUserRepository roUserRepository;

    @Inject
    private UserManager userManager;

    @Inject
    private GroupManager groupManager;

    @Inject
    private JsonMapper jsonMapper;

    private static final StaffClient staffClientMock = Mockito.mock(StaffClient.class);

    @MockBean(StaffClient.class)
    public StaffClient mockStaffClient() {
        return staffClientMock;
    }

    @BeforeEach
    void reset() {
        Mockito.reset(staffClientMock);
    }

    private static StaffUser.DepartmentGroup departmentGroup(long id, List<Long> ancestorIds) {
        val ancestors = StreamEx.of(ancestorIds)
            .map(GroupId::new)
            .map(ancestorId -> new StaffUser.DepartmentAncestor(ancestorId, false))
            .toImmutableList();
        return new StaffUser.DepartmentGroup(new GroupId(id), ancestors, false);
    }

    private static StaffUser staffUser(OffsetDateTime modifiedAt, long id, long uid, String login, boolean isDeleted,
                                       boolean isDismissed, boolean isRobot, boolean isHomeworker, Affiliation affiliation,
                                       StaffUser.DepartmentGroup departmentGroup, String workEmail, ZoneId tz,
                                       long officeId, int floor, String language, String name) {
        val position = new StaffLocalizedString("позиция", "position");
        val official = new StaffUser.Official(isDismissed, isRobot, isHomeworker, affiliation, position);
        val personal = new StaffUser.Personal(Gender.MALE);
        val office = new StaffUser.Location.Office(new LocationId(officeId));
        val table = new StaffUser.Location.Table(new StaffUser.Location.Table.Floor(OptionalInt.of(floor)));
        val userName = new StaffUser.Name(
            new StaffLocalizedString(name, name),
            new StaffLocalizedString(name, name),
            Optional.empty()
        );
        return new StaffUser(
            new Meta(modifiedAt),
            id,
            Long.toString(uid),
            login,
            isDeleted,
            official,
            personal,
            departmentGroup,
            Collections.emptyList(),
            workEmail,
            Optional.empty(),
            emptyList(),
            new StaffUser.Environment(tz),
            new StaffUser.Location(office, table),
            new StaffUser.Language(language),
            userName,
            emptyList(),
            Optional.empty()
        );
    }

    private static User<YtUserInfo> user(long id, long uid, String login, EnumSet<Trait> traits, Affiliation affiliation,
                                         String email, ZoneId tz, long officeId, int floor, String language, String name,
                                         Optional<Long> departmentId) {
        val info = new YtUserInfo(
            id,
            affiliation,
            email,
            tz,
            new LocationId(officeId),
            OptionalInt.of(floor),
            language,
            name,
            name,
            name,
            name,
            Optional.empty(),
            "позиция",
            "position",
            Gender.MALE,
            Optional.empty(),
            emptyList(),
            traits,
            emptyList(),
            departmentId.map(GroupId::new),
            Optional.empty()
        );
        return new User<>(new Uid(uid), UserType.YT, login, info);
    }

    private List<UserEntity> getAllUsers() {
        val users = roUserRepository.findAll();
        users.sort(Comparator.comparing(entity -> entity.getUid().getValue()));
        return users;
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that nothing changes if nothing to sync")
    void testEmptyChanges(@Mock TaskExecutionContext context) {
        when(staffClientMock.persons(anyInt(), any(), anyInt(), any(), any()))
            .thenReturn(completedFuture(StaffResult.empty()));

        val usersBefore = getAllUsers();
        val result = synchronizer.synchronize(context, Optional.empty()).block();
        assertThat(result)
            .isNull();

        verify(staffClientMock, only())
            .persons(anyInt(), any(), anyInt(), any(), any());

        assertThat(getAllUsers())
            .containsExactlyElementsOf(usersBefore);

        verify(context, never())
            .getExecutor();
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that users synchronization correctly applies changes from staff")
    void testSynchronization(@Mock TaskExecutionContext context) {
        val tz = ZoneId.systemDefault();
        val syncedDepartments = List.<Group<YtDepartmentInfo>>of(
            new Group<>(new GroupId(900L), StaffConstants.YT_DEPARTMENT_GROUP_TYPE, "g0", true),
            new Group<>(new GroupId(901L), StaffConstants.YT_DEPARTMENT_GROUP_TYPE, "g1", true),
            new Group<>(new GroupId(902L), StaffConstants.YT_DEPARTMENT_GROUP_TYPE, "g2", true),
            new Group<>(new GroupId(903L), StaffConstants.YT_DEPARTMENT_GROUP_TYPE, "g3", true)
        );

        groupManager.insert(syncedDepartments).get();

        val userCountBefore = (int) roUserRepository.count();

        val existingUsers = List.of(
            user(1, 100L, "login1", EnumSet.of(Trait.ROBOT), Affiliation.YAMONEY, "1@slave.my", tz, 1, 0, "Russian", "n1", Optional.empty()),
            user(2, 200L, "login2", EnumSet.of(Trait.HOMEWORKER, Trait.DELETED), Affiliation.EXTERNAL, "2@slave.my", tz, 1, 4, "Esperanto", "n2", Optional.empty()),
            user(3, 300L, "login3", EnumSet.of(Trait.ROBOT, Trait.HOMEWORKER), Affiliation.YAMONEY, "3@my.slave", tz, 1, 7, "C++", "n3", Optional.empty())
        );

        CompletableFuture.allOf(StreamEx.of(existingUsers)
            .map(userManager::insert)
            .toArray(CompletableFuture[]::new)
        ).get();

        val newStaffUser = staffUser(TODAY,42L, 42L, "login42", false, false, false, false, Affiliation.YANDEX,
            departmentGroup(900L, List.of(901L)), "42@master.my", tz, 1, 1, "Russian", "n0");
        val updatedStaffUser = staffUser(TOMORROW,1L, 100L, "login11", false, false, false, false, Affiliation.YANDEX,
            departmentGroup(902L, List.of(900L, 901L)), "1@master.my", tz, 2, 10, "English", "n1");
        val deletedStaffUser = staffUser(BC,2L, 200L, "login2",  true, true, false, true, Affiliation.EXTERNAL,
            departmentGroup(900L, emptyList()), "", tz, 0, 0, "", "n2");
        val upToDateStaffUser = staffUser(YESTERDAY,3L, 300L, "login3",  false, false, true, true, Affiliation.YAMONEY,
            departmentGroup(902L, List.of(900L, 901L, 903L)), "3@my.slave", tz, 1, 7, "C++", "n3");
        val items = List.of(upToDateStaffUser, deletedStaffUser, updatedStaffUser, newStaffUser);

        when(staffClientMock.persons(anyInt(), any(), anyInt(), any(), any()))
            .thenAnswer(new StaffResultAnswer<>(items, jsonMapper));

        val maxModificationTime = synchronizer.synchronize(context, Optional.empty()).block();
        assertThat(maxModificationTime)
            .isAtSameInstantAs(TOMORROW);

        val users = userManager.users(Pageable.first(Integer.MAX_VALUE), YtUserInfo.class).get().getElements();

        assertThat(users)
            .hasSize(userCountBefore + 4)
            .contains(
                user(42L, 42L, "login42", EnumSet.noneOf(Trait.class), Affiliation.YANDEX, "42@master.my", tz, 1, 1, "Russian", "n0", Optional.of(900L)),
                user(1L, 100L, "login1", EnumSet.noneOf(Trait.class), Affiliation.YANDEX,  "1@master.my", tz, 2, 10, "English", "n1", Optional.of(902L)),
                user(2L, 200L, "login2", EnumSet.of(Trait.HOMEWORKER, Trait.DELETED, Trait.DISMISSED), Affiliation.EXTERNAL, "", tz, 0, 0, "", "n2", Optional.of(900L)),
                user(3L, 300L, "login3", EnumSet.of(Trait.ROBOT, Trait.HOMEWORKER), Affiliation.YAMONEY, "3@my.slave", tz, 1, 7, "C++", "n3", Optional.of(902L))
            );

        val uids = mapToList(users, User::getUid);
        val usersGroups = roUserRepository.findUserGroupsByType(uids, StaffConstants.YT_DEPARTMENT_GROUP_TYPE).getSetMapping();

        assertThat(usersGroups)
            .contains(
                entry(new Uid(42L), Set.of(new GroupId(900L), new GroupId(901L))),
                entry(new Uid(100L), Set.of(new GroupId(900L), new GroupId(901L), new GroupId(902L))),
                entry(new Uid(200L), Set.of(new GroupId(900L))),
                entry(new Uid(300L), Set.of(new GroupId(900L), new GroupId(901L), new GroupId(902L), new GroupId(903L)))
            );

        verify(staffClientMock, times(3))
            .persons(anyInt(), any(), anyInt(), any(), any());
        verifyNoMoreInteractions(staffClientMock);

        verify(context, never())
            .getExecutor();
    }
}
