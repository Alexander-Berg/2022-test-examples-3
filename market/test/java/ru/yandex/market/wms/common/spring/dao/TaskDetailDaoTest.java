package ru.yandex.market.wms.common.spring.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.TaskDetail;
import ru.yandex.market.wms.common.spring.dao.implementation.TaskDetailDao;


public class TaskDetailDaoTest extends IntegrationTest {

    @Autowired
    private TaskDetailDao taskDetailDao;

    @Test
    @DatabaseSetup("/db/dao/task-detail/get-active-bbxd-task.xml")
    public void getActiveBbxdTask() {
        var result =
                taskDetailDao.getActiveBbxdTask("CARRIER", "ROV0000000000000000370", "465852", "TEST", "PL123").get();
        assertions.assertThat(result.getCarrierCode()).isEqualTo("CARRIER");
        assertions.assertThat(result.getSku()).isEqualTo("ROV0000000000000000370");
        assertions.assertThat(result.getStorerKey()).isEqualTo("465852");
        assertions.assertThat(result.getUserKey()).isEqualTo("TEST");
        assertions.assertThat(result.getFromId()).isEqualTo("PL123");
    }

    @Test
    @DatabaseSetup("/db/dao/task-detail/get-active-bbxd-task.xml")
    public void getActiveBbxdTasks() {
        var tasks =
                taskDetailDao.getActiveBbxdTasks();
        assertions.assertThat(tasks.size()).isOne();
        TaskDetail result = tasks.get(0);
        assertions.assertThat(result.getCarrierCode()).isEqualTo("CARRIER");
        assertions.assertThat(result.getSku()).isEqualTo("ROV0000000000000000370");
        assertions.assertThat(result.getStorerKey()).isEqualTo("465852");
        assertions.assertThat(result.getUserKey()).isEqualTo("TEST");
        assertions.assertThat(result.getFromId()).isEqualTo("PL123");
    }

    @Test
    @DatabaseSetup("/db/dao/task-detail/get-active-bbxd-task.xml")
    public void getActiveUserBbxdTask() {
        var result =
                taskDetailDao.getActiveUserBbxdTask("TEST").get();
        assertions.assertThat(result.getCarrierCode()).isEqualTo("CARRIER");
        assertions.assertThat(result.getSku()).isEqualTo("ROV0000000000000000370");
        assertions.assertThat(result.getStorerKey()).isEqualTo("465852");
        assertions.assertThat(result.getUserKey()).isEqualTo("TEST");
        assertions.assertThat(result.getFromId()).isEqualTo("PL123");
    }

    @Test
    @DatabaseSetup("/db/dao/task-detail/get-active-bbxd-task.xml")
    public void getBbxdTasks() {
        var tasks =
                taskDetailDao.getBbxdTasks("CARRIER", "ROV0000000000000000370", "465852");
        assertions.assertThat(tasks.size()).isOne();
        TaskDetail result = tasks.get(0);
        assertions.assertThat(result.getCarrierCode()).isEqualTo("CARRIER");
        assertions.assertThat(result.getSku()).isEqualTo("ROV0000000000000000370");
        assertions.assertThat(result.getStorerKey()).isEqualTo("465852");
        assertions.assertThat(result.getUserKey()).isEqualTo("TEST");
        assertions.assertThat(result.getFromId()).isEqualTo("PL123");
    }
}
