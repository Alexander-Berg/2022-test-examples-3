package ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.sync;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import ru.yandex.mail.cerberus.client.dto.Group;
import ru.yandex.mail.cerberus.core.group.GroupManager;
import ru.yandex.mail.cerberus.dao.group.GroupEntity;
import ru.yandex.mail.cerberus.dao.group.RoGroupRepository;
import ru.yandex.mail.cerberus.worker.api.TaskExecutionContext;
import ru.yandex.mail.cerberus.yt.data.YtDepartmentInfo;
import ru.yandex.mail.cerberus.yt.staff.client.StaffClient;
import ru.yandex.mail.cerberus.yt.staff.client.StaffResult;
import ru.yandex.mail.cerberus.yt.staff.dto.Meta;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffDepartmentGroup;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffDepartmentHead;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffLocalizedString;
import ru.yandex.mail.cerberus.yt.staff.dto.Utils;
import ru.yandex.mail.micronaut.common.JsonMapper;
import ru.yandex.mail.micronaut.common.Pageable;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
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
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.sync.GroupSynchronizerTest.DB_NAME;
import static ru.yandex.mail.cerberus.yt.staff.StaffConstants.YT_DEPARTMENT_GROUP_TYPE;

@Slf4j(topic = "group-sync-test")
@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
@ExtendWith(MockitoExtension.class)
class GroupSynchronizerTest {
    static final String DB_NAME = "group_synchronizer_test_db";

    @Inject
    @Named("group")
    private Synchronizer synchronizer;

    @Inject
    private RoGroupRepository roGroupRepository;

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

    private static Group<YtDepartmentInfo> group(long id, String name, String url, long chiefUid, boolean isActive) {
        return new Group<>(new GroupId(id), YT_DEPARTMENT_GROUP_TYPE, name, isActive,
                new YtDepartmentInfo(Set.of(chiefUid), url, name, name));
    }

    private static StaffDepartmentGroup department(OffsetDateTime modifiedAt, long id, String name, String url,
                                                   boolean isDeleted, long chiefUid) {
        return new StaffDepartmentGroup(
                new Meta(modifiedAt),
                new GroupId(id),
                "name",
                "url",
                StaffDepartmentGroup.Types.DEPARTMENT,
                Utils.createDepartment(
                        new StaffLocalizedString(name, name),
                        isDeleted,
                        Set.of(new StaffDepartmentHead(new StaffDepartmentHead.Person(String.valueOf(chiefUid)))),
                        url
                ),
                emptyList(),
                false
        );
    }

    private List<GroupEntity> getAllGroups() {
        val groups = roGroupRepository.findAll();
        groups.sort(Comparator.comparing(entity -> entity.getId().getValue()));
        return groups;
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that nothing changes if nothing to sync")
    void testEmptyChanges(@Mock TaskExecutionContext context) {
        when(staffClientMock.groups(anyInt(), any(), anyInt(), any(), any()))
                .thenReturn(completedFuture(StaffResult.empty()));

        val groupsBefore = getAllGroups();
        val result = synchronizer.synchronize(context, Optional.empty()).block();
        assertThat(result)
                .isNull();

        verify(staffClientMock, only())
                .groups(anyInt(), any(), anyInt(), any(), any());

        assertThat(getAllGroups())
                .containsExactlyElementsOf(groupsBefore);

        verify(context, never())
                .getExecutor();
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that departments synchronization correctly applies changes from staff")
    void testSynchronization(@Mock TaskExecutionContext context) {
        val groupsCountBefore = roGroupRepository.count();

        val existingGroups = List.of(
                group(100L, "group1", "url1", 1001L, true),
                group(101L, "group2", "url2", 1002L, false),
                group(102L, "group3", "url3", 1003L, true)
        );

        CompletableFuture.allOf(StreamEx.of(existingGroups)
                .map(groupManager::insert)
                .toArray(CompletableFuture[]::new)
        ).get();

        val newDepartment = department(TODAY, 99L, "group99", "url99", false, 88005553535L);
        val updatedDepartment = department(YESTERDAY, 100L, "group11", "url11", false, 10011L);
        val deletedDepartment = department(TOMORROW, 101L, "del", "", true, 0L);
        val upToDateDepartment = department(BC, 102L, "group3", "url3", false, 1003L);
        val items = List.of(upToDateDepartment, deletedDepartment, updatedDepartment, newDepartment);

        when(staffClientMock.groups(anyInt(), any(), anyInt(), any(), any()))
                .thenAnswer(new StaffResultAnswer<>(items, jsonMapper));

        val maxModificationTime = synchronizer.synchronize(context, Optional.empty()).block();
        assertThat(maxModificationTime)
                .isAtSameInstantAs(TOMORROW);

        assertThat(roGroupRepository.count())
                .isEqualTo(groupsCountBefore + 4);

        val groups =
                groupManager.groups(YT_DEPARTMENT_GROUP_TYPE, Pageable.first(10), YtDepartmentInfo.class).get().getElements();
        for (Group<YtDepartmentInfo> group : groups) {
          log.info("got {}", group.toString());
        }
        assertThat(groups)
                .containsExactlyInAnyOrder(
                        group(99L, "group99", "url99", 88005553535L, true),
                        group(100L, "group11", "url11", 10011L, true),
                        group(101L, "del", "", 0L, false),
                        group(102L, "group3", "url3", 1003L, true)
                );

        verify(staffClientMock, times(3))
                .groups(anyInt(), any(), anyInt(), any(), any());
        verifyNoMoreInteractions(staffClientMock);

        verify(context, never())
                .getExecutor();
    }
}
