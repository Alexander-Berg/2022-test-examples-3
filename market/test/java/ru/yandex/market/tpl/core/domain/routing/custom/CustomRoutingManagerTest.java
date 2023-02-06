package ru.yandex.market.tpl.core.domain.routing.custom;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.core.domain.routing.custom.transport.CustomRoutingTransportProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpRequest;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpRequestVehicles;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettingsProvider;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;
import ru.yandex.market.tpl.core.service.user.transport.TransportTypeRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.TplCoreTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RequiredArgsConstructor
public class CustomRoutingManagerTest extends TplAbstractTest {
    public static final String REGULAR_CARGO_EXPR = "regular_cargo_expr";
    public static final String BULKY_CARGO_EXPR = "bulky_cargo_expr";
    public static final String TRANSIT_EXPRESSION = "transit_expression";
    private final long SC_ID = 77777L;
    private final long REGULAR_CARGO_VEHICLE_ID = 11285;
    private final long BULKY_CARGO_VEHICLE_ID = 15631;
    private final long SPECIAL_VEHICLE_ID = 987654321;
    private final CustomRoutingManagerImpl customRoutingManager;
    private final CustomRoutingTransportManagerImpl transportManager;
    private final TestUserHelper testUserHelper;
    private final TransportTypeRepository transportTypeRepository;


    private final GlobalSettingsProvider globalSettingsProvider;

    private CustomRoutingConfig customRoutingConfig;
    private MvrpRequest mvrpRequest;
    private RoutingRequest routingRequest;
    private TransportType transportType;

    @Test
    void shouldUpdateCustomRoutingConfig() {
        customRoutingManager.updateRequest(routingRequest, mvrpRequest);
        assertThat(mvrpRequest.getOptions().getAvoidTolls()).isTrue();
        mvrpRequest.getLocations().forEach(orderLocation -> {
            if (orderLocation.getRef().equals("57651432_57651431")) {
                assertThat(orderLocation.getServiceDurationS()).isEqualTo(450L);
                assertThat(orderLocation.getSharedServiceDurationS()).isEqualTo(350L);
            }
            if (orderLocation.getRef().equals("57918775")) {
                assertThat(orderLocation.getServiceDurationS()).isEqualTo(451L);
                assertThat(orderLocation.getSharedServiceDurationS()).isEqualTo(351L);
            }
            if (orderLocation.getRef().equals("57980595")) {
                assertThat(orderLocation.getServiceDurationS()).isEqualTo(452L);
                assertThat(orderLocation.getSharedServiceDurationS()).isEqualTo(352L);
            }
            if (orderLocation.getRef().equals("57426155_57761037_57761036_57816182_58017205")) {
                assertThat(orderLocation.getServiceDurationS()).isEqualTo(453L);
                assertThat(orderLocation.getSharedServiceDurationS()).isEqualTo(353L);
            }
        });

        MvrpRequestVehicles regularCargoVehicle = getVehicleById(REGULAR_CARGO_VEHICLE_ID);
        assertThat(regularCargoVehicle.getCost()).isEqualTo(REGULAR_CARGO_EXPR);

        MvrpRequestVehicles bulkyCargoVehicle = getVehicleById(BULKY_CARGO_VEHICLE_ID);
        assertThat(bulkyCargoVehicle.getCost()).isEqualTo(BULKY_CARGO_EXPR);
    }

    @Test
    void shouldUpdateCustomRoutingTransportConfig() {
        addTransportConfig(customRoutingConfig);
        customRoutingManager.updateRequest(routingRequest, mvrpRequest);
        mvrpRequest.getVehicles().forEach(vehicles -> {
            assertThat(vehicles.getCapacity().getLimits().getUnitsPerc()).isEqualTo(BigDecimal.valueOf(77));
            assertThat(vehicles.getShifts().get(0).getMinimalUniqueStops()).isEqualTo(11);
            assertThat(vehicles.getShifts().get(0).getMaxMileageKm()).isEqualTo(BigDecimal.valueOf(800));
        });

        MvrpRequestVehicles specialVehicle = getVehicleById(SPECIAL_VEHICLE_ID);
        assertThat(specialVehicle.getCost()).isEqualTo(TRANSIT_EXPRESSION);
    }

