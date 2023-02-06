package ru.yandex.market.deliverycalculator.indexer.command;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static java.util.Arrays.asList;

class AddPreferableDeliveryRegionMappingCommandTest extends AbstractTmsCommandTest {

    @Autowired
    private AddPreferableDeliveryRegionMappingCommand tested;

    @Test
    @DbUnitDataSet(before = "addPreferableDeliveryRegionMapping.before.csv",
            after = "addPreferableDeliveryRegionMapping.after.csv")
    void testExecuteCommand() {
        CommandInvocation commandInvocation1 = commandInvocation("add-preferable-delivery-region-mapping", "1", "3");
        CommandInvocation commandInvocation2 = commandInvocation("add-preferable-delivery-region-mapping", "5", "6");
        CommandInvocation commandInvocation3 = commandInvocation("add-preferable-delivery-region-mapping", "6", "7");

        asList(commandInvocation1, commandInvocation2, commandInvocation3)
                .forEach(inv -> tested.executeCommand(inv, terminal()));
    }
}
