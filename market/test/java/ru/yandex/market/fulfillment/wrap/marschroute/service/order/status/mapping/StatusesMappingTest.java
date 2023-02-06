package ru.yandex.market.fulfillment.wrap.marschroute.service.order.status.mapping;

import com.google.common.collect.ImmutableMultimap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.marschroute.model.base.DeliveryId;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingStatus;
import ru.yandex.market.logistic.api.model.common.OrderStatusCategory;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

class StatusesMappingTest extends SoftAssertionSupport {

    /**
     * Проверяет, что в FF маппинге все статусы принадлежат категории FF.
     */
    @Test
    void testThatFulfillmentMappingStatusesBelongToFulfillment() throws Exception {
        assertStatusesBelongingToCategory(
            FulfillmentStatusesMapping.MAPPING,
            OrderStatusCategory.FULFILLMENT,
            DeliveryId.MARKET_DELIVERY
        );
    }

    /**
     * Проверяет, что в Delivery маппинге все статусы принадлежат категории Delivery.
     */
    @Test
    void testThatDeliveryMappingStatusesBelongToDelivery() throws Exception {
        assertStatusesBelongingToCategory(
            DeliveryStatusesMapping.MAPPING,
            OrderStatusCategory.DELIVERY,
            DeliveryId.MARSCHROUTE_COURIER
        );
    }

    /**
     * Проверяет, что в Other маппинге все статусы принадлежат категории Other.
     */
    @Test
    void testThatOtherMappingStatusesBelongToOther() throws Exception {
        assertStatusesBelongingToCategory(
            OtherStatusesMapping.MAPPING,
            OrderStatusCategory.OTHER,
            DeliveryId.MARKET_DELIVERY
        );
    }

    private void assertStatusesBelongingToCategory(ImmutableMultimap<TrackingStatus, StatusMapping> mapping,
                                                   OrderStatusCategory category,
                                                   DeliveryId targetDeliveryId) {
        mapping.forEach((key, value) -> {
            OrderStatusType orderStatusType = value.get(targetDeliveryId);

            softly.assertThat(orderStatusType.getCategory())
                .as("Asserting that status category is [" + category + "]")
                .isEqualTo(category);
        });
    }
}
