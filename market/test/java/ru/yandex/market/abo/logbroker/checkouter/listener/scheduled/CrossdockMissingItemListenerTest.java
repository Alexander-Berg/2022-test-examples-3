package ru.yandex.market.abo.logbroker.checkouter.listener.scheduled;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRule;
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRuleService;
import ru.yandex.market.abo.cpa.order.checkpoint.CheckpointType;
import ru.yandex.market.abo.cpa.order.service.CpaMissingItemCheckpointService;
import ru.yandex.market.abo.util.FakeUsers;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.ItemCount;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEventReasonDetails;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 01.12.2020
 */
class CrossdockMissingItemListenerTest {

    private static long ORDER_ID = 1325436L;

    private static final long MISSING_ITEM_ID = 123525L;
    private static final long ANOTHER_ITEM_ID = 128925L;

    private static final long SUPPLIER_ID = 123L;
    private static final String SHOP_SKU = "test-sku";

    private static final BlueOfferHidingRule EXPECTED_HIDING_RULE = buildExpectedHidingRule();

    @InjectMocks
    private CrossdockMissingItemListener crossdockMissingItemListener;

    @Mock
    private CpaMissingItemCheckpointService missingItemCheckpointService;
    @Mock
    private BlueOfferHidingRuleService blueOfferHidingRuleService;
    @Mock
    private ExceptionalShopsService exceptionalShopsService;


    @Mock
    private OrderHistoryEvent event;

    @Mock
    private Order orderBefore;
    @Mock
    private Order orderAfter;

    @Mock
    private Delivery delivery;

    @Mock
    private OrderItem missingItem;
    @Mock
    private OrderItem anotherItem;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(event.getOrderBefore()).thenReturn(orderBefore);
        when(event.getOrderAfter()).thenReturn(orderAfter);

        when(delivery.getDeliveryPartnerType()).thenReturn(DeliveryPartnerType.YANDEX_MARKET);

        when(orderBefore.getId()).thenReturn(ORDER_ID);
        when(orderBefore.getDelivery()).thenReturn(delivery);
        when(orderBefore.isFulfilment()).thenReturn(true);

        when(orderAfter.getId()).thenReturn(ORDER_ID);
        when(orderAfter.isFulfilment()).thenReturn(true);
        when(orderAfter.getDelivery()).thenReturn(delivery);

        when(missingItem.getId()).thenReturn(MISSING_ITEM_ID);
        when(missingItem.getSupplierId()).thenReturn(SUPPLIER_ID);
        when(missingItem.getShopSku()).thenReturn(SHOP_SKU);
        when(missingItem.getAtSupplierWarehouse()).thenReturn(true);

        when(anotherItem.getId()).thenReturn(ANOTHER_ITEM_ID);

