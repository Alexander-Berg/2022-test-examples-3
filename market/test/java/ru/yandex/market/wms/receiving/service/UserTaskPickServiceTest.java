package ru.yandex.market.wms.receiving.service;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.enums.TaskSkipReason;
import ru.yandex.market.wms.common.model.enums.TaskType;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.receiving.dao.AbstractTaskDao;
import ru.yandex.market.wms.receiving.model.entity.AbstractTask;
import ru.yandex.market.wms.receiving.model.entity.RelocationTask;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTaskPickServiceTest extends BaseTest {

    @Mock
    private AbstractTaskDao taskDao;
    @Mock
    private SecurityDataProvider userProvider;
    @Mock
    private DbConfigService configService;
    @Mock
    private RelocationTaskService relocationTaskService;
    UserTaskPickService service;

    @BeforeEach
    void before() {
        service = new UserTaskPickService(taskDao, userProvider, configService, relocationTaskService);
    }

    //множ. размщение не включено, возвращаем первое задание по порядку
    @Test
    void getNextPlacementTaskNoMultipicking() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(false, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("1");
    }

    @Test
    void getNextPlacementTaskNoPickedTasksNoAvailable() {
        AbstractTask nextPlacementTask =
                service.getNextRelocationTask(true, Collections.emptyList(), Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask).isNull();
    }

    @Test
    void getNextPlacementTaskNoPickedTasksNoOversize() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("1");
    }

    @Test
    void getNextPlacementTaskNoPicked1() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("4");
    }

    @Test
    void getNextPlacementTaskNoPicked2() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("2");
    }

    @Test
    void getNextPlacementTaskNoPicked3() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("3");
    }

    @Test
    void getNextPlacementTaskNoPicked4() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("3");
    }

    @Test
    void getNextPlacementTaskNoPicked5() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("1");
    }

    @Test
    void getNextPlacementTaskNoPicked6() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .logicalLocation("2")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .logicalLocation("1")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("1");
    }

    @Test
    void getNextPlacementTaskNoPicked7() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .logicalLocation("2")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .logicalLocation("1")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("1");
    }

    @Test
    void getNextPlacementTaskNoPicked8() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .logicalLocation("1")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .logicalLocation("2")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("3");
    }

    @Test
    void getNextPlacementTaskNoPicked9() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("3");
    }

    @Test
    void getNextPlacementTaskNoPickedWithSkipped1() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks, List.of("4"), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("2");
    }

    @Test
    void getNextPlacementTaskNoPickedWithSkipped2() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks, List.of("3"), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("4");
    }

    @Test
    void getNextPlacementTaskNoPickedWithSkipped3() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks, List.of("2"), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("4");
    }

    @Test
    void getNextPlacementTaskNoPickedWithSkipped4() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks, List.of("1"), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("4");
    }

    @Test
    void getNextPlacementTaskNoPickedWithSkipped5() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks, List.of("4", "2"), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("2");
    }

    @Test
    void getNextPlacementTaskNoPickedWithSkipped6() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                List.of("4", "3", "2"), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("2");
    }

    @Test
    void getNextPlacementTaskNoPickedWithSkipped7() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());
        AbstractTask nextPlacementTask =
                service.getNextRelocationTask(true, availableTasks, List.of("4", "3", "2", "1"), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("2");
    }

    @Test
    void getNextPlacementTaskNoPickedWithSkipped8() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .taskSkipReason(TaskSkipReason.TARE_ABSENT)
                        .build());
        AbstractTask nextPlacementTask =
                service.getNextRelocationTask(true, availableTasks, List.of("1"), TaskType.MSRMNT);
        assertions.assertThat(nextPlacementTask).isNull();
    }

    @Test
    void getNextPlacementTaskNoPickedWithSkipped9() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.TARE_ABSENT)
                        .build());
        AbstractTask nextPlacementTask =
                service.getNextRelocationTask(true, availableTasks, Collections.emptyList(), TaskType.MSRMNT);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("3");
    }

    @Test
    void getNextPlacementTaskPicked1() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build());

        when(relocationTaskService.getPickedTasks()).thenReturn(List.of(
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build()));
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("2");
    }

    @Test
    void getNextPlacementTaskPicked2() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());

        when(relocationTaskService.getPickedTasks()).thenReturn(List.of(
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build()));
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("4");
    }

    @Test
    void getNextPlacementTaskPicked3() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("1")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());

        when(relocationTaskService.getPickedTasks()).thenReturn(List.of(
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build()));
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("3");
    }

    @Test
    void getNextPlacementTaskPicked4() {
        List<AbstractTask> availableTasks = List.of(
                RelocationTask.builder()
                        .id("2")
                        .logicalLocation("3")
                        .build(),
                RelocationTask.builder()
                        .id("3")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .build(),
                RelocationTask.builder()
                        .id("4")
                        .taskSkipReason(TaskSkipReason.OVERSIZE)
                        .logicalLocation("2")
                        .build());

        when(relocationTaskService.getPickedTasks()).thenReturn(List.of(
                RelocationTask.builder()
                        .id("1")
                        .build()));
        AbstractTask nextPlacementTask = service.getNextRelocationTask(true, availableTasks,
                Collections.emptyList(), null);
        assertions.assertThat(nextPlacementTask.getId()).isEqualTo("4");
    }
}
