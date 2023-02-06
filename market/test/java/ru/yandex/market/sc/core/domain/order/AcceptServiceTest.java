package ru.yandex.market.sc.core.domain.order;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderState;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceId;
import ru.yandex.market.sc.core.domain.place.model.PlaceScRequest;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.stage.StageLoader;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.CourierDto;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension.testNotMigrated;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.useNewSortableFlow;

/**
 * @author mors741
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class AcceptServiceTest {

    private final AcceptService acceptService;
    private final OrderCommandService orderCommandService;
    private final PlaceRepository placeRepository;
    private final ScOrderRepository scOrderRepository;
    private final TestFactory testFactory;
    private final TransactionTemplate transactionTemplate;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(1234L);
        user = testFactory.storedUser(sortingCenter, 123L);
        TestFactory.setupMockClock(clock);
    }

    @Test
    void acceptOrders() {
        ScOrder order = testFactory.createOrder(sortingCenter).get();
        order = testFactory.updateForTodayDelivery(order);
        PlaceScRequest request = testFactory.placeScRequest(order, user);
        acceptService.acceptPlace(request);

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(order.getState()).isEqualTo(ScOrderState.ACCEPTED);
        checkLastEventOfStatus(order, ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void acceptCancelledOrderHas110Checkpoint() {
        ScOrder order = testFactory.createOrder(sortingCenter).cancel().get();

        PlaceScRequest request = testFactory.placeScRequest(order, user);
        acceptService.acceptPlace(request);

        transactionTemplate.execute(ts -> {
            var actualOrder = scOrderRepository.findByIdOrThrow(order.getId());
            assertThat(
                    actualOrder.getFfStatusHistory().stream()
                            .anyMatch(s -> s.getFfStatus() == ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE)
            ).isTrue();
            assertThat(actualOrder.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            assertThat(actualOrder.getState()).isEqualTo(ScOrderState.ACCEPTED);
            checkLastEventOfStatus(actualOrder, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            return null;
        });
    }

    @Test
    void acceptOrdersWithoutUpdate() {
        ScOrder order = testFactory.createOrder(sortingCenter).get();

        PlaceScRequest request = testFactory.placeScRequest(order, user);
        acceptService.acceptPlace(request);

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(order.getState()).isEqualTo(ScOrderState.ACCEPTED);
        checkLastEventOfStatus(order, ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void acceptOrdersTwice() {
        ScOrder order = testFactory.createOrderForToday(sortingCenter).accept().get();

        PlaceScRequest request = testFactory.placeScRequest(order, user);
        acceptService.acceptPlace(request);

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void acceptOrdersAfterCancel() {
        ScOrder order = testFactory.createOrder(sortingCenter).cancel().get();

        PlaceScRequest request = testFactory.placeScRequest(order, user);
        acceptService.acceptPlace(request);

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(order.getState()).isEqualTo(ScOrderState.ACCEPTED);
        checkLastEventOfStatus(order, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void acceptOrdersAfterReturn() {
        ScOrder order = testFactory.createOrderForToday(sortingCenter).accept()
                .makeReturn().get();

        PlaceScRequest request = testFactory.placeScRequest(order, user);
        acceptService.acceptPlace(request);

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void assertClarificationOrKeepedDirectWhenNeponyatno() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .ship()
                .accept()
                .getPlace();

        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.ARRIVED_DIRECT).getId());
    }

    @Test
    void acceptReturnedOrdersTwice() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().accept().makeReturn().getPlace();

        PlaceScRequest request = testFactory.placeScRequest(place, user);
        acceptService.acceptPlace(request);

        place = testFactory.getPlace(place.getId());
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);

        request = testFactory.placeScRequest(place, user);
        acceptService.acceptPlace(request);

        place = testFactory.getPlace(place.getId());
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void acceptPlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .get();
        transactionTemplate.execute(ts -> {
            var places = testFactory.orderPlaces(order);
            acceptService.acceptPlace(new PlaceScRequest(new PlaceId(
                    places.get(0).getOrderId(),
                    places.get(0).getMainPartnerCode()
            ), user));
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.CREATED);
            assertThat(scOrderRepository.findByIdOrThrow(order.getId()).getOrderStatus())
                    .isEqualTo(ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
            return null;
        });
    }

    @Test
    void doNotAcceptPlaceIfAlreadySorted() {
        var placeExternalIds = List.of("1", "2");
        var order = testFactory.createForToday(order(sortingCenter).places(placeExternalIds).build())
                .acceptPlaces(placeExternalIds)
                .sortPlaces(placeExternalIds)
                .get();
        transactionTemplate.execute(ts -> {
            var firstPlace = testFactory.orderPlaces(order).get(0);
            acceptService.acceptPlace(new PlaceScRequest(new PlaceId(
                    firstPlace.getOrderId(),
                    firstPlace.getMainPartnerCode()
            ), user));
            assertThat(firstPlace.getStatus()).isEqualTo(PlaceStatus.SORTED);
            assertThat(scOrderRepository.findByIdOrThrow(order.getId()).getOrderStatus())
                    .isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
            return null;
        });
    }

    @Test
    void acceptMiddleMilePlaceIfAlreadyShippedMultiPlaceOrder() {
        var placeExternalIds = List.of("1", "2");
        var order = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .places(placeExternalIds)
                                .build()
                )
                .acceptPlaces(placeExternalIds).sortPlaces(placeExternalIds).ship()
                .get();
        transactionTemplate.execute(ts -> {
            var firstPlace = testFactory.orderPlaces(order).get(0);
            acceptService.acceptPlace(new PlaceScRequest(new PlaceId(
                    firstPlace.getOrderId(),
                    firstPlace.getMainPartnerCode()
            ), user));
            assertThat(firstPlace.getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(firstPlace.getCell()).isNull();
            assertThat(firstPlace.getLot()).isNull();
            assertThat(scOrderRepository.findByIdOrThrow(order.getId()).getOrderStatus())
                    .isEqualTo(ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
            return null;
        });
    }

    @Test
    void acceptMiddleMileOrderIfAlreadyShipped() {
        var order = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build()
                )
                .accept().sort().ship()
                .get();
        PlaceScRequest request = testFactory.placeScRequest(order, user);
        acceptService.acceptPlace(request);
        var acceptedOrder = testFactory.getOrder(order.getId());
        assertThat(acceptedOrder.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void acceptLastPlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .get();
        transactionTemplate.execute(ts -> {
            var places = testFactory.orderPlaces(order);
            Place firstPlace = places.get(0);
            Place secondPlace = places.get(1);
            acceptService.acceptPlace(new PlaceScRequest(new PlaceId(
                    firstPlace.getOrderId(),
                    firstPlace.getMainPartnerCode()
            ), user));
            acceptService.acceptPlace(new PlaceScRequest(new PlaceId(
                    secondPlace.getOrderId(),
                    secondPlace.getMainPartnerCode()
            ), user));
            assertThat(firstPlace.getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(secondPlace.getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(scOrderRepository.findByIdOrThrow(order.getId()).getOrderStatus())
                    .isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
            return null;
        });
    }

    @Test
    void acceptPlaceTwice() {
        Map<String, Place> places = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlace("1")
                .getPlaces();
        acceptService.acceptPlace(new PlaceScRequest(new PlaceId(
                places.get("1").getOrderId(),
                places.get("1").getMainPartnerCode()
        ), user));
        assertThat(places.get("1").getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
    }

    @Test
    void acceptPlaceStoresUserInPlaceHistory() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }
        Place place1 = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .getPlace("1");
        acceptService.acceptPlace(new PlaceScRequest(new PlaceId(
                place1.getOrderId(),
                place1.getMainPartnerCode()
        ), user));
        transactionTemplate.execute(ts -> {
            var place = placeRepository.findByIdOrThrow(place1.getId());
            assertThat(
                    place.getHistory().stream()
                            .filter(h -> h.getMutableState().getPlaceStatus() == PlaceStatus.ACCEPTED)
                            .toList()
            ).isNotEmpty().allMatch(h -> Objects.equals(h.getUser(), user));
            return null;
        });
    }

    private void checkLastEventOfStatus(ScOrder order, ScOrderFFStatus ffStatus) {
        transactionTemplate.execute(ts -> {
            var actualOrder = scOrderRepository.findByIdOrThrow(order.getId());
            var historyItemsWithStatus = actualOrder.getFfStatusHistory().stream()
                    .filter(i -> i.getFfStatus() == ffStatus)
                    .toList();
            var lastHistoryItemWithStatus = historyItemsWithStatus.get(historyItemsWithStatus.size() - 1);
            assertThat(lastHistoryItemWithStatus.getCourier()).isEqualTo(actualOrder.getCourier());
            return null;
        });
    }

    @Test
    @DisplayName("принимаем шипнутые коробки на последней миле")
    void acceptShippedMultiplaceOnLastMile() {
        // шипаем двухместный заказ на последней миле
        ScOrder order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .accept().sort().ship().get();

        // принимаем первую коробку на СЦ (курьер вернул)
        acceptService.acceptPlace(new PlaceScRequest(new PlaceId(order.getId(), "1"), user));
        order = scOrderRepository.findByIdOrThrow(order.getId());
        List<Place> places = testFactory.orderPlaces(order);

        assertThat(order.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(order.getOutgoingRouteDate()).isNull();
        assertThat(testFactory.placeById(places, "1").getSortableStatus())
                .isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(testFactory.placeById(places, "1").getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.ARRIVED_DIRECT).getId());
        assertThat(testFactory.placeById(places, "1").getOutgoingRouteDate()).isNull();

        LocalDate today = LocalDate.now(clock);

        // принимаем вторую коробку на СЦ (курьер вернул)
        acceptService.acceptPlace(new PlaceScRequest(new PlaceId(order.getId(), "2"), user));
        order = scOrderRepository.findByIdOrThrow(order.getId());
        places = testFactory.orderPlaces(order);

        assertThat(order.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(order.getOutgoingRouteDate()).isNull();
        assertThat(testFactory.placeById(places, "2").getSortableStatus())
                .isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(testFactory.placeById(places, "2").getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.ARRIVED_DIRECT).getId());
        assertThat(testFactory.placeById(places, "2").getOutgoingRouteDate()).isNull();

    }

    @Test
    @DisplayName("принимаем шипнутые коробки на последней миле: приняли первую, пришел апдейт, приняли вторую")
    void acceptShippedMultiplaceOnLastMile_updateShipmentDateBetweenPlaceAcceptance() {
        // шипаем двухместный заказ на последней миле
        ScOrder order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .accept().sort().ship().get();

        // принимаем первую коробку на СЦ (курьер вернул)
        acceptService.acceptPlace(new PlaceScRequest(new PlaceId(order.getId(), "1"), user));
        order = scOrderRepository.findByIdOrThrow(order.getId());
        List<Place> places = testFactory.orderPlaces(order);

        assertThat(order.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(order.getOutgoingRouteDate()).isNull();

        assertThat(testFactory.placeById(places, "1").getSortableStatus())
                .isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(testFactory.placeById(places, "1").getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.ARRIVED_DIRECT).getId());
        assertThat(testFactory.placeById(places, "1").getOutgoingRouteDate()).isNull();

        LocalDate today = LocalDate.now(clock);




        // принимаем нового курьера и дату от курьерки
        testFactory.updateCourier(order.getId(),  new CourierDto(321L, "Другой курьер", null, null,
                null, null, null, false), sortingCenter );
        testFactory.updateShipmentDate(order.getId(), today, sortingCenter);

        order = scOrderRepository.findByIdOrThrow(order.getId());
        places = testFactory.orderPlaces(order);

        assertThat(order.getOutgoingRouteDate()).isEqualTo(today);
        assertThat(testFactory.placeById(places, "1").getOutgoingRouteDate()).isEqualTo(today);
        assertThat(testFactory.placeById(places, "2").getOutgoingRouteDate()).isEqualTo(today);

        // принимаем вторую коробку на СЦ (курьер вернул)
        acceptService.acceptPlace(new PlaceScRequest(new PlaceId(order.getId(), "2"), user));
        order = scOrderRepository.findByIdOrThrow(order.getId());
        places = testFactory.orderPlaces(order);

        assertThat(order.getOutgoingRouteDate()).isEqualTo(today);
        assertThat(testFactory.placeById(places, "1").getOutgoingRouteDate()).isEqualTo(today);
        assertThat(testFactory.placeById(places, "2").getOutgoingRouteDate()).isEqualTo(today);
        assertThat(testFactory.placeById(places, "2").getSortableStatus())
                .isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(testFactory.placeById(places, "2").getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.ARRIVED_DIRECT).getId());
    }

}
