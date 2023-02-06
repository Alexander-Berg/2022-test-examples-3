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
 * Тесты для {@link ManageBusinessContactCommand}.
 */
@DbUnitDataSet(before = "ManageBusinessContactCommand/testCommand.before.csv")
public class ManageBusinessContactCommandTest extends FunctionalTest {
    @Autowired
    private Terminal terminal;
    @Autowired
    private ManageBusinessContactCommand cmd;
    @Autowired
    private PrintWriter printWriter;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(after = "ManageBusinessContactCommand/delete.after.csv")
    void delete() {
        CommandInvocation commandInvocation = new CommandInvocation("manage-business-contact",
                new String[]{"delete", "1", "1"},
                Collections.emptyMap());
        cmd.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(after = "ManageBusinessContactCommand/create.after.csv")
    void create() {
        CommandInvocation commandInvocation = new CommandInvocation("manage-business-contact",
                new String[]{"create", "1", "3", "BUSINESS_ADMIN"},
                Collections.emptyMap());
        cmd.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(after = "ManageBusinessContactCommand/update.after.csv")
    void update() {
        CommandInvocation commandInvocation = new CommandInvocation("manage-business-contact",
                new String[]{"update", "1", "1", "BUSINESS_ADMIN"},
                Collections.emptyMap());
        cmd.executeCommand(commandInvocation, terminal);
    }
}
