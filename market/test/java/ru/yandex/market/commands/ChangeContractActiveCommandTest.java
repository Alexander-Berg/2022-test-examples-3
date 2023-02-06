package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.terminal.Command;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для команды {@link ChangeContractActiveCommand}
 */
@DbUnitDataSet(before = "ChangeContractActiveCommandTest.before.csv")
public class ChangeContractActiveCommandTest extends FunctionalTest {
    @Autowired
    private Terminal terminal;

    @Autowired
    @Qualifier("changeContractActiveCommand")
    private Command command;

    @BeforeEach
    void setUp() {
        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
    }

    @DisplayName("Делаем контракты неактивными")
    @Test
    @DbUnitDataSet(after = "ChangeContractActiveCommandTest.testDeactivateContracts.after.csv")
    void testDeactivateContracts() {
        invoke(426892L, 11387187L, false);
    }

    @DisplayName("Делаем контракты активными")
    @Test
    @DbUnitDataSet(after = "ChangeContractActiveCommandTest.testActivateContracts.after.csv")
    void testActivateContracts() {
        invoke(426893L, 11387188L, true);
    }

    @DisplayName("Ничего не делаем, если заявка в другом статусе")
    @Test
    @DbUnitDataSet(after = "ChangeContractActiveCommandTest.before.csv")
    void testCompletedRequest() {
        invoke(426894L, 11387189L, false);
    }

    private void invoke(long requestId, long partnerId, boolean isActive) {
        command.execute(
                new CommandInvocation("change-contract-active-command",
                        new String[]{
                                String.valueOf(requestId),
                                String.valueOf(partnerId),
                                String.valueOf(isActive)
                        },
                        Collections.emptyMap()),
                terminal
        );
    }
}
