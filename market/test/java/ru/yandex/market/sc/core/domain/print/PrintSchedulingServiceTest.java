package ru.yandex.market.sc.core.domain.print;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.sc.core.domain.print.generator.PrintStreamService;
import ru.yandex.market.sc.core.domain.print.mapper.PrintTaskMapper;
import ru.yandex.market.sc.core.domain.print.model.DestinationType;
import ru.yandex.market.sc.core.domain.print.model.PrintTaskCreateDto;
import ru.yandex.market.sc.core.domain.print.repository.PrintTask;
import ru.yandex.market.sc.core.domain.print.repository.PrintTaskRepository;
import ru.yandex.market.sc.core.domain.print.repository.PrintTaskStatus;
import ru.yandex.market.sc.core.domain.print.repository.PrintTemplateType;
import ru.yandex.market.sc.core.domain.print.service.PrintServiceWrapper;
import ru.yandex.market.sc.core.domain.user.repository.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrintSchedulingServiceTest {

    @Mock
    private PrintTaskRepository printTaskRepository;
    @Mock
    private PrintServiceWrapper printServiceWrapper;
    @Mock
    private PrintStreamService printStreamService;
    @Mock
    Clock clock;

    @InjectMocks
    private PrintSchedulingService printSchedulingService;

    @BeforeEach
    public void init() {
        doReturn(Instant.ofEpochMilli(0)).when(clock).instant();
    }

    @Test
    void rescheduleTask() {
        var user = new User(null, 1, null, null);
        var printTask = new PrintTask(1L, 1, "server", "printer", DestinationType.PLAIN, PrintTaskStatus.PROCESSING,
                Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("one", "two")
        );

        when(printTaskRepository.findByIdOrThrow(1L)).thenReturn(printTask);
        when(printStreamService.getPrintStream(any(), any(), any(), any())).thenReturn(mock(InputStream.class));

        var printTaskDto = printSchedulingService.retryTask(printTask.getId(), user);

        verify(printTaskRepository, times(1)).updatePrintTaskStatus(any(), any(), any(), any());
        verify(printServiceWrapper, times(1)).asyncPrint(eq(printTask.getId()), any());
        assertThat(printTaskDto).isEqualTo(PrintTaskMapper.mapPrintTask(printTask));
    }

    @Test
    void repeatTask() {
        var user = new User(null, 1, null, null);
        var printTaskOriginal = new PrintTask(777L, 1, "server", "printer", DestinationType.PLAIN,
                PrintTaskStatus.PROCESSING, Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("qr_text")
        );
        var printTaskRescheduled = new PrintTask(888L, 1, "server", "printer", DestinationType.PLAIN,
                PrintTaskStatus.PROCESSING, Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("qr_text")
        );

        doReturn(printTaskOriginal).when(printTaskRepository).findByIdOrThrow(any());
        doReturn(printTaskRescheduled).when(printTaskRepository).save(argThat(pt -> pt.getId() == null));
        doReturn(mock(InputStream.class)).when(printStreamService).getPrintStream(any(), any(), any(), any());

        var printTaskDto = printSchedulingService.repeatTask(printTaskOriginal.getId(), user);

        verify(printTaskRepository, times(1)).save(any());
        verify(printServiceWrapper, times(1)).asyncPrint(eq(printTaskRescheduled.getId()), any());
        assertThat(printTaskDto.getId()).isEqualTo(printTaskRescheduled.getId());
    }

    @Test
    void crateAndScheduleTask() {
        var user = new User(null, 1, null, null);
        var printTaskOriginal = new PrintTask(1L, 1, "server", "printer", DestinationType.PLAIN,
                PrintTaskStatus.PROCESSING, Instant.now(), "processing", 1L, 1, PrintTemplateType.LOT, List.of("qr_text")
        );

        when(printTaskRepository.save(any())).thenReturn(printTaskOriginal);
        when(printStreamService.getPrintStream(any(), any(), any(), any())).thenReturn(mock(InputStream.class));

        var createPrintTaskDto = new PrintTaskCreateDto("printer", DestinationType.ZPL, PrintTemplateType.LOT,
                List.of("field1"), 1);

        var printTaskDto = printSchedulingService.crateAndScheduleTask(createPrintTaskDto, user);

        verify(printTaskRepository, times(1)).save(any());
        verify(printServiceWrapper, times(1)).asyncPrint(eq(printTaskOriginal.getId()), any());
        assertThat(printTaskDto.getId()).isEqualTo(printTaskOriginal.getId());
    }

    @Test
    void crateAndScheduleTaskWithExceptionInStreamGeneration() {
        var user = new User(null, 1, null, null);
        var printTaskOriginal = new PrintTask(1L, 1, "server", "printer", DestinationType.PLAIN,
                PrintTaskStatus.CREATED, Instant.now(), null, 1L, 1, PrintTemplateType.LOT, List.of("qr_text")
        );

        when(printTaskRepository.save(any())).thenReturn(printTaskOriginal);
        when(printTaskRepository.updatePrintTaskStatus(any(), any(), any(), any())).thenReturn(printTaskOriginal);
        when(printStreamService.getPrintStream(any(), any(), any(), any())).thenThrow(new RuntimeException());

        var createPrintTaskDto = new PrintTaskCreateDto("printer", DestinationType.ZPL, PrintTemplateType.LOT,
                List.of("field1"), 1);

        var printTaskDto = printSchedulingService.crateAndScheduleTask(createPrintTaskDto, user);

        verify(printTaskRepository, times(1)).save(any());
        verify(printServiceWrapper, times(0)).asyncPrint(eq(printTaskOriginal.getId()), any());
        verify(printTaskRepository, times(1)).updatePrintTaskStatus(any(), eq(PrintTaskStatus.FAILED), any(), any());
        assertThat(printTaskDto.getId()).isEqualTo(printTaskOriginal.getId());
    }

}
