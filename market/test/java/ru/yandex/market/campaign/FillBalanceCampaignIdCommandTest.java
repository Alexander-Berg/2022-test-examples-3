package ru.yandex.market.campaign;

import java.io.PrintWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тесты для {@link FillBalanceCampaignIdCommand}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class FillBalanceCampaignIdCommandTest extends FunctionalTest {

    @Autowired
    private FillBalanceCampaignIdCommand fillBalanceCampaignIdCommand;

    @Test
    @DbUnitDataSet(
            before = "FillBalanceCampaignIdCommandTest.testFill.before.csv",
            after = "FillBalanceCampaignIdCommandTest.testFill.after.csv"
    )
    void testFill() {
        executeCommand("100");
    }

    private void executeCommand(String... args) {
        CommandInvocation commandInvocation = new CommandInvocation("fill-balance-campaign-ids", args, Map.of());
        Terminal terminal = createTerminal();

        fillBalanceCampaignIdCommand.execute(commandInvocation, terminal);
    }

    private Terminal createTerminal() {
        Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        return terminal;
    }
}
