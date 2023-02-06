package ru.yandex.market.logistics.lom.controller.order.processing;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.jobs.processor.UpdateMissingItemsRequestProcessor;

class UpdateMissingItemsRequestTest extends AbstractUpdateMissingItemsRequestTest {
    @Autowired
    private UpdateMissingItemsRequestProcessor processor;

    @Test
    @DisplayName("Отмена заказа, если сегмент запроса с типом ORDER_CHANGED_BY_PARTNER в статусе fail")
    @DatabaseSetup("/controller/order/updateitems/updaterequeststatus/before/order_changed_by_partner_fail.xml")
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/after/order_changed_by_partner_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failOrderChangedByPartner() {
        processor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Отмена заказа, если сегмент запроса с типом ITEM_NOT_FOUND в статусе fail")
    @DatabaseSetup("/controller/order/updateitems/updaterequeststatus/before/item_not_found_fail.xml")
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/after/item_not_found_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failItemNotFound() {
        processor.processPayload(PAYLOAD);
    }

    @Nonnull
    @Override
    UpdateMissingItemsRequestProcessor processor() {
        return processor;
    }
}
