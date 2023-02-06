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
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DisableTelephonyRedirectCommandTest extends AbstractCommunicationProxyTest {

    @Autowired
    private DisableTelephonyRedirectCommand tested;
    @Mock
    private Terminal terminal;
    @Mock
    private PrintWriter printWriter;
    @Autowired
    private TelephonyClient telephonyClient;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(
            before = "disableRedirect.before.csv",
            after = "disableRedirect.after.csv"
    )
    void testSuccessfulExecution() {
        CommandInvocation commandInvocation = new CommandInvocation("disable-telephony-redirect",
                new String[]{"DBS_ORDER", "1000", "2000", "3000"},
                Collections.emptyMap());

        tested.executeCommand(commandInvocation, terminal);

        verify(telephonyClient, times(2)).unlinkServiceNumber(anyString(), eq(true));
    }
}
