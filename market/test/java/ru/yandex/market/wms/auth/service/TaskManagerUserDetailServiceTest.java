package ru.yandex.market.wms.auth.service;

import java.util.LinkedHashSet;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.config.filters.SerialKeyColumnFilter;
import ru.yandex.market.wms.auth.dao.entity.TaskManagerUserDetail;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TaskManagerUserDetailServiceTest extends AuthIntegrationTest {

    @Autowired
    private TaskManagerUserDetailService taskManagerUserDetailService;

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
        taskManagerUserDetailService.create(userDetail);
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
                () -> taskManagerUserDetailService.create(userDetail),
                "Expected duplicate key exception, but it didn't"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/clone/before.xml")
    @ExpectedDatabase(
            value = "/db/dao/task-manager-user-detail/clone/after.xml",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {SerialKeyColumnFilter.class}
    )
    public void cloneDetails() {
        Set<String> logins = new LinkedHashSet<>(2);
        logins.add("AD2");
        logins.add("AD3");
        taskManagerUserDetailService.cloneDetails("AD1", logins);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/after.xml", assertionMode = NON_STRICT)
    public void deleteBySerialKey() {
        taskManagerUserDetailService.deleteBySerialKey("234");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/after.xml", assertionMode = NON_STRICT)
    public void deleteByPrimaryKey() {
        taskManagerUserDetailService.deleteByPrimaryKey("user1", "00005");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(
            value = "/db/dao/task-manager-user-detail/delete/after-delete-by-login.xml",
            assertionMode = NON_STRICT
    )
    public void deleteByLogin() {
        taskManagerUserDetailService.deleteByPrimaryKey("user1", null);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/before.xml", assertionMode = NON_STRICT)
    public void deleteNonExistentBySerialKey() {
        taskManagerUserDetailService.deleteBySerialKey("656");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/before.xml", assertionMode = NON_STRICT)
    public void deleteNonExistentByPrimaryKey() {
        taskManagerUserDetailService.deleteByPrimaryKey("user10", "00005");
    }
}
