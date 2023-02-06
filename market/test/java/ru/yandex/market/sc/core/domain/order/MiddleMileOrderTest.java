package ru.yandex.market.sc.core.domain.order;

import java.util.Objects;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
public class MiddleMileOrderTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    OrderQueryService orderQueryService;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    private ScOrder createDirectOrder(PlaceState aState, PlaceState bState, PlaceState cState) {
        var placesOutOfWarehouse = StreamEx.of(aState == PlaceState.WAREHOUSE ? Stream.empty() : Stream.of("a"))
                .append(bState == PlaceState.WAREHOUSE ? Stream.empty() : Stream.of("b"))
                .append(cState == PlaceState.WAREHOUSE ? Stream.empty() : Stream.of("c"))
                .toList();

        var placesOnCourier = StreamEx.of(aState != PlaceState.COURIER ? Stream.empty() : Stream.of("a"))
                .append(bState != PlaceState.COURIER ? Stream.empty() : Stream.of("b"))
                .append(cState != PlaceState.COURIER ? Stream.empty() : Stream.of("c"))
                .toList();
        return testFactory.create(
                order(sortingCenter).externalId("o").places("a", "b", "c")
                        .dsType(DeliveryServiceType.TRANSIT).build()
        )
                .acceptPlaces(placesOutOfWarehouse)
                .sortPlaces(placesOnCourier)
                .shipPlaces(placesOnCourier)
                .get();
    }

    private ScOrder createReturnOrder(PlaceState aState, PlaceState bState, PlaceState cState) {
        var placesOutOfCourier = StreamEx.of(aState == PlaceState.COURIER ? Stream.empty() : Stream.of("a"))
                .append(bState == PlaceState.COURIER ? Stream.empty() : Stream.of("b"))
                .append(cState == PlaceState.COURIER ? Stream.empty() : Stream.of("c"))
                .toList();

        var placesOnWarehouse = StreamEx.of(aState != PlaceState.WAREHOUSE ? Stream.empty() : Stream.of("a"))
                .append(bState != PlaceState.WAREHOUSE ? Stream.empty() : Stream.of("b"))
                .append(cState != PlaceState.WAREHOUSE ? Stream.empty() : Stream.of("c"))
                .toList();
        return testFactory.create(
                order(sortingCenter).externalId("o").places("a", "b", "c")
                        .dsType(DeliveryServiceType.TRANSIT).build()
        )
                .acceptPlaces("a", "b", "c")
                .sortPlaces("a", "b", "c")
                .shipPlaces("a", "b", "c")
                .makeReturn()
                .acceptPlaces(placesOutOfCourier)
                .sortPlaces(placesOnWarehouse)
                .shipPlaces(placesOnWarehouse)
                .get();
    }

    private enum PlaceState {
        WAREHOUSE,
        SORTING_CENTER,
        COURIER
    }

    private void verifyPlaceApiStatus(OrderLike order, String placeExternalId, ApiOrderStatus expected) {
        assertThat(
                Objects.requireNonNull(
                                orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(),
                                        placeExternalId).getPlaces()
                        ).stream()
                        .filter(p -> Objects.equals(placeExternalId, p.getExternalId()))
                        .findFirst().orElseThrow()
                        .getStatus()
        )
                .isEqualTo(expected);
    }

    @Test
    void allAtWarehouse() {
        var order = createDirectOrder(
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE
        );
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.CREATED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.CREATED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void acceptFirstPlaceFromWarehouse() {
        var order = createDirectOrder(
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE
        );
        order = testFactory.acceptPlace(order, "a").getOrder(order.getId());
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.CREATED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void acceptSecondPlaceFromWarehouse() {
        var order = createDirectOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE
        );
        order = testFactory.acceptPlace(order, "b").getOrder(order.getId());
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void acceptThirdPlaceFromWarehouse() {
        var order = createDirectOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE
        );
        order = testFactory.acceptPlace(order, "c").getOrder(order.getId());
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void shipFirstPlaceToCourierFirstAccepted() {
        var order = createDirectOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE
        );
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        testFactory.sortPlace(order, "a");
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "a").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.CREATED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void shipFirstPlaceToCourierSecondAccepted() {
        var order = createDirectOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE
        );
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        testFactory.sortPlace(order, "a");
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "a").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void acceptSecondPlaceFirstShipped() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE
        );
        order = testFactory.acceptPlace(order, "b").getOrder(order.getId());
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void acceptThirdPlaceFirstShipped() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE
        );
        order = testFactory.acceptPlace(order, "c").getOrder(order.getId());
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void shipSecondPlaceThirdCreated() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE
        );
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.SORT_TO_COURIER);
        testFactory.sortPlace(order, "b");
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "b").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void shipFirstPlaceThirdAccepted() {
        var order = createDirectOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER
        );
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        testFactory.sortPlace(order, "a");
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "a").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void shipSecondPlaceThirdAccepted() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER
        );
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.SORT_TO_COURIER);
        testFactory.sortPlace(order, "b");
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "b").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void acceptThirdPlaceSecondShipped() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.COURIER,
                PlaceState.WAREHOUSE
        );
        order = testFactory.acceptPlace(order, "c").getOrder(order.getId());
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void shipThirdPlace() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER
        );
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.SORT_TO_COURIER);
        testFactory.sortPlace(order, "c");
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "c").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void toCourierAfterFirstShippedFirstAcceptedFromCourier() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE
        );
        order = testFactory.acceptPlace(order, "a").getOrder(order.getId());
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.CREATED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void toCourierAfterFirstShippedSecondAcceptedFirstAcceptedFromCourier() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE
        );
        order = testFactory.acceptPlace(order, "a").getOrder(order.getId());
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void toCourierAfterFirstShippedThirdAcceptedFirstAcceptedFromCourier() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER
        );
        order = testFactory.acceptPlace(order, "a").getOrder(order.getId());
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void toCourierAfterSecondShippedFirstAcceptedFromCourier() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.COURIER,
                PlaceState.WAREHOUSE
        );
        order = testFactory.acceptPlace(order, "a").getOrder(order.getId());
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void toCourierAfterSecondShippedThirdAcceptedFirstAcceptedFromCourier() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER
        );
        order = testFactory.acceptPlace(order, "a").getOrder(order.getId());
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void toCourierAfterThirdShippedFirstAcceptedFromCourier() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.COURIER,
                PlaceState.COURIER
        );
        order = testFactory.acceptPlace(order, "a").getOrder(order.getId());
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_COURIER);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void allAtWarehouseCancel() {
        var order = createDirectOrder(
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
    }

    @Test
    void firstAcceptedCancel() {
        var order = createDirectOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void secondAcceptedCancel() {
        var order = createDirectOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void thirdAcceptedCancel() {
        var order = createDirectOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void firstShippedCancel() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    @Test
    void firstShippedSecondAcceptedCancel() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER,
                PlaceState.WAREHOUSE
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void firstShippedThirdAcceptedCancel() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void secondShippedCancel() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.COURIER,
                PlaceState.WAREHOUSE
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    @Test
    void secondShippedThirdAcceptedCancel() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.COURIER,
                PlaceState.SORTING_CENTER
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void thirdShippedCancel() {
        var order = createDirectOrder(
                PlaceState.COURIER,
                PlaceState.COURIER,
                PlaceState.COURIER
        );
        order = testFactory.cancelOrder(order.getId()).getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    @Test
    void acceptFirstPlaceFromCourier() {
        var order = createReturnOrder(
                PlaceState.COURIER,
                PlaceState.COURIER,
                PlaceState.COURIER
        );
        order = testFactory.acceptPlace(order, "a").getOrder(order.getId());
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void returnFirstPlaceToWarehouse() {
        var order = createReturnOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.COURIER,
                PlaceState.COURIER
        );
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_WAREHOUSE);
        testFactory.sortPlace(order, "a");
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "a").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void acceptSecondPlaceFromCourier() {
        var order = createReturnOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.COURIER,
                PlaceState.COURIER
        );
        order = testFactory.acceptPlace(order, "b").getOrder(order.getId());
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void returnFirstPlaceToWarehouseSecondAccepted() {
        var order = createReturnOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER,
                PlaceState.COURIER
        );
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_WAREHOUSE);
        testFactory.sortPlace(order, "a");
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "a").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void acceptThirdPlaceFromCourier() {
        var order = createReturnOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER,
                PlaceState.COURIER
        );
        order = testFactory.acceptPlace(order, "c").getOrder(order.getId());
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void acceptSecondPlaceFromCourierFirstReturned() {
        var order = createReturnOrder(
                PlaceState.WAREHOUSE,
                PlaceState.COURIER,
                PlaceState.COURIER
        );
        order = testFactory.acceptPlace(order, "b").getOrder(order.getId());
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void returnSecondPlaceToWarehouse() {
        var order = createReturnOrder(
                PlaceState.WAREHOUSE,
                PlaceState.SORTING_CENTER,
                PlaceState.COURIER
        );
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.SORT_TO_WAREHOUSE);
        testFactory.sortPlace(order, "b");
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "b").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void returnFirstPlaceToWarehouseThirdAccepted() {
        var order = createReturnOrder(
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER
        );
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.SORT_TO_WAREHOUSE);
        testFactory.sortPlace(order, "a");
        verifyPlaceApiStatus(order, "a", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "a").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void acceptThirdPlaceFromCourierFirstReturnedSecondAccepted() {
        var order = createReturnOrder(
                PlaceState.WAREHOUSE,
                PlaceState.SORTING_CENTER,
                PlaceState.COURIER
        );
        order = testFactory.acceptPlace(order, "c").getOrder(order.getId());
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void acceptThirdPlaceFromCourierSecondReturned() {
        var order = createReturnOrder(
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE,
                PlaceState.COURIER
        );
        order = testFactory.acceptPlace(order, "c").getOrder(order.getId());
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void returnSecondPlaceToWarehouseThirdAccepted() {
        var order = createReturnOrder(
                PlaceState.WAREHOUSE,
                PlaceState.SORTING_CENTER,
                PlaceState.SORTING_CENTER
        );
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.SORT_TO_WAREHOUSE);
        testFactory.sortPlace(order, "b");
        verifyPlaceApiStatus(order, "b", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "b").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void returnThirdPlaceToWarehouse() {
        var order = createReturnOrder(
                PlaceState.WAREHOUSE,
                PlaceState.WAREHOUSE,
                PlaceState.SORTING_CENTER
        );
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.SORT_TO_WAREHOUSE);
        testFactory.sortPlace(order, "c");
        verifyPlaceApiStatus(order, "c", ApiOrderStatus.OK);
        order = testFactory.shipPlace(order, "c").getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(testFactory.orderPlace(order, "a").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "b").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "c").getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

}
