package ru.yandex.travel.orders.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.experiments.KVExperiment;
import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.TestOrderItem;
import ru.yandex.travel.orders.commons.proto.EDisplayOrderState;
import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.commons.proto.EOrderType;
import ru.yandex.travel.orders.entities.AdminListOrdersParams;
import ru.yandex.travel.orders.entities.AuthorizedUser;
import ru.yandex.travel.orders.entities.FxRate;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.Order;
import ru.yandex.travel.orders.entities.OrderAggregateState;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.workflow.order.aeroflot.proto.EAeroflotOrderState;
import ru.yandex.travel.orders.workflows.order.OrderCreateHelper;

import static org.assertj.core.api.Assertions.assertThat;

// Sample test, will be removed probably

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuthorizedUserRepository authRepository;

    @Autowired
    private OrderAggregateStateRepository orderAggregateStateRepository;

    @Autowired
    private EntityManager em;

    @Test
    public void testCorrectOrderOfOrderItemsAfterSaveAndReload() {
        OrderItem flight = new TestOrderItem();
        flight.setId(UUID.randomUUID());
        OrderItem train = new TestOrderItem();
        train.setId(UUID.randomUUID());
        OrderItem hotel = new TestOrderItem();

        HotelOrder newOrder = new HotelOrder();
        newOrder.setId(UUID.randomUUID());
        newOrder.addOrderItem(flight);
        newOrder.addOrderItem(train);
        newOrder.addOrderItem(hotel);

        newOrder = orderRepository.saveAndFlush(newOrder);

        Order savedOrder = orderRepository.getOne(newOrder.getId());
        assertThat(savedOrder.getOrderItems().size()).isEqualTo(3);
    }

    @Test
    public void testFxRatesIsStoredCorrectly() {
        HotelOrder newOrder = OrderCreateHelper.createTestHotelOrder();
        FxRate newFxRate = new FxRate();
        newFxRate.putIfAbsent(ECurrency.C_RUB, BigDecimal.valueOf(100, 2));
        newFxRate.putIfAbsent(ECurrency.C_USD, BigDecimal.valueOf(6650, 2));
        newOrder.setFxRate(newFxRate);
        newOrder.setCurrency(ProtoCurrencyUnit.RUB);
        newOrder = orderRepository.saveAndFlush(newOrder);
        em.clear();

        Order savedOrder = orderRepository.getOne(newOrder.getId());
        FxRate savedFxRate = savedOrder.getFxRate();

        assertThat(savedFxRate.get(ECurrency.C_RUB)).isEqualTo(newFxRate.get(ECurrency.C_RUB));
        assertThat(savedFxRate.get(ECurrency.C_USD)).isEqualTo(newFxRate.get(ECurrency.C_USD));
        assertThat(savedOrder.getCurrency()).isEqualTo(newOrder.getCurrency());
    }

    @Test
    public void testKVExperimentsIsStoredCorrectly() {
        HotelOrder newOrder = OrderCreateHelper.createTestHotelOrder();
        List<KVExperiment> experiments = new ArrayList<>();
        experiments.add(new KVExperiment("exp1", "val1"));
        experiments.add(new KVExperiment("exp2", "val2"));
        newOrder.setKVExperiments(experiments);
        newOrder = orderRepository.saveAndFlush(newOrder);
        em.clear();

        Order savedOrder = orderRepository.getOne(newOrder.getId());
        List<KVExperiment> savedExperiments = savedOrder.getKVExperiments();

        assertThat(savedExperiments.get(0)).isEqualTo(experiments.get(0));
        assertThat(savedExperiments.get(1)).isEqualTo(experiments.get(1));
        assertThat(savedOrder.getCurrency()).isEqualTo(newOrder.getCurrency());
    }

    @Test
    public void testCorrectlyGettingPrettyIdSequenceValue() {
        Long val = orderRepository.getNextPrettyIdSequenceValue();
        assertThat(val).isEqualTo(1L);
    }

    @Test
    public void testRemovedNotFoundViaAdminSearch() {
        HotelOrder newOrder = OrderCreateHelper.createTestHotelOrder();
        HotelOrder removedOrder = OrderCreateHelper.createTestHotelOrder();
        removedOrder.setRemoved(true);
        List<HotelOrder> orderList = List.of(newOrder, removedOrder);
        orderRepository.saveAll(orderList);
        orderRepository.flush();

        AdminListOrdersParams adminListOrdersParams = new AdminListOrdersParams(List.of(newOrder.getId(), removedOrder.getId()), null);
        assertThat(orderRepository.findOrdersWithAdminFilters(adminListOrdersParams).size()).isEqualTo(1);
    }

    @Test
    public void testSelectForListOrders() {
        for (int i = 0; i < 5; i++) {
            createTestOrderWithOwnerData(null, null, null, "loggedOut", EOrderType.OT_HOTEL_EXPEDIA);
        }
        for (int i = 0; i < 11; i++) {
            createTestOrderWithOwnerData(null, "passport1", "login1", "loggedIn", EOrderType.OT_AVIA_AEROFLOT);
        }
        for (int i = 0; i < 13; i++) {
            createTestOrderWithOwnerData(null, "passport2", "login2", "loggedIn", EOrderType.OT_HOTEL_EXPEDIA);
        }

        assertThat(orderRepository.findOrdersOwnedByUser(
                "passport1",
                Set.of(EDisplayOrderType.DT_HOTEL, EDisplayOrderType.DT_AVIA),
                new HashSet<>(Arrays.asList(EDisplayOrderState.values())),
                PageRequest.of(1, 1)
        ).size()).isEqualTo(2); // 2 because we ask for more to return hasMoreOrders
    }

    @Test
    public void testSelectNotRemovedOrders() {
        var orderIds = new HashSet<UUID>();
        var order = createTestOrderWithOwnerData(null, "passport1", "login1", "loggedOut", EOrderType.OT_HOTEL_EXPEDIA);
        orderIds.add(order.getId());
        order = createTestOrderWithOwnerData(null, "passport1", "login1", "loggedIn", EOrderType.OT_AVIA_AEROFLOT);
        orderIds.add(order.getId());
        order = createTestOrderWithOwnerData(null, "passport1", "login1", "loggedIn", EOrderType.OT_HOTEL_EXPEDIA);
        orderIds.add(order.getId());

        assertThat(orderRepository.selectNotRemovedOrders(orderIds).size()).isEqualTo(3);
    }

    @Test
    public void testFindOrdersOwnedByUserWithoutExcluded() {
        createTestOrderWithOwnerData(null, "passport1", "login1", "loggedOut", EOrderType.OT_HOTEL_EXPEDIA);
        createTestOrderWithOwnerData(null, "passport1", "login1", "loggedIn", EOrderType.OT_AVIA_AEROFLOT);

        var excludedOrder = createTestOrderWithOwnerData(null, "passport1", "login1", "loggedIn", EOrderType.OT_HOTEL_EXPEDIA);
        var excludedOrderIds = new HashSet<>(List.of(excludedOrder.getId()));

        assertThat(orderRepository.findOrdersOwnedByUserWithoutExcluded(
                "passport1",
                excludedOrderIds,
                Set.of(EDisplayOrderType.DT_HOTEL, EDisplayOrderType.DT_AVIA),
                new HashSet<>(Arrays.asList(EDisplayOrderState.values())),
                PageRequest.of(0, 10)).size()
        ).isEqualTo(2);
    }

    private Order createTestOrderWithOwnerData(String sessionKey, String passportId, String login, String yandexUid,
                                               EOrderType orderType) {
        Order newOrder;
        if (orderType == EOrderType.OT_HOTEL_EXPEDIA) {
            newOrder = OrderCreateHelper.createTestHotelOrder();
        } else if (orderType == EOrderType.OT_AVIA_AEROFLOT) {
            newOrder = OrderCreateHelper.createTestAeroflotOrder(EAeroflotOrderState.OS_NEW);
        } else {
            throw new RuntimeException("Unexpected order type");
        }
        AuthorizedUser owner = null;
        if (passportId != null) {
            owner = AuthorizedUser.createLogged(newOrder.getId(), yandexUid, passportId, login,
                    AuthorizedUser.OrderUserRole.OWNER);
        } else if (sessionKey != null) {
            owner = AuthorizedUser.createGuest(newOrder.getId(), sessionKey, yandexUid,
                    AuthorizedUser.OrderUserRole.OWNER);
        }
        if (owner != null) {
            authRepository.saveAndFlush(owner);
        }

        newOrder = orderRepository.saveAndFlush(newOrder);

        OrderAggregateState state = new OrderAggregateState();
        state.setId(newOrder.getId());
        state.setOrderPrettyId(newOrder.getPrettyId());
        state.setOrderType(newOrder.getPublicType());
        state.setOrderDisplayState(EDisplayOrderState.OS_IN_PROGRESS);
        orderAggregateStateRepository.saveAndFlush(state);

        return newOrder;
    }
}
