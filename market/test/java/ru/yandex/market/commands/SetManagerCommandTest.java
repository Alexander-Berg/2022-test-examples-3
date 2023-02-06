package ru.yandex.market.commands;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.marketmanager.MarketManagerService;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link SetManagerCommand}
 */
@DbUnitDataSet(before = "SetManagerCommand/before.csv")
class SetManagerCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    private SetManagerCommand cmd;
    @Autowired
    private MarketManagerService marketManagerService;

    @Test
    @DbUnitDataSet(after = "SetManagerCommand/testSetMain.after.csv")
    void testSetMain() {
        executeSet(10L, 333L, false);
    }

    @Test
    @DbUnitDataSet(after = "SetManagerCommand/testSetIndustrial.after.csv")
    void testSetIndustrial() {
        executeSet(300L, 100L, true);
    }

    @Test
    @DbUnitDataSet(after = "SetManagerCommand/testDeleteMain.after.csv")
    void testDeleteMain() {
        executeDelete(10L, false);
    }

    @Test
    @DbUnitDataSet(after = "SetManagerCommand/testDeleteIndustrial.after.csv")
    void testDeleteIndustrial() {
        executeDelete(300L, true);
        assertThat(marketManagerService.getIndustrialManagerClientLinks(300L)).isEmpty();
    }


    private void executeSet(long entityId, long managerId, boolean isIndustrial) {
        CommandInvocation commandInvocation = new CommandInvocation("set-manager",
                new String[]{"set", isIndustrial ? "industrial" : "main",
                        String.valueOf(entityId), String.valueOf(managerId)},
                Collections.emptyMap());
        cmd.executeCommand(commandInvocation, terminal);
    }

    private void executeDelete(long entityId, boolean isIndustrial) {
        CommandInvocation commandInvocation = new CommandInvocation("set-manager",
                new String[]{"delete", isIndustrial ? "industrial" : "main",
                        String.valueOf(entityId)},
                Collections.emptyMap());
        cmd.executeCommand(commandInvocation, terminal);
    }
}
