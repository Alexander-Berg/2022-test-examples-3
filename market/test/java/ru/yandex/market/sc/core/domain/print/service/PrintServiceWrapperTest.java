package ru.yandex.market.sc.core.domain.print.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.sc.core.domain.print.model.DestinationType;
import ru.yandex.market.sc.core.domain.print.model.PrintResult;
import ru.yandex.market.sc.core.domain.print.repository.PrintTask;
import ru.yandex.market.sc.core.domain.print.repository.PrintTaskRepository;
import ru.yandex.market.sc.core.domain.print.repository.PrintTaskStatus;
import ru.yandex.market.sc.core.domain.print.repository.PrintTemplateType;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.exception.TplException;
import ru.yandex.market.tpl.common.util.monitoring.Monitorings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrintServiceWrapperTest {

    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault());

    private PrintTaskRepository printTaskRepository;

    private PrintService printServiceForServer1;

    private PrintService printServiceForServer2;

    private PrintServiceWrapper printServiceWrapper;

    @BeforeEach
    void init() {
        printTaskRepository = mock(PrintTaskRepository.class);
        printServiceForServer1 = mock(PrintService.class);
        printServiceForServer2 = mock(PrintService.class);

        printServiceWrapper = new PrintServiceWrapper(
                clock,
                List.of(printServiceForServer1, printServiceForServer2),
                printTaskRepository
        );
    }

    @Test
    @SneakyThrows
    void printGoodPrintTask() {
        var printTask = new PrintTask(1L, 1, "server1", "printer", DestinationType.PLAIN,
                PrintTaskStatus.PROCESSING,
                Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );
        var printResult = new PrintResult("server2", "printer", 1, "mock print job completed",
                PrintTaskStatus.COMPLETED, null);
        when(printTaskRepository.findById(1L)).thenReturn(Optional.of(printTask));
        when(printServiceForServer1.serviceIsAvailable()).thenReturn(true);
        when(printServiceForServer1.getPrintServerName()).thenReturn("server1");
        when(printServiceForServer1.print(any(), any(), anyInt(), any())).thenReturn(printResult);

        printServiceWrapper.asyncPrint(1L, null);
        verify(printTaskRepository, times(1))
                .updatePrintTaskStatus(any(), eq(PrintTaskStatus.COMPLETED), any(), any(), any(), any());
    }

    @Test
    void printTaskWithException() throws Exception {
        var printTask = new PrintTask(1L, 1, "server1", "printer", DestinationType.PLAIN,
                PrintTaskStatus.PROCESSING,
                Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );
        when(printTaskRepository.findById(1L)).thenReturn(Optional.of(printTask));
        when(printServiceForServer1.serviceIsAvailable()).thenReturn(true);
        when(printServiceForServer1.getPrintServerName()).thenReturn("server1");
        when(printServiceForServer1.print(any(), any(), anyInt(), any())).thenThrow(new Exception());
        printServiceWrapper.asyncPrint(1L, null);
        verify(printTaskRepository, times(1))
                .updatePrintTaskStatus(any(), eq(PrintTaskStatus.FAILED), any(), any(), any(), any());
    }

    @Test
    void printUnknownPrintTask() {
        printServiceWrapper.asyncPrint(1L, null);
        assertThat(Monitorings.getMonitoring().getResult("invalidPrintTaskState")).isNotNull();
    }

    @Test
    void selectPrintService() {
        doReturn(false).when(printServiceForServer1).serviceIsAvailable();
        doReturn("server1").when(printServiceForServer1).getPrintServerName();
        doReturn(true).when(printServiceForServer2).serviceIsAvailable();
        doReturn("server2").when(printServiceForServer2).getPrintServerName();

        var taskForServer1 = new PrintTask(1L, 1, "server1", "printer", DestinationType.PLAIN,
                PrintTaskStatus.PROCESSING,
                Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );
        var taskForServer2 = new PrintTask(2L, 1, "server2", "printer", DestinationType.PLAIN,
                PrintTaskStatus.PROCESSING,
                Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );
        var taskForServer3 = new PrintTask(3L, 1, "server3", "printer", DestinationType.PLAIN,
                PrintTaskStatus.PROCESSING,
                Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );
        var taskWithNoServer = new PrintTask(4L, 1, null, "printer", DestinationType.PLAIN, PrintTaskStatus.PROCESSING,
                Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );

        assertThrows(TplException.class, () -> printServiceWrapper.selectPrintService(taskForServer1));
        assertThat(printServiceWrapper.selectPrintService(taskForServer2).getPrintServerName()).isEqualTo("server2");
        assertThrows(TplException.class, () -> printServiceWrapper.selectPrintService(taskForServer3));
        assertThat(printServiceWrapper.selectPrintService(taskWithNoServer)).isNotNull();
    }

    @Test
    void retrieveAndUpdatePrintTaskStatus() {
        checkIllegalStatesForMissingJobId();
        checkThrowingExceptionOnMissingPrintTask();
    }

    private void checkThrowingExceptionOnMissingPrintTask() {
        doThrow(new TplEntityNotFoundException("Print task {} not found", 100L)).when(printTaskRepository).findById(100L);
        assertThrows(TplEntityNotFoundException.class,
                () -> printServiceWrapper.retrieveAndUpdatePrintTaskStatus(100L));
    }

    private void checkIllegalStatesForMissingJobId() {
        var createdTask = new PrintTask(1L, null, "server1", "printer", DestinationType.PLAIN, PrintTaskStatus.CREATED,
                Instant.now(), "no description", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );
        when(printTaskRepository.findById(1L)).thenReturn(Optional.of(createdTask));
        assertThat(createdTask).isEqualTo(printServiceWrapper.retrieveAndUpdatePrintTaskStatus(1L));

        var failedTask = new PrintTask(2L, null, "server1", "printer", DestinationType.PLAIN, PrintTaskStatus.FAILED,
                Instant.now(), "no description", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );
        when(printTaskRepository.findById(2L)).thenReturn(Optional.of(failedTask));
        when(printTaskRepository.updatePrintTaskStatus(any(), eq(PrintTaskStatus.FAILED), any(), any(), any(), any()))
                .thenReturn(failedTask);
        assertThat(failedTask).isEqualTo(printServiceWrapper.retrieveAndUpdatePrintTaskStatus(2L));

        var completedTask = new PrintTask(3L, null, "server1", "printer", DestinationType.PLAIN,
                PrintTaskStatus.COMPLETED, Instant.now(), "no description", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );
        when(printTaskRepository.findById(3L)).thenReturn(Optional.of(completedTask));
        assertThat(printServiceWrapper.retrieveAndUpdatePrintTaskStatus(3L).getStatus())
                .isEqualTo(PrintTaskStatus.COMPLETED);

        var processingTask = new PrintTask(4L, null, "server1", "printer", DestinationType.PLAIN,
                PrintTaskStatus.PROCESSING, Instant.now(), "no description", 1L, 1, PrintTemplateType.LOT, List.of("one")
        );
        when(printTaskRepository.findById(4L)).thenReturn(Optional.of(processingTask));
        assertThat(printServiceWrapper.retrieveAndUpdatePrintTaskStatus(4L).getStatus())
                .isEqualTo(PrintTaskStatus.FAILED);
    }

}
