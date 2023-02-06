package ru.yandex.market.tpl.core.domain.routing.orders;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DeliveryServiceRegionRepository;
import ru.yandex.market.tpl.core.domain.ds.DsRegion;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class RoutingFarOrdersServiceTest {
    private static final Long DS_ID_1 = -1L;
    private static final Long DS_ID_2 = 198L;
    private static final Set<Long> DS_IDS = Set.of(DS_ID_1, DS_ID_2);

    private final TestUserHelper userHelper;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserScheduleRuleRepository scheduleRuleRepository;
    private final RoutingFarOrdersService routingFarOrdersService;
    private final RoutingDroppedFarOrdersRepository routingDroppedFarOrdersRepository;
    private final DeliveryServiceRegionRepository deliveryServiceRegionRepository;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;

    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private Shift shift;

    @BeforeEach
    void init() {
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
    }

    @Test
    void testAllOrdersDroppedAndSavedDropForDsEnabled_addressValidation() {
        //given
        var command = generateCommand();

        Collection<Order> orders = command
                .getData()
                .getOrders();

        orders.forEach(order -> order.setIsAddressValid(RandomUtils.nextBoolean()));

        Set<Long> expectedFarOrdersIds = orders
                .stream()
                .filter(order -> order.getIsAddressValid() != null && !order.getIsAddressValid())
                .map(Order::getId)
                .collect(Collectors.toSet());

        //when
        var result = routingFarOrdersService.getFarOrdersAndSave(
                orders,
                command.getProfileType(),
                command.getData().getRouteDate());


        //then
        assertThat(result).containsExactlyInAnyOrderElementsOf(
                expectedFarOrdersIds);

        var savedDroppedOrders = routingDroppedFarOrdersRepository.findAll();
        assertThat(savedDroppedOrders
                .stream()
                .map(RoutingDroppedFarOrders::getOrders)
                .flatMap(Collection::stream)
                .collect(Collectors.toList())).containsExactlyInAnyOrderElementsOf(expectedFarOrdersIds);
    }

    @Test
    void testAllOrdersDroppedAndSavedDropForDsEnabled() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.IS_DROP_FAR_ORDERS_FROM_ROUTING_ENABLED))
                .thenReturn(true);
        var command = generateCommand();
        Mockito.when(configurationProviderAdapter.getValueAsLongs(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES))
                .thenReturn(DS_IDS);

        var result = routingFarOrdersService.getFarOrdersAndSave(command);

        assertThat(result).containsExactlyInAnyOrderElementsOf(
                command.getData().getOrders().stream()
                        .map(Order::getId)
                        .collect(Collectors.toList())
        );
        var savedDroppedOrders = routingDroppedFarOrdersRepository.findAll();
        assertThat(savedDroppedOrders).hasSize(DS_IDS.size());
    }

    @Test
    void testDroppedOrdersSavedButNotDroppedDropForDsDisabled() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_DROP_FAR_ORDERS_FROM_ROUTING_ENABLED))
                .thenReturn(true);
        var command = generateCommand();
        Mockito.when(configurationProviderAdapter.getValueAsLongs(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES))
                .thenReturn(Set.of());

        var result = routingFarOrdersService.getFarOrdersAndSave(command);

        assertThat(result).isEmpty();
        var savedDroppedOrders = routingDroppedFarOrdersRepository.findAll();
        assertThat(savedDroppedOrders).hasSize(DS_IDS.size());
    }

    @Test
    void testNoFarOrdersDropForDsEnabled() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_DROP_FAR_ORDERS_FROM_ROUTING_ENABLED))
                .thenReturn(true);
        var command = generateCommand();
        createDsDeliveryRegions(command.getData().getOrders());
        Mockito.when(configurationProviderAdapter.getValueAsLongs(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES))
                .thenReturn(DS_IDS);

        var result = routingFarOrdersService.getFarOrdersAndSave(command);

        assertThat(result).isEmpty();
        var savedDroppedOrders = routingDroppedFarOrdersRepository.findAll();
        assertThat(savedDroppedOrders).hasSize(0);
    }

    @Test
    void testNotFailOnDuplicateDsRegion() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_DROP_FAR_ORDERS_FROM_ROUTING_ENABLED))
                .thenReturn(true);
        var command = generateCommand();
        createDsRegion(DS_ID_1, 1, LocalDate.now(clock));
        createDsRegion(DS_ID_1, 1, LocalDate.now(clock).plusDays(1));

        routingFarOrdersService.getFarOrdersAndSave(command);
    }

    private void createDsDeliveryRegions(Collection<Order> orders) {
        orders.stream().collect(Collectors.groupingBy(
                Order::getDeliveryServiceId,
                Collectors.mapping(
                        order -> order.getDelivery().getDeliveryAddress().getRegionId(),
                        Collectors.toList()
                )
        )).forEach(this::createDsRegion);
    }

    private void createDsRegion(Long dsId, List<Integer> regionIds) {
        regionIds.stream()
                .distinct()
                .map(regionId -> mapDsRegion(dsId, regionId))
                .forEach(deliveryServiceRegionRepository::save);
    }

    private DsRegion mapDsRegion(Long dsId, Integer regionId) {
        return new DsRegion(dsId, regionId, 1, true, true,
                LocalDate.now(clock), 1);
    }

    private void createDsRegion(Long dsId, Integer regionId, LocalDate createdAt) {
        var dsRegion = new DsRegion(dsId, regionId, 1, true,
                true, createdAt, 1);
        deliveryServiceRegionRepository.save(dsRegion);
    }

    private CreateShiftRoutingRequestCommand<CreateShiftRoutingRequestCommandData> generateCommand() {
        List<Order> orders = StreamEx.of(Stream.generate(
                () -> orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DS_ID_1)
                        .build())).limit(5)
        ).append(
                Stream.generate(
                        () -> orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                                .deliveryServiceId(DS_ID_2)
                                .build())).limit(5)
        ).collect(Collectors.toList());
        List<UserScheduleRule> users = scheduleRuleRepository.findAllWorkingRulesForDate(
                shift.getShiftDate(),
                shift.getSortingCenter().getId());

        Map<Long, RoutingCourier> couriersById =
                createShiftRoutingRequestCommandFactory.mapCouriersFromUserSchedules(
                        users,
                        false,
                        Map.of(),
                        Map.of()
                );

        return (CreateShiftRoutingRequestCommand<CreateShiftRoutingRequestCommandData>)
                (CreateShiftRoutingRequestCommand) CreateShiftRoutingRequestCommand.builder()
                .data(CreateShiftRoutingRequestCommandData.builder()
                        .routeDate(shift.getShiftDate())
                        .sortingCenter(shift.getSortingCenter())
                        .couriers(new HashSet<>(couriersById.values()))
                        .orders(orders)
                        .movements(List.of())
                        .build())
                .mockType(RoutingMockType.MANUAL)
                .build();
    }
}
