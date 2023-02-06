package ru.yandex.market.tpl.core.external.routing.vrp.mapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.base.property.BooleanDefinition;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.external.routing.RoutingCommonTestUtils;
import ru.yandex.market.tpl.core.external.routing.api.DimensionsClass;
import ru.yandex.market.tpl.core.external.routing.api.RoutingAddress;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourierVehicleType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingDepot;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationGroup;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingScheduleData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingTimeMultiplierUtil;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpRequest;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpRequestVehicles;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.VrpSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.tag.RoutingOrderTagProvider;
import ru.yandex.market.tpl.core.service.routing.TplRoutingUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.MVRP_GROUP_LOCATIONS_BY_HOUSE_DISABLED;

/**
 * @author kukabara
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles("unit-test")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class MvrpRequestMapperTest {

    public static final long EXPECTED_BINDED_REGION_COST_FOR_SC = 1777L;
    public static final long ROUTING_REQUEST_SC_ID = RoutingApiDataHelper.DEPOT_ID;


    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();
    private final DepotSettingsProvider depotSettingsProvider;
    private final GlobalSettingsProvider globalSettingsProvider;
    private final MvrpRequestMapper mvrpRequestMapper;
    private final MvrpVehicleTagsMapper mvrpVehicleTagsMapper;
    private final RoutingOrderTagProvider routingOrderTagProvider;

    @BeforeEach
    void setUp() {
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(Mockito.isA(BooleanDefinition.class), Mockito.any()))
                .thenReturn(false);
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(Mockito.eq(DepotSettings.DROPSHIP_PICKUP_IN_SECONDS), Mockito.any()))
                .thenReturn(1800L);
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(Mockito.eq(DepotSettings.BULKY_CARGO_SHARED_SERVICE_DURATION_IN_SECONDS), Mockito.any()))
                .thenReturn(300L);
    }

    @Test
    void shouldGroupLocationsWithoutHouseInRoutingLocationGroups() {
        var scId = 5L;
        var geoPointScale = 4;
        var house1 = "house1";
        var house2 = "house2";
        String arrival = "10:04:26";
        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(MVRP_GROUP_LOCATIONS_BY_HOUSE_DISABLED,
                scId))
                .thenReturn(true);
        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings
                .MVRP_GROUP_LOCATIONS_BY_BUILDING_ENABLED, scId))
                .thenReturn(false);
        when(globalSettingsProvider.isBooleanEnabled(GlobalSettings.MVRP_USE_GEO_POINT_SCALE_PROP))
                .thenReturn(true);
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.GEO_POINT_SCALE))
                .thenReturn(Optional.of(geoPointScale));

        List<RoutingRequestItem> items = toListOfRoutingRequestItems(
                List.of(
                        routingApiDataHelper.order(RoutingApiDataHelper.LAT, RoutingApiDataHelper.LON, house1,
                                RoutingApiDataHelper.INTERVAL, arrival),
                        routingApiDataHelper.order(RoutingApiDataHelper.LAT, RoutingApiDataHelper.LON, house2,
                                RoutingApiDataHelper.INTERVAL, arrival)
                )
        );

        Map<RoutingLocationGroup, List<String>> locationGroups = mvrpRequestMapper.getLocationGroups(scId, items);

        assertThat(locationGroups).hasSize(1);
        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(RoutingApiDataHelper.INTERVAL),
                        RoutingGeoPoint.ofLatLon(geoPointScale, BigDecimal.valueOf(RoutingApiDataHelper.LAT),
                                BigDecimal.valueOf(RoutingApiDataHelper.LON)),
                        null,
                        null,
                        null,
                        null,
                        null
                )
        )).isNotNull().hasSize(2);
    }

    @Test
    void shouldGroupLocations() {
        var scId = 5L;
        var geoPointScale = 4;
        double lat = 55.70018211;
        double lon = 37.58015822;
        var house1 = "house1";
        var house2 = "house2";
        var building1 = "building1";
        var building2 = "building2";
        var housing = "building1";
        String arrival = "10:04:26";

        when(globalSettingsProvider.isBooleanEnabled(GlobalSettings.MVRP_USE_GEO_POINT_SCALE_PROP))
                .thenReturn(true);
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.GEO_POINT_SCALE))
                .thenReturn(Optional.of(geoPointScale));

        List<RoutingRequestItem> items = toListOfRoutingRequestItems(
                List.of(
                        routingApiDataHelper.order(lat, lon, house1, building1, housing,
                                RoutingApiDataHelper.INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house1, building2, housing,
                                RoutingApiDataHelper.INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house2, RoutingApiDataHelper.INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house2, RoutingApiDataHelper.INTERVAL, arrival)
                )
        );

        Map<RoutingLocationGroup, List<String>> locationGroups = mvrpRequestMapper.getLocationGroups(scId, items);

        assertThat(locationGroups).hasSize(2);
        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(RoutingApiDataHelper.INTERVAL),
                        RoutingGeoPoint.ofLatLon(
                                geoPointScale, BigDecimal.valueOf(lat), BigDecimal.valueOf(lon)
                        ),
                        house1,
                        null,
                        null,
                        null,
                        null
                )
        )).isNotNull().hasSize(2);
        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(RoutingApiDataHelper.INTERVAL),
                        RoutingGeoPoint.ofLatLon(
                                geoPointScale, BigDecimal.valueOf(lat), BigDecimal.valueOf(lon)
                        ),
                        house2,
                        null,
                        null,
                        null,
                        null
                )
        )).isNotNull().hasSize(2);
    }

    @Test
    void shouldGroupLocationsWithBuilding() {
        var scId = 5L;
        var geoPointScale = 4;
        double lat = 55.70018211;
        double lon = 37.58015822;
        var house1 = "house1";
        var house2 = "house2";
        var building1 = "building1";
        var building2 = "building2";

        var housing = "housing";
        String arrival = "10:04:26";

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(MVRP_GROUP_LOCATIONS_BY_HOUSE_DISABLED,
                scId))
                .thenReturn(false);
        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings
                .MVRP_GROUP_LOCATIONS_BY_BUILDING_ENABLED, scId))
                .thenReturn(true);
        when(globalSettingsProvider.isBooleanEnabled(GlobalSettings.MVRP_USE_GEO_POINT_SCALE_PROP))
                .thenReturn(true);
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.GEO_POINT_SCALE))
                .thenReturn(Optional.of(geoPointScale));

        List<RoutingRequestItem> items = toListOfRoutingRequestItems(
                List.of(
                        routingApiDataHelper.order(lat, lon, house1, building1, housing,
                                RoutingApiDataHelper.INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house1, building1, housing,
                                RoutingApiDataHelper.INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house1, building2, housing,
                                RoutingApiDataHelper.INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house1, building2, housing,
                                RoutingApiDataHelper.INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house2, RoutingApiDataHelper.INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house2, RoutingApiDataHelper.INTERVAL, arrival)
                )
        );

        Map<RoutingLocationGroup, List<String>> locationGroups = mvrpRequestMapper.getLocationGroups(scId, items);

        assertThat(locationGroups).hasSize(3);
        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(RoutingApiDataHelper.INTERVAL),
                        RoutingGeoPoint.ofLatLon(
                                geoPointScale, BigDecimal.valueOf(lat), BigDecimal.valueOf(lon)
                        ),
                        house1,
                        building1,
                        housing,
                        null,
                        null
                )
        )).isNotNull().hasSize(2);

        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(RoutingApiDataHelper.INTERVAL),
                        RoutingGeoPoint.ofLatLon(
                                geoPointScale, BigDecimal.valueOf(lat), BigDecimal.valueOf(lon)
                        ),
                        house1,
                        building2,
                        housing,
                        null,
                        null
                )
        )).isNotNull().hasSize(2);

        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(RoutingApiDataHelper.INTERVAL),
                        RoutingGeoPoint.ofLatLon(
                                geoPointScale, BigDecimal.valueOf(lat), BigDecimal.valueOf(lon)
                        ),
                        house2,
                        null,
                        null,
                        null,
                        null
                )
        )).isNotNull().hasSize(2);
    }

    @Test
    void shouldGroupLocationsWithDefaultGeoPointScaleWhenPropGeoPointScaleDisabled() {
        var scId = 5L;
        var geoPointScale = RoutingGeoPoint.GEO_POINT_SCALE - 2;
        double lat = 55.70018211;
        double lon = 37.58015822;
        String arrival = "10:04:26";

        when(globalSettingsProvider.isBooleanEnabled(GlobalSettings.MVRP_USE_GEO_POINT_SCALE_PROP))
                .thenReturn(false);
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.GEO_POINT_SCALE))
                .thenReturn(Optional.of(geoPointScale));

        Map<RoutingLocationGroup, List<String>> locationGroups = mvrpRequestMapper.getLocationGroups(
                scId,
                toListOfRoutingRequestItems(
                        List.of(
                                routingApiDataHelper.order(lat, lon, null, RoutingApiDataHelper.INTERVAL, arrival),
                                routingApiDataHelper.order(lat, lon, null, RoutingApiDataHelper.INTERVAL, arrival)
                        )
                )
        );

        verify(globalSettingsProvider, never()).getValueAsInteger(GlobalSettings.GEO_POINT_SCALE);

        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(RoutingApiDataHelper.INTERVAL),
                        RoutingGeoPoint.ofLatLon(
                                RoutingGeoPoint.GEO_POINT_SCALE, BigDecimal.valueOf(lat), BigDecimal.valueOf(lon)
                        ),
                        null,
                        null,
                        null,
                        null,
                        null
                )
        )).isNotNull().hasSize(2);
    }

    @Test
    void shouldGroupLocationsByLocationGroup() {
        var scId = 6L;
        double lat = 55.70018211;
        double lon = 37.58015822;

        String arrival = "10:04:26";

        when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                DepotSettings.MVRP_GROUP_LOCATIONS_BY_LOCATION_GROUP_ENABLED, scId
        )).thenReturn(true);

        Map<RoutingLocationGroup, List<String>> locationGroups = mvrpRequestMapper.getLocationGroups(
                scId,
                toListOfRoutingRequestItems(
                        List.of(
                                routingApiDataHelper.order(lat, lon, null, null, null, RoutingApiDataHelper.INTERVAL,
                                        arrival, "abc"),
                                routingApiDataHelper.order(lat, lon, null, null, null, RoutingApiDataHelper.INTERVAL,
                                        arrival, "abc"),
                                routingApiDataHelper.order(lat, lon, null, null, null, RoutingApiDataHelper.INTERVAL,
                                        arrival, null)

                        )
                )
        );

        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(RoutingApiDataHelper.INTERVAL),
                        RoutingGeoPoint.ofLatLon(
                                RoutingGeoPoint.GEO_POINT_SCALE, BigDecimal.valueOf(lat), BigDecimal.valueOf(lon)
                        ),
                        null,
                        null,
                        null,
                        "abc",
                        null
                )
        )).isNotNull().hasSize(2);
    }

    private List<RoutingRequestItem> toListOfRoutingRequestItems(
            List<Supplier<Pair<RoutingRequestItem, LocalTime>>> routingRequestItems
    ) {
        return routingRequestItems
                .stream()
                .map(Supplier::get)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Test
    @SneakyThrows
    void createMvrpRequestWithDimensions() {
        VrpSettings vrpSettings = RoutingCommonTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                VrpSettings.class);
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(LocalDate.parse("2020-05-14"), 10, 2);
        MvrpRequest mvrpRequest = mvrpRequestMapper.createMvrpRequest(
                routingRequest, vrpSettings, VrpClient.ApiType.MVRP, new HashSet<>());

        List<BigDecimal> mvrRequestVehicleCapacity = Optional.ofNullable(mvrpRequest.getVehicles())
                .orElseGet(() -> List.of(mvrpRequest.getVehicle())).stream()
                .map(v -> v.getCapacity().getUnits())
                .collect(Collectors.toList());
        assertThat(mvrRequestVehicleCapacity).containsOnly(
                routingRequest.getUsers().stream().map(RoutingCourier::getVehicleCapacity).toArray(BigDecimal[]::new)
        );

        List<BigDecimal> mvrRequestOrderUnits = mvrpRequest.getLocations().stream()
                .filter(l -> l.getShipmentSize() != null)
                .map(o -> o.getShipmentSize().getUnits()).collect(Collectors.toList());
        assertThat(mvrRequestOrderUnits).containsOnly(
                routingRequest.getItems().stream().map(RoutingRequestItem::getVolume).toArray(BigDecimal[]::new)
        );
    }

    @Test
    @SneakyThrows
    void createMvrpRequestWithExcludeTags() {

        VrpSettings vrpSettings = RoutingCommonTestUtils
                .mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                        VrpSettings.class);
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequestPickup(LocalDate.parse("2020-05-14"),
                10, 2);

        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(CARGO_TYPE_REQUIRED_TAG_ENABLED
                        , 1L))
                .thenReturn(true);

        MvrpRequest mvrpRequest = mvrpRequestMapper.createMvrpRequest(
                routingRequest, vrpSettings, VrpClient.ApiType.MVRP, new HashSet<>());
        assertThat(mvrpRequest.getVehicles().stream().flatMap(e -> e.getExcludedTags().stream())
                .filter("bulky_cargo"::equals).count()).isEqualTo(2L);


        routingRequest = routingApiDataHelper.getRoutingRequest(LocalDate.parse("2020-05-14"), 10, 2);
        MvrpRequest mvrpRequest2 = mvrpRequestMapper.createMvrpRequest(
                routingRequest, vrpSettings, VrpClient.ApiType.MVRP, new HashSet<>());

        assertThat(mvrpRequest2.getVehicles().stream().flatMap(e -> e.getExcludedTags().stream())
                .filter("bulky_cargo"::equals).count()).isEqualTo(0);
    }

    @Test
    @SneakyThrows
    void createMvrpRequestWithVehicleTags_oldVersion() {
        //given
        VrpSettings vrpSettings = RoutingCommonTestUtils
                .mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                        VrpSettings.class);
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequestPickup(LocalDate.parse("2020-05-14"),
                10, 2);

        MvrpRequestVehicles vehicleDefaults = vrpSettings.getVehicleDefaults(-1L, RoutingCourierVehicleType.CAR);
        List<String> tagsFromSettings = Optional.ofNullable(vehicleDefaults)
                .map(MvrpRequestVehicles::getTags)
                .orElseGet(java.util.Collections::emptyList);
        List<String> excludedTagsFromSettings = Optional.ofNullable(vehicleDefaults)
                .map(MvrpRequestVehicles::getExcludedTags)
                .orElseGet(java.util.Collections::emptyList);

        Set<String> excludedTagsFromRequest = Set.of("exclTag1", "exclTag2");
        Set<String> additionalTagsFromRequest = Set.of("additionalTag1", "additionalTag2");

        routingRequest.getUsers()
                .forEach(routingCourier -> {
                    var newETags = new HashSet<>(routingCourier.getExcludedTags());
                    newETags.addAll(excludedTagsFromRequest);
                    routingCourier.setExcludedTags(newETags);

                    var addTags = new HashSet<>(routingCourier.getAdditionalTags());
                    addTags.addAll(additionalTagsFromRequest);
                    routingCourier.setAdditionalTags(addTags);
                });

        //when
        MvrpRequest mvrpRequest = mvrpRequestMapper.createMvrpRequest(
                routingRequest, vrpSettings, VrpClient.ApiType.MVRP, new HashSet<>());

        //then
        assertThat(mvrpRequest.getVehicles()).hasSize(2);

        var expectedTags = new HashSet<>(additionalTagsFromRequest);
        expectedTags.addAll(tagsFromSettings);

        var expectedExcludeTags = new HashSet<>(excludedTagsFromRequest);
        expectedExcludeTags.addAll(excludedTagsFromSettings);
        expectedExcludeTags.addAll(mvrpVehicleTagsMapper.mapExcludedTags(routingRequest.getUsers().iterator().next()));


        for (var vehicle : mvrpRequest.getVehicles()) {
            assertThat(vehicle.getTags())
                    .containsExactlyInAnyOrderElementsOf(expectedTags);
            assertThat(vehicle.getExcludedTags())
                    .containsExactlyInAnyOrderElementsOf(expectedExcludeTags);
        }
    }

    @Test
    @SneakyThrows
    void createMvrpRequestWithPenalizeLateService() {
        when(globalSettingsProvider.getValueAsDouble(GlobalSettings.CAPACITY_BOUND)).thenReturn(Optional.of(1.36));
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.MINIMAL_STOPS_FRO_LOW_CAPACITY_VEHICLE))
                .thenReturn(Optional.of(30));
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.MINIMAL_STOPS_FOR_HIGH_CAPACITY_VEHICLE))
                .thenReturn(Optional.of(40));
        VrpSettings vrpSettings = RoutingCommonTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                VrpSettings.class);
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(LocalDate.parse("2020-08-19"), 10, 2);
        MvrpRequest mvrpRequest = mvrpRequestMapper.createMvrpRequest(
                routingRequest, vrpSettings, VrpClient.ApiType.MVRP, new HashSet<>());
        assertThat(mvrpRequest.getOptions().getPenalizeLateService()).isTrue();
        assertThat(mvrpRequest.getVehicles().stream().flatMap(e -> e.getShifts().stream())
                .filter(e -> e.getMinimalUniqueStops() == null).count()).isEqualTo(0L);
        assertThat(mvrpRequest.getVehicles().stream().flatMap(e -> e.getShifts().stream())
                .filter(e -> e.getMaximalStops() == null).count()).isEqualTo(0L);
    }

    @Test
    @SneakyThrows
    void performRouteRequest_withStrongBindedRegion_Enabled() {
        //given
        VrpSettings vrpSettings = RoutingCommonTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                VrpSettings.class);
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings.REGION_OPTIONAL_TAG_ROUTE_COST, 1L))
                .thenReturn(EXPECTED_BINDED_REGION_COST_FOR_SC);

        //when
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(LocalDate.parse("2020-08-19"), 10, 2);
        MvrpRequest mvrpRequest = mvrpRequestMapper.createMvrpRequest(
                routingRequest,
                vrpSettings,
                VrpClient.ApiType.MVRP,
                Set.of(123)
        );


        //then
        assertThat(StreamEx.of(mvrpRequest.getLocations())
                .flatMap(l -> l.getOptionalTags().stream())
                .filter(t -> t.getTag().startsWith(TplRoutingUtils.Tags.REGION_PREFIX))
                .filter(t -> t.getValue() == EXPECTED_BINDED_REGION_COST_FOR_SC)
                .collect(Collectors.toList()))
                .isNotEmpty();

        assertThat(StreamEx.of(mvrpRequest.getLocations())
                .map(l -> l.getRequiredTags() != null)
                .collect(Collectors.toList())).isNotEmpty();
    }

    @Test
    @SneakyThrows
    void performRouteRequest_withStrongBindedRegion_Enabled_RegionNotStrong() {
        //given
        final int expectedCommonCost = 100777;
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.OPTIONAL_TAG_ROUTE_COST))
                .thenReturn(Optional.of(expectedCommonCost));
        VrpSettings vrpSettings = RoutingCommonTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                VrpSettings.class);
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(LocalDate.parse("2020-08-19"), 10, 2);


        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings.REGION_OPTIONAL_TAG_ROUTE_COST, 1L))
                .thenReturn(EXPECTED_BINDED_REGION_COST_FOR_SC);

        //when
        MvrpRequest mvrpRequest = mvrpRequestMapper.createMvrpRequest(
                routingRequest,
                vrpSettings,
                VrpClient.ApiType.MVRP,
                Set.of()
        );


        //then
        assertThat(StreamEx.of(mvrpRequest.getLocations())
                .flatMap(l -> l.getOptionalTags().stream())
                .filter(t -> t.getTag().startsWith(TplRoutingUtils.Tags.REGION_PREFIX))
                .filter(t -> t.getValue() == expectedCommonCost)
                .collect(Collectors.toList()))
                .isNotEmpty();

        assertThat(StreamEx.of(mvrpRequest.getLocations())
                .map(l -> l.getRequiredTags() != null)
                .collect(Collectors.toList())).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("bulkyNoBulkOrderClientReturnArgs")
    @SneakyThrows
    public void createMvrpRequestWithGrouping_Bulky_and_NoBulky_Divided(boolean withFlags,
                                                                        RoutingRequestItemType itemsType,
                                                                        String multiId) {
        if (withFlags) {
            Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                    CARGO_TYPE_REQUIRED_TAG_ENABLED, ROUTING_REQUEST_SC_ID)).thenReturn(true);

            Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                            BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED, ROUTING_REQUEST_SC_ID))
                    .thenReturn(true);

            RoutingOrderTag tag = new RoutingOrderTag("bulky_cargo", BigDecimal.valueOf(400L), BigDecimal.ONE);
            when(routingOrderTagProvider.findAllMapByTagName())
                    .thenReturn(Map.of("bulky_cargo", tag));

        }

        VrpSettings vrpSettings = RoutingCommonTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                VrpSettings.class);

        RoutingScheduleData userScheduleData = new RoutingScheduleData(RoutingCourierVehicleType.CAR,
                RelativeTimeInterval.valueOf("09:00-19:00"));
        RoutingCourier bulkyCargoCourier = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(ROUTING_REQUEST_SC_ID)
                .scheduleData(userScheduleData)
                .routingTimeMultiplier(RoutingTimeMultiplierUtil.DEFAULT_CAR)
                .additionalTags(Set.of(RequiredRoutingTag.BULKY_CARGO.getCode()))
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();

        RoutingCourier noBulkyCargoCourier = RoutingCourier.builder()
                .id(1L)
                .ref("ref")
                .depotId(ROUTING_REQUEST_SC_ID)
                .scheduleData(userScheduleData)
                .routingTimeMultiplier(RoutingTimeMultiplierUtil.DEFAULT_CAR)
                .additionalTags(Set.of())
                .excludedTags(Set.of())
                .taskIdsWithFixedOrder(List.of())
                .plannedTaskIds(List.of())
                .build();

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        int subTaskCount = 2;
        RoutingRequestItem itemBulky = new RoutingRequestItem(
                itemsType,
                multiId,
                subTaskCount,
                String.valueOf(ROUTING_REQUEST_SC_ID),
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.BULKY_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        RoutingRequestItem itemBulky2 = new RoutingRequestItem(
                itemsType,
                multiId + "2",
                subTaskCount,
                String.valueOf(ROUTING_REQUEST_SC_ID),
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.BULKY_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        RoutingRequestItem itemNoBulky = new RoutingRequestItem(
                itemsType,
                multiId + "1",
                subTaskCount,
                String.valueOf(ROUTING_REQUEST_SC_ID),
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                123,
                0,
                0,
                false,
                false
        );


        RoutingDepot depot = new RoutingDepot(ROUTING_REQUEST_SC_ID,
                RoutingGeoPoint.ofLatLon(new BigDecimal("37.716980"), new BigDecimal("55.741526")),
                LocalTimeInterval.valueOf("09:00-22:00")
        );

        RoutingRequest routingRequest = RoutingRequest.createShiftRequest(
                Instant.now(),
                LocalDate.now(),
                DateTimeUtil.DEFAULT_ZONE_ID,
                RoutingMockType.MANUAL,
                Set.of(bulkyCargoCourier, noBulkyCargoCourier),
                depot,
                List.of(itemBulky, itemNoBulky, itemBulky2),
                null,
                null, Set.of());


        MvrpRequest mvrpRequest = mvrpRequestMapper.createMvrpRequest(
                routingRequest, vrpSettings, VrpClient.ApiType.MVRP, new HashSet<>());
        if (withFlags) {
            assertThat(mvrpRequest.getOptions().getLocationGroups()).hasSize(1);
            assertThat(mvrpRequest.getOptions().getLocationGroups().get(0).getLocationIds())
                    .containsExactlyInAnyOrderElementsOf(List.of(itemBulky.getTaskId(), itemBulky2.getTaskId()));
        } else {
            assertThat(mvrpRequest.getOptions().getLocationGroups()).hasSize(1);
            assertThat(mvrpRequest.getOptions().getLocationGroups().get(0).getLocationIds())
                    .containsExactlyInAnyOrderElementsOf(
                            List.of(itemBulky.getTaskId(), itemBulky2.getTaskId(), itemNoBulky.getTaskId())
                    );
        }

    }

    private static Stream<Arguments> bulkyNoBulkOrderClientReturnArgs() {
        return Stream.of(
                Arguments.of(true, RoutingRequestItemType.CLIENT, "m_123_456"),
                Arguments.of(false, RoutingRequestItemType.CLIENT, "m_123_456"),
                Arguments.of(true, RoutingRequestItemType.CLIENT_RETURN, "cr_123_456"),
                Arguments.of(false, RoutingRequestItemType.CLIENT_RETURN, "cr_123_456")
        );
    }

    @ComponentScan(
            value = "ru.yandex.market.tpl.core.external.routing.vrp.mapper",
            excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*TestConfig")
    )
    @MockBean({GlobalSettingsProvider.class, RoutingOrderTagProvider.class})
    @MockBean(value = {DepotSettingsProvider.class}, answer = Answers.RETURNS_SMART_NULLS)
    @Profile("unit-test")
    @Configuration
    public static class MvrpRequestMapperTestConfig {
        @Bean
        public Clock clock() {
            return Clock.systemDefaultZone();
        }
    }
}
