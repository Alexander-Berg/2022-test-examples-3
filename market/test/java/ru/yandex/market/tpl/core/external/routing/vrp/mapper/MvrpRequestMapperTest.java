package ru.yandex.market.tpl.core.external.routing.vrp.mapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterUtil;
import ru.yandex.market.tpl.core.domain.routing.PerformRoutingRequestManager;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourierVehicleType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationGroup;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpRequest;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpRequestVehicles;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.VrpSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettingsProvider;
import ru.yandex.market.tpl.core.service.routing.TplRoutingUtils;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.TplCoreTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.partner.SortingCenter.DEFAULT_SC_ID;
import static ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper.INTERVAL;
import static ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper.LAT;
import static ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper.LON;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.CAPACITY_BOUND;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.GEO_POINT_SCALE;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.MINIMAL_STOPS_FOR_HIGH_CAPACITY_VEHICLE;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.MINIMAL_STOPS_FRO_LOW_CAPACITY_VEHICLE;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.MVRP_USE_GEO_POINT_SCALE_PROP;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.OPTIONAL_TAG_HIGH_ROUTE_COST;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.OPTIONAL_TAG_ROUTE_COST;

/**
 * @author kukabara
 */
@Slf4j
@RequiredArgsConstructor
class MvrpRequestMapperTest extends TplAbstractTest {

    public static final long EXPECTED_BINDED_REGION_COST_FOR_SC = 1777L;
    public static final long ROUTING_REQUEST_SC_ID = RoutingApiDataHelper.DEPOT_ID;

    private final MvrpRequestMapper mvrpRequestMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();
    private final Clock clock;
    private final TestUserHelper testUserHelper;
    private User user;
    private final PerformRoutingRequestManager performRoutingRequestManager;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final PartnerRepository<SortingCenter> scRepository;
    private final SortingCenterRepository sortingCenterRepository;

    private final GlobalSettingsProvider globalSettingsProvider;
    private final MvrpVehicleTagsMapper mvrpVehicleTagsMapper;

    @AfterEach
    void afterEach() {
        sortingCenterPropertyService.deletePropertyFromSortingCenter(scRepository.findByIdOrThrow(ROUTING_REQUEST_SC_ID),
                SortingCenterProperties.REGION_OPTIONAL_TAG_ROUTE_COST);

        Mockito.reset(globalSettingsProvider);

    }

    @Test
    void shouldGroupLocationsWithoutHouseInRoutingLocationGroups() {
        var scId = 5L;
        var geoPointScale = 4;
        var house1 = "house1";
        var house2 = "house2";
        String arrival = "10:04:26";

        var sortingCenter = sortingCenterRepository.save(
                SortingCenterUtil.sortingCenter(scId)
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                DepotSettings.MVRP_GROUP_LOCATIONS_BY_HOUSE_DISABLED,
                true
        );
        when(globalSettingsProvider.isBooleanEnabled(MVRP_USE_GEO_POINT_SCALE_PROP))
                .thenReturn(true);
        when(globalSettingsProvider.getValueAsInteger(GEO_POINT_SCALE))
                .thenReturn(Optional.of(geoPointScale));

        List<RoutingRequestItem> items = toListOfRoutingRequestItems(
                List.of(
                        routingApiDataHelper.order(LAT, LON, house1, INTERVAL, arrival),
                        routingApiDataHelper.order(LAT, LON, house2, INTERVAL, arrival)
                )
        );

        Map<RoutingLocationGroup, List<String>> locationGroups = mvrpRequestMapper.getLocationGroups(scId, items);

        assertThat(locationGroups).hasSize(1);
        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(INTERVAL),
                        RoutingGeoPoint.ofLatLon(geoPointScale, BigDecimal.valueOf(LAT), BigDecimal.valueOf(LON)),
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
        String arrival = "10:04:26";

        when(globalSettingsProvider.isBooleanEnabled(MVRP_USE_GEO_POINT_SCALE_PROP))
                .thenReturn(true);
        when(globalSettingsProvider.getValueAsInteger(GEO_POINT_SCALE))
                .thenReturn(Optional.of(geoPointScale));

        List<RoutingRequestItem> items = toListOfRoutingRequestItems(
                List.of(
                        routingApiDataHelper.order(lat, lon, house1, INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house1, INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house2, INTERVAL, arrival),
                        routingApiDataHelper.order(lat, lon, house2, INTERVAL, arrival)
                )
        );

        Map<RoutingLocationGroup, List<String>> locationGroups = mvrpRequestMapper.getLocationGroups(scId, items);

        assertThat(locationGroups).hasSize(2);
        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(INTERVAL),
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
                        RelativeTimeInterval.valueOf(INTERVAL),
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
        var geoPointScale = GeoPoint.GEO_POINT_SCALE - 2;
        double lat = 55.70018211;
        double lon = 37.58015822;
        String arrival = "10:04:26";

        when(globalSettingsProvider.isBooleanEnabled(MVRP_USE_GEO_POINT_SCALE_PROP))
                .thenReturn(false);
        when(globalSettingsProvider.getValueAsInteger(GEO_POINT_SCALE))
                .thenReturn(Optional.of(geoPointScale));

        Map<RoutingLocationGroup, List<String>> locationGroups = mvrpRequestMapper.getLocationGroups(
                scId,
                toListOfRoutingRequestItems(
                        List.of(
                                routingApiDataHelper.order(lat, lon, null, INTERVAL, arrival),
                                routingApiDataHelper.order(lat, lon, null, INTERVAL, arrival)
                        )
                )
        );

        verify(globalSettingsProvider, never()).getValueAsInteger(GEO_POINT_SCALE);

        assertThat(locationGroups.get(
                new RoutingLocationGroup(
                        RelativeTimeInterval.valueOf(INTERVAL),
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
        VrpSettings vrpSettings = TplCoreTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
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

        VrpSettings vrpSettings = TplCoreTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                VrpSettings.class);
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequestPickup(LocalDate.parse("2020-05-14"),
                10, 2);

        var sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.CARGO_TYPE_REQUIRED_TAG_ENABLED,
                true
        );

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
        VrpSettings vrpSettings = TplCoreTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
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
        when(globalSettingsProvider.getValueAsDouble(CAPACITY_BOUND)).thenReturn(Optional.of(1.36));
        when(globalSettingsProvider.getValueAsInteger(MINIMAL_STOPS_FRO_LOW_CAPACITY_VEHICLE)).thenReturn(Optional.of(30));
        when(globalSettingsProvider.getValueAsInteger(MINIMAL_STOPS_FOR_HIGH_CAPACITY_VEHICLE)).thenReturn(Optional.of(40));
        VrpSettings vrpSettings = TplCoreTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                VrpSettings.class);
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(LocalDate.parse("2020-08-19"), 10, 2);
        MvrpRequest mvrpRequest = mvrpRequestMapper.createMvrpRequest(
                routingRequest, vrpSettings, VrpClient.ApiType.MVRP, new HashSet<>());
        assertThat(mvrpRequest.getOptions().getPenalizeLateService()).isTrue();
        assertThat(mvrpRequest.getVehicles().stream().flatMap(e -> e.getShifts().stream())
                .filter(e -> e.getMinimalUniqueStops() == null).count()).isEqualTo(0L);
    }

