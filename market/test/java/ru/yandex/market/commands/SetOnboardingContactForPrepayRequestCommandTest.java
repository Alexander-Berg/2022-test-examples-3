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

public class SetOnboardingContactForPrepayRequestCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    private SetOnboardingContactForPrepayRequestCommand tested;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    @DbUnitDataSet(before = "SetOnboardingContactForPrepayRequestCommand.before.csv",
            after = "SetOnboardingContactForPrepayRequestCommand.after.csv")
    void testExecuteCommand() {
        CommandInvocation commandInvocation = new CommandInvocation("set-onboarding-contact-for-prepay-request",
                new String[]{},
                Collections.emptyMap());

        tested.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(before = "SetOnboardingContactForPrepayRequestCommand.before.csv",
            after = "SetOnboardingContactForPrepayRequestCommand.certainSup.after.csv")
    void testExecuteCommand_forCertainSupplier() {
        CommandInvocation commandInvocation = new CommandInvocation("set-onboarding-contact-for-prepay-request",
                new String[]{"1", "2"},
                Collections.emptyMap());

        tested.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(before = "SetOnboardingContactForPrepayRequestCommand.before.csv",
            after = "SetOnboardingContactForPrepayRequestCommand.before.csv")
    void testExecuteCommand_forCertainSupplier_supplierNotFound() {
        CommandInvocation commandInvocation = new CommandInvocation("set-onboarding-contact-for-prepay-request",
                new String[]{"1111", "111113"},
                Collections.emptyMap());

        tested.executeCommand(commandInvocation, terminal);
    }
}
