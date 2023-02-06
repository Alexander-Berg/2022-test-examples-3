package ru.yandex.market.tpl.core.domain.usershift.location;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.tpl.api.enumdto.BindingTypeDto;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.OrderTypeFilter;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.user.partner.PartnerCourierZonesResponseDto;
import ru.yandex.market.tpl.api.model.usershift.location.OrderRegionDto;
import ru.yandex.market.tpl.api.model.usershift.location.RegionInfoDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.ds.DeliveryServiceRegionRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.TplOrderGenerateConstants;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.Partner;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.OrderTypeFilter.DROPSHIP;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_YT_TAKEN_ZONES_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.ZONES_CITY_DETALIZATION_REGION_TYPE_CODES;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.ZONES_OVERLAPPED_MODE_ENABLED;
import static ru.yandex.market.tpl.core.domain.order.address.AddressGenerator.AddressGenerateParam.DEFAULT_REGION_ID;
import static ru.yandex.market.tpl.core.domain.partner.DeliveryService.DEFAULT_DS_ID;
import static ru.yandex.market.tpl.core.domain.usershift.location.DetailingEnum.CITY_DISTRICT_RURAL;
import static ru.yandex.market.tpl.core.domain.usershift.location.DetailingEnum.CITY_FEDERATION_DISTRICT;
import static ru.yandex.market.tpl.core.domain.usershift.location.DetailingEnum.SECONDARY_DISTRICT_REGION_VILLAGE;

@RequiredArgsConstructor
@Sql("classpath:geobase/RegionDaoTest.sql")
class DeliveryRegionInfoServiceTest extends TplAbstractTest {
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final OrderGenerateService orderGenerateService;
    private final DeliveryRegionInfoService deliveryRegionInfoService;
    private final PartnerRepository partnerRepository;
    private final DeliveryServiceRegionRepository deliveryServiceRegionRepository;
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final SortingCenterService sortingCenterService;

    private final JdbcTemplate jdbcTemplate;

    private final Clock clock;
    private Order orderToday;
    private Order orderMinus3DaysRegion39;

    private Order lockerOrder;
    private Order pvzOrder;
    private final MovementGenerator movementGenerator;
    private final AddressGenerator addressGenerator;
    private Movement movement;

    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private User user;
    private User user2;

    public static final Integer NOT_EXPECTED_IN_RESULT_REGION_ID = 120543;


    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        user2 = testUserHelper.findOrCreateUser(2L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        orderToday = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryDate(LocalDate.now(clock))
                .buyerYandexUid(100500L)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());

