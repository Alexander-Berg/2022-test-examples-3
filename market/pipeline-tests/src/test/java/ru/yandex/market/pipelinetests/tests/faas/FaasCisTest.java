package ru.yandex.market.pipelinetests.tests.faas;

import java.util.List;
import java.util.Set;

import client.DsFfMockClient;
import dto.requests.mock.GetOrderInstancesData;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import step.LomOrderSteps;
import step.OrderServiceSteps;
import step.TrackerSteps;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.order_service.client.model.DeliveryOptionsDto;
import ru.yandex.market.order_service.client.model.GetOrderItemsLogisticsDto;
import ru.yandex.market.order_service.client.model.GetOrderLogisticsDto;
import ru.yandex.market.order_service.client.model.OrderStatus;
import ru.yandex.market.order_service.client.model.OrderSubStatus;

@DisplayName("Получение КИЗов для Faas заказа")
public class FaasCisTest {
    private static final long SHOP_ID = 10427354L;
    private static final OrderServiceSteps ORDER_SERVICE_STEPS = new OrderServiceSteps();
    private static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    private static final TrackerSteps TRACKER_STEPS = new TrackerSteps();
    protected static final DsFfMockClient MOCK_CLIENT = new DsFfMockClient();

    private Long lomOrderId;
    private Long mbiOsOrderId;
    private OrderDto lomOrderData;

    private Integer mockId;

    @BeforeEach
    public void init() {
        DeliveryOptionsDto deliveryOptions = ORDER_SERVICE_STEPS.getDeliveryOptions(SHOP_ID);
        mbiOsOrderId = ORDER_SERVICE_STEPS.createOrder(
            SHOP_ID,
            deliveryOptions.getOptions().get(0)
        )
            .getOrderId();

        lomOrderId = LOM_ORDER_STEPS.getLomOrderData(
            OrderSearchFilter.builder()
                .externalIds(Set.of(mbiOsOrderId.toString()))
                .platformClientIds(Set.of(PlatformClient.FAAS.getId()))
                .build()
        )
            .getId();

        LOM_ORDER_STEPS.verifyAllSegmentsAreCreated(lomOrderId);
        ORDER_SERVICE_STEPS.assertOrderStatus(SHOP_ID, mbiOsOrderId, OrderStatus.PROCESSING, OrderSubStatus.PACKAGING);
        lomOrderData = LOM_ORDER_STEPS.getLomOrderData(
            OrderSearchFilter.builder()
                .externalIds(Set.of(mbiOsOrderId.toString()))
                .platformClientIds(Set.of(PlatformClient.FAAS.getId()))
                .build()
        );
    }

    @AfterEach
    @Step("Чистка моков после теста")
    public void tearDown() {
        if (mockId != null) {
            MOCK_CLIENT.deleteMockById(mockId);
            mockId = null;
        }
    }

    @Test
    @DisplayName("Заказ получил КИЗы")
    void orderReceivedCis() {
        String cis = "011002566481941121mbg:zCaRlUc050";
        GetOrderLogisticsDto order = ORDER_SERVICE_STEPS.getOrder(SHOP_ID, mbiOsOrderId);
        List<WaybillSegmentDto> waybillSegments = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId);
        WaybillSegmentDto ffSegment = waybillSegments
            .stream()
            .filter(segment -> segment.getPartnerType() == PartnerType.FULFILLMENT)
            .findFirst()
            .orElseThrow();
        GetOrderItemsLogisticsDto item = order.getItems().get(0);
        GetOrderInstancesData mockData = GetOrderInstancesData.builder()
            .yandexId(lomOrderData.getBarcode())
            .ffTrackCode(ffSegment.getExternalId())
            .supplierId(item.getPartnerId())
            .items(
                List.of(
                    GetOrderInstancesData.GetOrderInstancesItem.builder()
                        .shopSku(item.getShopSku())
                        .price(item.getPrice())
                        .cis(cis)
                        .build()
                )
            )
            .build();
        MOCK_CLIENT.mockGetOrderCis(mockData);
        TRACKER_STEPS.addOrderCheckpointToTracker(
            ffSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED
        );
        ORDER_SERVICE_STEPS.assertCis(SHOP_ID, mbiOsOrderId, cis);
    }
}
