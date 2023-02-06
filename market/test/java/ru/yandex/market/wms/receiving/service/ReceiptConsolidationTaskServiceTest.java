package ru.yandex.market.wms.receiving.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.auth.core.service.DefaultInforAuthenticationSecurityDataProvider;
import ru.yandex.market.wms.common.model.enums.TaskType;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao;
import ru.yandex.market.wms.common.spring.exception.ForbiddenException;
import ru.yandex.market.wms.common.spring.service.ReceiptConsolidationTaskCreateService;
import ru.yandex.market.wms.receiving.dao.AbstractTaskDao;
import ru.yandex.market.wms.receiving.exception.NoReceiptConsolidationContainerException;
import ru.yandex.market.wms.receiving.exception.TaskAlreadyFinishedException;
import ru.yandex.market.wms.receiving.exception.TaskAlreadyHasContainer;
import ru.yandex.market.wms.receiving.model.entity.ReceiptConsolidationTask;
import ru.yandex.market.wms.receiving.model.entity.enums.UserTaskStatus;
import ru.yandex.market.wms.receiving.service.straight.AnomalyLotService;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReceiptConsolidationTaskServiceTest {

    private ReceiptConsolidationTaskService service;

    private AbstractTaskDao taskDao;

    private SecurityDataProvider securityDataProvider;

    @BeforeEach
    public void setUp() {
        taskDao = mock(AbstractTaskDao.class);
        securityDataProvider = mock(DefaultInforAuthenticationSecurityDataProvider.class);
        service = new ReceiptConsolidationTaskService(taskDao, null, null, securityDataProvider,
                null, mock(AnomalyLotService.class), mock(ReceiptDao.class),
                mock(ReceiptConsolidationTaskCreateService.class), mock(ScanningOperationLog.class), null);
    }

    @Test
    public void openTaskReturnContainerDifferentUsersThrowsException() {
        ReceiptConsolidationTask task =
                new ReceiptConsolidationTask("ID",
                        TaskType.ANOMALY_CONSOLIDATION,
                        UserTaskStatus.ASSIGNED,
                        "User1",
                        "R",
                        "SR",
                        "Cont",
                        "",
                        List.of());
        when(securityDataProvider.getUser()).thenReturn("User2");
        when(taskDao.findById("Task", ReceiptConsolidationTask.class)).thenReturn(Optional.of(task));
        Assertions.assertThrows(ForbiddenException.class, () -> service.openTaskReturnContainer("Task", "Cont"));
    }

    @Test
    public void openTaskReturnContainerFinishedThrowsException() {
        ReceiptConsolidationTask task =
                new ReceiptConsolidationTask("ID",
                        TaskType.ANOMALY_CONSOLIDATION,
                        UserTaskStatus.FINISHED,
                        "User",
                        "R",
                        "SR",
                        "Cont",
                        "",
                        List.of());
        when(securityDataProvider.getUser()).thenReturn("User");
        when(taskDao.findById("Task", ReceiptConsolidationTask.class)).thenReturn(Optional.of(task));
        Assertions.assertThrows(TaskAlreadyFinishedException.class,
                () -> service.openTaskReturnContainer("Task", "Cont"));
    }

    @Test
    public void openTaskReturnContainerDifferentContainersThrowsException() {
        ReceiptConsolidationTask task =
                new ReceiptConsolidationTask("ID",
                        TaskType.ANOMALY_CONSOLIDATION,
                        UserTaskStatus.ASSIGNED,
                        "User",
                        "R",
                        "SR",
                        "Cont1",
                        "",
                        List.of());
        when(securityDataProvider.getUser()).thenReturn("User");
        when(taskDao.findById("Task", ReceiptConsolidationTask.class)).thenReturn(Optional.of(task));
        Assertions.assertThrows(TaskAlreadyHasContainer.class, () -> service.openTaskReturnContainer("Task", "Cont2"));
    }


    @Test
    void moveAllItemsToTaskReturnContainerFromOtherDifferentUsersThrowsException() {
        ReceiptConsolidationTask task =
                new ReceiptConsolidationTask("ID",
                        TaskType.ANOMALY_CONSOLIDATION,
                        UserTaskStatus.ASSIGNED,
                        "User1",
                        "R",
                        "SR",
                        "C",
                        "",
                        List.of());
        when(securityDataProvider.getUser()).thenReturn("User2");
        when(taskDao.findById("Task", ReceiptConsolidationTask.class)).thenReturn(Optional.of(task));
        Assertions.assertThrows(ForbiddenException.class,
                () -> service.moveAllItemsToTaskReturnContainerFromOther("Task", "1"));
    }

    @Test
    void moveAllItemsToTaskReturnContainerFromOtherNoContainerThrowsException() {
        ReceiptConsolidationTask task =
                new ReceiptConsolidationTask("ID",
                        TaskType.ANOMALY_CONSOLIDATION,
                        UserTaskStatus.ASSIGNED,
                        "User",
                        "R",
                        "SR",
                        null,
                        "",
                        null);
        when(securityDataProvider.getUser()).thenReturn("User");
        when(taskDao.findById("Task", ReceiptConsolidationTask.class)).thenReturn(Optional.of(task));
        Assertions.assertThrows(NoReceiptConsolidationContainerException.class,
                () -> service.moveAllItemsToTaskReturnContainerFromOther("Task", "1"));
    }
}
