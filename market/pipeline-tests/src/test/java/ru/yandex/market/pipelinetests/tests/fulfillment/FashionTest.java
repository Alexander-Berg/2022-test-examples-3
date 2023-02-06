package ru.yandex.market.pipelinetests.tests.fulfillment;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import delivery.client.lrm.client.model.CreateReturnResponse;
import delivery.client.lrm.client.model.LogisticPointType;
import delivery.client.lrm.client.model.ReturnBoxStatus;
import delivery.client.lrm.client.model.ReturnSegmentStatus;
import delivery.client.lrm.client.model.ReturnSource;
import delivery.client.lrm.client.model.ShipmentDestination;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import dto.requests.mock.GetOrderInstancesData;
import dto.requests.mock.GetOrderInstancesData.GetOrderInstancesItem;
import dto.requests.report.OfferItem;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import step.LrmSteps;
import step.ScIntSteps;
import step.TplSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties", "delivery/fashion.properties"})
@DisplayName("Blue FF Fashion")
@Epic("Blue FF")
@Tag("FashionFbyPartlyReturnTest")
@Slf4j
public class FashionTest extends AbstractFulfillmentTest {
    @Property("delivery.marketCourier")
    private long marketCourier;

    @Property("delivery.fashionOutletId")
    private long outletId;
    @Property("delivery.fashionPvzMarketId")
    private long pvzMarketId;

    @Property("delivery.mkScPiterId")
    private long mkSortingCenterId;
    @Property("delivery.mkScPiterLogisticPoint")
    private long mkSortingCenterLogisticPoint;
    @Property("delivery.secondWaveDS")
    private long mkDeliveryServiceId;

    @Property("delivery.tomilino")
    private long lastMilePartnerId;

    private static final LrmSteps LRM_STEPS = new LrmSteps();
    private static final TplSteps TPL_STEPS = new TplSteps();
    private static final ScIntSteps SC_INT_STEPS = new ScIntSteps();

    private String uit1;
    private String uit2;
    private List<String> itemsOfferIds;
    private String returnedItemOfferId;

    private Integer mockId;

    public void createOrderCourier() {
        List<OfferItem> items = List.of(
            OfferItems.FF_171_1P_FASHION_1.getItem(),
            OfferItems.FF_171_1P_FASHION_2.getItem()
        );
        itemsOfferIds = AbstractFulfillmentTest.LOM_ORDER_STEPS.getItemsOfferIds(items);
        returnedItemOfferId = itemsOfferIds.get(0);

        params = CreateOrderParameters
            .newBuilder(regionId, items, DeliveryType.DELIVERY)
            .forceDeliveryId(marketCourier)
            .experiment(EnumSet.of(RearrFactor.FORCE_DELIVERY_ID, RearrFactor.FASHION))
            .build();
        order = AbstractFulfillmentTest.ORDER_STEPS.createOrder(params);

        lomOrderId = AbstractFulfillmentTest.LOM_ORDER_STEPS.getLomOrderId(order);

        AbstractFulfillmentTest.ORDER_STEPS.verifySDTracksCreated(order);
        AbstractFulfillmentTest.ORDER_STEPS.verifyFFTrackCreated(order);
    }

    public void createOrderPvz() {
        List<OfferItem> items = List.of(
            OfferItems.FF_171_1P_FASHION_1.getItem(),
            OfferItems.FF_171_1P_FASHION_2.getItem()
        );
        itemsOfferIds = AbstractFulfillmentTest.LOM_ORDER_STEPS.getItemsOfferIds(items);
        returnedItemOfferId = itemsOfferIds.get(0);

        params = CreateOrderParameters
            .newBuilder(regionId, items, DeliveryType.PICKUP)
            .deliveryPredicate(Delivery::isMarketPartner)
            .experiment(EnumSet.of(RearrFactor.FASHION))
            .build();
        order = AbstractFulfillmentTest.ORDER_STEPS.createOrder(params);

        lomOrderId = AbstractFulfillmentTest.LOM_ORDER_STEPS.getLomOrderId(order);

        AbstractFulfillmentTest.ORDER_STEPS.verifySDTracksCreated(order);
        AbstractFulfillmentTest.ORDER_STEPS.verifyFFTrackCreated(order);
    }

