//package ru.yandex.market.sc.core.domain.place;
//
//import java.time.Clock;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.annotation.Transactional;
//
//import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
//import ru.yandex.market.sc.core.domain.place.repository.Place;
//import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
//import ru.yandex.market.sc.core.domain.route_so.model.RouteType;
//import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
//import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
//import ru.yandex.market.sc.core.domain.user.repository.User;
//import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
//import ru.yandex.market.sc.core.test.EmbeddedDbTest;
//import ru.yandex.market.sc.core.test.TestFactory;
//import ru.yandex.market.sc.core.util.ScDateUtils;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static ru.yandex.market.sc.core.test.TestFactory.order;
//
//@EmbeddedDbTest
//TODO:KIR переписать тест под новые реалии или удалить, если он не нужен
//public class PlaceServiceTest {
//
//    @Autowired
//    PlaceRouteService placeService;
//
//    @Autowired
//    TestFactory testFactory;
//
//
//    Clock clock;
//
//    SortingCenter sortingCenter;
//    User user;
//    Warehouse warehouse;
//
//    @BeforeEach
//    void init() {
//        sortingCenter = testFactory.storedSortingCenter();
//        user = testFactory.storedUser(sortingCenter, -20);
//        warehouse = testFactory.storedWarehouse("я склад");
//    }
//
//    @Test
//    @Transactional
//    void testUpdateRoutesOnRoutingFieldsChangeForWarehouseInRoute() {
//        //Готовимся
//        ScOrder scOrder = testFactory.createForToday(
//                order(sortingCenter).warehouseFromId(warehouse.getYandexId()).build()).get();
//        Place place = testFactory.orderPlace(scOrder);
//
//        //Делаем
//        placeService.updateRoutesOnRoutingFieldsChange(place, user);
//
//        //Проверяем
//        RouteSo inRoute = place.getInRoute();
//
//        assertThat(inRoute).isNotNull();
//        assertThat(inRoute.getType()).isEqualTo(RouteType.IN_DIRECT);
//        assertThat(inRoute.getDestinationType()).isEqualTo(RouteDestinationType.WAREHOUSE);
//        assertThat(inRoute.getDestinationId()).isEqualTo(warehouse.getId());
//        assertThat(inRoute.getDestinationYandexId()).isEqualTo(warehouse.getYandexId());
//        assertThat(inRoute.getCarNumber()).isNull();
//        assertThat(inRoute.getIntervalFrom()).isEqualTo(ScDateUtils.beginningOfDay());
//        assertThat(inRoute.getIntervalTo()).isEqualTo(ScDateUtils.endOfDay());
//
//
//
//    }
//}
