package ru.yandex.market.billing.fulfillment.billing.command;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.agency_commission.AgencyCommissionBillingCommand;
import ru.yandex.market.billing.fulfillment.billing.disposal.DisposalBillingCommand;
import ru.yandex.market.billing.fulfillment.command.ExpressDeliveryBillingCommand;
import ru.yandex.market.billing.fulfillment.command.FulfillmentOrderBillingCommand;
import ru.yandex.market.billing.fulfillment.command.StorageReturnsBillingCommand;
import ru.yandex.market.billing.returns.ReturnItemBillingCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * NOTE: Чек на корректное существование команд в контексте был добавлен, так как зачастую мы не проверяем
 * функциональными тестами их поведение, и могут возникнуть проблемы, приводящие к пропаданию команды из инструментов.
 */
class CommandFunctionalTest extends FunctionalTest {
    @Autowired
    private FulfillmentOrderBillingCommand fulfillmentOrderBillingCommand;

    @Autowired
    private StorageReturnsBillingCommand storageReturnsBillingCommand;

    @Autowired
    private ExpressDeliveryBillingCommand expressDeliveryBillingCommand;

    @Autowired
    private ReturnItemBillingCommand returnItemBillingCommand;

    @Autowired
    private AgencyCommissionBillingCommand agencyCommissionBillingCommand;

    @Autowired
    private DisposalBillingCommand disposalBillingCommand;

    @Test
    void test_existsInContext() {
        assertNotNull(fulfillmentOrderBillingCommand);
        assertEquals(fulfillmentOrderBillingCommand.getNames()[0], "recalculate-fulfillment-order-billing");

        assertNotNull(storageReturnsBillingCommand);
        assertEquals(storageReturnsBillingCommand.getNames()[0], "recalculate-storage-returns-billing");

        assertNotNull(expressDeliveryBillingCommand);
        assertEquals(expressDeliveryBillingCommand.getNames()[0], "recalculate-express-delivery-billing");

        assertNotNull(agencyCommissionBillingCommand);
        assertEquals(agencyCommissionBillingCommand.getNames()[0], "recalculate-agency-commission-billing");

        assertNotNull(disposalBillingCommand);
        assertEquals(disposalBillingCommand.getNames()[0], "recalculate-disposal-billing");

        assertNotNull(returnItemBillingCommand);
        assertEquals(returnItemBillingCommand.getNames()[0], "recalculate-return-item-billing");
    }
}
