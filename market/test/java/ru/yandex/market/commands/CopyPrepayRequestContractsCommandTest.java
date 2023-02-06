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
 * Функциональные тесты для команды {@link CopyPrepayRequestContractsCommand}
 */
@DbUnitDataSet(before = "CopyPrepayRequestContractsCommandTest.before.csv")
public class CopyPrepayRequestContractsCommandTest extends FunctionalTest {
    @Autowired
    private Terminal terminal;

    @Autowired
    @Qualifier("copyPrepayRequestContractsCommand")
    private Command command;

    @BeforeEach
    void setUp() {
        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
    }

    @DisplayName("Копирование контрактов")
    @Test
    @DbUnitDataSet(after = "CopyPrepayRequestContractsCommandTest.testCopyContracts.after.csv")
    void testCopyContracts() {
        invoke(11387187L, 11387188L);
    }

    @DisplayName("Не падаем и ничего не делаем, если аргументы не верны")
    @Test
    @DbUnitDataSet(after = "CopyPrepayRequestContractsCommandTest.before.csv")
    void testWrongArguments() {
        invoke(11387187L, 1L);
    }

    private void invoke(long sourcePartnerId, long destinationPartnerId) {
        command.execute(
                new CommandInvocation("copy-prepay-request-contracts",
                        new String[]{String.valueOf(sourcePartnerId), String.valueOf(destinationPartnerId)},
                        Collections.emptyMap()),
                terminal
        );
    }
}
