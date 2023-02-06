package ru.yandex.market.pipelinetests.tests.fulfillment;

import delivery.client.lrm.client.model.LogisticPointType;
import delivery.client.lrm.client.model.ReturnBoxStatus;
import delivery.client.lrm.client.model.ReturnSegmentStatus;
import delivery.client.lrm.client.model.ShipmentDestination;
import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import step.FfwfApiSteps;
import step.ScIntSteps;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue FF Multibox Cancellation Return Test")
@Epic("Blue FF")
@Slf4j
public class MultiBoxCancellationReturnViaLrmTest extends AbstractFulfillmentTest {

    private static final int FIRST_KOROBYTE = 2;
    private static final int SECOND_KOROBYTE = 3;

    @Property("delivery.tomilino")
    private long tomilino;

    @Property("delivery.mkScZapadId")
    private long mkScId;

    @Property("delivery.mkScZapadLogisticPoint")
    private long mkScLogisticPointId;

    private static final ScIntSteps SC_INT_STEPS = new ScIntSteps();
    private static final FfwfApiSteps FFWF_API_STEPS = new FfwfApiSteps();

    private Integer mockId;

    @BeforeEach
    @Step("Подготовка данных: Создаем заказ")
    public void setUp() {
        log.info("Creating order for getOrder test...");

        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_171_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
            .build();

        order = ORDER_STEPS.createOrder(params);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        orderExternalId = LOM_ORDER_STEPS.getLomOrderData(order).getExternalId();

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
    @Tag("MultiboxFbyCancellationReturnTest")
    @DisplayName("Многокоробочный невыкуп FBY")
    public void twoBoxesGetOrderTest() {
        splitOrderToTwoBoxes();
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
        ORDER_STEPS.cancelOrder(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);

        String barcode1 = orderExternalId + "_KOROBKA1";
        String barcode2 = orderExternalId + "_KOROBKA2";

        ShipmentDestination shipmentDestination = LRM_STEPS.verifyScSegmentCreation(
            barcode1,
            mkScId,
            mkScLogisticPointId,
            tomilino
        );
        LRM_STEPS.verifyScSegmentCreation(barcode2, mkScId, mkScLogisticPointId, tomilino);
        LRM_STEPS.verifyLastMileSegmentCreation(
            barcode1,
            shipmentDestination.getPartnerId(),
            shipmentDestination.getLogisticPointId(),
            LogisticPointType.FULFILLMENT
        );
        LRM_STEPS.verifyLastMileSegmentCreation(
            barcode2,
            shipmentDestination.getPartnerId(),
            shipmentDestination.getLogisticPointId(),
            LogisticPointType.FULFILLMENT
        );

        LRM_STEPS.verifySegmentStatus(barcode1, mkScId, ReturnSegmentStatus.CREATED, true);
        LRM_STEPS.verifySegmentStatus(barcode2, mkScId, ReturnSegmentStatus.CREATED, true);
        LRM_STEPS.verifyBoxStatus(barcode1, ReturnBoxStatus.CREATED);
        LRM_STEPS.verifyBoxStatus(barcode2, ReturnBoxStatus.CREATED);

        SC_INT_STEPS.acceptAndSortOrder(orderExternalId, barcode1, mkScId);
        SC_INT_STEPS.acceptAndSortOrder(orderExternalId, barcode2, mkScId);
        LRM_STEPS.verifySegmentStatus(barcode1, mkScId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifySegmentStatus(barcode2, mkScId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifyBoxStatus(barcode1, ReturnBoxStatus.IN_TRANSIT);
        LRM_STEPS.verifyBoxStatus(barcode2, ReturnBoxStatus.IN_TRANSIT);

        SC_INT_STEPS.shipOrder(orderExternalId, barcode1, mkScId);
        SC_INT_STEPS.shipOrder(orderExternalId, barcode2, mkScId);
        LRM_STEPS.verifySegmentStatus(barcode1, mkScId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifySegmentStatus(barcode2, mkScId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifyBoxStatus(barcode1, ReturnBoxStatus.IN_TRANSIT);
        LRM_STEPS.verifyBoxStatus(barcode2, ReturnBoxStatus.IN_TRANSIT);

        FFWF_API_STEPS.confirmBoxRecieved(orderExternalId, barcode1, tomilino);
        FFWF_API_STEPS.confirmBoxRecieved(orderExternalId, barcode2, tomilino);
        LRM_STEPS.verifySegmentStatus(barcode1, tomilino, ReturnSegmentStatus.IN);
        LRM_STEPS.verifySegmentStatus(barcode2, tomilino, ReturnSegmentStatus.IN);
        LRM_STEPS.verifyBoxStatus(barcode1, ReturnBoxStatus.FULFILMENT_RECEIVED);
        LRM_STEPS.verifyBoxStatus(barcode2, ReturnBoxStatus.FULFILMENT_RECEIVED);
    }

    private void splitOrderToTwoBoxes() {
        long orderId = order.getId();
        Track trackNumber = ORDER_STEPS.getTrackNumbers(order, DeliveryServiceType.FULFILLMENT);
        mockId = MOCK_CLIENT.mockGetOrder(
            orderId,
            trackNumber.getTrackCode(),
            FIRST_KOROBYTE,
            SECOND_KOROBYTE
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

        long lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderHasTwoBoxes(lomOrderId);
    }
}
