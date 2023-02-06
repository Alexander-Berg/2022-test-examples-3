package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Тесты для {@link InsertPartnerApplicationCommand}.
 */
@DbUnitDataSet(before = "InsertPartnerApplicationCommand/InsertPartnerApplicationCommandTest.csv")
public class InsertPartnerApplicationCommandTest extends FunctionalTest {
    @Autowired
    private InsertPartnerApplicationCommand command;
    @Autowired
    private Terminal terminal;
    @Autowired
    private PrintWriter printWriter;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(after = "InsertPartnerApplicationCommand/InsertPartnerApplicationCommandTest.after.csv")
    void testInsertRequest() {
        invoke(14L,2224L, PartnerApplicationStatus.INTERNAL_CLOSED);
    }

    @Test
    @DbUnitDataSet(after = "InsertPartnerApplicationCommand/InsertPartnerApplicationCommandTest.csv")
    void testInsertExistingRequest() {
        invoke(12L,32424L, PartnerApplicationStatus.INTERNAL_CLOSED);
    }

    @Test
    @DbUnitDataSet(after = "InsertPartnerApplicationCommand/InsertPartnerApplicationCommandTest.csv")
    void testInsertNotExistentMeta() {
        invoke(122L,132424L, PartnerApplicationStatus.INTERNAL_CLOSED);
    }

    private void invoke(long requestId, long partnerId, PartnerApplicationStatus partnerApplicationStatus) {
        CommandInvocation invocation = new CommandInvocation("insert-partner-app",
                new String[]{String.valueOf(requestId), String.valueOf(partnerId), partnerApplicationStatus.getName()},
                Collections.emptyMap());
        command.executeCommand(invocation, terminal);
    }
}
