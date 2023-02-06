package ru.yandex.market.wms.auth.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.config.filters.SerialKeyColumnFilter;
import ru.yandex.market.wms.auth.dao.entity.TaskManagerUserDetail;
import ru.yandex.market.wms.common.model.enums.DatabaseSchema;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskManagerUserDetailDaoTest extends AuthIntegrationTest {

    @Autowired
    private TaskManagerUserDetailDao taskManagerUserDetailDao;

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/insert/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/insert/after.xml", assertionMode = NON_STRICT)
    public void insert() {
        TaskManagerUserDetail userDetail = TaskManagerUserDetail.builder()
                .userKey("user2")
                .userLineNumber("00006")
                .permissionType("")
                .areaKey("")
                .permission("1")
                .description("")
                .addWho("auth")
                .editWho("auth")
                .build();
        taskManagerUserDetailDao.create(userDetail, DatabaseSchema.WMWHSE1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/insert/before-many.xml")
    @ExpectedDatabase(
            value = "/db/dao/task-manager-user-detail/insert/after-many.xml",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {SerialKeyColumnFilter.class}
    )
    public void insertBatch() {
        List<TaskManagerUserDetail> details = List.of(
                TaskManagerUserDetail.builder()
                        .userKey("ADD666")
                        .userLineNumber("00001")
                        .permissionType("ACNS")
                        .areaKey("DOLOR123")
                        .permission("1")
                        .description("")
                        .taskManagerUserDetailId("rtqerctwertcqerw1")
                        .addWho("datacreator")
                        .editWho("datacreator")
                        .build(),
                TaskManagerUserDetail.builder()
                        .userKey("ADD666")
                        .userLineNumber("00002")
                        .permissionType("QC")
                        .areaKey("DOLOR123")
                        .permission("1")
                        .description("")
                        .taskManagerUserDetailId("qwerqwereqw")
                        .addWho("datacreator")
                        .editWho("datacreator")
                        .build()
        );
        taskManagerUserDetailDao.create(details, DatabaseSchema.WMWHSE1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/insert/before.xml")
    public void insertDuplicate() {
        TaskManagerUserDetail userDetail = TaskManagerUserDetail.builder()
                .userKey("user2")
                .userLineNumber("00004")
                .addWho("auth")
                .editWho("auth")
                .build();
        assertThrows(
                DataIntegrityViolationException.class,
                () -> taskManagerUserDetailDao.create(userDetail, DatabaseSchema.WMWHSE1),
                "Expected duplicate key exception, but it didn't"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/get/details.xml")
    public void get() {
        List<TaskManagerUserDetail> expected = List.of(
                TaskManagerUserDetail.builder()
                        .userKey("ADD666")
                        .userLineNumber("00001")
                        .permissionType("ACNS")
                        .areaKey("DOLOR123")
                        .permission("1")
                        .description("")
                        .taskManagerUserDetailId("rtqerctwertcqerw1")
                        .addWho("datacreator")
                        .editWho("datacreator")
                        .build(),
                TaskManagerUserDetail.builder()
                        .userKey("ADD666")
                        .userLineNumber("00002")
                        .permissionType("QC")
                        .areaKey("DOLOR123")
                        .permission("1")
                        .description("")
                        .taskManagerUserDetailId("qwerqwereqw")
                        .addWho("datacreator")
                        .editWho("datacreator")
                        .build()
        );
        List<TaskManagerUserDetail> actual = taskManagerUserDetailDao.get("ADD666", DatabaseSchema.WMWHSE1);
        assertTrue(expected.size() == actual.size() && actual.containsAll(expected) && expected.containsAll(actual));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/after.xml", assertionMode = NON_STRICT)
    public void deleteBySerialKey() {
        taskManagerUserDetailDao.deleteBySerialKey("234", DatabaseSchema.WMWHSE1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/after.xml", assertionMode = NON_STRICT)
    public void deleteByPrimaryKey() {
        taskManagerUserDetailDao.deleteByPrimaryKey("user1", "00005", DatabaseSchema.WMWHSE1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(
            value = "/db/dao/task-manager-user-detail/delete/after-delete-by-login.xml",
            assertionMode = NON_STRICT
    )
    public void deleteByLogin() {
        taskManagerUserDetailDao.deleteByPrimaryKey("user1", null, DatabaseSchema.WMWHSE1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/before.xml", assertionMode = NON_STRICT)
    public void deleteNonExistentBySerialKey() {
        taskManagerUserDetailDao.deleteBySerialKey("656", DatabaseSchema.WMWHSE1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/before.xml", assertionMode = NON_STRICT)
    public void deleteNonExistentByPrimaryKey() {
        taskManagerUserDetailDao.deleteByPrimaryKey("user10", "00005", DatabaseSchema.WMWHSE1);
    }
}
