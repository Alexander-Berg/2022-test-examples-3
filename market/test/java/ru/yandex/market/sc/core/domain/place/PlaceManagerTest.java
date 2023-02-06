package ru.yandex.market.sc.core.domain.place;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.FFApiOrderUpdateRequest;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceId;
import ru.yandex.market.sc.core.domain.place.model.PlaceScRequest;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.model.PlacesScRequest;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.ffOrder;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
class PlaceManagerTest {

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    OrderCommandService orderCommandService;

    @Autowired
    RouteCommandService routeCommandService;

    @Autowired
    PlaceCommandService placeCommandService;

    @Autowired
    AcceptService acceptService;

    @Autowired
    ScOrderRepository orderRepository;

    @Autowired
    TestFactory testFactory;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 1L);
    }

    @Test
    void createNewPlaceAfterOrderAccept() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_1.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-asfjehwfhjsdhfjkeh",
                        "FAKEPARCEL-23454341-qweqwkjniuh2e294uhi"
                );
        System.out.println("-------------------------------------");
        acceptService.acceptPlace(new PlaceScRequest(
                new PlaceId(order.getId(), "000000-place-1"),
                user
        ));
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_2.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-21312klsdf"
                );
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_no_fake.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("000000-place-1", "000000-place-2");
    }

    @Test
    void createNewPlaceAfterOrderShip() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).get();
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_1.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-asfjehwfhjsdhfjkeh",
                        "FAKEPARCEL-23454341-qweqwkjniuh2e294uhi"
                );
        acceptService.acceptPlace(new PlaceScRequest(
                new PlaceId(order.getId(), "000000-place-1"),
                user
        ));
        Route route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        route.allowReading();
        placeCommandService.sortPlace(
                new PlaceScRequest(
                        new PlaceId(order.getId(), "000000-place-1"),
                        user
                ),
                route.getRouteCells().stream()
                        .findFirst().orElseThrow().getCellId(),
                false
        );
        placeCommandService.shipPlaces(new PlacesScRequest(
                List.of(new PlaceId(order.getId(), "000000-place-1")), user
        ));
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_2.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-21312klsdf"
                );
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_no_fake.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("000000-place-1", "000000-place-2");
    }

    @Test
    void createNewPlaceAfterOrderMiddleMileShipToCourierThenCancelled() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).get();
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_1.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-asfjehwfhjsdhfjkeh",
                        "FAKEPARCEL-23454341-qweqwkjniuh2e294uhi"
                );
        acceptService.acceptPlace(new PlaceScRequest(
                new PlaceId(order.getId(), "000000-place-1"),
                user
        ));
        Route route = testFactory.findOutgoingCourierRoute(
                        testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow();
        route.allowNextRead();
        placeCommandService.sortPlace(
                new PlaceScRequest(
                        new PlaceId(order.getId(), "000000-place-1"),
                        user
                ),
                route.getRouteCells().stream()
                        .findFirst().orElseThrow().getCellId(),
                false
        );
        placeCommandService.shipPlaces(new PlacesScRequest(
                List.of(new PlaceId(order.getId(), "000000-place-1")), user
        ));
        orderCommandService.cancelOrder(order.getId(), null, false, user);
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_2.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-21312klsdf"
                );
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_no_fake.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("000000-place-1", "000000-place-2");
    }

    @Test
    void createNewPlaceAfterOrderLastMileCancelThenShippedToWarehouse() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.LAST_MILE_COURIER).build()
        ).get();
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_1.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-asfjehwfhjsdhfjkeh",
                        "FAKEPARCEL-23454341-qweqwkjniuh2e294uhi"
                );
        acceptService.acceptPlace(new PlaceScRequest(
                new PlaceId(order.getId(), "000000-place-1"),
                user
        ));
        orderCommandService.cancelOrder(order.getId(), null, false, user);
        Route route = testFactory.findOutgoingWarehouseRoute(
                        testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow();
        route.allowNextRead();
        placeCommandService.sortPlace(
                new PlaceScRequest(
                        new PlaceId(order.getId(), "000000-place-1"),
                        user
                ),
                route
                        .getRouteCells()
                        .stream()
                            .findFirst()
                            .orElseThrow()
                            .getCellId(),
                false
        );
        placeCommandService.shipPlaces(new PlacesScRequest(
                List.of(new PlaceId(order.getId(), "000000-place-1")), user
        ));
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_2.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-21312klsdf"
                );
    }

    @Test
    void createNewPlaceAfterOrderLastMileCancelThenSortToWarehouse() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.LAST_MILE_COURIER).build()
        ).get();
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_1.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-asfjehwfhjsdhfjkeh",
                        "FAKEPARCEL-23454341-qweqwkjniuh2e294uhi"
                );
        acceptService.acceptPlace(new PlaceScRequest(
                new PlaceId(order.getId(), "000000-place-1"),
                user
        ));
        orderCommandService.cancelOrder(order.getId(), null, false, user);
        Route route = testFactory.findOutgoingWarehouseRoute(
                        testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow();
        route.allowNextRead();
        placeCommandService.sortPlace(
                new PlaceScRequest(
                        new PlaceId(order.getId(), "000000-place-1"),
                        user
                ),
                route
                            .getRouteCells()
                            .stream()
                                .findFirst()
                                .orElseThrow()
                                .getCellId(),
                false
        );
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_2.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-21312klsdf"
                );
    }

    @Test
    void createNewPlaceAfterOrderMiddleMileCancelledThenShippedToWarehouseReturn() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).get();
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_1.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-asfjehwfhjsdhfjkeh",
                        "FAKEPARCEL-23454341-qweqwkjniuh2e294uhi"
                );
        acceptService.acceptPlace(new PlaceScRequest(
                new PlaceId(order.getId(), "000000-place-1"),
                user
        ));
        orderCommandService.cancelOrder(order.getId(), null, false, user);
        Route route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                                    .orElseThrow();
        route.allowNextRead();
        placeCommandService.sortPlace(
                new PlaceScRequest(
                        new PlaceId(order.getId(), "000000-place-1"),
                        user
                ),
                route.getRouteCells()
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .getCellId(),
                false
        );
        placeCommandService.shipPlaces(new PlacesScRequest(
                List.of(new PlaceId(order.getId(), "000000-place-1")), user
        ));
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_fake_2.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder(
                        "000000-place-1",
                        "FAKEPARCEL-23454341-21312klsdf"
                );
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place_no_fake.xml", sortingCenter.getToken())
        ), user);
        places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("000000-place-1", "000000-place-2");
    }

    @Test
    void createNewPlacesAfterOrderCancel() {
        var order = testFactory.createOrderForToday(sortingCenter).cancel().get();
        assertThat(placeRepository.findAllByOrderIdOrderById(order.getId())).hasSize(1);

        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("000000-place-1", "000000-place-2");
        assertThat(places).allMatch(p -> p.getSortableStatus() == SortableStatus.CANCELLED);
    }

    @Test
    void createNewPlacesAfterAcceptReturn() {
        var order = testFactory.createForToday(order(sortingCenter).places("000000-place-1").build())
                .cancel().accept().get();
        assertThat(placeRepository.findAllByOrderIdOrderById(order.getId())).hasSize(1);

        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("000000-place-1", "000000-place-2");
        assertThat(places.stream().map(Place::getSortableStatus).toList())
                .containsExactlyInAnyOrder(SortableStatus.ACCEPTED_RETURN, SortableStatus.AWAITING_RETURN);
    }

    @Test
    void createPlacesForMultiPlaceOrder() {
        var order = testFactory.createForToday(order(sortingCenter).createTwoPlaces(true).build()).get();
        assertThat(placeRepository.findAllByOrderIdOrderById(order.getId())).isNotEmpty();
    }

    @Test
    void setPlacesForNonMultiPlaceOrder() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        assertThat(placeRepository.findAllByOrderIdOrderById(order.getId())).hasSize(1);

        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("000000-place-1", "000000-place-2");
    }

    @Test
    void replacePlacesInMultiPlaceOrderPlaceWithoutYandexId() {
        var order = testFactory.createForToday(order(sortingCenter).createTwoPlaces(true).build()).get();
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("pk-" + order.getExternalId() + "-1", "pk-" + order.getExternalId() + "-2");
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_single_place_no_partner_id.xml", sortingCenter.getToken())
        ), user);
        List<Place> newPlaces = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(newPlaces).hasSize(1);
        assertThat(newPlaces.get(0).getMainPartnerCode()).isEqualTo("000000-place-1");
    }

    @Test
    void removePlacesFromMultiPlaceOrder() {
        var order = testFactory.createForToday(order(sortingCenter).createTwoPlaces(true).build()).get();

        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order.xml", sortingCenter.getToken())
        ), user);
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places).isNotEmpty();
    }

    @Test
    void replacePlacesInMultiPlaceOrder() {
        var order = testFactory.createForToday(order(sortingCenter).createTwoPlaces(true).build()).get();
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("pk-" + order.getExternalId() + "-1", "pk-" + order.getExternalId() + "-2");

        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place.xml", sortingCenter.getToken())
        ), user);
        List<Place> newPlaces = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(newPlaces.stream().map(Place::getMainPartnerCode).toList())
                .containsExactlyInAnyOrder("000000-place-1", "000000-place-2");
    }

    @Test
    void shipPlacesOnRouteFinish() {
        var order1 = testFactory.createForToday(
                order(sortingCenter).externalId("1").createTwoPlaces(true).build()
        ).acceptPlaces().sortPlaces().get();
        var order2 = testFactory.createForToday(
                order(sortingCenter).externalId("2").createTwoPlaces(true).build()
        ).acceptPlaces().sortPlaces().get();
        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order1));
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));

        assertThat(testFactory.orderPlaces(order1))
                .isNotEmpty().allMatch(p -> p.getStatus() == PlaceStatus.SHIPPED);
        assertThat(testFactory.orderPlaces(order2))
                .isNotEmpty().allMatch(p -> p.getStatus() == PlaceStatus.SHIPPED);
    }

    @Test
    void returnPlacesOnRouteFinish() {
        var order1 = testFactory.createForToday(
                order(sortingCenter).externalId("1").createTwoPlaces(true).build()
        ).cancel().acceptPlaces().sortPlaces().get();
        var order2 = testFactory.createForToday(
                order(sortingCenter).externalId("2").createTwoPlaces(true).build()
        ).cancel().acceptPlaces().sortPlaces().get();
        var route = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order1));

        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));

        assertThat(testFactory.orderPlaces(order1))
                .isNotEmpty().allMatch(p -> p.getStatus() == PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlaces(order2))
                .isNotEmpty().allMatch(p -> p.getStatus() == PlaceStatus.RETURNED);
    }

}
