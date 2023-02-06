package ru.yandex.market.tms.quartz2.remote;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.SchedulerException;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.tms.quartz2.model.JobInfo;
import ru.yandex.market.tms.quartz2.service.JobService;

/**
 * @author fbokovikov
 */
@ExtendWith(MockitoExtension.class)
class ListJobsCommandTest {

    private static final String SORTED_JOBS =
            "dailyLimitPulseCountExecutor    DEFAULT    0 59 * * * ?\n" +
                    "dynamicStatsExecutor    DEFAULT    0 0 8 * * ?\n" +
                    "getInfoFromClickIndexerExecutor    DEFAULT    0 13 6-23 * * ?\n" +
                    "marketstatReceiverManagerRGExecutor    DEFAULT    0 1/5 * * * ?\n";

    private static final CommandInvocation MOCKED_INVOCATION = Mockito.mock(CommandInvocation.class);

    @Mock
    private JobService jobService;

    @Mock
    private Terminal terminal;

    @InjectMocks
    private ListJobsCommand listJobsCommand;

    @Test
    @DisplayName("Проверяем, что команды возвращаются в отсортированном порядке")
    void listJobsCommand() throws SchedulerException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        Mockito.when(terminal.getWriter()).thenReturn(printWriter);
        Mockito.when(jobService.getAllJobs()).thenReturn(
                ImmutableList.of(
                        new JobInfo("getInfoFromClickIndexerExecutor", "0 13 6-23 * * ?", "DEFAULT"),
                        new JobInfo("dynamicStatsExecutor", "0 0 8 * * ?", "DEFAULT"),
                        new JobInfo("marketstatReceiverManagerRGExecutor", "0 1/5 * * * ?", "DEFAULT"),
                        new JobInfo("dailyLimitPulseCountExecutor", "0 59 * * * ?", "DEFAULT")
                )
        );
        listJobsCommand.executeCommand(MOCKED_INVOCATION, terminal);
        Assertions.assertEquals(
                SORTED_JOBS,
                stringWriter.toString()
        );
    }
}
