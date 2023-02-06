package ru.yandex.market.tpl.core.external.routing.vrp.mapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourierVehicleType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingScheduleData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingTimeMultiplier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingTimeMultiplierUtil;
import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpRequestVehicles;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.HardcodedVrpSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.VrpSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettingsProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.MIX_DROPSHIP_WITH_DELIVERY_ENABLED;


@ExtendWith(MockitoExtension.class)
class MvrpVehicleTagsMapperUnitTest {

    public static final RoutingTimeMultiplier DEFAULT_CAR = new RoutingTimeMultiplier(
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE
    );

    public static final long EXISTED_SC_ID = 1L;
    @SuppressWarnings("unused") // иначе падаем с npe
    @Mock
    private GlobalSettingsProvider globalSettingsProvider;
    @Mock
    private DepotSettingsProvider depotSettingsProvider;
    @Mock
    private Clock clock;

    private MvrpVehicleTagsMapper mvrpVehicleTagsMapper;
    private MvrpVehicleMapper mvrpVehicleMapper;

    @BeforeEach
    void before() {
        doReturn(Instant.now())
                .when(clock).instant();

        mvrpVehicleTagsMapper = new MvrpVehicleTagsMapper(depotSettingsProvider);
        mvrpVehicleMapper = new MvrpVehicleMapper(
                clock,
                globalSettingsProvider,
                mvrpVehicleTagsMapper,
                depotSettingsProvider
        );
    }

    @AfterEach
    void after() {
        Mockito.reset(clock);
    }

