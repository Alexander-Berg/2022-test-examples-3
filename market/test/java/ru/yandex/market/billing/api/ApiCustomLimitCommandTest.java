package ru.yandex.market.billing.api;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Тесты для {@link ApiCustomLimitCommand}.
 */
@DbUnitDataSet(before = "ApiCustomLimitCommandTest.before.csv")
class ApiCustomLimitCommandTest extends FunctionalTest {

    @Autowired
    private ApiCustomLimitCommand command;

    @Test
    @DbUnitDataSet(after = "ApiCustomLimitCommandTest.set.after.csv")
    void testSet() {
        runCommand("set", "1", "5", "1000");
    }

    @Test
    @DbUnitDataSet(after = "ApiCustomLimitCommandTest.update.after.csv")
    void testUpdate() {
        runCommand("set", "3", "38", "1000");
    }

    @Test
    @DbUnitDataSet(after = "ApiCustomLimitCommandTest.delete.after.csv")
    void testDelete() {
        runCommand("delete", "5", "29");
    }

    private void runCommand(final String... args) {
        final CommandInvocation invocation = new CommandInvocation("custom-api-limit", args, Map.of());
        final Terminal terminal = mock(Terminal.class, withSettings().defaultAnswer(RETURNS_MOCKS));
        command.executeCommand(invocation, terminal);
    }

}
