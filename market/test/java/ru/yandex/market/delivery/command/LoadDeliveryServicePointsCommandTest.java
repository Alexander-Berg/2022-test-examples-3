package ru.yandex.market.delivery.command;

import java.io.PrintWriter;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.delivery.points.LmsPointsLoaderService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link LoadDeliveryServicePointsCommand}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class LoadDeliveryServicePointsCommandTest {

    @Mock
    private LmsPointsLoaderService lmsPointsLoaderService;
    @Mock
    private Terminal terminal;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    void testLoadLMSPoints() {
        final LoadDeliveryServicePointsCommand command
                = new LoadDeliveryServicePointsCommand(lmsPointsLoaderService);

        final String[] args = {"123", "456"};
        final CommandInvocation invocation = new CommandInvocation(
                LoadDeliveryServicePointsCommand.COMMAND_NAME,
                args,
                Map.of()
        );
        command.executeCommand(invocation, terminal);

        final ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(lmsPointsLoaderService, times(2)).refreshInlets(captor.capture(), any());

        assertThat(captor.getAllValues(), Matchers.containsInAnyOrder(123L, 456L));
    }
}