    @Test
    void when_CargoTypeEnabled_BulkyCargoVehicleOnlyBulkyCargoLocationEnabled() {
        //given
        RoutingScheduleData userScheduleData = new RoutingScheduleData(RoutingCourierVehicleType.CAR,
                RelativeTimeInterval.valueOf("09:00-19:00"));
        RoutingCourier bulkyCargoCourier = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .scheduleData(userScheduleData)
                .routingTimeMultiplier(DEFAULT_CAR)
                .additionalTags(Set.of(RequiredRoutingTag.BULKY_CARGO.getCode()))
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();

        RoutingCourier noBulkyCargoCourier = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .scheduleData(userScheduleData)
                .routingTimeMultiplier(DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(CARGO_TYPE_REQUIRED_TAG_ENABLED), eq(EXISTED_SC_ID))
        ).thenReturn(true);

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED), eq(EXISTED_SC_ID))
        ).thenReturn(true);

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(MIX_DROPSHIP_WITH_DELIVERY_ENABLED), eq(EXISTED_SC_ID))
        ).thenReturn(true);

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        //when
        MvrpRequestVehicles bulkyCargoVehicle = mvrpVehicleMapper.mapToVehicle(bulkyCargoCourier, settings,
                VrpClient.ApiType.SVRP, false);

        MvrpRequestVehicles noBulkyCargoVehicle = mvrpVehicleMapper.mapToVehicle(noBulkyCargoCourier, settings,
                VrpClient.ApiType.SVRP, false);

        //then bulky
        assertThat(bulkyCargoVehicle.getTags()).contains("bulky_cargo");
        assertThat(bulkyCargoVehicle.getTags()).doesNotContain("no_bulky_cargo");
        assertThat(bulkyCargoVehicle.getExcludedTags()).contains("no_bulky_cargo");
        assertThat(bulkyCargoVehicle.getExcludedTags()).doesNotContain("bulky_cargo");

        //then noBulky
        assertThat(noBulkyCargoVehicle.getTags()).contains("no_bulky_cargo");
        assertThat(noBulkyCargoVehicle.getTags()).doesNotContain("bulky_cargo");
        assertThat(noBulkyCargoVehicle.getExcludedTags()).isEmpty();

    }

    @Test
    void when_isDropOffReturnCourier_mixReturnsDisabled() {
        //given
        RoutingCourier dropOffCourier = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .additionalTags(Set.of(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode()))
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();


        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.MIX_DROPOFF_RETURN_WITH_ANOTHER_TASKS_ENABLED), eq(EXISTED_SC_ID))).thenReturn(false);

        //when
        List<String> excludedTags = mvrpVehicleTagsMapper.mapExcludedTags(dropOffCourier);

        //then
        assertThat(excludedTags).containsExactlyInAnyOrderElementsOf(StreamEx.of(RequiredRoutingTag.values())
                .filter(Predicate.not(RequiredRoutingTag.DROPOFF_CARGO_RETURN::equals))
                .map(RequiredRoutingTag::getCode)
                .collect(Collectors.toSet()));
        assertThat(excludedTags).doesNotContain(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode());
    }

    @Test
    void when_isDropOffReturnCourier_mixReturnsEnabled_DropoffDisabled() {
        //given
        RoutingCourier dropOffCourier = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .additionalTags(Set.of(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode()))
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .servicedLocationType(RoutingLocationType.delivery)
                .build();


        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.MIX_DROPOFF_RETURN_WITH_ANOTHER_TASKS_ENABLED), eq(EXISTED_SC_ID))).thenReturn(true);

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.MIX_DROPSHIP_WITH_DELIVERY_ENABLED), eq(EXISTED_SC_ID))).thenReturn(false);

        //when
        List<String> excludedTags = mvrpVehicleTagsMapper.mapExcludedTags(dropOffCourier);

        //then
        assertThat(excludedTags).containsExactlyInAnyOrderElementsOf(Set.of(RequiredRoutingTag.DROPSHIP.getCode()));
    }

    @Test
    void when_isDropOffReturnCourier_mixBothEnabled() {
        //given
        RoutingCourier dropOffCourier = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .additionalTags(Set.of(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode()))
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();


        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.MIX_DROPOFF_RETURN_WITH_ANOTHER_TASKS_ENABLED), eq(EXISTED_SC_ID))).thenReturn(true);

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.MIX_DROPSHIP_WITH_DELIVERY_ENABLED), eq(EXISTED_SC_ID))).thenReturn(true);

        //when
        List<String> excludedTags = mvrpVehicleTagsMapper.mapExcludedTags(dropOffCourier);

        //then
        assertThat(excludedTags).isEmpty();
    }

    @Test
    void when_isDropOffReturnCourier_mixEnabled_CargoTypeEnabled() {
        //given
        RoutingCourier dropOffCourier = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .additionalTags(Set.of(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode()))
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();


        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.MIX_DROPOFF_RETURN_WITH_ANOTHER_TASKS_ENABLED), eq(EXISTED_SC_ID))).thenReturn(true);

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.MIX_DROPSHIP_WITH_DELIVERY_ENABLED), eq(EXISTED_SC_ID))).thenReturn(false);

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED), eq(EXISTED_SC_ID))).thenReturn(true);

        //when
        List<String> excludedTags = mvrpVehicleTagsMapper.mapExcludedTags(dropOffCourier);

        //then
        assertThat(excludedTags).containsExactlyInAnyOrderElementsOf(
                Set.of(
                        RequiredRoutingTag.DELIVERY.getCode(),
                        RequiredRoutingTag.BULKY_CARGO.getCode()
                )
        );
    }

    @Test
    void when_isDropOffReturnCourier_mixEnabled_CargoTypeDisabled() {
        //given
        RoutingCourier dropOffCourier = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .additionalTags(Set.of(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode()))
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();


        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.MIX_DROPOFF_RETURN_WITH_ANOTHER_TASKS_ENABLED), eq(EXISTED_SC_ID))).thenReturn(true);

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.MIX_DROPSHIP_WITH_DELIVERY_ENABLED), eq(EXISTED_SC_ID))).thenReturn(false);

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED), eq(EXISTED_SC_ID))).thenReturn(false);

        //when
        List<String> excludedTags = mvrpVehicleTagsMapper.mapExcludedTags(dropOffCourier);

        //then
        assertThat(excludedTags).containsExactlyInAnyOrderElementsOf(
                Set.of(RequiredRoutingTag.DELIVERY.getCode())
        );
    }

    @Test
    void when_VisitDepotAtStartEnabled() {
        //given
        RoutingScheduleData userScheduleData = new RoutingScheduleData(RoutingCourierVehicleType.CAR,
                RelativeTimeInterval.valueOf("09:00-19:00"));
        RoutingCourier courierWithDelivery = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .scheduleData(userScheduleData)
                .routingTimeMultiplier(RoutingTimeMultiplierUtil.DEFAULT_CAR)
                .additionalTags(Set.of(RequiredRoutingTag.CLIENT.getCode(), RequiredRoutingTag.DELIVERY.getCode()))
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();

        RoutingCourier courierWithoutDelivery = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .scheduleData(userScheduleData)
                .routingTimeMultiplier(RoutingTimeMultiplierUtil.DEFAULT_CAR)
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();


        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED), eq(EXISTED_SC_ID))
        ).thenReturn(false);

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(MIX_DROPSHIP_WITH_DELIVERY_ENABLED), eq(EXISTED_SC_ID))
        ).thenReturn(true);

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        //when
        MvrpRequestVehicles vehicleWithDelivery = mvrpVehicleMapper.mapToVehicle(courierWithDelivery, settings,
                VrpClient.ApiType.SVRP, false);

        MvrpRequestVehicles vehicleWithoutDelivery = mvrpVehicleMapper.mapToVehicle(courierWithoutDelivery, settings,
                VrpClient.ApiType.SVRP, false);

        //then with delivery
        assertThat(vehicleWithDelivery.getVisitDepotAtStart()).isTrue();

        //then without delivery
        assertThat(vehicleWithoutDelivery.getVisitDepotAtStart()).isFalse();

    }

    @Test
    @DisplayName("Если тип клиента SVRP и это не рероут, то не проставляем PlannedRouteTime")
    void shouldNotSetPlannedRouteTimeIfRerouteIsFalse() {
        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(MIX_DROPSHIP_WITH_DELIVERY_ENABLED), eq(EXISTED_SC_ID))
        ).thenReturn(true);

        RoutingScheduleData userScheduleData = new RoutingScheduleData(RoutingCourierVehicleType.CAR,
                RelativeTimeInterval.valueOf("09:00-19:00"));
        RoutingCourier courierWithDelivery = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(EXISTED_SC_ID)
                .scheduleData(userScheduleData)
                .routingTimeMultiplier(RoutingTimeMultiplierUtil.DEFAULT_CAR)
                .additionalTags(Set.of(RequiredRoutingTag.CLIENT.getCode(), RequiredRoutingTag.DELIVERY.getCode()))
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of("1", "2"))
                .build();

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                eq(DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED), eq(EXISTED_SC_ID))
        ).thenReturn(false);

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        MvrpRequestVehicles vehicleWithPlannedRouteTime = mvrpVehicleMapper.mapToVehicle(courierWithDelivery, settings,
                VrpClient.ApiType.SVRP, true);

        MvrpRequestVehicles vehicleWithoutPlannedRouteTime = mvrpVehicleMapper.mapToVehicle(courierWithDelivery,
                settings,
                VrpClient.ApiType.SVRP, false);

        assertThat(vehicleWithPlannedRouteTime.getPlannedRoute().getStartTime()).isNotNull();
        assertThat(vehicleWithoutPlannedRouteTime.getPlannedRoute().getStartTime()).isNull();
    }
}
