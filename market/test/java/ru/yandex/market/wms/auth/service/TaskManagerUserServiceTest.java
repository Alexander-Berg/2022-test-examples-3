package ru.yandex.market.wms.auth.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.config.filters.SerialKeyColumnFilter;
import ru.yandex.market.wms.auth.dao.entity.TaskManagerUser;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskManagerUserServiceTest extends AuthIntegrationTest {

    @Autowired
    private TaskManagerUserService taskManagerUserService;

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/insert/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/insert/after.xml", assertionMode = NON_STRICT)
    public void insert() {
        TaskManagerUser user = TaskManagerUser.builder()
                .userKey("user3")
                .priorityTaskType("1")
                .strategyKey("")
                .equipmentProfileKey("")
                .lastCaseIdPicked("")
                .lastWaveKey("")
                .ttmStrategyKey("STD")
                .lastLoc("")
                .usrStatus(1)
                .hourlyRate(new BigDecimal(0))
                .addWho("auth")
                .editWho("auth")
                .isNewbie(true)
                .build();
        int serialKey = taskManagerUserService.create(user);
        assertTrue(serialKey > 0);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/insert/before.xml")
    public void insertDuplicate() {
        TaskManagerUser user = TaskManagerUser.builder()
                .userKey("user2")
                .priorityTaskType("1")
                .strategyKey("")
                .equipmentProfileKey("")
                .lastCaseIdPicked("")
                .lastWaveKey("")
                .ttmStrategyKey("STD")
                .lastLoc("")
                .usrStatus(1)
                .hourlyRate(new BigDecimal(0))
                .addWho("auth")
                .editWho("auth")
                .isNewbie(false)
                .build();
        assertThrows(
                DuplicateKeyException.class,
                () -> taskManagerUserService.create(user),
                "Expected duplicate key exception, but it didn't"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/clone/before.xml")
    @ExpectedDatabase(
            value = "/db/dao/task-manager-user/clone/after.xml",
            assertionMode = NON_STRICT,
            columnFilters = {SerialKeyColumnFilter.class}
    )
    public void cloneUser() {
        taskManagerUserService.cloneUser("AD1", Set.of("AD2"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/delete/after.xml", assertionMode = NON_STRICT)
    public void delete() {
        taskManagerUserService.delete("user3");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/delete/before.xml", assertionMode = NON_STRICT)
    public void deleteNonExistent() {
        taskManagerUserService.delete("user5");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/update/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/update/after.xml", assertionMode = NON_STRICT)
    public void updateNewbieStatus() {
        taskManagerUserService.updateNewbieStatus(List.of("user1", "user2"));
    }
}
