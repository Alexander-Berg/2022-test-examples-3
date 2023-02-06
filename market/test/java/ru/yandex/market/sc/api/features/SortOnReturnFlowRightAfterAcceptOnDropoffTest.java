package ru.yandex.market.sc.api.features;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.PlaceCommandService;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * Сортировка посылок сразу после их приемки на возвратном потоке на дропоффах.
 * Регулируется флагом.
 * Суть: сразу проставляем статус 175 вместо 170 при вызове ручки @PutMapping("/orders/acceptReturn") на экране
 * "первичная приемка возвратов"
 */
public class SortOnReturnFlowRightAfterAcceptOnDropoffTest extends BaseApiControllerTest {

    @MockBean
    Clock clock;
    @Autowired
    SortingCenterPropertySource sortingCenterPropertySource;
    TestControllerCaller caller;
    @Autowired
    OrderCommandService orderCommandService;
    @Autowired
    PlaceCommandService placeCommandService;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TestFactory testFactory;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    AcceptService acceptService;
    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        int userId = 234;
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ACCEPT_AND_SORT_RETURN_ON_DROPOFF, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, true);
        user = testFactory.storedUser(sortingCenter, userId, UserRole.STOCKMAN);
        testFactory.setupMockClock(clock);
        caller = TestControllerCaller.createCaller(mockMvc, userId);
    }

    @Test
    @DisplayName("Флаг включен. Полная приемка возврата многоместного заказа")
    @SneakyThrows
    public void acceptReturnedMultiplaceFull() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3")
                .sortPlaces("p1", "p2", "p3")
                .shipPlaces("p1", "p2", "p3")
                .makeReturn()
                .get();
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p1"))
                .andExpect(status().isOk());
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p2"))
                .andExpect(status().isOk());
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p3"))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    @DisplayName("Флаг включен. Полная приемка отмененного многоместного заказа")
    @SneakyThrows
    public void acceptCanceledMultiplaceFull() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .cancel()
                .get();
        caller.acceptOrder(new AcceptOrderRequestDto("1", "p1"))
                .andExpect(status().isOk());
        caller.acceptOrder(new AcceptOrderRequestDto("1", "p2"))
                .andExpect(status().isOk());
        caller.acceptOrder(new AcceptOrderRequestDto("1", "p3"))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    @DisplayName("Флаг включен. Полная приемка отмененного многоместного заказа. Затем сортируем одну посылку.")
    @SneakyThrows
    public void acceptCanceledMultiplaceFullThenSortOnePlace() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .cancel()
                .get();
        caller.acceptOrder(new AcceptOrderRequestDto("1", "p1"))
                .andExpect(status().isOk());
        caller.acceptOrder(new AcceptOrderRequestDto("1", "p2"))
                .andExpect(status().isOk());
        caller.acceptOrder(new AcceptOrderRequestDto("1", "p3"))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));
        caller.sortableBetaSort(new SortableSortRequestDto(
                        order.getExternalId(),
                        "p3",
                        cell.getId().toString()))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Флаг включен. Полная приемка отмененного одноместного заказа. Затем сортируем его.")
    @SneakyThrows
    public void acceptCanceledRegularOrderThenSortIt() {
        var place = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .cancel()
                .getPlace();
        caller.acceptOrder(new AcceptOrderRequestDto("1", null))
                .andExpect(status().isOk());
        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        var route = testFactory.findOutgoingWarehouseRoute(place)
                .orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, place));
        caller.sortableBetaSort(new SortableSortRequestDto(place, cell))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Флаг включен. Полная приемка одноместного заказа. Затем сортируем его.")
    @SneakyThrows
    public void acceptRegularOrderThenSortIt() {
        var place = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .getPlace();
        caller.acceptOrder(new AcceptOrderRequestDto("1", null))
                .andExpect(status().isOk());
        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        var route = testFactory.findOutgoingCourierRoute(place)
                .orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, place));
        caller.sortableBetaSort(new SortableSortRequestDto(place, cell))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Флаг включен. Частичная приемка возврата многоместного заказа. Приняли только 1 место")
    @SneakyThrows
    public void acceptReturnedMultiplacePartialOnePlace() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3")
                .sortPlaces("p1", "p2", "p3")
                .shipPlaces("p1", "p2", "p3")
                .makeReturn()
                .get();
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p1"))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("Флаг включен. Частичная приемка возврата многоместного заказа")
    @SneakyThrows
    public void acceptReturnedMultiplacePartial() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3")
                .sortPlaces("p1", "p2", "p3")
                .shipPlaces("p1", "p2", "p3")
                .makeReturn()
                .get();
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p1"))
                .andExpect(status().isOk());
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p2"))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("Флаг включен. Приемка одноместного заказа")
    @SneakyThrows
    public void acceptReturned() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept()
                .sort()
                .ship()
                .makeReturn()
                .get();
        caller.acceptReturn(new AcceptOrderRequestDto("1", null))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    @DisplayName("Флаг выключен. Приемка одноместного заказа")
    @SneakyThrows
    public void acceptReturnedFlagOff() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ACCEPT_AND_SORT_RETURN_ON_DROPOFF, false);
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept()
                .sort()
                .ship()
                .makeReturn()
                .get();
        caller.acceptReturn(new AcceptOrderRequestDto("1", null))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("Флаг выключен. Приемка одноместного отмененного заказа")
    @SneakyThrows
    public void acceptCanceledFlagOff() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ACCEPT_AND_SORT_RETURN_ON_DROPOFF, false);
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .cancel()
                .get();
        caller.acceptOrder(new AcceptOrderRequestDto("1", null))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("Флаг выключен. " +
            "Частичная приемка возврата многоместного заказа. Приняли только 1 место")
    @SneakyThrows
    public void acceptReturnedMultiplacePartialOnePlaceFlagOff() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ACCEPT_AND_SORT_RETURN_ON_DROPOFF, false);
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3")
                .sortPlaces("p1", "p2", "p3")
                .shipPlaces("p1", "p2", "p3")
                .makeReturn()
                .get();
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p1"))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("Флаг выключен. Полная приемка возврата многоместного заказа")
    @SneakyThrows
    public void acceptReturnedMultiplaceAcceptAllPlaceFlagOff() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ACCEPT_AND_SORT_RETURN_ON_DROPOFF, false);
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3")
                .sortPlaces("p1", "p2", "p3")
                .shipPlaces("p1", "p2", "p3")
                .makeReturn()
                .get();
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p1"))
                .andExpect(status().isOk());
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p2"))
                .andExpect(status().isOk());
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p3"))
                .andExpect(status().isOk());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("Флаг выключен. " +
            "Частичная приемка возврата многоместного заказа. Приняли и отсортировали только 1 место")
    @SneakyThrows
    public void acceptAndSortReturnedMultiplacePartialOnePlaceFlagOff() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ACCEPT_AND_SORT_RETURN_ON_DROPOFF, false);
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3")
                .sortPlaces("p1", "p2", "p3")
                .shipPlaces("p1", "p2", "p3")
                .makeReturn()
                .get();
        var route = testFactory.findOutgoingWarehouseRoute(order.getId()).orElseThrow();
        var cell = route.getCells(LocalDate.now(clock)).stream().findFirst().orElseThrow();
        caller.acceptReturn(new AcceptOrderRequestDto("1", "p1"))
                .andExpect(status().isOk());
        caller.sortableBetaSort(new SortableSortRequestDto(order.getExternalId(), "p1", cell.getId().toString()));
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }
}
