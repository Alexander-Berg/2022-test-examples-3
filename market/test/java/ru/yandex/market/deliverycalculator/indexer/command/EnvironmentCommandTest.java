package ru.yandex.market.deliverycalculator.indexer.command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тест для {@link EnvironmentCommand}.
 */
@DbUnitDataSet(before = "environment.before.csv")
class EnvironmentCommandTest extends AbstractTmsCommandTest {

    @Autowired
    EnvironmentCommand environmentCommand;

    @Test
    @DisplayName("Получение значения переменной")
    void testGetCommand() {
        Assertions.assertEquals("old_value", executeCommand("get", "to_update"));
    }

    @Test
    @DisplayName("Установка значения не существовавшей ранее переменной")
    void testSetNewCommand() {
        executeCommand("set", "to_add", "perfect_new_value");
        Assertions.assertEquals(
                "Value have been set\n" +
                "perfect_new_value",
                executeCommand("get", "to_add"));
    }

    @Test
    @DisplayName("Обновление значения переменной")
    void testSetUpdateCommand() {
        executeCommand("set", "to_update", "new_value");
        Assertions.assertEquals(
                "Value have been set, old value was old_value\n" +
                "new_value",
                executeCommand("get", "to_update"));
    }

    @Test
    @DisplayName("Удаление переменной")
    void testDeleteCommand() {
        executeCommand("delete", "to_delete");
        Assertions.assertEquals("Variable have been deleted, old value was useless_value\nnull",
                executeCommand("get", "to_delete"));
    }

    @Test
    @DisplayName("Попытка удалить переменную, которой и так нет")
    void testDeleteNoSuchVariableCommand() {
        Assertions.assertEquals("Variable unknown_variable not found", executeCommand("delete", "unknown_variable"));
    }

    private String executeCommand(String ...args) {
        final CommandInvocation commandInvocation = commandInvocation("env", (Object[]) args);
        environmentCommand.executeCommand(commandInvocation, terminal());
        return terminalData();
    }
}
