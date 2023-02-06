package ru.yandex.market.billing.imports.bonus;

import java.io.PrintWriter;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.billing.imports.bonus.RemainBonusAccountIdImportCommand.COMMAND_NAME;

@ExtendWith(MockitoExtension.class)
public class RemainBonusAccountIdImportCommandTest {
    @Mock
    private Terminal terminal;

    @Mock
    private NettingBonusService nettingBonusService;

    private RemainBonusAccountIdImportCommand command;

    private void init() {
        command = new RemainBonusAccountIdImportCommand(nettingBonusService);

        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
        when(terminal.confirm(anyString())).thenReturn(true);
    }

    @DisplayName("Выполнение команды")
    @Test
    public void executeCommand() {
        init();

        CommandInvocation commandInvocation = new CommandInvocation(COMMAND_NAME, new String[]{}, Map.of());
        command.executeCommand(commandInvocation, terminal);

        verify(nettingBonusService).updatePartnersBonusAccountIds();
        Mockito.verifyNoMoreInteractions(nettingBonusService);
    }

}