    @Test
    @SneakyThrows
    void performRouteRequest_withStrongBindedRegion_Enabled() {
        //given
        user = testUserHelper.findOrCreateUser(1L);

        VrpSettings vrpSettings = TplCoreTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                VrpSettings.class);
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(LocalDate.parse("2020-08-19"), 10, 2);
        jdbcTemplate.update("UPDATE user_region " +
                        "SET binding_type = 'STRONG' " +
                        "where region_id = 123 and user_id = ?",
                user.getId());
        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                "VALUES (?, 123, 'STRONG')", user.getId());

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                scRepository.findByIdOrThrow(routingRequest.getDepot().getId()),
                SortingCenterProperties.REGION_OPTIONAL_TAG_ROUTE_COST,
                EXPECTED_BINDED_REGION_COST_FOR_SC
        );

        //when
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
        user = testUserHelper.findOrCreateUser(1L);
        when(globalSettingsProvider.getValueAsInteger(OPTIONAL_TAG_ROUTE_COST))
                .thenReturn(Optional.of(expectedCommonCost));
        VrpSettings vrpSettings = TplCoreTestUtils.mapFromResource("/vrp/mapper/vrp_settings_custom_2.json",
                VrpSettings.class);
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(LocalDate.parse("2020-08-19"), 10, 2);
        jdbcTemplate.update("UPDATE user_region " +
                        "SET binding_type = 'STRONG' " +
                        "where region_id = 123 and user_id = ?",
                user.getId());
        jdbcTemplate.update("INSERT into user_region (user_id, region_id, binding_type) " +
                "VALUES (?, 123, 'STRONG')", user.getId());

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                scRepository.findByIdOrThrow(routingRequest.getDepot().getId()),
                SortingCenterProperties.REGION_OPTIONAL_TAG_ROUTE_COST,
                EXPECTED_BINDED_REGION_COST_FOR_SC
        );

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

    @Test
    @SneakyThrows
    void performMvrpRequestWithRegion() {
        final int expectedHighCost = 100500;
        user = testUserHelper.findOrCreateUser(1L);
        when(globalSettingsProvider.getValueAsInteger(OPTIONAL_TAG_HIGH_ROUTE_COST)).thenReturn(Optional.of(expectedHighCost));
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(LocalDate.parse("2020-08-19"), 10, 2);

        Set<Integer> regionsWithStrongBinding = performRoutingRequestManager.getRegionsWithStrongBinding(
                routingRequest.getUsers().stream().map(e -> e.getId()).collect(Collectors.toSet()),
                routingRequest.getDepot()
        );
        assertThat(regionsWithStrongBinding).isEmpty();


        regionsWithStrongBinding = performRoutingRequestManager.getRegionsWithStrongBinding(
                routingRequest.getUsers().stream().map(e -> e.getId()).collect(Collectors.toSet()),
                routingRequest.getDepot()
        );
        assertThat(regionsWithStrongBinding).isEmpty();

        Long dsId = DeliveryService.DEFAULT_DS_ID;
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, 123, true, true, ?, ?, ?, 1, 1)",
                dsId, LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDate.now(clock));
        jdbcTemplate.update("INSERT into user_region (user_id, region_id) " +
                "VALUES (?, 123)", user.getId());
        regionsWithStrongBinding = performRoutingRequestManager.getRegionsWithStrongBinding(
                Set.of(user.getId()),
                routingRequest.getDepot()
        );
        assertThat(regionsWithStrongBinding).isEmpty();
        jdbcTemplate.update("UPDATE user_region " +
                        "SET binding_type = 'STRONG' " +
                        "where region_id = 123 and user_id = ?",
                user.getId());
        regionsWithStrongBinding = performRoutingRequestManager.getRegionsWithStrongBinding(
                Set.of(user.getId()),
                routingRequest.getDepot()
        );
        assertThat(regionsWithStrongBinding).isNotEmpty();

    }
}
