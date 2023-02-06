package ru.yandex.market.sc.core.domain.place;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.PreShipService;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceId;
import ru.yandex.market.sc.core.domain.place.model.PlaceScRequest;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.model.PlacesReplaceRequest;
import ru.yandex.market.sc.core.domain.place.model.PlacesScRequest;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertyRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.StageLoader;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.exception.ScInvalidTransitionException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension.testNotMigrated;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.useNewSortableFlow;

/**
 * @author valter
 */
@EmbeddedDbTest
class PlaceCommandServiceTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    PlaceCommandService placeCommandService;
    @Autowired
    PreShipService preShipService;
    @Autowired
    AcceptService acceptService;
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    SortableLotService sortableLotService;
    @Autowired
    RouteSoRepository routeSoRepository;
    @Autowired
    SortingCenterPropertyRepository sortingCenterPropertyRepository;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        user = testFactory.storedUser(sortingCenter, 1L);
    }



    @Test
    void sortedPlaceIsIdempotentToAccept() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .cancel().acceptPlaces("1").sortPlaces("1").acceptPlaces("1").get();
        var place = testFactory.orderPlaces(order).stream()
                .filter(p -> Objects.equals(p.getMainPartnerCode(), "1"))
                .findFirst().orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.SORTED_RETURN).getId());
    }

    @Test
    void createPlaces() {
        List<String> placeExternalIds = List.of("1", "2");
        Map<String, Place> places = testFactory.createForToday(order(sortingCenter).places(placeExternalIds).build())
                .getPlaces();
        assertThat(places.keySet().stream().sorted())
                .isEqualTo(placeExternalIds);
    }

    @Test
    void createPlacesWithSortingCenter() {
        List<String> placeExternalIds = List.of("1", "2");
        Map<String, Place> places = testFactory.createForToday(order(sortingCenter).places(placeExternalIds).build())
                .getPlaces();
        assertThat(places.get("1").getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(places.get("2").getSortingCenter()).isEqualTo(sortingCenter);
    }

    @Test
    void createPlacesWithNoPartnerCodesUsesPartnerIdAsMainPartnerCode() {
        var orderParams = order(sortingCenter).externalId("1").createTwoPlaces(true).build();
        var order = testFactory.create(orderParams).get();
        String partnerId1 = "partnerId1";
        String partnerId2 = "partnerId2";
        Order ffOrder = testFactory.new TestOrderBuilder()
                .orderRequestBuilder(orderParams)
                .setPlaces(
                        List.of(
                                new ru.yandex.market.logistic.api.model.fulfillment.Place(
                                        new ResourceId("yandexId1", partnerId1),
                                        orderParams.getRequest().getKorobyte(),
                                        Collections.emptyList(),
                                        List.of()
                                ),
                                new ru.yandex.market.logistic.api.model.fulfillment.Place(
                                        new ResourceId("yandexId2", partnerId2),
                                        orderParams.getRequest().getKorobyte(),
                                        Collections.emptyList(),
                                        List.of()
                                )
                        )
                )
                .build();
        placeCommandService.replaceOrderPlaces(new PlacesReplaceRequest(order, ffOrder), user);
        var orderPlaces = testFactory.orderPlaces(order.getId());
        orderPlaces.sort(Comparator.comparing(Place::getMainPartnerCode));
        assertThat(orderPlaces.size()).isEqualTo(2);
        assertThat(orderPlaces.get(0).getMainPartnerCode()).isEqualTo(partnerId1);
        assertThat(orderPlaces.get(1).getMainPartnerCode()).isEqualTo(partnerId2);
    }

    @Test
    void createPlacesWithSamePartnerCode() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "1").build())
                .get();
        assertThat(order.getPlaceCount()).isEqualTo(1);
    }

    @Test
    void replacePlacesSamePlaces() {
        var orderParamsBuilder = order(sortingCenter).externalId("1").places("1", "2");
        var order = testFactory.create(orderParamsBuilder.build()).get();

        Order ffOrder = testFactory.new TestOrderBuilder().orderRequest(orderParamsBuilder.build());
        placeCommandService.replaceOrderPlaces(new PlacesReplaceRequest(order, ffOrder), user);
        assertThat(placeRepository.findAllByOrderIdOrderById(order.getId()).stream().map(Place::getMainPartnerCode))
                .isEqualTo(List.of("1", "2"));
    }


    @Test
    void replacePlacesNoPlacesWithCreateSinglePlaceOrderProperty() {
        var orderParamsBuilder = order(sortingCenter).externalId("1").places("1", "2");
        var order = testFactory.create(orderParamsBuilder.build()).get();
        Order ffOrder = testFactory.new TestOrderBuilder().orderRequest(orderParamsBuilder.places().build());
        placeCommandService.replaceOrderPlaces(new PlacesReplaceRequest(order, ffOrder), user);
        final List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places).hasSize(1);
        final Place place = places.get(0);
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.CREATED);
        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.AWAITING_DIRECT).getId());
        assertThat(place.getMainPartnerCode()).isEqualTo(ffOrder.getOrderId().getYandexId());
        assertThat(place.getCell()).isNull();
        assertThat(place.getLot()).isNull();
    }

    @Test
    @DisplayName("Удаление фейковых (FAKEPARCEL) посылок при частично принятом заказе")
    void replacePlacesOnlyDeleteFakePlacesIfAnyPlaceAccepted() {
        var orderParamsBuilder = order(sortingCenter).externalId("o1")
                .places("1", "2", "FAKEPARCEL-0000000001-00001-3");
        var order = testFactory.create(orderParamsBuilder.build()).acceptPlaces("1").get();
        Order ffOrder = testFactory.new TestOrderBuilder()
                .orderRequest(orderParamsBuilder.places("1").build());

        placeCommandService.replaceOrderPlaces(new PlacesReplaceRequest(order, ffOrder), user);
        // Заказ частично пришел на СЦ, поэтому мы не можем удалять никакие плейсы, кроме FAKEPARCEL,
        assertThat(placeRepository.findAllByOrderIdOrderById(order.getId()).stream().map(Place::getMainPartnerCode))
                .isEqualTo(List.of("1", "2"));
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("Изменение статуса заказа после удаления фейковых посылок")
    void replacePlacesOnlyDeleteFakePlacesAndChangeOrderStatusIfAllPlacesAccepted() {
        var orderParamsBuilder = order(sortingCenter).externalId("o1")
                .places("1", "FAKEPARCEL-0000000001-00001-2");
        var order = testFactory.create(orderParamsBuilder.build()).acceptPlaces("1").get();
        Order ffOrder = testFactory.new TestOrderBuilder()
                .orderRequest(orderParamsBuilder.places("1").build());

        placeCommandService.replaceOrderPlaces(new PlacesReplaceRequest(order, ffOrder), user);
        assertThat(placeRepository.findAllByOrderIdOrderById(order.getId()).stream().map(Place::getMainPartnerCode))
                .isEqualTo(List.of("1"));
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);

    }

    @Test
    void replacePlacesDifferentPlaces() {
        var orderParamsBuilder = order(sortingCenter).externalId("1").places("1", "2");
        var order = testFactory.create(orderParamsBuilder.build()).get();

        Order ffOrder = testFactory.new TestOrderBuilder()
                .orderRequest(orderParamsBuilder.places("3", "4").build());
        placeCommandService.replaceOrderPlaces(new PlacesReplaceRequest(order, ffOrder), user);
        assertThat(placeRepository.findAllByOrderIdOrderById(order.getId()).stream().map(Place::getMainPartnerCode))
                .isEqualTo(List.of("3", "4"));
    }

    @Test
    void replacePlacesMixedPlaces() {
        var orderParamsBuilder = order(sortingCenter).externalId("1").places("1", "2");
        var order = testFactory.create(orderParamsBuilder.build()).get();

        Order ffOrder = testFactory.new TestOrderBuilder()
                .orderRequest(orderParamsBuilder.places("1", "3").build());
        placeCommandService.replaceOrderPlaces(new PlacesReplaceRequest(order, ffOrder), user);
        assertThat(placeRepository.findAllByOrderIdOrderById(order.getId()).stream().map(Place::getMainPartnerCode))
                .isEqualTo(List.of("1", "3"));
    }

    @Test
    void sortPlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2")
                .get();
        transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            Route route = testFactory.findOutgoingCourierRoute(
                            testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
            long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order)).getId();
            placeCommandService.sortPlace(new PlaceScRequest(new PlaceId(
                    places.get(0).getOrderId(),
                    places.get(0).getMainPartnerCode()
            ), user), cellId, false);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SORTED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(places.get(0).getMutableState().getStageId())
                    .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.SORTED_DIRECT).getId());
            assertThat(places.get(1).getMutableState().getStageId())
                    .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.ARRIVED_DIRECT).getId());
            return null;
        });
    }

    @Test
    void preparePlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2")
                .get();
        transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            Route route = testFactory.findOutgoingCourierRoute(
                            testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
            long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order)).getId();

            Long routeId;
            if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
                routeId = testFactory.getRouteSo(route).getId();
            } else {
                routeId = testFactory.getRouteIdForSortableFlow(route);
            }

            preShipService.prepareToShipPlace(
                    new PlaceScRequest(PlaceId.of(places.get(0)), user),
                    routeId,
                    cellId);

            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.PREPARED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.SORTED);
            assertThat(places.get(0).getMutableState().getStageId())
                    .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.PREPARED_DIRECT).getId());
            assertThat(places.get(1).getMutableState().getStageId())
                    .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.SORTED_DIRECT).getId());
            return null;
        });
    }

    @Test
    void cantPrepareNotSortedPlace() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER);
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("2")
                .get();
        var route = testFactory.findOutgoingCourierRoute(
                testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
        var place1 = transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            return places.stream()
                    .filter(place -> Objects.equals("1", place.getMainPartnerCode()))
                    .findAny().orElseThrow();
        });
        assertThatThrownBy(
                () -> preShipService.prepareToShipPlace(
                        new PlaceScRequest(
                                PlaceId.of(place1),
                                user),
                        testFactory.getRouteIdForSortableFlow(route),
                        cell.getId())
        ).isInstanceOf(ScInvalidTransitionException.class);
    }

    @Test
    void cantPrepareToShipPartiallySortedMultiplaceOrder() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1").sortPlaces("1").get();
        var route = testFactory.findOutgoingCourierRoute(
                testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
        var cell = testFactory.determineRouteCell(
                Objects.requireNonNull(testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow()), order);

        Long routeId;
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            routeId = testFactory.getRouteSo(route).getId();
        } else {
            routeId = testFactory.getRouteIdForSortableFlow(route);
        }

        assertThatThrownBy(
                () -> preShipService.prepareToShipPlace(
                        new PlaceScRequest(new PlaceId(order.getId(), "1"), user),
                        routeId,
                        cell.getId())
        )
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.PLACE_NOT_PREPARED_NOT_ALL_PLACES_SORTED.name());
    }

    @Test
    void cantSortPlaceToSameCell() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2")
                .get();
        Route route = testFactory.findOutgoingCourierRoute(
                testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order))
                .getId();
        List<Place> orderPlaces = transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            placeCommandService.sortPlace(new PlaceScRequest(new PlaceId(
                    places.get(0).getOrderId(),
                    places.get(0).getMainPartnerCode()
            ), user), cellId, false);
            return places;
        });
        assertThatThrownBy(() -> placeCommandService.sortPlace(new PlaceScRequest(new PlaceId(
                Objects.requireNonNull(orderPlaces).get(0).getOrderId(),
                orderPlaces.get(0).getMainPartnerCode()
        ), user), cellId, false)).isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void sortPlaceStoresUserInPlaceHistory() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlace("1")
                .get();
        transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            var route = testFactory.findOutgoingCourierRoute(
                    testFactory.getOrderLikeForRouteLookup(order))
                        .orElseThrow();
            long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order)).getId();
            placeCommandService.sortPlace(new PlaceScRequest(new PlaceId(
                    places.get(0).getOrderId(),
                    places.get(0).getMainPartnerCode()
            ), user), cellId, false);
            assertThat(
                    places.get(0).getHistory().stream()
                            .filter(h -> h.getMutableState().getPlaceStatus() == PlaceStatus.SORTED)
                            .toList()
            ).isNotEmpty().allMatch(h -> Objects.equals(h.getUser(), user));
            return null;
        });
    }

    @Test
    void sortPlaceBadStatus() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build()).get();
        List<Place> places = testFactory.orderPlaces(order);
        var route = testFactory.findOutgoingCourierRoute(
                testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order)).getId();
        assertThatThrownBy(
                () -> placeCommandService.sortPlace(
                        new PlaceScRequest(new PlaceId(
                                places.get(0).getOrderId(),
                                places.get(0).getMainPartnerCode()
                        ), user),
                        cellId, false)
        ).isInstanceOf(ScException.class);
    }

    @Test
    void shipPlaces() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlace("1").sortPlace("1", user)
                .acceptPlace("2").sortPlace("2", user)
                .get();
        transactionTemplate.execute(ts -> {
            var places = testFactory.orderPlaces(order);
            placeCommandService.shipPlaces(new PlacesScRequest(
                    places.stream()
                            .map(place -> new PlaceId(place.getOrderId(), place.getMainPartnerCode()))
                            .toList(),
                    user
            ));
            assertThat(places).isNotEmpty().allMatch(place -> place.getStatus() == PlaceStatus.SHIPPED);
            assertThat(places).isNotEmpty().allMatch(place -> Objects.equals(place.getMutableState().getStageId(),
                    StageLoader.getBySortableStatus(SortableStatus.SHIPPED_DIRECT).getId()));
            return null;
        });
    }

    @Test
    void shipPreparedPlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlace("1").sortPlace("1", user)
                .acceptPlace("2").sortPlace("2", user)
                .preparePlace("1").preparePlace("2")
                .get();
        transactionTemplate.execute(ts -> {
            var places = testFactory.orderPlaces(order);
            placeCommandService.shipPlaces(new PlacesScRequest(
                    places.stream()
                            .map(place -> new PlaceId(place.getOrderId(), place.getMainPartnerCode()))
                            .toList(),
                    user
            ));
            assertThat(places).isNotEmpty().allMatch(place -> place.getStatus() == PlaceStatus.SHIPPED);
            return null;
        });
    }

    @Test
    void shipPlacesBadStatus() {
        Map<String, Place> places = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlace("1").sortPlace("1", user)
                .acceptPlace("2")
                .getPlaces();
        assertThatThrownBy(() -> placeCommandService.shipPlaces(new PlacesScRequest(
                places.values().stream()
                        .map(place -> new PlaceId(place.getOrderId(), place.getMainPartnerCode()))
                        .toList(),
                user
        ))).isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void shipPlacesStoresUserInPlaceHistory() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlace("1").sortPlace("1")
                .acceptPlace("2").sortPlace("2")
                .get();
        transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            placeCommandService.shipPlaces(new PlacesScRequest(
                    places.stream()
                            .map(place -> new PlaceId(place.getOrderId(), place.getMainPartnerCode()))
                            .toList(),
                    user
            ));
            assertThat(
                    places.stream().flatMap(p -> p.getHistory().stream())
                            .filter(h -> h.getMutableState().getPlaceStatus() == PlaceStatus.SHIPPED)
                            .toList()
            ).isNotEmpty().allMatch(h -> Objects.equals(h.getUser(), user));
            return null;
        });
    }

    @Test
    void returnPlaces() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .cancel()
                .acceptPlace("1").sortPlace("1", user)
                .acceptPlace("2").sortPlace("2", user)
                .get();
        transactionTemplate.execute(ts -> {
            var places = testFactory.orderPlaces(order);
            placeCommandService.returnPlaces(places, user);
            assertThat(places).isNotEmpty().allMatch(place -> place.getStatus() == PlaceStatus.RETURNED);
            return null;
        });
    }

    @Test
    void returnPlacesBadStatus() {
        List<Place> places = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .cancel()
                .acceptPlace("1").sortPlace("1", user)
                .acceptPlace("2")
                .getPlacesList();
        assertThatThrownBy(() -> placeCommandService.returnPlaces(places, user))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void returnPlacesStoresUserInPlaceHistory() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .cancel()
                .acceptPlace("1").sortPlace("1")
                .acceptPlace("2").sortPlace("2")
                .get();
        transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            placeCommandService.returnPlaces(places, user);
            assertThat(
                    places.stream().flatMap(p -> p.getHistory().stream())
                            .filter(h -> h.getMutableState().getPlaceStatus() == PlaceStatus.RETURNED)
                            .toList()
            ).isNotEmpty().allMatch(h -> Objects.equals(h.getUser(), user));
            return null;
        });
    }

    @Test
    void acceptAndSortPlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build()).get();
        transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            var route = testFactory.findOutgoingCourierRoute(
                            testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
            long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order)).getId();
            PlaceScRequest request = new PlaceScRequest(
                    new PlaceId(places.get(0).getOrderId(), places.get(0).getMainPartnerCode()), user);
            acceptService.acceptPlace(request);
            placeCommandService.sortPlace(request, cellId, false);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SORTED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.CREATED);
            assertThat(places.get(0).getMutableState().getStageId())
                    .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.SORTED_DIRECT).getId());
            assertThat(places.get(1).getMutableState().getStageId())
                    .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.AWAITING_DIRECT).getId());
            return null;
        });
    }

    @Test
    void acceptAndSortPlaceAccepted() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlace("1")
                .get();
        transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            var route = testFactory.findOutgoingCourierRoute(
                            testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
            long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order)).getId();
            PlaceScRequest request = new PlaceScRequest(
                    new PlaceId(places.get(0).getOrderId(), places.get(0).getMainPartnerCode()), user);
            acceptService.acceptPlace(request);
            placeCommandService.sortPlace(request, cellId, false);

            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SORTED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.CREATED);
            assertThat(places.get(0).getMutableState().getStageId())
                    .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.SORTED_DIRECT).getId());
            assertThat(places.get(1).getMutableState().getStageId())
                    .isEqualTo(StageLoader.getBySortableStatus(SortableStatus.AWAITING_DIRECT).getId());
            return null;
        });
    }

    @Test
    void cantSortSingleCourierPlaceToReturn() {
        var cell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlace("p1")
                .get();
        assertThatThrownBy(() -> placeCommandService.sortPlace(
                new PlaceScRequest(new PlaceId(order.getId(), "p1"), user),
                cell.getId(),
                false
        )).isInstanceOf(ScException.class);
    }

    @Test
    void cantKeepSingleCourierPlace() {
        var cell = testFactory.storedCell(sortingCenter, "b1", CellType.BUFFER);
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlace("p1").get();
        assertThatThrownBy(() -> placeCommandService.sortPlace(
                new PlaceScRequest(new PlaceId(order.getId(), "p1"), user),
                cell.getId(),
                false
        )).isInstanceOf(ScException.class);
    }

    @Test
    void sortMultiplaceOrderToLot() {
        var returnCell = testFactory.storedCell(sortingCenter, "return1", CellType.RETURN);
        var pallet = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);
        var order = testFactory.createForToday(
                        TestFactory.CreateOrderParams.builder()
                                .places("p1", "p2")
                                .sortingCenter(sortingCenter)
                                .build())
                .cancel()
                .acceptPlaces("p1", "p2")
                .sortPlace("p1", returnCell.getId())
                .sortPlace("p2", returnCell.getId())
                .get();
        List<Place> places = testFactory.orderPlaces(order);
        var sortedPlace0 = testFactory.sortPlaceToLot(places.get(0), pallet, user);
        assertThat(sortedPlace0.getCell()).isNull();
        assertThat(sortedPlace0.getLot().getId()).isEqualTo(pallet.getLotId());
        transactionTemplate.execute(ts -> {
            var lot = sortableLotService.findByLotIdOrThrow(pallet.getLotId());
            assertThat(lot.getLotStatusOrNull()).isEqualTo(LotStatus.PROCESSING);
            return null;
        });
    }

    @Test
    void dontSortPlaceToUnsuitableLot() {
        var clientReturnCell = testFactory.storedCell(
                sortingCenter, "clientReturn1", CellType.RETURN, CellSubType.CLIENT_RETURN);
        var clientReturnPallet = testFactory.storedLot(sortingCenter, SortableType.PALLET, clientReturnCell);
        var returnCell = testFactory.storedCell(sortingCenter, "return1", CellType.RETURN);
        var order = testFactory.createForToday(
                        TestFactory.CreateOrderParams.builder()
                                .places("p1", "p2")
                                .sortingCenter(sortingCenter)
                                .build())
                .cancel()
                .acceptPlaces("p1", "p2")
                .sortPlace("p1", returnCell.getId())
                .sortPlace("p2", returnCell.getId())
                .get();
        List<Place> places = testFactory.orderPlaces(order);
        assertThatThrownBy(() -> testFactory.sortPlaceToLot(places.get(0), clientReturnPallet, user))
                .isInstanceOf(ScException.class);
    }

    @Test
    void createPlacesAndCheckIsForShooting() {
        var order = testFactory.createForToday(
                TestFactory.CreateOrderParams.builder()
                        .places("p1")
                        .sortingCenter(sortingCenter)
                        .externalId("1")
                        .recipientEmail("checkouter-shooting@yandex-team.ru")
                        .build())
                .acceptPlace("p1")
                .get();

        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places.get(0).getIsForShooting()).isTrue();

        var order2 = testFactory.createForToday(
                        TestFactory.CreateOrderParams.builder()
                                .places("p2")
                                .sortingCenter(sortingCenter)
                                .externalId("2")
                                .recipientEmail(null)
                                .build())
                .acceptPlace("p2")
                .get();

        List<Place> places2 = testFactory.orderPlaces(order2);
        assertThat(places2.get(0).getIsForShooting()).isFalse();

        var order3 = testFactory.createForToday(
                        TestFactory.CreateOrderParams.builder()
                                .places("p3")
                                .sortingCenter(sortingCenter)
                                .externalId("3")
                                .recipientEmail("test@test.ru")
                                .build())
                .acceptPlaces("p3")
                .get();

        List<Place> places3 = testFactory.orderPlaces(order3);
        assertThat(places3.get(0).getIsForShooting()).isFalse();
    }

    @Test
    void setLotForPlaces() {
        var order = testFactory.createForToday(
                        TestFactory.CreateOrderParams.builder()
                                .places("p1", "p2")
                                .sortingCenter(sortingCenter)
                                .externalId("1")
                                .recipientEmail("checkouter-shooting@yandex-team.ru")
                                .build())
                .get();

        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET,
                testFactory.storedCell(sortingCenter, "cell-1", CellType.COURIER));

        placeCommandService.setLotForPlace("1", "p1", lot, user);
        placeCommandService.setLotForPlace("1", "p2", lot, user);
        var places = placeRepository.findAllByOrderIdOrderById(order.getId());

        assertThat(places).allMatch(place -> Objects.equals(place.getLot(), lot.getLot()));
    }


}
