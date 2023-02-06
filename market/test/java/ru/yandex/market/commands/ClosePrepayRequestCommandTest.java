package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ClosePrepayRequestCommand}.
 */
@DbUnitDataSet(before = "ClosePrepayRequestCommand/ClosePrepayRequestCommandTest.csv")
public class ClosePrepayRequestCommandTest extends FunctionalTest {
    @Autowired
    private ClosePrepayRequestCommand command;
    @Autowired
    private Terminal terminal;
    @Autowired
    private PrintWriter printWriter;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(after = "ClosePrepayRequestCommand/testCloseSingle.after.csv")
    void testCloseSingle() {
        invoke(2223L);
    }

    @Test
    @DbUnitDataSet(after = "ClosePrepayRequestCommand/testCloseWithAnotherPartner.after.csv")
    void testCloseWithAnotherPartner() {
        invoke(32424L);
    }

    @Test
    @DbUnitDataSet(after = "ClosePrepayRequestCommand/testCloseWithAnotherPartnerOld.after.csv")
    void testCloseWithAnotherPartnerOld() {
        invoke(2225L);
    }

    private void invoke(long partnerId) {
        CommandInvocation invocation = new CommandInvocation("close-prepay-request",
                new String[]{String.valueOf(partnerId)},
                Collections.emptyMap());
        command.executeCommand(invocation, terminal);
    }
}
