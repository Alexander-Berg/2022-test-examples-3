package ru.yandex.market.commands;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

@DbUnitDataSet(before = "CleanEmptyContactLinksCommandFunctionalTest.csv")
public class CleanEmptyContactLinksCommandFunctionalTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    private CleanEmptyContactLinksCommand cmd;


    private static CommandInvocation commandInvocation() {
        return new CommandInvocation("clean-empty-contact-links", new String[]{}, Collections.emptyMap());
    }

    @Test
    @DbUnitDataSet(after = "CleanEmptyContactLinksCommandFunctionalTest.after.csv")
    void cleanEmptyContactLinks() {
        cmd.executeCommand(commandInvocation(), terminal);
    }
}
