package ru.yandex.market.billing.fulfillment.command;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.agency.commission.AgencyCommissionBillingCommand;
import ru.yandex.market.billing.fulfillment.orders.command.FulfillmentOrderBillingCommand;
import ru.yandex.market.billing.sorting.SortingBillingCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * NOTE: Чек на корректное существование команд в контексте был добавлен, так как зачастую мы не проверяем
 * функциональными тестами их поведение, и могут возникнуть проблемы, приводящие к пропаданию команды из инструментов.
 */
class CommandFunctionalTest extends FunctionalTest {
    @Autowired
    private StorageBillingCommand storageBillingCommand;

    @Autowired
    private FulfillmentOrderBillingCommand fulfillmentOrderBillingCommand;

    @Autowired
    private AgencyCommissionBillingCommand agencyCommissionBillingCommand;

    @Autowired
    private SortingBillingCommand sortingBillingCommand;

    @Test
    void test_existsInContext() {
        assertNotNull(storageBillingCommand);
        assertEquals(storageBillingCommand.getNames()[0], "recalculate-storage-billing");

        assertNotNull(fulfillmentOrderBillingCommand);
        assertEquals(fulfillmentOrderBillingCommand.getNames()[0], "recalculate-fulfillment-order-billing");

        assertNotNull(agencyCommissionBillingCommand);
        assertEquals(agencyCommissionBillingCommand.getNames()[0], "recalculate-agency-commission-billing");

        assertNotNull(sortingBillingCommand);
        assertEquals(sortingBillingCommand.getNames()[0], "recalculate-sorting-billing");
    }
}
