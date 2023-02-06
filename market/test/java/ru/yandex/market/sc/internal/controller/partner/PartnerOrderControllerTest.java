package ru.yandex.market.sc.internal.controller.partner;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.route_so.Routable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.XDOC_ENABLED;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
public class PartnerOrderControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    Clock clock;
    @Autowired
    TestFactory testFactory;
    @Autowired
    JdbcTemplate jdbcTemplate;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter, XDOC_ENABLED, false);
    }

    @Test
    @SneakyThrows
    public void getOrders() {
        int numberOfOrders = 2;
        List<ScOrder> scOrders = Stream.iterate(1, i -> i + 1)
                .limit(numberOfOrders)
                .map(i -> testFactory.createForToday(order(sortingCenter).externalId(i.toString()).build()).get())
                .toList();
        assertThat(scOrders).isNotNull();
        var courier = testFactory.storedCourier(777);
        Stream.iterate(1, i -> i + 1)
                .limit(numberOfOrders)
                .forEach(i ->
                        testFactory.createForToday(order(sortingCenter).externalId(i.toString()).build())
                                .updateCourier(courier).get()
                );
        assertThat(scOrders).isNotNull();
        assertThat(scOrders).hasSize(numberOfOrders);

        var routeOptional = testFactory.findOutgoingRoute(scOrders.get(0));
        assertThat(routeOptional).isPresent();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId() + "/orders/")
                                .param("routeId", testFactory.getRoutable(routeOptional.get()).getId().toString())
                                .param("routeOnlyCurrent", "true")
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(numberOfOrders)))
                .andReturn();
    }

    @Test
    @SneakyThrows
    public void getOrdersWithSimplifiedPagination() {
        int numberOfOrders = 30;
        var courier = testFactory.storedCourier(100);
        List<ScOrder> scOrders = Stream.iterate(1, i -> i + 1)
                .limit(numberOfOrders)
                .map(i -> testFactory.createForToday(order(sortingCenter).externalId(i.toString()).build())
                        .updateCourier(courier).get())
                .toList();
        assertThat(scOrders).isNotNull();
        assertThat(scOrders).hasSize(numberOfOrders);
        var routeOptional = testFactory.findOutgoingRoute(scOrders.get(0));
        assertThat(routeOptional).isPresent();
        var routable = testFactory.getRoutable(routeOptional.get());

        retrieveAndCheckPage(routable, 0, 10, 10, new ExpectedState(2, 11, true, false));
        retrieveAndCheckPage(routable, 1, 10, 10, new ExpectedState(3, 21, false, false));
        retrieveAndCheckPage(routable, 2, 10, 10, new ExpectedState(3, 30, false, true));
        retrieveAndCheckPage(routable, 0, 20, 20, new ExpectedState(2, 21, true, false));
        retrieveAndCheckPage(routable, 1, 20, 10, new ExpectedState(2, 30, false, true));
    }

    @Test
    @SneakyThrows
    public void getOrders_checkInboundSet() {
        int numberOfOrders = 2;
        List<ScOrder> scOrders = createSomeOrders(numberOfOrders);
        assertThat(scOrders).isNotNull();
        assertThat(scOrders).hasSize(numberOfOrders);

        var inbound = createInboundForOrders(scOrders);

        scOrders.stream()
                .map(o -> testFactory.orderPlaces(o.getId()))
                .flatMap(List::stream)
                .forEach(p -> {
                    assertThat(p).isNotNull();
                    assertThat(p.getInboundId()).isEqualTo(inbound.getId());
                });
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId() + "/orders/")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundId", equalTo(inbound.getId()), Long.class));
    }

    private List<ScOrder> createSomeOrders(int numberOfOrders) {
        return Stream.iterate(1, i -> i + 1)
                .limit(numberOfOrders)
                .map(String::valueOf)
                .map(i -> testFactory.createForToday(order(sortingCenter).externalId(i).build()).get())
                .toList();
    }

    private Inbound createInboundForOrders(List<ScOrder> scOrders) {
        assertThat(scOrders).isNotNull();
        assertThat(scOrders).isNotEmpty();
        var warehouseOrderMap = scOrders.stream().collect(Collectors.groupingBy(ScOrder::getWarehouseFrom));

        assertThat(warehouseOrderMap).isNotNull();
        assertThat(warehouseOrderMap).hasSize(1);
        var warehouse = warehouseOrderMap.keySet().iterator().next();

        var ordersForInbound = scOrders.stream()
                .map(order -> new Pair<>(order.getExternalId(), order.getExternalId()))
                .toList();

        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .sortingCenter(sortingCenter)
                .plainOrders(ordersForInbound)
                .build();

        return testFactory.createInbound(params);
    }

    private void retrieveAndCheckPage(Routable routable, int page, int size, int elementsOnPage, ExpectedState expectedState)
            throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId() + "/orders/")
                                .param("routeId", String.valueOf(routable.getId()))
                                .param("page", String.valueOf(page))
                                .param("size", String.valueOf(size))
                ).andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                                        {
                                            "pageable": {"offset": %d, "pageNumber": %d, "pageSize": %d},
                                            "totalPages": %d, "totalElements": %d, "numberOfElements": %d,
                                            "first": %b, "last": %b
                                        }
                                        """, page * size, page, size, expectedState.totalPages, expectedState.total,
                                elementsOnPage, expectedState.isFirst, expectedState.isLast),
                        false))
                .andExpect(jsonPath("$.content", hasSize(elementsOnPage)));
    }

    private record ExpectedState(int totalPages, int total, boolean isFirst, boolean isLast) {
    }
}
