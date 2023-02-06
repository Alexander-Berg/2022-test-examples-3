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

class MigratePartnerFeedbackContactCommandTest extends FunctionalTest {
    @Autowired
    private Terminal terminal;

    @Autowired
    private MigratePartnerFeedbackContactCommand migratePartnerFeedbackContactCommand;

    @BeforeEach
    public void init() {
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    @DbUnitDataSet(
            before = "MigratePartnerFeedbackContactCommand.before.csv",
            after = "MigratePartnerFeedbackContactCommand.after.csv"
    )
    void testMigrateOnePartnerContact() {
        CommandInvocation command = new CommandInvocation("migrate-feedback-contact", new String[]{"1"},
                Collections.emptyMap());

        migratePartnerFeedbackContactCommand.executeCommand(command, terminal);
    }

    @Test
    @DbUnitDataSet(
            before = "MigratePartnerFeedbackContactCommand.before.csv",
            after = "MigratePartnerFeedbackContactCommand.all.after.csv"
    )
    void testMigrateAllPartnerContact() {
        CommandInvocation command = new CommandInvocation("migrate-feedback-contact", new String[]{"1000"},
                Collections.emptyMap());

        migratePartnerFeedbackContactCommand.executeCommand(command, terminal);
    }
}
