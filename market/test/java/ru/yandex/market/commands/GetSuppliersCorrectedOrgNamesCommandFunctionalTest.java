package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

class GetSuppliersCorrectedOrgNamesCommandFunctionalTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;

    @Autowired
    private GetSuppliersCorrectedOrgNamesCommand correctedOrgNamesCommand;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    @DbUnitDataSet(
            before = "GetSuppliersCorrectedOrgNamesCommandFunctionalTest.before.csv",
            after = "GetSuppliersCorrectedOrgNamesCommandFunctionalTest.after.csv"
    )
    void testExecuteCommand() {
        CommandInvocation commandInvocation = new CommandInvocation(
                "get-suppliers-corrected-org-names",
                new String[]{},
                Collections.emptyMap()
        );
        correctedOrgNamesCommand.executeCommand(commandInvocation, terminal);
    }
}
