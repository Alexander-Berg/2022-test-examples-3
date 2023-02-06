package ru.yandex.market.wms.receiving.dao;

import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.TaskType;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.model.entity.ReceiptConsolidationTask;
import ru.yandex.market.wms.receiving.model.entity.RelocationTask;
import ru.yandex.market.wms.receiving.model.entity.enums.UserTaskStatus;

public class AbstractTaskDaoTest extends ReceivingIntegrationTest {

    @Autowired
    private AbstractTaskDao abstractTaskDao;

    @Test
    @DatabaseSetup("/dao/task/before.xml")
    void findByIdToIdCorrect() {
        Optional<ReceiptConsolidationTask> task = abstractTaskDao.findById("TDK3", ReceiptConsolidationTask.class);
        assertions.assertThat(task.get().getCurrentContainerId()).isEqualTo("toid");
    }

    @Test
    @DatabaseSetup("/dao/task/before.xml")
    void findByIdEmptyToIdReplacedWithNullEmptyLine() {
        Optional<ReceiptConsolidationTask> task = abstractTaskDao.findById("TDK", ReceiptConsolidationTask.class);
        assertions.assertThat(task.get().getCurrentContainerId()).isNull();
    }

    @Test
    @DatabaseSetup("/dao/task/before.xml")
    void findByIdEmptyToIdReplacedWithNullSpaces() {
        Optional<ReceiptConsolidationTask> task = abstractTaskDao.findById("TDK2", ReceiptConsolidationTask.class);
        assertions.assertThat(task.get().getCurrentContainerId()).isNull();
    }

    @Test
    @DatabaseSetup("/dao/task/before.xml")
    void findPlacementTasks() {
        List<RelocationTask> task = abstractTaskDao.findTasks("U5", UserTaskStatus.ASSIGNED,
                TaskType.ANOMALY_PLACEMENT);
        assertions.assertThat(task.get(0).getStatus()).isEqualTo(UserTaskStatus.ASSIGNED);
    }

}
