package ru.yandex.market.commands;

import java.io.PrintWriter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.replication.ReplicationParamService;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReplicationParamsCommandTest extends FunctionalTest {

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private ReplicationParamService replicationParamService;

    @Mock
    private CommandInvocation commandInvocation;

    @Mock
    private Terminal terminal;

    private ReplicationParamsCommand replicationParamsCommand;

    @BeforeAll
    void setUp() {
        commandInvocation = mock(CommandInvocation.class);
        terminal = mock(Terminal.class);
        when(terminal.getWriter()).thenReturn(mock(PrintWriter.class));
        replicationParamsCommand = new ReplicationParamsCommand(replicationParamService, protocolService);
    }

    @Test
    @DisplayName("Добавление параметров для репликации магазинов")
    @DbUnitDataSet(before = "ReplicationParamsCommandTest/shopReplicationParams.before.csv",
            after = "ReplicationParamsCommandTest/shopReplicationParams.after.csv")
    void addParams() {
        executeCommandWithArgs(new String[]{"add", "SKIP_FEED", "5", "6"});
    }

    @Test
    @DisplayName("Удаление параметров для репликации магазинов по имени параметра")
    @DbUnitDataSet(before = "ReplicationParamsCommandTest/shopReplicationParams.before.csv",
            after = "ReplicationParamsCommandTest/shopReplicationParamsDelete.after.csv")
    void deleteByParam() {
        executeCommandWithArgs(new String[]{"delete", "SKIP_FEED", "1", "2", "4"});
    }

    private void executeCommandWithArgs(String[] args) {
        when(commandInvocation.getArguments()).thenReturn(args);
        replicationParamsCommand.executeCommand(commandInvocation, terminal);
    }
}
