package ru.yandex.market.communication.proxy.tms.command;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ToggleTelephonyFailedDialPlaybacksCommandTest extends AbstractCommunicationProxyTest {

    @Autowired
    private ToggleTelephonyFailedDialPlaybacksCommand command;
    @Autowired
    private TelephonyClient telephonyClient;

    @Mock
    private Terminal terminal;
    @Mock
    private PrintWriter printWriter;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    public void testSuccess() {
        CommandInvocation commandInvocation = new CommandInvocation("toggle-dial-playbacks",
                new String[]{"true"},
                Collections.emptyMap());

        command.executeCommand(commandInvocation, terminal);

        verify(telephonyClient).updateServiceSettings(any());
    }

}
