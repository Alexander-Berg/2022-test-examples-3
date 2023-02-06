package ru.yandex.market.partner;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link FixPartnerIDNCommand}.
 *
 * @author Vladislav Bauer
 */
class FixPartnerIDNCommandTest extends FunctionalTest {

    @Autowired
    private FixPartnerIDNCommand fixPartnerIDNCommand;

    @Test
    @DbUnitDataSet(
            before = "FixPartnerIDNCommandTest.before.csv",
            after = "FixPartnerIDNCommandTest.after.csv"
    )
    void testCommand() {
        final List<Long> partnerIds = List.of(1L, 2L, 3L, 4L, 5L);

        final Terminal terminal = createTerminal();
        final CommandInvocation commandInvocation = createCommandInvocation(
                StringUtils.collectionToCommaDelimitedString(partnerIds));

        fixPartnerIDNCommand.executeCommand(commandInvocation, terminal);
        Mockito.verify(terminal, Mockito.times(partnerIds.size())).getWriter();
    }

    private CommandInvocation createCommandInvocation(final String... args) {
        return new CommandInvocation("fix-partner-idn", args, Collections.emptyMap());
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }

}
