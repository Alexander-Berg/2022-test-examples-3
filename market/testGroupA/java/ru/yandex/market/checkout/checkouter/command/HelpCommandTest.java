package ru.yandex.market.checkout.checkouter.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.HelpCommand;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;

public class HelpCommandTest extends AbstractServicesTestBase {

    @Autowired
    private HelpCommand helpCommand;

    @Test
    public void testHelp() {
        TestTerminal terminal = new TestTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        helpCommand.execute(new CommandInvocation("help", new String[0], Collections.emptyMap()), terminal);
    }
}