        orderMinus3DaysRegion39 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryDate(LocalDate.now(clock).minusDays(3))
                .buyerYandexUid(100500L)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .regionId(39)
                        .build())
                .build());

        Order orderTodayRegion213 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryDate(LocalDate.now(clock))
                .buyerYandexUid(100500L)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .regionId(213)
                        .build())
                .build());


        PickupPoint pickupPoint1 = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L,
                1L);
        clearAfterTest(pickupPoint1);
        lockerOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryDate(LocalDate.now(clock))
                .buyerYandexUid(100500L)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint1)
                .build());


        PickupPoint pickupPoint2 = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 2L, 1L);
        clearAfterTest(pickupPoint2);
        pvzOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryDate(LocalDate.now(clock))
                .buyerYandexUid(100500L)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint2)
                .build());

        movement = createAndSaveMovement(DEFAULT_REGION_ID, TplOrderGenerateConstants.DEFAULT_DS_ID);
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE from delivery_service_region");
        jdbcTemplate.update("DELETE from user_region");
        jdbcTemplate.update("DELETE from region");

        Mockito.reset(configurationProviderAdapter);
    }

    @Test
    void getRegionInfoWithStatistics_success() {
        //given
        long dsId = DeliveryService.FAKE_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        linkUserToRegion(user.getId(), DEFAULT_REGION_ID);

        //when
        var res = deliveryRegionInfoService.getRegionInfoWithStatistics(DEFAULT_REGION_ID, null,
                null, scForDsId.getId());

        //then
        assertThat(res.getId()).isEqualTo(DEFAULT_REGION_ID);
        assertThat(res.getRegionOrdersDto().getOrdersCount()).isEqualTo(3);
    }

    @Test
    void getRegionInfoWithStatistics_fail_whenScNull() {
        //when
        assertThrows(TplInvalidParameterException.class,
                () -> deliveryRegionInfoService.getRegionInfoWithStatistics(DEFAULT_REGION_ID, null,
                        null, (Long) null));
    }

    @Test
    void getOrdersForSC_whenInside() {
        //given
        long dsId = TplOrderGenerateConstants.DEFAULT_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);

        //when
        List<OrderRegionDto> ordersByRegion =
                deliveryRegionInfoService.getOrdersForSc(
                        null,
                        OrdersRegionFilterEnum.INSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                        LocalTime.MIN, LocalTime.MAX, scForDsId.getId());

        //then
        assertThat(ordersByRegion).hasSize(4);

        Set<String> resultExternalIds = ordersByRegion
                .stream()
                .map(OrderRegionDto::getExternalOrderId)
                .collect(Collectors.toSet());

        assertThat(resultExternalIds).contains(orderToday.getExternalOrderId(),
                pvzOrder.getExternalOrderId(), lockerOrder.getExternalOrderId(), movement.getExternalId());
    }


    @Test
    void getOrdersForSC_whenFilterClient() {
        //given
        long dsId = TplOrderGenerateConstants.DEFAULT_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);

        //when
        List<OrderRegionDto> ordersByRegion =
                deliveryRegionInfoService.getOrdersForSc(
                        Set.of(OrderTypeFilter.CLIENT),
                        OrdersRegionFilterEnum.INSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                        LocalTime.MIN, LocalTime.MAX, scForDsId.getId());

        //then
        assertThat(ordersByRegion).hasSize(1);

        Set<String> resultExternalIds = ordersByRegion
                .stream()
                .map(OrderRegionDto::getExternalOrderId)
                .collect(Collectors.toSet());

        assertThat(resultExternalIds).contains(orderToday.getExternalOrderId());
    }

    @Test
    void getOrdersForSC_whenFilterDropship() {
        //given
        long dsId = TplOrderGenerateConstants.DEFAULT_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);

        //when
        List<OrderRegionDto> ordersByRegion =
                deliveryRegionInfoService.getOrdersForSc(
                        Set.of(DROPSHIP),
                        OrdersRegionFilterEnum.INSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                        LocalTime.MIN, LocalTime.MAX, scForDsId.getId());

        //then
        assertThat(ordersByRegion).hasSize(1);

        Set<String> resultExternalIds = ordersByRegion
                .stream()
                .map(OrderRegionDto::getExternalOrderId)
                .collect(Collectors.toSet());

        assertThat(resultExternalIds).contains(movement.getExternalId());
    }

    @Test
    void getOrdersForSC_whenOutsideEnabled() {
        //given
        long dsId = TplOrderGenerateConstants.NOT_DEFAULT_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, 20279, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);

        //Добавляем поддержку региона другой СД + Order и Movement для проверяемой СД в этом регионе
        //Ожидаем, что Order и Movement будут в списке OUTSIDE и иметь значение enabledRegion = true
        //Так как обслуживается другой СД
        int outsideEnabledRegionId = 110297;
        addDeliveryServiceRegion(TplOrderGenerateConstants.DEFAULT_DS_ID, outsideEnabledRegionId,
                LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);

        Order orderForOtherRegion = createOrderForDsWithRegion(outsideEnabledRegionId,
                TplOrderGenerateConstants.NOT_DEFAULT_DS_ID);
        Movement movementOutside = createAndSaveMovement(outsideEnabledRegionId,
                TplOrderGenerateConstants.NOT_DEFAULT_DS_ID);

        //when
        List<OrderRegionDto> ordersByRegion =
                deliveryRegionInfoService.getOrdersForSc(
                        null,
                        OrdersRegionFilterEnum.OUTSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                        LocalTime.MIN, LocalTime.MAX, scForDsId.getId());

        //then
        assertThat(ordersByRegion).hasSize(2);

        assertThat(ordersByRegion
                .stream()
                .map(OrderRegionDto::getRegionEnabled)
                .anyMatch(Boolean.FALSE::equals)).isFalse();

        Set<String> resultExternalIds = ordersByRegion
                .stream()
                .map(OrderRegionDto::getExternalOrderId)
                .collect(Collectors.toSet());

        assertThat(resultExternalIds)
                .contains(orderForOtherRegion.getExternalOrderId(), movementOutside.getExternalId());
    }

    @Test
    void getOrdersForSC_whenOutsideDisabled() {
        //given
        long dsId = TplOrderGenerateConstants.NOT_DEFAULT_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, 20279, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);

        //Добавляем Order и Movement для проверяемой СД в этом регионе, который не обслуживается ниодной СД
        //Ожидаем, что Order и Movement будут в списке OUTSIDE и иметь значение enabledRegion = false
        int outsideDisabledRegionId = 110295;
        Order orderForOtherDisabledRegion = createOrderForDsWithRegion(outsideDisabledRegionId,
                TplOrderGenerateConstants.NOT_DEFAULT_DS_ID);
        Movement movementOutsideDisable = createAndSaveMovement(outsideDisabledRegionId,
                TplOrderGenerateConstants.NOT_DEFAULT_DS_ID);


        //when
        List<OrderRegionDto> ordersByRegion =
                deliveryRegionInfoService.getOrdersForSc(
                        null,
                        OrdersRegionFilterEnum.OUTSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                        LocalTime.MIN, LocalTime.MAX, scForDsId.getId());

        //then
        assertThat(ordersByRegion).hasSize(2);

        assertThat(ordersByRegion
                .stream()
                .filter(orderRegionDto -> DROPSHIP != orderRegionDto.getOrderType())
                .map(OrderRegionDto::getRegionEnabled)
                .anyMatch(Boolean.TRUE::equals)).isFalse();

        Set<String> resultExternalIds = ordersByRegion
                .stream()
                .map(OrderRegionDto::getExternalOrderId)
                .collect(Collectors.toSet());

        assertThat(resultExternalIds)
                .contains(orderForOtherDisabledRegion.getExternalOrderId(), movementOutsideDisable.getExternalId());
    }


    @Test
    void getOrdersByRegionRecursively() {
        when(configurationProviderAdapter.isBooleanEnabled(IS_YT_TAKEN_ZONES_ENABLED)).thenReturn(true);
        Collection<OrderRegionDto> result = deliveryRegionInfoService.getOrdersByRegion(1, null,
                OrdersRegionFilterEnum.DO_NOT_SHOW, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result).isEmpty();
        result = deliveryRegionInfoService.getOrdersByRegion(1, null,
                OrdersRegionFilterEnum.OUTSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(4);
        assertThat(deliveryServiceRegionRepository.findAll().stream().filter(e -> e.getRegionId()
                == 117065).count()).isEqualTo(0L);

        Long dsId = DeliveryService.FAKE_DS_ID;
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, 117065, true, true, ?, ?, ?, 1, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock));
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, 117065, true, true, ?, ?, ?, 1, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock).minusDays(1));
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, 117065, true, true, ?, ?, ?, 1, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock).minusDays(2));
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, 120542, true, true, ?, ?, ?, 1, 1)",
                dsId, LocalDateTime.now(clock).minusDays(2), LocalDateTime.now(clock).minusDays(2),
                LocalDate.now(clock).minusDays(2));
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, 213, true, true, ?, ?, ?, 1, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock).minusDays(1));
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, 20279, true, true, ?, ?, ?, 1, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock).minusDays(1));
        jdbcTemplate.update("INSERT into user_region (user_id, region_id) " +
                "VALUES (?, 117065)", user.getId());
        result = deliveryRegionInfoService.getOrdersByRegion(1, null,
                OrdersRegionFilterEnum.OUTSIDE,
                LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(0);

        result = deliveryRegionInfoService.getOrdersByRegion(1, null,
                OrdersRegionFilterEnum.INSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(3);

        OrderRegionDto orderTodayRegionDto = result.stream().filter(e -> e.getExternalOrderId()
                .equals(orderToday.getExternalOrderId())).findFirst().get();
        assertThat(orderTodayRegionDto.getLatitude()).isEqualTo(orderToday.getDelivery()
                .getDeliveryAddress().getLatitude());
        assertThat(orderTodayRegionDto.getIntervalFrom()).isEqualTo(orderToday.getDelivery().getDeliveryIntervalFrom());
        assertThat(orderTodayRegionDto.getIntervalTo()).isEqualTo(orderToday.getDelivery().getDeliveryIntervalTo());
        assertThat(result.stream().filter(e -> e.getExternalOrderId()
                .equals(orderMinus3DaysRegion39.getExternalOrderId())).findFirst())
                .isEmpty();
        assertThat(result.stream().filter(e -> e.getOrderType() == OrderTypeFilter.PVZ)
                .count()).isEqualTo(1);
        List<OrderTypeFilter> orderTypeFilters = new ArrayList<>();
        orderTypeFilters.add(OrderTypeFilter.LOCKER);
        result = deliveryRegionInfoService.getOrdersByRegion(1, orderTypeFilters,
                OrdersRegionFilterEnum.INSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        OrderRegionDto lockerOrderRegionDto = result.stream().filter(e -> e.getExternalOrderId()
                .equals(lockerOrder.getExternalOrderId())).findFirst().get();
        assertThat(lockerOrderRegionDto.getOrderType()).isEqualTo(OrderTypeFilter.LOCKER);
        assertThat(lockerOrderRegionDto.getPickupPointInfo()).isNotNull();
        assertThat(result.stream().filter(e -> e.getOrderType() == OrderTypeFilter.CLIENT)
                .count()).isEqualTo(1);
        assertThat(result.stream().filter(e -> e.getOrderType() == OrderTypeFilter.PVZ)
                .count()).isEqualTo(0);
        result = deliveryRegionInfoService.getOrdersByRegion(1, null,
                OrdersRegionFilterEnum.INSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                LocalTime.of(7, 0), LocalTime.of(23, 30, 0), Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(3);
        result = deliveryRegionInfoService.getOrdersByRegion(1, orderTypeFilters,
                OrdersRegionFilterEnum.INSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock).plusDays(2),
                LocalTime.of(7, 0), LocalTime.of(23, 30, 0), Set.of(DeliveryService.FAKE_DS_ID));
        lockerOrderRegionDto = result.stream().filter(e -> e.getExternalOrderId()
                .equals(lockerOrder.getExternalOrderId())).findFirst().get();
        assertThat(result.size()).isEqualTo(2);
        assertThat(lockerOrderRegionDto.getOrderType()).isEqualTo(OrderTypeFilter.LOCKER);

        orderTypeFilters.remove(OrderTypeFilter.LOCKER);
        result = deliveryRegionInfoService.getOrdersByRegion(120542, orderTypeFilters,
                OrdersRegionFilterEnum.INSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock).plusDays(2),
                LocalTime.of(7, 0), LocalTime.of(10, 00, 0), Set.of(DeliveryService.FAKE_DS_ID));

        assertThat(result.size()).isEqualTo(0);

        result = deliveryRegionInfoService.getOrdersByRegion(120542, orderTypeFilters,
                OrdersRegionFilterEnum.INSIDE, LocalDate.now(clock).minusDays(1), LocalDate.now(clock).plusDays(2),
                LocalTime.of(16, 0), LocalTime.of(18, 0, 0), Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    void getRegionInfoWithStatistics() {
        Long dsId = ((Partner) partnerRepository.findAll().get(0)).getId();
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id) " +
                        "VALUES (?, 117065, true, true, ?, ?, ?, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock));
        jdbcTemplate.update("INSERT into user_region (user_id, region_id) " +
                        "VALUES (?, 117065)",
                user.getId());
        var res = deliveryRegionInfoService.getRegionInfoWithStatistics(117065, null,
                null, (Set) null);
        assertThat(res.getRegionOrdersDto().getOrdersCount()).isEqualTo(3);
        assertThat(res.getCouriers().stream().filter(e -> e.getBindingType() == BindingTypeDto.SOFT).count()).isEqualTo(1);
        assertThat(res.getCouriers().stream().filter(e -> e.getBindingType() == BindingTypeDto.STRONG).count()).isEqualTo(0);
        assertThat(res.getCouriers().stream().findFirst().get().getName()).isEqualTo(user.getName());

        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                        "VALUES (?, 117065, 'STRONG')",
                user2.getId());

        res = deliveryRegionInfoService.getRegionInfoWithStatistics(117065, null,
                null, (Set) null);
        assertThat(res.getCouriers().stream().filter(e -> e.getBindingType() == BindingTypeDto.SOFT).count()).isEqualTo(1);
        assertThat(res.getCouriers().stream().filter(e -> e.getBindingType() == BindingTypeDto.STRONG).count()).isEqualTo(1);

        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id) " +
                        "VALUES (?, 120542, true, true, ?, ?, ?, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock));
        res = deliveryRegionInfoService.getRegionInfoWithStatistics(20279, null,
                null, (Set) null);
        assertThat(res.getCouriers()).isNull();
        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                        "VALUES (?, 120542, 'STRONG')",
                user.getId());
        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                        "VALUES (?, 120542, 'STRONG')",
                user2.getId());
        res = deliveryRegionInfoService.getRegionInfoWithStatistics(20279, null,
                null, (Set) null);
        assertThat(res.getCouriers().stream().filter(e -> e.getBindingType() == BindingTypeDto.SOFT).count()).isEqualTo(0);
        assertThat(res.getCouriers().stream().filter(e -> e.getBindingType() == BindingTypeDto.STRONG).count()).isEqualTo(1);
    }

    @Test
    void getRegionInfoWithStatisticsForGroupOfCityForPassedDate() {
        Long dsId = ((Partner) partnerRepository.findAll().get(0)).getId();
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id) " +
                        "VALUES (?, 39, true, true, ?, ?, ?, 1)",
                dsId, LocalDateTime.now(clock).minusDays(3), LocalDateTime.now(clock).minusDays(3),
                LocalDate.now(clock).minusDays(3));
        jdbcTemplate.update("INSERT into user_region (user_id, region_id) " +
                        "VALUES (?, 39)",
                user.getId());
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id) " +
                        "VALUES (?, 117065, true, true, ?, ?, ?, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock));
        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                        "VALUES (?, 117065, 'SOFT')",
                user.getId());
        var res = deliveryRegionInfoService.getRegionInfoWithStatistics(121146, LocalDate.now(clock).minusDays(3),
                LocalDate.now(clock).minusDays(3), (Set) null);
        assertThat(res.getRegionOrdersDto().getOrdersCount()).isEqualTo(1);
        assertThat(res.getCouriers().stream().findFirst().get().getName()).isEqualTo(user.getName());
    }

    @Test
    void getRegionLayersRecursivelyWithBindingType() {
        //given
        long dsId = DeliveryService.FAKE_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        //Not expected Region for another DS
        addDeliveryServiceRegion(DEFAULT_DS_ID, NOT_EXPECTED_IN_RESULT_REGION_ID, LocalDateTime.now(clock),
                LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id) " +
                        "VALUES (?, 117065, true, true, ?, ?, ?, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock));
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id) " +
                        "VALUES (?, 120542, true, true, ?, ?, ?, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock));
        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                        "VALUES (?, 117065, 'SOFT')",
                user.getId());


        //when
        PartnerCourierZonesResponseDto regionLayers = deliveryRegionInfoService.getRegionLayers(
                SECONDARY_DISTRICT_REGION_VILLAGE, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId());
        Integer regionIdWithCourier = 117065;

        //then
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(regionIdWithCourier))
                .map(RegionInfoDto::getCouriersSoftBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(1);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(regionIdWithCourier))
                .map(RegionInfoDto::getCouriersStrongBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(0);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(regionIdWithCourier))
                .map(RegionInfoDto::getFullCouriersCount).reduce(Integer::sum).orElse(0)).isEqualTo(1);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(regionIdWithCourier))
                .map(RegionInfoDto::getCouriersCount).reduce(Integer::sum).orElse(0)).isEqualTo(1);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().compareTo(regionIdWithCourier) != 0)
                .map(RegionInfoDto::getCouriersSoftBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(0);


        regionLayers = deliveryRegionInfoService.getRegionLayers(
                CITY_DISTRICT_RURAL, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId());
        Integer cityRuralIdWithCourier = 20279;

        //then
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(cityRuralIdWithCourier))
                .map(RegionInfoDto::getCouriersSoftBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(0L);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(cityRuralIdWithCourier))
                .map(RegionInfoDto::getCouriersStrongBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(0L);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(cityRuralIdWithCourier))
                .map(RegionInfoDto::getFullCouriersCount).reduce(Integer::sum).orElse(0)).isEqualTo(1L);


        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                        "VALUES (?, 120542, 'STRONG')",
                user.getId());
        regionLayers = deliveryRegionInfoService.getRegionLayers(
                SECONDARY_DISTRICT_REGION_VILLAGE, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId());

        //then
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(regionIdWithCourier))
                .map(RegionInfoDto::getCouriersSoftBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(1);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(regionIdWithCourier))
                .map(RegionInfoDto::getCouriersStrongBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(0);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(regionIdWithCourier))
                .map(RegionInfoDto::getFullCouriersCount).reduce(Integer::sum).orElse(0)).isEqualTo(1);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(regionIdWithCourier))
                .map(RegionInfoDto::getCouriersCount).reduce(Integer::sum).orElse(0)).isEqualTo(1);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().compareTo(regionIdWithCourier) != 0)
                .map(RegionInfoDto::getCouriersSoftBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(0);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().compareTo(regionIdWithCourier) != 0)
                .map(RegionInfoDto::getCouriersStrongBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(1);

        regionLayers = deliveryRegionInfoService.getRegionLayers(
                CITY_DISTRICT_RURAL, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId());
        //then
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(cityRuralIdWithCourier))
                .map(RegionInfoDto::getCouriersSoftBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(0L);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(cityRuralIdWithCourier))
                .map(RegionInfoDto::getCouriersStrongBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(0L);
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(cityRuralIdWithCourier))
                .map(RegionInfoDto::getFullCouriersCount).reduce(Integer::sum).orElse(0)).isEqualTo(1L);

        jdbcTemplate.update("UPDATE user_region " +
                        "SET binding_type = null " +
                        "where region_id = 120542 and user_id = ?",
                user.getId());
        regionLayers = deliveryRegionInfoService.getRegionLayers(
                CITY_DISTRICT_RURAL, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId());
        assertThat(regionLayers.getZones().stream().filter(e -> e.getId().equals(cityRuralIdWithCourier))
                .map(RegionInfoDto::getCouriersSoftBindingCount).reduce(Integer::sum).orElse(0)).isEqualTo(1L);
    }

    @Test
    void getRegionLayersRecursivelyWithBindingType_whenChildrenRegions() {
        //given
        long dsId = DeliveryService.FAKE_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, 100120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 0);
        addDeliveryServiceRegion(dsId, 100120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 0);
        addDeliveryServiceRegion(dsId, 120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);

        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                        "VALUES (?, 100120542, 'SOFT')",
                user.getId());
        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                "VALUES (?, 120542, 'SOFT')", user.getId());

        //when
        PartnerCourierZonesResponseDto regionLayers = deliveryRegionInfoService.getRegionLayers(
                DetailingEnum.CITY_FEDERATION_DISTRICT, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId());


        RegionInfoDto regionInfoWithStatistics = deliveryRegionInfoService.getRegionInfoWithStatistics(
                213, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId());

        //then
        assertThat(regionLayers.getZones()).hasSize(1);
        RegionInfoDto regionInfoDto = regionLayers.getZones().iterator().next();

        assertThat(regionInfoDto.getCouriersCount()).isEqualTo(regionInfoWithStatistics.getCouriersCount());
        assertThat(regionInfoDto.getCouriersSoftBindingCount()).isEqualTo(regionInfoWithStatistics.getCouriersSoftBindingCount());
        assertThat(regionInfoDto.getCouriersStrongBindingCount()).isEqualTo(regionInfoWithStatistics.getCouriersStrongBindingCount());
    }

    @Test
    void getRegionLayersRecursivelyV2_when_RequestSecondaryDistrictDetailing() {
        //given
        //All tested orders by default were created with FAKE_DS_ID delivery service.
        long dsId = DeliveryService.FAKE_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        //Not expected Region for another DS
        addDeliveryServiceRegion(DEFAULT_DS_ID, NOT_EXPECTED_IN_RESULT_REGION_ID, LocalDateTime.now(clock),
                LocalDateTime.now(clock),
                LocalDate.now(clock), 1);


        //when
        PartnerCourierZonesResponseDto regionLayers = deliveryRegionInfoService.getRegionLayers(
                SECONDARY_DISTRICT_REGION_VILLAGE, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId());

        //then
        assertThat(regionLayers.getZones().size()).isEqualTo(2);

        assertFalse(regionLayers.getZones()
                .stream()
                .anyMatch(zone -> NOT_EXPECTED_IN_RESULT_REGION_ID.equals(zone.getId())));

        RegionInfoDto dto = regionLayers.getZones()
                .stream()
                .filter(zone -> DEFAULT_REGION_ID.equals(zone.getId()))
                .findFirst().get();
        assertThat(dto.getRegionOrdersDto().getOrdersCount()).isEqualTo(3);
        assertThat(dto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.PVZ)).isEqualTo(1);
        assertThat(dto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.LOCKER)).isEqualTo(1);
        assertThat(dto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.CLIENT)).isEqualTo(1);

        //asserts for Center Position
        assertThat(regionLayers.getCenterPosition().getLatitude()).isEqualTo(scForDsId.getLatitude());
        assertThat(regionLayers.getCenterPosition().getLongitude()).isEqualTo(scForDsId.getLongitude());
    }


    @Test
    void getRegionLayersRecursivelyV2_when_RequestCityDistrictDetailing() {
        //given
        //All tested orders by default were created with FAKE_DS_ID delivery service.
        long dsId = DeliveryService.FAKE_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);


        //when
        Collection<RegionInfoDto> result = deliveryRegionInfoService.getRegionLayers(
                CITY_DISTRICT_RURAL, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId())
                .getZones();

        //then
        assertThat(result.size()).isEqualTo(1);
        RegionInfoDto dto = result.stream().findFirst().get();
        assertThat(dto.getRegionOrdersDto().getOrdersCount()).isEqualTo(3);
        assertThat(dto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.PVZ)).isEqualTo(1);
        assertThat(dto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.LOCKER)).isEqualTo(1);
        assertThat(dto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.CLIENT)).isEqualTo(1);
    }

    @Test
    void getRegionLayersRecursivelyV2_when_RequestCityFederationDetailing() {
        //given
        //All tested orders by default were created with FAKE_DS_ID delivery service.
        long dsId = DeliveryService.FAKE_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);


        //when
        Collection<RegionInfoDto> result = deliveryRegionInfoService.getRegionLayers(
                CITY_FEDERATION_DISTRICT, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId())
                .getZones();

        //then
        assertThat(result.size()).isEqualTo(1);
        RegionInfoDto dto = result.stream().findFirst().get();
        assertThat(dto.getRegionOrdersDto().getOrdersCount()).isEqualTo(4);
        assertThat(dto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.PVZ)).isEqualTo(1);
        assertThat(dto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.LOCKER)).isEqualTo(1);
        assertThat(dto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.CLIENT)).isEqualTo(2);
    }

    @Test
    void getRegionLayersRecursivelyV2_when_RequestCityFederationDetailing_Overlapped() {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(ZONES_OVERLAPPED_MODE_ENABLED)).thenReturn(true);
        //All tested orders by default were created with FAKE_DS_ID delivery service.
        long dsId = DeliveryService.FAKE_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 20674, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 10731, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        createOrderForDsWithRegion(20674, dsId);

        //when
        Collection<RegionInfoDto> result = deliveryRegionInfoService.getRegionLayers(
                CITY_FEDERATION_DISTRICT, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId())
                .getZones();

        //then
        assertThat(result.size()).isEqualTo(6);
        RegionInfoDto mskDto = result.stream()
                .filter(regionInfoDto -> regionInfoDto.getId().equals(213))
                .findFirst().get();
        assertThat(mskDto.getRegionOrdersDto().getOrdersCount()).isEqualTo(5);
        assertThat(mskDto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.PVZ)).isEqualTo(1);
        assertThat(mskDto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.LOCKER)).isEqualTo(1);
        assertThat(mskDto.getRegionOrdersDto().getOrdersCountByType().get(OrderType.CLIENT)).isEqualTo(3);

        //Проверяем что Москва(213),
        // Троицкий АО(114620)
        // Центральный административный округ Москвы (20279)
        // округ Истра (98586)  есть в результатах в определенном порядке
        // г. Истра(10731)  есть в результатах в определенном порядке
        // г. Троицк(20674)  есть в результатах в определенном порядке
        //213
        Map<Integer, Integer> regionIndexes = new HashMap<>();
        int i = 0;
        for (RegionInfoDto regionInfoDto : result) {
            Integer regionId = regionInfoDto.getId();
            regionIndexes.put(regionId, i++);
        }

        assertThat(regionIndexes).containsKeys(213, 98586, 10731, 20674, 114620);
        assertThat(regionIndexes.get(213)).isLessThan(regionIndexes.get(114620));
        assertThat(regionIndexes.get(114620)).isLessThan(regionIndexes.get(20674));
        assertThat(regionIndexes.get(98586)).isLessThan(regionIndexes.get(10731));
    }

    @Test
    void getRegionLayersRecursivelyV2_when_RequestCityFederationDetailing_EmptyRegionDS() {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(ZONES_OVERLAPPED_MODE_ENABLED)).thenReturn(true);
        //All tested orders by default were created with FAKE_DS_ID delivery service.
        long dsId = DeliveryService.FAKE_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        //when
        Collection<RegionInfoDto> result = deliveryRegionInfoService.getRegionLayers(
                CITY_FEDERATION_DISTRICT, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId())
                .getZones();

        //then
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    void getRegionLayersRecursivelyV2_when_RequestCityFederationDetailing_Overlapped_TypesProperties() {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(ZONES_OVERLAPPED_MODE_ENABLED)).thenReturn(true);
        when(configurationProviderAdapter.getValueAsIntegers(ZONES_CITY_DETALIZATION_REGION_TYPE_CODES))
                .thenReturn(Set.of(RegionType.CITY.getCode()));
        //All tested orders by default were created with FAKE_DS_ID delivery service.
        long dsId = DeliveryService.FAKE_DS_ID;
        SortingCenter scForDsId = sortingCenterService.findSortCenterForDs(dsId);

        addDeliveryServiceRegion(dsId, DEFAULT_REGION_ID, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 120542, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        addDeliveryServiceRegion(dsId, 20674, LocalDateTime.now(clock), LocalDateTime.now(clock),
                LocalDate.now(clock), 1);
        createOrderForDsWithRegion(20674, dsId);

        //when
        Collection<RegionInfoDto> result = deliveryRegionInfoService.getRegionLayers(
                CITY_FEDERATION_DISTRICT, LocalDate.now(clock).minusDays(1),
                LocalDate.now(clock),
                scForDsId.getId())
                .getZones();

        //then
        assertThat(result.size()).isEqualTo(2);
        assertFalse(result
                .stream()
                .anyMatch(regionInfoDto -> regionInfoDto.getType() != RegionType.CITY.getCode()));
    }

    @Test
    void getOrdersByRegionTakenRegionByTableRegion() {
        when(configurationProviderAdapter.isBooleanEnabled(IS_YT_TAKEN_ZONES_ENABLED)).thenReturn(true);
        Long dsId = DeliveryService.FAKE_DS_ID;
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, 117065, true, true, ?, ?, ?, 1, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock));

        Collection<OrderRegionDto> result = deliveryRegionInfoService.getOrdersByRegion(1, null,
                OrdersRegionFilterEnum.ALL,
                LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(4);

        result = deliveryRegionInfoService.getOrdersByRegion(1, null,
                OrdersRegionFilterEnum.ALL,
                LocalDate.now(clock).minusDays(1), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(4);

        result = deliveryRegionInfoService.getOrdersByRegion(1, List.of(OrderTypeFilter.CLIENT),
                OrdersRegionFilterEnum.ALL,
                LocalDate.now(clock), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(2);

        result = deliveryRegionInfoService.getOrdersByRegion(1, null,
                OrdersRegionFilterEnum.INSIDE,
                LocalDate.now(clock), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(3);

        result = deliveryRegionInfoService.getOrdersByRegion(1, List.of(OrderTypeFilter.CLIENT),
                OrdersRegionFilterEnum.INSIDE,
                LocalDate.now(clock), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(1);

        result = deliveryRegionInfoService.getOrdersByRegion(1, null,
                OrdersRegionFilterEnum.OUTSIDE,
                LocalDate.now(clock), LocalDate.now(clock),
                LocalTime.MIN, LocalTime.MAX, Set.of(DeliveryService.FAKE_DS_ID));
        assertThat(result.size()).isEqualTo(1);
    }


    private Order createOrderForDsWithRegion(Integer regionId, Long deliveryServiceId) {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(deliveryServiceId)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryDate(LocalDate.now(clock))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(regionId)
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .build());
    }

    private void addDeliveryServiceRegion(long dsId, int regionId, LocalDateTime createdAt,
                                          LocalDateTime updatedAt, LocalDate createdDate, int dataVersion) {
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, ?, true, true, ?, ?, ?, 1, ?)",
                dsId, regionId, createdAt, updatedAt, createdDate, dataVersion);
    }

    private void linkUserToRegion(long userId, int regionId) {
        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                "VALUES (?, ?, 'SOFT')", userId, regionId);
    }

    private Movement createAndSaveMovement(Integer regionId, Long dsId) {
        return movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(dsId)
                .orderWarehouse(orderWarehouseRepository.saveAndFlush(OrderWarehouse.builder()
                        .regionId(regionId)
                        .yandexId("123")
                        .incorporation("corp")
                        .address(addressGenerator.generateWarehouseAddress(
                                AddressGenerator.AddressGenerateParam.builder()
                                        .street("Пушкина")
                                        .house("Колотушкина")
                                        .apartment("10")
                                        .floor(1)
                                        .build()
                        ))

                        .phones(List.of("223322223322"))
                        .description("Спросить старшего")
                        .contact("Иван Дропшипов")
                        .build()
                ))
                .build());
    }
}
