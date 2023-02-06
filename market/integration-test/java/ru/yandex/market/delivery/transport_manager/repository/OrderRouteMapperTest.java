package ru.yandex.market.delivery.transport_manager.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderBindingType;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderRoute;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderRouteType;
import ru.yandex.market.delivery.transport_manager.domain.entity.RegisterResourceData;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.model.enums.PartnerType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.OrderRouteMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.order.MatchingTransportationData;

@DatabaseSetup("/repository/order_route/orders.xml")
public class OrderRouteMapperTest extends AbstractContextualTest {

    @Autowired
    private OrderRouteMapper routeMapper;

    @Autowired
    private TransportationMapper transportationMapper;

    private static final List<OrderRoute> ROUTES = List.of(
        new OrderRoute()
            .setType(OrderRouteType.LOM)
            .setOrderId(1L)
            .setIndex(0)
            .setOutboundPartnerId(1L)
            .setInboundPartnerId(2L)
            .setMovingPartnerId(2L)
            .setOutboundPointId(11L)
            .setOriginOutboundPointId(11L)
            .setInboundPointId(null)
            .setOutboundPartnerType(PartnerType.DROPSHIP)
            .setInboundPartnerType(PartnerType.SORTING_CENTER)
            .setShipmentDate(LocalDateTime.of(2021, 11, 8, 10, 0))
            .setInboundExternalId("ext_1"),
        new OrderRoute()
            .setType(OrderRouteType.LOM)
            .setOrderId(1L)
            .setIndex(1)
            .setOutboundPartnerId(2L)
            .setInboundPartnerId(3L)
            .setMovingPartnerId(5L)
            .setOutboundPointId(15L)
            .setOriginOutboundPointId(15L)
            .setOutboundPartnerType(PartnerType.SORTING_CENTER)
            .setInboundPartnerType(PartnerType.SORTING_CENTER)
            .setInboundPointId(10L)
            .setOriginInboundPointId(10L)
            .setShipmentDate(LocalDateTime.of(2021, 11, 9, 12, 0))
            .setInboundExternalId("ext_2")
    );

    private static final OrderRoute ROUTE_TO_MATCH = new OrderRoute()
        .setOutboundPartnerId(5L)
        .setOutboundPointId(10000004403L)
        .setShipmentDate(LocalDateTime.of(2020, 9, 7, 20, 0))
        .setInboundPartnerId(6L)
        .setInboundPointId(10000004555L)
        .setMovingPartnerId(5L)
        .setIndex(0)
        .setOrderId(1L);

    @Test
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        updateSequence("order_route", 1);
        softly.assertThat(ROUTES.get(0).getId()).isNull();
        softly.assertThat(ROUTES.get(1).getId()).isNull();

        routeMapper.insert(ROUTES);