    private MvrpRequestVehicles getVehicleById(long vehicleId) {
        return StreamEx.of(mvrpRequest.getVehicles())
                .filter(v -> v.getId().equals(vehicleId))
                .findFirst()
                .orElseThrow();
    }

    @BeforeEach
    void init() {
        doReturn(true).when(globalSettingsProvider)
                .isBooleanEnabled(GlobalSettings.CUSTOM_ROUTING_ENABLED);
        customRoutingConfig = createCustomRoutingConfig();
        mvrpRequest = makeMvrpRequest();
        routingRequest = makeRoutingRequest();
        routingRequest.getDepot().getId();
    }

    private CustomRoutingConfig createCustomRoutingConfig() {
        var sc = testUserHelper.sortingCenter(SC_ID);
        var config = createConfig("test_config", sc.getId());
        return config;
    }

    public CustomRoutingConfig createConfig(String name, Long scId) {
        CustomRoutingConfig config = new CustomRoutingConfig();
        config.setName(name);
        config = customRoutingManager.createCustomRoutingConfig(config);
        customRoutingManager.addScToCustomRouting(config.getId(), scId);
        addProperties(config);
        return config;
    }

    private void addProperties(CustomRoutingConfig config) {
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.AVOID_TOLLS.getName(), "true");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.PVZ_HARD_WINDOW.getName(), "true");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.LAVKA_HARD_WINDOW.getName(), "true");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.LOCKER_HARD_WINDOW.getName(), "true");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.CLIENT_HARD_WINDOW.getName(), "true");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.CLIENT_SERVICE_DURATION.getName(), "450");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.CLIENT_SHARED_SERVICE_DURATION.getName(), "350");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.LAVKA_SERVICE_DURATION.getName(), "451");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.LAVKA_SHARED_SERVICE_DURATION.getName(), "351");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.LOCKER_SERVICE_DURATION.getName(), "452");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.LOCKER_SHARED_SERVICE_DURATION.getName(), "352");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.PVZ_SERVICE_DURATION.getName(), "453");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(), CustomRoutingProperties.PVZ_SHARED_SERVICE_DURATION.getName(), "353");
        customRoutingManager.createCustomRoutingProperty(
                config.getId(),
                CustomRoutingProperties.REGULAR_CARGO_COST_EXPRESSION.getName(), REGULAR_CARGO_EXPR
        );
        customRoutingManager.createCustomRoutingProperty(
                config.getId(),
                CustomRoutingProperties.BULKY_CARGO_COST_EXPRESSION.getName(), BULKY_CARGO_EXPR
        );
    }


    private void addTransportConfig(CustomRoutingConfig config) {
        var tt = new TransportType("Машинка777", BigDecimal.TEN, null, 100, RoutingVehicleType.COMMON, 0, null);
        transportType = transportTypeRepository.saveAndFlush(tt);
        var transportConfigId = transportManager.createTransportsConfig(config.getId(), "transport_config");
        transportManager.addTtToGroup(transportConfigId, transportType.getId());
        addTransportProperties(transportConfigId);
    }

    private void addTransportProperties(Long transportConfigId) {
        transportManager.createProperty(
                transportConfigId, CustomRoutingTransportProperties.MAX_MILEAGE_KM.getName(), "800"
        );

        transportManager.createProperty(
                transportConfigId, CustomRoutingTransportProperties.MINIMAL_UNIQUE_STOPS.getName(), "11"
        );

        transportManager.createProperty(
                transportConfigId, CustomRoutingTransportProperties.UNITS_PERCENT.getName(), "77"
        );

        transportManager.createProperty(
                transportConfigId,
                CustomRoutingTransportProperties.SPECIAL_VEHICLE_COST_EXPRESION.getName(), TRANSIT_EXPRESSION
        );
    }

    @SneakyThrows
    private MvrpRequest makeMvrpRequest() {
        return TplCoreTestUtils.mapFromResource("/customrouting/raw_request.json", MvrpRequest.class);
    }

    @SneakyThrows
    private RoutingRequest makeRoutingRequest() {
        return TplCoreTestUtils.mapFromResource("/customrouting/request.json", RoutingRequest.class);
    }
}
