package ru.yandex.market.deliveryintegrationtests.delivery.tests.fulfillment;

import java.util.List;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.mock.GetOrderInstancesData;
import dto.requests.mock.GetOrderInstancesData.GetOrderInstancesItem;
import dto.responses.lgw.LgwTaskFlow;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;

@Resource.Classpath({"delivery/checkouter.properties"})
@DisplayName("CIS Test")
@Epic("Blue FF")
@Slf4j

public class CisTest extends AbstractFulfillmentTest {

    @Property("delivery.marketCourierMiddleDS")
    protected Long marketServiceId;

    private Integer mockId;

    @BeforeEach
    @Step("Подготовка данных: Создаем заказ в ПВЗ")
    public void setUp() {

        log.info("Creating order for CIS test...");

        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_171_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
            .forceDeliveryId(marketServiceId)
            .build();
        order = ORDER_STEPS.createOrder(params);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);
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
    @TmsLink("logistic-21")
    @DisplayName("КИЗы: Проброс КИЗа в чекаутер через мок getOrder-а")
    public void cisTest() {
        log.info("Starting CIS test...");

        long orderId = order.getId();
        Track trackNumber = ORDER_STEPS.getTrackNumbers(order, DeliveryServiceType.FULFILLMENT);
        String cis = "011002566481941121mbg:zCaRlUc050";
        OrderItem item = ORDER_STEPS.getItems(order).get(0);
        mockId = MOCK_CLIENT.mockGetOrderCis(
            GetOrderInstancesData.builder()
                .yandexId(String.valueOf(orderId))
                .ffTrackCode(trackNumber.getTrackCode())
                .supplierId(item.getSupplierId())
                .items(List.of(
                    GetOrderInstancesItem.builder()
                        .shopSku(item.getShopSku())
                        .price(item.getPrice())
                        .cis(cis)
                        .build()
                ))
                .build()
        );
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            trackNumber.getTrackerId(),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED
        );
        ORDER_STEPS.verifyForCheckpointReceived(
            order,
            OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED,
            DeliveryServiceType.FULFILLMENT
        );

        LGW_STEPS.getTaskFromListWithEntityIdAndRequestFlow(
            String.valueOf(orderId),
            LgwTaskFlow.DS_UPDATE_ITEMS_INSTANCES,
            "NEW"
        );

        Assertions.assertEquals(ORDER_STEPS.getCis(order), cis, "CIS в чекаутере не совпадает с CIS в моке");
    }

}
