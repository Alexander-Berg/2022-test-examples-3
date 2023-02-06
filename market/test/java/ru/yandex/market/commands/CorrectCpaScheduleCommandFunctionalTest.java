package ru.yandex.market.commands;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Функциональные тесты для {@link CorrectCpaScheduleCommand}.
 */
@DbUnitDataSet(before = "CorrectCpaScheduleCommandFunctionalTest.before.csv")
public class CorrectCpaScheduleCommandFunctionalTest extends FunctionalTest {
    @Autowired
    private Terminal terminal;
    @Autowired
    private CorrectCpaScheduleCommand command;

    @DisplayName("Исправление расписания")
    @Test
    @DbUnitDataSet(after = "CorrectCpaScheduleCommandFunctionalTest.testCorrectSchedule.after.csv")
    void testCorrectSchedule() {
        invoke(1L);
    }

    @DisplayName("Исправление сложного расписания")
    @Test
    @DbUnitDataSet(after = "CorrectCpaScheduleCommandFunctionalTest.testCorrectScheduleComplex.after.csv")
    void testCorrectScheduleComplex() {
        invoke(2L);
    }

    private void invoke(long... partnerId) {
        String[] params = Arrays.stream(partnerId)
                .mapToObj(String::valueOf)
                .toArray(String[]::new);

        command.executeCommand(
                new CommandInvocation("correct-cpa-schedule",
                        params,
                        Collections.emptyMap()),
                terminal
        );
    }
}