        softly.assertThat(ROUTES.get(0).getId()).isEqualTo(1);
        softly.assertThat(ROUTES.get(1).getId()).isEqualTo(2);
    }

    @Test
    @DatabaseSetup("/repository/order_route/routes.xml")
    void getByOrderId() {
        List<OrderRoute> byOrderId = routeMapper.getByOrderId(1L);
        assertContainsExactlyInAnyOrder(byOrderId, ROUTES.get(0), ROUTES.get(1));
    }

    @Test
    @DatabaseSetup("/repository/order_route/routes.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() {
        OrderRoute route = new OrderRoute()
            .setType(OrderRouteType.LOM)
            .setOrderId(1L)
            .setIndex(0)
            .setOutboundPartnerId(1L)
            .setInboundPartnerId(2L)
            .setMovingPartnerId(2L)
            .setOutboundPointId(10L)
            .setOriginOutboundPointId(15L)
            .setInboundPointId(null)
            .setShipmentDate(LocalDateTime.of(2021, 11, 8, 10, 0))
            .setInboundExternalId("ext_1");

        route.setInboundExternalId("123ext");
        route.setShipmentDate(LocalDateTime.of(2021, 10, 12, 1, 1));
        routeMapper.update(1L, route);
    }

    @Test
    @DatabaseSetup("/repository/order_route/routes.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/routes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNoUpdateWithNull() {
        OrderRoute route = new OrderRoute()
            .setType(OrderRouteType.LOM)
            .setOrderId(1L)
            .setIndex(0)
            .setOutboundPartnerId(1L)
            .setInboundPartnerId(2L)
            .setMovingPartnerId(2L)
            .setOutboundPointId(11L)
            .setOriginOutboundPointId(11L)
            .setInboundPartnerType(PartnerType.SORTING_CENTER)
            .setOutboundPartnerType(PartnerType.DROPSHIP)
            .setInboundPointId(null)
            .setShipmentDate(null)
            .setInboundExternalId(null);

        routeMapper.update(1L, route);
    }

    @Test
    @DatabaseSetup("/repository/order_route/routes.xml")
    void getByIds() {
        List<OrderRoute> byIds = routeMapper.getByIds(Set.of(1L, 2L));
        assertContainsExactlyInAnyOrder(byIds, ROUTES.get(0), ROUTES.get(1));
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/order_route/routes.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_plan_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updatePlan() {
        routeMapper.updatePlan(Set.of(3L, 4L), OrderBindingType.ON_TRANSPORTATION_CREATION, 1L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/order_route/after/after_plan_update.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/routes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updatePlanNull() {
        routeMapper.updatePlan(Set.of(3L, 4L), null, null);
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/setup/register_2.xml")
    void matchWithTransportation() {
        MatchingTransportationData transportationData = routeMapper.matchWithTransportation(ROUTE_TO_MATCH, 0);
        MatchingTransportationData expected = new MatchingTransportationData(1L, 2L, 1L);
        softly.assertThat(transportationData).isEqualTo(expected);
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/setup/register_1.xml")
    @DatabaseSetup(value = "/repository/register/setup/plan_with_partner_id.xml", type = DatabaseOperation.UPDATE)
    void matchSkipRegisterWithPartnerId() {
        MatchingTransportationData transportationData = routeMapper.matchWithTransportation(ROUTE_TO_MATCH, 0);
        MatchingTransportationData expected = new MatchingTransportationData(1L, null, null);
        softly.assertThat(transportationData).isEqualTo(expected);
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/setup/register_mess.xml")
    void matchCorrectRegisters() {
        MatchingTransportationData transportationData = routeMapper.matchWithTransportation(ROUTE_TO_MATCH, 0);
        MatchingTransportationData expected = new MatchingTransportationData(1L, 3L, 4L);
        softly.assertThat(transportationData).isEqualTo(expected);
    }

    @Test
    @DatabaseSetup("/repository/transportation/deleted_for_today_active_for_tomorrow.xml")
    void matchSameDateIsTheMostImportant() {
        MatchingTransportationData transportationData = routeMapper.matchWithTransportation(ROUTE_TO_MATCH, 0);
        MatchingTransportationData expected = new MatchingTransportationData(1L, null, null);
        softly.assertThat(transportationData).isEqualTo(expected);
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/order_route/routes_for_transportation.xml")
    void matchWithRoutes() {
        Transportation transportation = transportationMapper.getById(1L);
        List<OrderRoute> orderRoutes = routeMapper.matchWithRoutes(transportation);

        OrderRoute expected = new OrderRoute()
            .setId(3L)
            .setOrderId(2L)
            .setOutboundPartnerId(5L)
            .setOutboundPointId(10000004403L)
            .setShipmentDate(LocalDateTime.of(2020, 9, 7, 0, 0))
            .setInboundPartnerId(6L)
            .setInboundPointId(10000004555L)
            .setMovingPartnerId(5L)
            .setInboundExternalId("ext_3")
            .setType(OrderRouteType.COMBINATOR)
            .setIndex(0);

        softly.assertThat(orderRoutes).containsExactly(expected);
    }

    @Test
    @DatabaseSetup("/repository/order_route/routes.xml")
    @DatabaseSetup("/repository/order_route/registers.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_unbind_from_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unbindFromRegister() {
        routeMapper.unbindFromRegister(Set.of(1L, 2L), Set.of(1L, 3L));
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/order_route/unbound_for_date.xml")
    void getImportantUnboundDropshipForDate() {
        List<Long> routeIds = routeMapper.getImportantUnboundDropshipRoutesForDate(LocalDate.of(2021, 11, 9));
        softly.assertThat(routeIds).containsExactlyInAnyOrder(1L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/order_route/after/after_plan_update.xml")
    @DatabaseSetup("/repository/order_route/register_units.xml")
    void getResourceData() {
        List<RegisterResourceData> routes = routeMapper.getRegisterResourceData(1L, 1L);

        softly.assertThat(routes).containsExactlyInAnyOrder(
            new RegisterResourceData("barcode3", "ext_4")
        );
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/order_route/after/after_plan_update.xml")
    void hasOrders() {
        softly.assertThat(routeMapper.hasOrders(1L)).isTrue();
        softly.assertThat(routeMapper.hasOrders(3L)).isFalse();
    }

    @Test
    @DatabaseSetup("/repository/order_route/routes.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_point_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updatePoints() {
        routeMapper.updateRoutePoints(16L, 20L);
    }

    @Test
    @DatabaseSetup("/repository/order_route/routes.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_point_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updatePointsById() {
        routeMapper.updateOutboundPointById(3L, 20L);
        routeMapper.updateOutboundPointById(4L, 20L);
        routeMapper.updateInboundPointById(5L, 20L);
    }
}
