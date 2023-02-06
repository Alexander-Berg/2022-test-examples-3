package ru.yandex.market.billing.tasks.billing;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

@ExtendWith(MockitoExtension.class)
public class NullModifiedTimeInitCommandTest extends FunctionalTest {

    @Qualifier("namedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate oraJdbcTemplate;

    @Autowired
    private EnvironmentService environmentService;

    @Mock
    private Terminal terminal;

    private NullModifiedTimeInitCommand command;


    @BeforeEach
    void setup() {
        StringWriter terminalBuffer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(terminalBuffer);
        Mockito.when(terminal.getWriter())
                .thenReturn(printWriter);
        this.command = new NullModifiedTimeInitCommand(oraJdbcTemplate, environmentService);
    }
    @DbUnitDataSet(
            before = "db/NullModifiedTimeInitCommandTest.updateAllRows.before.csv",
            after = "db/NullModifiedTimeInitCommandTest.updateAllRows.after.csv"
    )
    @Test
    public void updateAllRows() {
        executeCommand();
    }

    @DbUnitDataSet(
            before = "db/NullModifiedTimeInitCommandTest.updateHalfOfRows.before.csv",
            after = "db/NullModifiedTimeInitCommandTest.updateHalfOfRows.after.csv"
    )
    @Test
    public void updateHalfOfRows() {
        executeCommand();
    }


    @DbUnitDataSet(
            before = "db/NullModifiedTimeInitCommandTest.noUpdates.before.csv",
            after = "db/NullModifiedTimeInitCommandTest.noUpdates.after.csv"
    )
    @Test
    public void noUpdates() {
        executeCommand();
    }

    private void executeCommand() {
        String[] strings = {};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        ImmutableMap.<String, String>builder()
                                .put("table-name", "market_billing.bank_order")
                                .put("init-column-name", "trantime")
                                .build()
                ),
                terminal
        );
    }

}
