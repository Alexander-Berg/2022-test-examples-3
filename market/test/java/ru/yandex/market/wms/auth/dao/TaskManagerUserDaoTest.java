package ru.yandex.market.wms.auth.dao;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.dao.entity.TaskManagerUser;
import ru.yandex.market.wms.common.model.enums.DatabaseSchema;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskManagerUserDaoTest extends AuthIntegrationTest {

    @Autowired
    private TaskManagerUserDao taskManagerUserDao;

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
                .isOutstaff(false)
                .isNewbie(false)
                .build();
        int serialKey = taskManagerUserDao.create(user, DatabaseSchema.WMWHSE1);
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
                .isNewbie(true)
                .build();
        assertThrows(
                DataIntegrityViolationException.class,
                () -> taskManagerUserDao.create(user, DatabaseSchema.WMWHSE1),
                "Expected duplicate key exception, but it didn't"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/delete/after.xml", assertionMode = NON_STRICT)
    public void delete() {
        taskManagerUserDao.deleteByLogin("user3", DatabaseSchema.WMWHSE1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/delete/before.xml", assertionMode = NON_STRICT)
    public void deleteNonExistent() {
        taskManagerUserDao.deleteByLogin("user5", DatabaseSchema.WMWHSE1);
    }
}