    public void mockAndGetUIT(Order order) {
        Track trackNumber = AbstractFulfillmentTest.ORDER_STEPS.getTrackNumbers(order, DeliveryServiceType.FULFILLMENT);
        List<OrderItem> items = AbstractFulfillmentTest.ORDER_STEPS.getItems(order);
        mockId = AbstractFulfillmentTest.MOCK_CLIENT.mockGetOrderUit(
            GetOrderInstancesData.builder()
                .yandexId(String.valueOf(order.getId()))
                .ffTrackCode(trackNumber.getTrackCode())
                .supplierId(items.get(0).getSupplierId())
                .items(List.of(
                    GetOrderInstancesItem.builder()
                        .shopSku(items.get(0).getShopSku())
                        .price(items.get(0).getPrice())
                        .uit(uit1)
                        .build(),
                    GetOrderInstancesItem.builder()
                        .shopSku(items.get(1).getShopSku())
                        .price(items.get(1).getPrice())
                        .uit(uit2)
                        .build()
                ))
                .build()
        );
        AbstractFulfillmentTest.DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            trackNumber.getTrackerId(),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED
        );
        AbstractFulfillmentTest.ORDER_STEPS.verifyForCheckpointReceived(
            order,
            OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED,
            DeliveryServiceType.FULFILLMENT
        );

    }

    @AfterEach
    @Step("Чистка данных после теста")
    public void tearDown() {
        if (mockId != null) {
            MOCK_CLIENT.deleteMockById(mockId);
            mockId = null;
        }
    }

    @Test
    @DisplayName("Fashion FBY: частичный возврат заказа. Курьерка")
    public void fashionTest() {

        createOrderCourier();

        uit1 = "uit-1-" + order.getId();
        uit2 = "uit-2-" + order.getId();

        mockAndGetUIT(order);

        //Проверяем, что uit-ы. которые мы задали в моке проросли в чекаутер и ЛОМ
        OrderDto orderDto = AbstractFulfillmentTest.LOM_ORDER_STEPS.verifyInstances(order);
        List<String> instances = orderDto.getItems().stream()
                .flatMap(item -> item.getInstances().stream())
                .flatMap(insances -> insances.values().stream())
                .collect(Collectors.toList());
        Assertions.assertTrue(instances.contains(uit1),
                "UIT для 1-го товара не совпадает в ЛОМе и в моке");
        Assertions.assertTrue(uit2.equals(instances.get(0))
                || uit2.equals(instances.get(1)),
            "UIT для 2-го товара не совпадает в ЛОМе и в моке"
        );

        Assertions.assertTrue(
            uit1.equals(AbstractFulfillmentTest.ORDER_STEPS.getUit(order, 0))
                || uit1.equals(AbstractFulfillmentTest.ORDER_STEPS.getUit(order, 1)),
            "UIT для 1-го товара не совпадает в чекуатере и в моке"
        );
        Assertions.assertTrue(
            uit2.equals(AbstractFulfillmentTest.ORDER_STEPS.getUit(order, 0))
                || uit2.equals(AbstractFulfillmentTest.ORDER_STEPS.getUit(order, 1)),
            "UIT для 2-го товара не совпадает в чекуатере и в моке"
        );

        //Переводим в статус деливери, потому что только из него может быть создан частичный возврат
        WaybillSegmentDto lastMileWaybillSegment = AbstractFulfillmentTest.LOM_ORDER_STEPS
            .getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        AbstractFulfillmentTest.DELIVERY_TRACKER_STEPS
            .addOrderCheckpointToTracker(
                lastMileWaybillSegment.getTrackerId(),
                OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT
            );
        AbstractFulfillmentTest.ORDER_STEPS.verifyForOrderStatus(order, OrderStatus.DELIVERY);

        //Создаем возврат 1 товара в ЛРМ. Проверяем, что он удалился из чекаутера
        CreateReturnResponse returnRequest = LRM_STEPS.createReturnLrm(
            order.getId(),
            ReturnSource.COURIER,
            returnedItemOfferId,
            mkSortingCenterLogisticPoint
        );
        LRM_STEPS.commitReturnLrm(returnRequest.getId());

        AbstractFulfillmentTest.ORDER_STEPS.verifyItemsCount(order.getId(), 1);
        List<String> itemsInCOOfferIds = AbstractFulfillmentTest.ORDER_STEPS.getItemsFromCheckouterOfferIds(
            AbstractFulfillmentTest.ORDER_STEPS.getOrder(order.getId()));
        List<String> itemsOfferIdsAfterReturn = itemsOfferIds
                .stream()
                .filter(offerId -> !offerId.equals(returnedItemOfferId))
                .collect(Collectors.toList());

        Assertions.assertEquals(itemsOfferIdsAfterReturn, itemsInCOOfferIds,
            "Состав заказа " + order.getId() +
                " в чекаутере НЕ изменился после создания частичной отмены. Оставшийся товар в заказе: "
        );

        String barcode = "box-" + order.getId();

        LRM_STEPS.verifyReturnCommit(barcode);

        TPL_STEPS.receiveReturnFromPvzToSc(barcode, mkSortingCenterId, mkDeliveryServiceId);

        ShipmentDestination courierShipmentDestination =
            LRM_STEPS.verifyCourierSegmentShipment(barcode, mkSortingCenterId);

        ShipmentDestination mkScShipmentDestination = LRM_STEPS.verifyScSegmentCreation(
            barcode,
            courierShipmentDestination.getPartnerId(),
            courierShipmentDestination.getLogisticPointId(),
            lastMilePartnerId
        );

        SC_INT_STEPS.acceptAndSortOrder(barcode, barcode, mkSortingCenterId);

        LRM_STEPS.verifySegmentStatus(barcode, mkSortingCenterId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifyBoxStatus(barcode, ReturnBoxStatus.IN_TRANSIT);

        SC_INT_STEPS.shipOrder(barcode, barcode, mkSortingCenterId);

        LRM_STEPS.verifySegmentStatus(barcode, mkSortingCenterId, ReturnSegmentStatus.OUT);

        LRM_STEPS.verifyLastMileSegmentCreation(
            barcode,
            mkScShipmentDestination.getPartnerId(),
            mkScShipmentDestination.getLogisticPointId(),
            LogisticPointType.FULFILLMENT
        );
    }

    @Test
    @DisplayName("Fashion FBY: частичный возврат заказа. ПВЗ")
    public void fashionTestPvz() {

        createOrderPvz();

        uit1 = "uit-1-" + order.getId();
        uit2 = "uit-2-" + order.getId();

        mockAndGetUIT(order);

        //Проверяем, что uit-ы. которые мы задали в моке проросли в чекаутер и ЛОМ
        OrderDto orderDto = AbstractFulfillmentTest.LOM_ORDER_STEPS.verifyInstances(order);
        List<String> instances = orderDto.getItems().stream()
                .flatMap(item -> item.getInstances().stream())
                .flatMap(insances -> insances.values().stream())
                .collect(Collectors.toList());
        Assertions.assertTrue(instances.contains(uit1),
                "UIT для 1-го товара не совпадает в ЛОМе и в моке");
        Assertions.assertTrue(uit2.equals(instances.get(0))
                || uit2.equals(instances.get(1)),
            "UIT для 2-го товара не совпадает в ЛОМе и в моке"
        );

        Assertions.assertTrue(
            uit1.equals(AbstractFulfillmentTest.ORDER_STEPS.getUit(order, 0))
                || uit1.equals(AbstractFulfillmentTest.ORDER_STEPS.getUit(order, 1)),
            "UIT для 1-го товара не совпадает в чекуатере и в моке"
        );
        Assertions.assertTrue(
            uit2.equals(AbstractFulfillmentTest.ORDER_STEPS.getUit(order, 0))
                || uit2.equals(AbstractFulfillmentTest.ORDER_STEPS.getUit(order, 1)),
            "UIT для 2-го товара не совпадает в чекуатере и в моке"
        );

        //Переводим в статус деливери, потому что только из него может быть создан частичный возврат
        WaybillSegmentDto lastMileWaybillSegment = AbstractFulfillmentTest.LOM_ORDER_STEPS
            .getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        AbstractFulfillmentTest.DELIVERY_TRACKER_STEPS
            .addOrderCheckpointToTracker(
                lastMileWaybillSegment.getTrackerId(),
                OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT
            );
        AbstractFulfillmentTest.ORDER_STEPS.verifyForOrderStatus(order, OrderStatus.DELIVERY);

        //Создаем возврат 1 товара в ЛРМ. Проверяем, что он удалился из чекаутера
        CreateReturnResponse returnRequest = LRM_STEPS.createReturnLrm(
            order.getId(),
            ReturnSource.PICKUP_POINT,
            returnedItemOfferId,
            null
        );
        LRM_STEPS.commitReturnLrm(returnRequest.getId());

        AbstractFulfillmentTest.ORDER_STEPS.verifyItemsCount(order.getId(), 1);
        List<String> itemsInCOOfferIds = AbstractFulfillmentTest.ORDER_STEPS.getItemsFromCheckouterOfferIds(
            AbstractFulfillmentTest.ORDER_STEPS.getOrder(order.getId()));
        List<String> itemsOfferIdsAfterReturn = itemsOfferIds
                .stream()
                .filter(offerId -> !offerId.equals(returnedItemOfferId))
                .collect(Collectors.toList());

        Assertions.assertEquals(itemsOfferIdsAfterReturn, itemsInCOOfferIds,
            "Состав заказа " + order.getId() +
                " в чекаутере НЕ изменился после создания частичной отмены. Оставшийся товар в заказе: "
        );

        String barcode = "box-" + order.getId();

        LRM_STEPS.verifyReturnCommit(barcode);

        TPL_STEPS.receiveReturnFromPvzToSc(barcode, mkSortingCenterId, mkDeliveryServiceId);

        ShipmentDestination pvzShipmentDestination = LRM_STEPS.verifyPvzSegmentShipment(barcode, mkSortingCenterId);

        ShipmentDestination mkScShipmentDestination = LRM_STEPS.verifyScSegmentCreation(
            barcode,
            pvzShipmentDestination.getPartnerId(),
            pvzShipmentDestination.getLogisticPointId(),
            lastMilePartnerId
        );

        SC_INT_STEPS.acceptAndSortOrder(barcode, barcode, mkSortingCenterId);

        LRM_STEPS.verifySegmentStatus(barcode, mkSortingCenterId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifyBoxStatus(barcode, ReturnBoxStatus.IN_TRANSIT);

        SC_INT_STEPS.shipOrder(barcode, barcode, mkSortingCenterId);

        LRM_STEPS.verifySegmentStatus(barcode, mkSortingCenterId, ReturnSegmentStatus.OUT);

        LRM_STEPS.verifyLastMileSegmentCreation(
            barcode,
            mkScShipmentDestination.getPartnerId(),
            mkScShipmentDestination.getLogisticPointId(),
            LogisticPointType.FULFILLMENT
        );
    }
}
