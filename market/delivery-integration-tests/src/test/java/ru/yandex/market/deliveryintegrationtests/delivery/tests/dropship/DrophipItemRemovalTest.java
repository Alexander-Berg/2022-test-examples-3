package ru.yandex.market.deliveryintegrationtests.delivery.tests.dropship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dto.Item;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.report.OfferItem;
import factory.OfferItems;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import step.PartnerApiSteps;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.logistics.lom.model.enums.OrderStatus.CANCELLED;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/report.properties", "delivery/delivery.properties"})
@DisplayName("Blue Dropship Item Removal Test")
@Epic("Blue Dropship")
@Slf4j
public class DrophipItemRemovalTest extends AbstractDropshipTest {

    @Property("reportblue.dropshipSCCampaignId")
    private long dropshipSCCampaignId;

    @Property("reportblue.dropshipSCUID")
    private long dropshipSCUID;

    @Property("delivery.marketCourier")
    protected static Long marketCourier;

    @BeforeEach
    public void setUp() {
        partnerApiSteps = new PartnerApiSteps(dropshipSCUID, dropshipSCCampaignId);
    }

    @ParameterizedTest(name = "{0}")
    @TmsLinks({@TmsLink("logistic-97"),@TmsLink("logistic-101")})
    @MethodSource("getParams")
    @DisplayName("Удаление товара из заказа")
    public void itemRemovalTest(String name, int itemsCount, boolean completeRemoval, int callNumber) {
        createOrder(itemsCount);

        OrderDto lomOrder = LOM_ORDER_STEPS.getLomOrderData(order);
        lomOrderId = lomOrder.getId();
        List<ItemDto> lomItemsBeforeRemoval = lomOrder.getItems();

        List<OrderItem> orderItems = removeItems(completeRemoval, callNumber);

        verifyLomOrderBeforeShip(lomItemsBeforeRemoval);

        partnerApiSteps.packOrder(order);
        ORDER_STEPS.shipDropshipOrder(order);

        verifyLomOrderAfterShip(orderItems);
    }

    public static Stream<Arguments> getParams() {
        return Stream.of(
            //arguments("Два разных товара, полностью удаляем один", 2, true, 1),
            //arguments("Два разных товара, удаляем одну штуку одного из них", 2, false, 1),
            arguments("Несколько одинаковых товаров, удаляем одну штуку", 1, false, 1),
            arguments("Несколько одинаковых товаров, удаляем одну штуку два раза", 1, false, 2)
        );
    }

    private void createOrder(int itemsCount) {
        List<OfferItem> offerItems = OfferItems.DROPSHIP_SC.getItems(itemsCount, true);
        for (OfferItem offerItem : offerItems) {
            for (Item item : offerItem.getItems()) {
                item.setCount(4);
            }
        }
        params = CreateOrderParameters
            .newBuilder(regionId, offerItems, DeliveryType.DELIVERY)
            .forceDeliveryId(marketCourier)
            .build();
        order = ORDER_STEPS.createOrder(params);
    }

    private List<OrderItem> removeItems(boolean completeRemoval, int callNumber) {
        List<OrderItem> orderItems = new ArrayList<>(order.getItems());
        ORDER_STEPS.verifyForOrderStatus(order, PROCESSING);

        for (int i = 0; i < callNumber; ++i) {
            removeItem(orderItems, completeRemoval);
            ORDER_STEPS.removeItems(order);
        }

        DELIVERY_TRACKER_STEPS.instantRequest(order.getId());
        order = ORDER_STEPS.verifyChangeRequests(order.getId(), ChangeRequestType.ITEMS_REMOVAL, callNumber);
        Assertions.assertTrue(
            checkouterItemsRemoved(orderItems, order.getItems()),
            "Неверный состав заказа в чекаутере после удаления товара в заказе " + order.getId()
        );

        return orderItems;
    }

    private List<OrderItem> removeItems(boolean completeRemoval) {
        return removeItems(completeRemoval, 1);
    }

    private void removeItem(List<OrderItem> orderItems, boolean completeRemoval) {
        OrderItem removalItem = orderItems.get(0);
        if (completeRemoval) {
            removalItem.setCount(0);
        } else {
            removalItem.setCount(removalItem.getCount() - 1);
        }
    }

    private void verifyLomOrderBeforeShip(List<ItemDto> lomItemsBeforeRemoval) {
        OrderDto lomOrder = LOM_ORDER_STEPS.verifyChangeRequest(
            lomOrderId,
            ChangeOrderRequestType.ORDER_CHANGED_BY_PARTNER,
            Set.of(ChangeOrderRequestStatus.CREATED)
        );

        Assertions.assertTrue(
            lomItemsSame(lomItemsBeforeRemoval, lomOrder.getItems()),
            "В LOM поменялся состав заказа до 120 чекпоинта"
        );
    }

    private void verifyLomOrderAfterShip(List<OrderItem> orderItems) {
        DELIVERY_TRACKER_STEPS.instantRequest(order.getId());
        OrderDto lomOrder = LOM_ORDER_STEPS.verifyChangeRequest(
            lomOrderId,
            ChangeOrderRequestType.ORDER_CHANGED_BY_PARTNER,
            Set.of(ChangeOrderRequestStatus.SUCCESS, ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS)
        );

        Assertions.assertTrue(
            lomItemsRemoved(orderItems, lomOrder.getItems()),
            "Неверный состав заказа в LOM после удаления товара в заказе " + order.getId()
        );
    }

    private boolean checkouterItemsRemoved(Collection<OrderItem> expected, Collection<OrderItem> actual) {
        Map<Long, Integer> expectedItemsById = expected.stream()
            .filter(item -> item.getCount() > 0)
            .collect(Collectors.toMap(OrderItem::getId, OrderItem::getCount));

        return expectedItemsById.size() == actual.size()
            && actual.stream().allMatch(item -> item.getCount().equals(expectedItemsById.get(item.getId())));
    }

    private boolean lomItemsRemoved(Collection<OrderItem> expected, Collection<ItemDto> actual) {
        Map<Pair<Long, String>, Integer> expectedItemsById = expected.stream()
            .filter(item -> item.getCount() > 0)
            .collect(Collectors.toMap(
                    item -> Pair.of(item.getSupplierId(), item.getShopSku()),
                    OrderItem::getCount
                )
            );

        return expectedItemsById.size() == actual.size()
            && actual.stream().allMatch(
            item -> item.getCount().equals(expectedItemsById.get(Pair.of(item.getVendorId(), item.getArticle())))
        );
    }

    private boolean lomItemsSame(List<ItemDto> expected, List<ItemDto> actual) {
        Map<Pair<Long, String>, Integer> expectedItemsById = expected.stream()
            .collect(Collectors.toMap(
                    item -> Pair.of(item.getVendorId(), item.getArticle()),
                    ItemDto::getCount
                )
            );

        return expectedItemsById.size() == actual.size()
            && actual.stream().allMatch(
            item -> item.getCount().equals(expectedItemsById.get(Pair.of(item.getVendorId(), item.getArticle())))
        );
    }
}