        when(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_MISSING_ITEM)).thenReturn(Collections.emptySet());
    }

    @Test
    void validateCancelledEvent__notMissingItemReason() {
        mocksForCancelledOrder(true);

        when(orderAfter.getSubstatus()).thenReturn(OrderSubstatus.USER_CHANGED_MIND);

        assertFalse(crossdockMissingItemListener.validateEvent(event));
    }

    @Test
    void createHidingsForCancelledOrder__notCrossdockItem() {
        mocksForCancelledOrder(true);

        when(delivery.getDeliveryPartnerType()).thenReturn(DeliveryPartnerType.SHOP);

        assertNull(crossdockMissingItemListener.prepareListenerTaskBody(event));
        verifyNoMoreInteractions(blueOfferHidingRuleService);
    }

    @Test
    void createHidingsForCancelledOrder__supplierIgnored() {
        mocksForCancelledOrder(true);

        when(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_MISSING_ITEM)).thenReturn(Set.of(SUPPLIER_ID));

        assertNull(crossdockMissingItemListener.prepareListenerTaskBody(event));
        verifyNoMoreInteractions(blueOfferHidingRuleService);
    }

    @Test
    void createHidingsForCancelledOrder__detailsNotExistAndSeveralItems() {
        mocksForCancelledOrder(false);

        assertNull(crossdockMissingItemListener.prepareListenerTaskBody(event));
    }

    @Test
    void createHidingsForCancelledOrder__createHiding() {
        mocksForCancelledOrder(true);

        var taskBody = crossdockMissingItemListener.prepareListenerTaskBody(event);
        crossdockMissingItemListener.processListenerTask(taskBody);

        verify(blueOfferHidingRuleService).saveWithSskuExistingFilter(List.of(EXPECTED_HIDING_RULE), FakeUsers.BLUE_OFFER_HIDING_PROCESSOR.getId());
    }

    @Test
    void createHidingsForCancelledOrder__singleItem() {
        mocksForCancelledOrder(false);
        when(orderAfter.getItems()).thenReturn(List.of(missingItem));

        var taskBody = crossdockMissingItemListener.prepareListenerTaskBody(event);
        crossdockMissingItemListener.processListenerTask(taskBody);

        verify(blueOfferHidingRuleService).saveWithSskuExistingFilter(List.of(EXPECTED_HIDING_RULE), FakeUsers.BLUE_OFFER_HIDING_PROCESSOR.getId());
    }

    @Test
    void createHidingsForUpdatedItems__itemsSetNotChanged() {
        mockForUpdatedItems();
        when(orderAfter.getItems()).thenReturn(List.of(missingItem, anotherItem));

        assertNull(crossdockMissingItemListener.prepareListenerTaskBody(event));
        verifyNoMoreInteractions(blueOfferHidingRuleService);
    }

    @Test
    void createHidingsForUpdatedItems__createHidings() {
        mockForUpdatedItems();

        var taskBody = crossdockMissingItemListener.prepareListenerTaskBody(event);
        crossdockMissingItemListener.processListenerTask(taskBody);

        verify(blueOfferHidingRuleService).saveWithSskuExistingFilter(List.of(EXPECTED_HIDING_RULE), FakeUsers.BLUE_OFFER_HIDING_PROCESSOR.getId());
    }

    private void mocksForCancelledOrder(boolean reasonDetailsExist) {
        when(orderAfter.getSubstatus()).thenReturn(OrderSubstatus.MISSING_ITEM);
        when(orderAfter.getItems()).thenReturn(List.of(missingItem, anotherItem));

        when(event.getType()).thenReturn(HistoryEventType.ORDER_STATUS_UPDATED);

        if (reasonDetailsExist) {
            var reasonDetails = mock(OrderHistoryEventReasonDetails.class);
            var missingItem = mock(ItemCount.class);
            when(missingItem.getId()).thenReturn(MISSING_ITEM_ID);
            when(missingItem.getCount()).thenReturn(1);
            when(reasonDetails.getMissingOrderItems()).thenReturn(List.of(missingItem));

            when(event.getReasonDetails()).thenReturn(reasonDetails);
        }

        when(missingItemCheckpointService.checkpointExists(eq(ORDER_ID), eq(CheckpointType.ORDER_CANCELLED_BY_MISSING_ITEM)))
                .thenReturn(true);
    }

    private void mockForUpdatedItems() {
        when(orderBefore.getItems()).thenReturn(List.of(missingItem, anotherItem));
        when(orderAfter.getItems()).thenReturn(List.of(anotherItem));

        when(event.getType()).thenReturn(HistoryEventType.ITEMS_UPDATED);

        when(missingItemCheckpointService.checkpointExists(eq(ORDER_ID), eq(CheckpointType.ITEMS_UPDATE_BY_MISSING_ITEM)))
                .thenReturn(true);
    }

    private static BlueOfferHidingRule buildExpectedHidingRule() {
        var hidingRule = new BlueOfferHidingRule();
        hidingRule.setSupplierId(SUPPLIER_ID);
        hidingRule.setShopSku(SHOP_SKU);
        hidingRule.setHidingReason(BlueOfferHidingReason.MISSING_ITEM);
        hidingRule.setComment(String.valueOf(ORDER_ID));
        hidingRule.setDeleted(false);

        return hidingRule;
    }

    @Test
    void deserializeTest() throws Exception {
        var json = "{\"order_id\":1,\"items\":[{\"order_id\":1,\"supplier_id\":2,\"shop_sku\":\"3\"}]}";
        var taskBody = new CrossdockMissingItemListener.TaskBody(1L, List.of(
                new CrossdockMissingItemListener.MissingItemJson(1L, 2L, "3")
        ));
        assertEquals(taskBody, new ObjectMapper().readValue(json, CrossdockMissingItemListener.TaskBody.class));
    }
}
