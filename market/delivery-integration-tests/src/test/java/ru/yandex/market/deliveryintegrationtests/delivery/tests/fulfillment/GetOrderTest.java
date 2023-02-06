package ru.yandex.market.deliveryintegrationtests.delivery.tests.fulfillment;

import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;

@Resource.Classpath({"delivery/checkouter.properties"})
@DisplayName("Blue FF getOrder Test")
@Epic("Blue FF")
@Slf4j
public class GetOrderTest extends AbstractFulfillmentTest {

    private static final int FIRST_KOROBYTE = 2;
    private static final int SECOND_KOROBYTE = 3;

    private Integer mockId;

    @BeforeEach
    @Step("Подготовка данных: Создаем заказ")
    public void setUp() {

        log.info("Creating order for getOrder test...");

        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.FF_171_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
                .build();

        order = ORDER_STEPS.createOrder(params);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

    }

    @AfterEach
    @Step("Удаление данных после выполнения теста")
    public void teardown() {
        if (mockId != null) {
            MOCK_CLIENT.deleteMockById(mockId);
            mockId = null;
        }
    }

    @Test
    @TmsLink("logistic-11")
    @DisplayName("FF: Разбитие заказа на 2 коробки")
    public void twoBoxesGetOrderTest() {
        log.info("Starting Two Boxes GetOrder test...");
        long orderId = order.getId();
        Track trackNumber = ORDER_STEPS.getTrackNumbers(order, DeliveryServiceType.FULFILLMENT);
        mockId = MOCK_CLIENT.mockGetOrder(
                orderId,
                trackNumber.getTrackCode(),
                FIRST_KOROBYTE,
                SECOND_KOROBYTE
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(trackNumber.getTrackerId(), OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED);
        ORDER_STEPS.verifyForCheckpointReceived(order, OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED, DeliveryServiceType.FULFILLMENT);

        long lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderHasTwoBoxes(lomOrderId);
    }

}
