package ru.yandex.market.deliveryintegrationtests.delivery.tests.fulfillment;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import dto.requests.report.OfferItem;
import factory.OfferItems;
import org.junit.jupiter.api.AfterEach;
import step.WmsSteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue FF Item Removal Test")
@Epic("Blue FF")
@Slf4j
public class ItemRemovalTest extends AbstractFulfillmentTest {

    @Property("delivery.tomilino")
    private long ffId;
    @Property("delivery.marketCourier")
    private long marketCourier;

    private static final WmsSteps WMS_STEPS = new WmsSteps();
    private List<OfferItem> items;

    private Integer mockId;

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        items = OfferItems.FF_171_UNFAIR_STOCK.getItems(2, true);

        CreateOrderParameters params = CreateOrderParameters
                .newBuilder(regionId, items, DeliveryType.DELIVERY)
                .forceDeliveryId(marketCourier)
                .experiment(EnumSet.of(RearrFactor.FORCE_DELIVERY_ID))
                .build();
        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

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

    private void setMock(List<String> itemsOfferIds) {
        Track trackNumber = ORDER_STEPS.getTrackNumbers(order, DeliveryServiceType.FULFILLMENT);
        mockId = MOCK_CLIENT.mockGetOrderItemRemoval(
                String.valueOf(order.getId()),
                trackNumber.getTrackCode(),
                StringUtils.substringAfter(itemsOfferIds.get(0), "."),
                order.getItems().iterator().next().getSupplierId(),
                items.get(0).getItems().get(0).getBuyerPrice(),
                StringUtils.substringAfter(itemsOfferIds.get(1), "."),
                items.get(0).getItems().get(1).getBuyerPrice()
        );
    }

    @Test
    @TmsLink("logistic-9")
    @DisplayName("Удаление товара из заказа")
    public void itemRemovalTest() {
        log.debug("Start itemRemovalTest");

        //проверяем изначальный состав заказа в чекаутере
        List<String> itemsOfferIds = LOM_ORDER_STEPS.getItemsOfferIds(items);
        List<String> itemsInCOOfferIds = ORDER_STEPS.getItemsFromCheckouterOfferIds(order);
        Assertions.assertEquals(itemsOfferIds, itemsInCOOfferIds,
                "Заказ создан в чекаутере не с теми товарами, которые мы ожидали");

        WaybillSegmentDto ffWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, ffId);

        //задаем мок
        setMock(itemsOfferIds);

        //отправляем 114 чп
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(ffWaybillSegment.getTrackerId(),
                OrderDeliveryCheckpointStatus.SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS);


        // по 120 чп получаем новый состав заказа, ЛОМ автоматически обрабатывает изменения в заказе
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(ffWaybillSegment.getTrackerId(),
                OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED);
        ORDER_STEPS.verifyItemsCount(order.getId(), 1);

        List<String> itemsInCOAfterRemoveOfferId = ORDER_STEPS.getItemsFromCheckouterOfferIds(ORDER_STEPS.getOrder(order.getId()));
        List<String> articles = itemsInCOAfterRemoveOfferId
                .stream()
                .map(article -> StringUtils.substringAfter(article, "."))
                .collect(Collectors.toList());

        List<ItemDto> lomItems = LOM_ORDER_STEPS.getLomOrderData(order).getItems();
        List<String> lomItemsAfterRemoveOfferId = lomItems.stream().map(ItemDto::getArticle).collect(Collectors.toList());
        Assertions.assertEquals(articles, lomItemsAfterRemoveOfferId,
                "Неверный состав заказа в LOM после удаления товарав заказе " + order.getId());
    }


    @Test
    @TmsLink("logistic-12")
    @DisplayName("Отмена заказа, если товар не нашелся на складе")
    public void cancelOrderIfItemNotInStockTest() {
        log.debug("Start cancelOrderIfItemNotInStockTest");

        WaybillSegmentDto ffWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, ffId);

        //задаем мок
        setMock(LOM_ORDER_STEPS.getItemsOfferIds(items));

        DELIVERY_TRACKER_STEPS.verifyCheckpoint(ffWaybillSegment.getTrackerId(), 101);

        //отправляем 113 чп, проверяем, что заказ отменился
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(ffWaybillSegment.getTrackerId(),
                OrderDeliveryCheckpointStatus.SORTING_CENTER_OUT_OF_STOCK);

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);

        LOM_ORDER_STEPS.verifyOrderAnyMileSegmentStatus(
            lomOrderId,
            ffWaybillSegment.getPartnerId(),
            SegmentStatus.TRANSIT_OUT_OF_STOCK
        );
    }
}
