package ru.yandex.market.partner;

import java.io.PrintWriter;
import java.util.Map;

import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class TransferToThirdPartyCommandTest extends FunctionalTest {

    @Autowired
    private TransferToThirdPartyCommand transferToThirdPartyCommand;

    private Terminal terminal = Mockito.mock(Terminal.class);

    @BeforeEach
    void setUp() {
        Mockito.reset(terminal);
        Mockito.when(terminal.getWriter()).thenReturn(new PrintWriter(ByteStreams.nullOutputStream()));
        Mockito.when(terminal.confirm(Mockito.anyString())).thenReturn(true);
    }

    @Test
    @DbUnitDataSet(
            before = "TransferToThirdPartyCommandTest.before.csv",
            after = "TransferToThirdPartyCommandTest.after.csv"
    )
    void execute() {
        CommandInvocation ci = new CommandInvocation(
                "transfer-to-third-party",
                new String[0],
                Map.of(TransferToThirdPartyCommand.NEW_BALANCE_ID_OPTION, "99210881"));
        transferToThirdPartyCommand.executeCommand(ci, terminal);
    }

    @Test
    @DbUnitDataSet(
            before = "TransferToThirdPartyCommandTest.after.csv",
            after = "TransferToThirdPartyCommandTest.after.3p.csv"
    )
    void switchType() {
        CommandInvocation ci = new CommandInvocation(
                "transfer-to-third-party",
                new String[]{"change_type_to_3p"},
                Map.of(TransferToThirdPartyCommand.NEW_BALANCE_ID_OPTION, "99210881"));
        transferToThirdPartyCommand.executeCommand(ci, terminal);
    }

    @Test
    @DbUnitDataSet(
            before = "TransferToThirdPartyCommandTest.after.csv",
            after = "TransferToThirdPartyCommandTest.after.revert.csv"
    )
    void revert() {
        CommandInvocation ci = new CommandInvocation(
                "transfer-to-third-party",
                new String[]{"revert"},
                Map.of(TransferToThirdPartyCommand.NEW_BALANCE_ID_OPTION, "39989801"));
        transferToThirdPartyCommand.executeCommand(ci, terminal);
    }
}
