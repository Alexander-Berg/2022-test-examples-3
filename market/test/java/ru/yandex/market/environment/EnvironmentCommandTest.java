package ru.yandex.market.environment;

import java.io.PrintWriter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tms.command.EnvironmentCommand;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class EnvironmentCommandTest extends FunctionalTest {
    @Autowired
    private EnvironmentCommand environmentCommandPg;

    @Test
    @DbUnitDataSet(before = "EnvironmentCommand.before.csv")
    void testMultipleValues() {
        // given
        var invocation = mock(CommandInvocation.class);
        when(invocation.getArguments()).thenReturn(
                new String[]{"get", "environment.test.first.name"}
        );
        var terminal = mock(Terminal.class);
        var writer = mock(PrintWriter.class);
        when(terminal.getWriter()).thenReturn(writer);

        // when
        environmentCommandPg.execute(invocation, terminal);

        // then
        verify(writer).println((Object) argThat(arg -> {
            if (!(arg instanceof List)) {
                return false;
            }
            var listArg = (List) arg;
            return listArg.contains("400") && listArg.contains("500") && listArg.size() == 2;
        }));
    }

}
