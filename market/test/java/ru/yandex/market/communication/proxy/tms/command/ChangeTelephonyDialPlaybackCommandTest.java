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

import static org.mockito.Mockito.when;

public class ChangeTelephonyDialPlaybackCommandTest extends AbstractCommunicationProxyTest {

    @Autowired
    private ChangeTelephonyDialPlaybackCommand command;
    @Mock
    private Terminal terminal;
    @Mock
    private PrintWriter printWriter;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    void testSuccesfulPlaybackChange() {
        CommandInvocation commandInvocation = new CommandInvocation("set-telephony-dial-playback",
                new String[]{"DIAL_UNAVAILABLE", "dc9a5bbb-3111-456a-9e57-bcb487dead66"},
                Collections.emptyMap());

        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    void testSuccesfulPlaybackReset() {
        CommandInvocation commandInvocation = new CommandInvocation("set-telephony-dial-playback",
                new String[]{"DIAL_UNAVAILABLE"},
                Collections.emptyMap());

        command.executeCommand(commandInvocation, terminal);
    }
}
