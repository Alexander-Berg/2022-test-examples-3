package ru.yandex.market.tpl.core.external.routing.vrp.mapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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

import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.base.property.PropertyDefinition;
import ru.yandex.market.tpl.core.domain.routing.tag.OptionalRoutingTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.external.routing.api.AdditionalTag;
import ru.yandex.market.tpl.core.external.routing.api.DimensionsClass;
import ru.yandex.market.tpl.core.external.routing.api.RoutingAddress;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.external.routing.vrp.mapper.location.DurationCalculator;
import ru.yandex.market.tpl.core.external.routing.vrp.mapper.location.MvrpLocationMapper;
import ru.yandex.market.tpl.core.external.routing.vrp.model.OrderLocation;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.HardcodedVrpSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.VrpSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.tag.RoutingOrderTagProvider;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.external.routing.vrp.mapper.location.MvrpLocationTagMapper.DEFAULT_REGIONAL_OPTIONAL_TAG_ROUTE_COST;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.BULKY_CARGO_SHARED_SERVICE_DURATION_IN_SECONDS;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.DROPSHIP_PICKUP_IN_SECONDS;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.FORCE_HARD_WINDOW_FOR_DROPSHIP_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.HARD_WINDOW_FOR_DROPSHIP_COURIERS_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.MIX_DROPSHIP_WITH_DELIVERY_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.ONLY_PARTNER_REQUIRED_TAG_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.REGION_OPTIONAL_TAG_ROUTE_COST;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.WIDE_INTERVAL_FOR_CLIENT_ORDER_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.NO_BULKY_CARGO_FOR_DROPOFF_CARGO_RETURN_DISABLED;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("unit-test")
public class MvrpLocationMapperTest {

    @Autowired
    private DepotSettingsProvider depotSettingsProvider;
    @Autowired
    private GlobalSettingsProvider globalSettingsProvider;
    @Autowired
    private RoutingOrderTagProvider routingOrderTagProvider;
    @Autowired
    private MvrpLocationMapper mvrpLocationMapper;

    @BeforeEach
    void setUp() {
        Set.of(
                MIX_DROPSHIP_WITH_DELIVERY_ENABLED,
                CARGO_TYPE_REQUIRED_TAG_ENABLED,
                WIDE_INTERVAL_FOR_CLIENT_ORDER_ENABLED,
                WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                HARD_WINDOW_FOR_DROPSHIP_COURIERS_ENABLED,
                BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED
        ).forEach(k -> {
            Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(eq(k), any()))
                    .thenReturn(false);
        });

        Set.of(
                FORCE_HARD_WINDOW_FOR_DROPSHIP_ENABLED
        ).forEach(k -> {
            Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(eq(k), any()))
                    .thenReturn(true);
        });

        Set.of(
                REGION_OPTIONAL_TAG_ROUTE_COST
        ).forEach(k -> {
            Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(eq(k), any()))
                    .thenReturn((long) DEFAULT_REGIONAL_OPTIONAL_TAG_ROUTE_COST);
        });

        Set.of(
                BULKY_CARGO_SHARED_SERVICE_DURATION_IN_SECONDS,
                DROPSHIP_PICKUP_IN_SECONDS
        ).forEach(k -> {
            Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(eq(k), any()))
                    .thenReturn(k.getDefaultValue());
        });

        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        eq(ONLY_PARTNER_REQUIRED_TAG_ENABLED), any()
                )
        ).thenReturn(false);
    }

    @Test
    void multiplyServiceDurationForMultiOrder() {
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.OPTIONAL_TAG_ROUTE_COST)).thenReturn(java.util.Optional.of(10));
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.OPTIONAL_TAG_HIGH_ROUTE_COST)).thenReturn(java.util.Optional.of(1000));

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
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

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getServiceDurationS()).isNotNull();
        assertThat(location.getOptionalTags().stream().mapToInt(OrderLocation.OptionalTag::getValue).sum()).isEqualTo(10);

        Long expectedServiceDuration =
                (long) (settings.getLocationDefaults("").getServiceDurationS() * subTaskCount * MvrpLocationMapper.MULTI_ORDER_SERVICE_DURATION_MULTIPLIER);
        assertThat(location.getServiceDurationS()).isEqualTo(expectedServiceDuration);
    }

    @Test
    void optionalTagForMultiOrder() {
        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();

        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
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

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getServiceDurationS()).isNotNull();
        assertThat(location.getOptionalTags()).isNotNull();

        Long expectedServiceDuration =
                (long) (settings.getLocationDefaults("").getServiceDurationS() * subTaskCount * MvrpLocationMapper.MULTI_ORDER_SERVICE_DURATION_MULTIPLIER);
        assertThat(location.getServiceDurationS()).isEqualTo(expectedServiceDuration);

        geoPoint = RoutingGeoPointGenerator.generateLonLat();
        multiOrderId = "m_123_452";
        RoutingRequestItem item2 = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "1",
                new RoutingAddress("address2", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                null,
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        OrderLocation location2 = mvrpLocationMapper.mapDeliveryLocation(item2, settings, null);

        assertThat(location2.getOptionalTags()).isNotEmpty();
    }

    @Test
    void optionalTagForMultiOrderWithHighCost() {
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.OPTIONAL_TAG_ROUTE_COST)).thenReturn(java.util.Optional.of(10));
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.OPTIONAL_TAG_HIGH_ROUTE_COST)).thenReturn(java.util.Optional.of(1000));

        long depotId = 123L;

        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(REGION_OPTIONAL_TAG_ROUTE_COST,
                        depotId))
                .thenReturn(1000L);

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "" + depotId,
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                120542,
                0,
                0,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings,
                new HashSet<>(Arrays.asList(120542)));

        assertThat(location.getServiceDurationS()).isNotNull();

        assertThat(location.getOptionalTags().stream().mapToInt(OrderLocation.OptionalTag::getValue).sum()).isEqualTo(1000);
    }


    @ParameterizedTest
    @MethodSource("provideDataForWideTimeWindowTest")
    void wideTimeWindowTest(
            PropertyDefinition<Boolean> property,
            boolean isEnable,
            RoutingRequestItemType routingRequestItemType,
            String expectedTimeWindow
    ) {
        long depotId = 1L;
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(property, depotId))
                .thenReturn(isEnable);

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                routingRequestItemType,
                multiOrderId,
                subTaskCount,
                "" + depotId,
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                120542,
                0,
                0,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings,
                new HashSet<>(Arrays.asList(120542)));

        assertThat(location.getTimeWindow()).isEqualTo(expectedTimeWindow);
    }

    private static Stream<Arguments> provideDataForWideTimeWindowTest() {
        return Stream.of(
                Arguments.of(
                        DepotSettings.WIDE_INTERVAL_FOR_CLIENT_ORDER_ENABLED,
                        true,
                        RoutingRequestItemType.CLIENT,
                        "09:00:00-23:00:00"
                ),
                Arguments.of(
                        DepotSettings.WIDE_INTERVAL_FOR_CLIENT_ORDER_ENABLED,
                        false,
                        RoutingRequestItemType.CLIENT,
                        "10:00:00-14:00:00"
                ),
                Arguments.of(
                        DepotSettings.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        true,
                        RoutingRequestItemType.PVZ,
                        "10:00:00-18:00:00"
                ),
                Arguments.of(
                        DepotSettings.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        false,
                        RoutingRequestItemType.PVZ,
                        "10:00:00-14:00:00"
                ),
                Arguments.of(
                        DepotSettings.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        true,
                        RoutingRequestItemType.LAVKA,
                        "10:00:00-18:00:00"
                ),
                Arguments.of(
                        DepotSettings.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        false,
                        RoutingRequestItemType.LAVKA,
                        "10:00:00-14:00:00"
                ),
                Arguments.of(
                        DepotSettings.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        true,
                        RoutingRequestItemType.LOCKER,
                        "10:00:00-18:00:00"
                ),
                Arguments.of(
                        DepotSettings.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        false,
                        RoutingRequestItemType.LOCKER,
                        "10:00:00-14:00:00"
                )
        );
    }

    @Test
    void customServiceDurationForBulkyCargoOrder() {
        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "1234567890";
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                Set.of(),
                null,
                true,
                DimensionsClass.BULKY_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getServiceDurationS()).isNotNull();
        assertThat(location.getOptionalTags()).isNotEmpty();

        Long expectedServiceDuration = DurationCalculator.BULKY_CARGO_SERVICE_DURATION_IN_SECONDS;
        Long expectedSharedServiceDuration =
                DepotSettings.BULKY_CARGO_SHARED_SERVICE_DURATION_IN_SECONDS.getDefaultValue();
        assertThat(location.getServiceDurationS()).isEqualTo(expectedServiceDuration);
        assertThat(location.getSharedServiceDurationS()).isEqualTo(expectedSharedServiceDuration);
        assertThat(location.getRequiredTags()).isNullOrEmpty();
    }

    @Test
    void dropoffReturnTags() {
        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String id = "1234567890";
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.DROPOFF_CARGO_RETURN,
                id,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + id,
                Set.of(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode()),
                Set.of(),
                null,
                true,
                DimensionsClass.BULKY_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED, 1L)
        ).thenReturn(true);
        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        CARGO_TYPE_REQUIRED_TAG_ENABLED, 1L)
        ).thenReturn(true);
        when(
                globalSettingsProvider.isBooleanEnabled(NO_BULKY_CARGO_FOR_DROPOFF_CARGO_RETURN_DISABLED)
        ).thenReturn(true);

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);
        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);
        assertThat(location.getRequiredTags().contains(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode())).isTrue();
        assertThat(location.getRequiredTags().contains(RequiredRoutingTag.NO_BULKY_CARGO.getCode())).isFalse();

        RoutingRequestItem item2 = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                id,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + id,
                Set.of(),
                Set.of(),
                null,
                true,
                DimensionsClass.BULKY_CARGO,
                123,
                0,
                0,
                false,
                false
        );
        OrderLocation location2 = mvrpLocationMapper.mapDeliveryLocation(item2, settings, null);
        assertThat(location2.getRequiredTags().contains(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode())).isFalse();
        assertThat(location2.getRequiredTags().contains(RequiredRoutingTag.NO_BULKY_CARGO.getCode())).isTrue();

        when(
                globalSettingsProvider.isBooleanEnabled(NO_BULKY_CARGO_FOR_DROPOFF_CARGO_RETURN_DISABLED)
        ).thenReturn(false);
        location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);
        assertThat(location.getRequiredTags().contains(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode())).isTrue();
        assertThat(location.getRequiredTags().contains(RequiredRoutingTag.NO_BULKY_CARGO.getCode())).isTrue();
    }

    @Test
    void bulkyCargoClientReturn() {
        var tag = new RoutingOrderTag("bulky_cargo", BigDecimal.valueOf(400L), BigDecimal.ONE);
        when(routingOrderTagProvider.findAllMapByTagName()).thenReturn(Map.of("bulky_cargo", tag));

        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED, 1L)
        ).thenReturn(true);
        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        CARGO_TYPE_REQUIRED_TAG_ENABLED, 1L)
        ).thenReturn(true);

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);
        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String id = "1234567890";

        //item dimension class is bulky
        RoutingRequestItem itemNoTag = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT_RETURN,
                id,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "clientReturn=" + id,
                Set.of(),
                Set.of(),
                null,
                true,
                DimensionsClass.BULKY_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(itemNoTag, settings, null);
        //location is bulky
        AssertionsForClassTypes.assertThat(location.getRequiredTags().contains(RequiredRoutingTag.BULKY_CARGO.getCode())).isTrue();
        AssertionsForClassTypes.assertThat(location.getRequiredTags().contains(RequiredRoutingTag.NO_BULKY_CARGO.getCode())).isFalse();

        //bulky dim and bulky tag already
        RoutingRequestItem itemBulkyCargoTag = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT_RETURN,
                id,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "clientReturn=" + id,
                Set.of(RequiredRoutingTag.BULKY_CARGO.getCode()),
                Set.of(),
                null,
                true,
                DimensionsClass.BULKY_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        OrderLocation locationBulkyCargo = mvrpLocationMapper.mapDeliveryLocation(itemBulkyCargoTag, settings, null);
        //location is bulky
        AssertionsForClassTypes.assertThat(locationBulkyCargo.getRequiredTags().contains(RequiredRoutingTag.BULKY_CARGO.getCode())).isTrue();
        AssertionsForClassTypes.assertThat(locationBulkyCargo.getRequiredTags().contains(RequiredRoutingTag.NO_BULKY_CARGO.getCode())).isFalse();

        //dimension not bulky
        RoutingRequestItem itemNoBulkyCargoTag = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT_RETURN,
                id,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "clientReturn=" + id,
                Set.of(),
                Set.of(),
                null,
                true,
                DimensionsClass.REGULAR_CARGO,
                123,
                0,
                0,
                false,
                false
        );


        OrderLocation locationBoBulkyCargoTag = mvrpLocationMapper.mapDeliveryLocation(itemNoBulkyCargoTag, settings,
                null);
        //location is not bulky
        AssertionsForClassTypes.assertThat(locationBoBulkyCargoTag.getRequiredTags().contains(RequiredRoutingTag.BULKY_CARGO.getCode())).isFalse();
        AssertionsForClassTypes.assertThat(locationBoBulkyCargoTag.getRequiredTags().contains(RequiredRoutingTag.NO_BULKY_CARGO.getCode())).isTrue();
    }

    @Test
    void bulkyCargoMultyOrderTest() {
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED, 1L))
                .thenReturn(true);

        RoutingOrderTag tag = new RoutingOrderTag("bulky_cargo", BigDecimal.valueOf(400L), BigDecimal.ONE);
        when(routingOrderTagProvider.findAllMapByTagName())
                .thenReturn(Map.of("bulky_cargo", tag));

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.BULKY_CARGO,
                123,
                0,
                0,
                true,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);
        assertThat(location.getRequiredTags()).isNullOrEmpty();

        geoPoint = geoPoint = RoutingGeoPointGenerator.generateLonLat();
        multiOrderId = "m_123_457";
        subTaskCount = 2;
        item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
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

        settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getRequiredTags()).contains("bulky_cargo");
    }

    @Test
    void noBulkyCargoLockerOrderTest() {
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED, 1L))
                .thenReturn(true);

        RoutingOrderTag tag = new RoutingOrderTag("bulky_cargo", BigDecimal.valueOf(400L), BigDecimal.ONE);
        when(routingOrderTagProvider.findAllMapByTagName())
                .thenReturn(Map.of("bulky_cargo", tag));

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "123_457";
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.PVZ,
                multiOrderId,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
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

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getRequiredTags()).isEmpty();

    }

    @Test
    void noBulkyCargoForOtherScTest() {
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED, 1L))
                .thenReturn(true);

        RoutingOrderTag tag = new RoutingOrderTag("bulky_cargo", BigDecimal.valueOf(400L), BigDecimal.ONE);
        when(routingOrderTagProvider.findAllMapByTagName())
                .thenReturn(Map.of("bulky_cargo", tag));

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "123_457";
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
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

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);
        assertThat(location.getRequiredTags().get(0)).isEqualTo("bulky_cargo");

        tag = new RoutingOrderTag("bulky_cargo", BigDecimal.valueOf(400L), BigDecimal.ONE);
        when(routingOrderTagProvider.findAllMapByTagName()).thenReturn(Map.of("bulky_cargo", tag));

        geoPoint = RoutingGeoPointGenerator.generateLonLat();
        multiOrderId = "123_459";
        item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                1,
                "2",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
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

        settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getRequiredTags()).isEmpty();

    }

    @Test
    void calculatedAdditionalTimeForSurvey() {
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.OPTIONAL_TAG_ROUTE_COST)).thenReturn(java.util.Optional.of(10));
        when(globalSettingsProvider.getValueAsInteger(GlobalSettings.OPTIONAL_TAG_HIGH_ROUTE_COST)).thenReturn(java.util.Optional.of(1000));

        long depotId = 1L;
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(REGION_OPTIONAL_TAG_ROUTE_COST,
                        1L))
                .thenReturn(1000L);

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.PVZ,
                multiOrderId,
                subTaskCount,
                "" + depotId,
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                120542,
                140,
                0,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings,
                new HashSet<>(Arrays.asList(120542)));

        assertThat(location.getServiceDurationS()).isNotNull();
        assertThat(location.getServiceDurationS()).isEqualTo(500);

        assertThat(location.getOptionalTags().stream().mapToInt(OrderLocation.OptionalTag::getValue).sum()).isEqualTo(1000);
    }

    @Test
    public void bulkyCargoAndNoBulkyCargoOrderTest() {
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings.CARGO_TYPE_REQUIRED_TAG_ENABLED, 1L))
                .thenReturn(true);
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings.BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED, 1L))
                .thenReturn(true);

        RoutingOrderTag tag = new RoutingOrderTag("bulky_cargo", BigDecimal.valueOf(400L), BigDecimal.ONE);
        when(routingOrderTagProvider.findAllMapByTagName())
                .thenReturn(Map.of("bulky_cargo", tag));

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
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

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);
        assertThat(location.getRequiredTags()).contains("bulky_cargo");
        assertThat(location.getRequiredTags()).doesNotContain("no_bulky_cargo");


        geoPoint = RoutingGeoPointGenerator.generateLonLat();
        multiOrderId = "m_123_457";
        subTaskCount = 2;
        item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
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

        settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getRequiredTags()).contains("no_bulky_cargo");
        assertThat(location.getRequiredTags()).doesNotContain("bulky_cargo");

    }

    @ParameterizedTest
    @MethodSource("provideFashionOrdersAmount")
    void calculatedAdditionalTimeForFashion(int fashionOrdersCount, long expectedServiceDuration,
                                            DimensionsClass dimensionsClass) {
        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                dimensionsClass,
                120542,
                0,
                fashionOrdersCount,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings,
                new HashSet(List.of(120542)));

        assertThat(location.getServiceDurationS()).isEqualTo(expectedServiceDuration);

    }

    // Не удалять, используется в методе calculatedAdditionalTimeForFashion в качестве аргументов
    private static Stream<Arguments> provideFashionOrdersAmount() {
        return Stream.of(
                Arguments.of(0, 280, DimensionsClass.REGULAR_CARGO),
                Arguments.of(0, 900, DimensionsClass.BULKY_CARGO),
                Arguments.of(1, 1180, DimensionsClass.REGULAR_CARGO),
                Arguments.of(1, 1800, DimensionsClass.BULKY_CARGO),
                Arguments.of(2, 1480, DimensionsClass.REGULAR_CARGO),
                Arguments.of(2, 2100, DimensionsClass.BULKY_CARGO),
                Arguments.of(3, 1780, DimensionsClass.REGULAR_CARGO),
                Arguments.of(3, 2400, DimensionsClass.BULKY_CARGO)
        );
    }


    @ParameterizedTest
    @MethodSource("provideItemTypes")
    void calculatedShipmentSizeWeightKg(
            RoutingRequestItemType routingRequestItemType,
            int subTaskCount,
            BigDecimal expectedShipmentSizeWeightKg,
            BigDecimal expectedDropValue,
            BigDecimal expectedOutOfTimeFixed,
            BigDecimal expectedOutOfTimeMinute,
            Boolean hardWindow
    ) {
        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "m_123_456";
        RoutingRequestItem item = new RoutingRequestItem(
                routingRequestItemType,
                multiOrderId,
                subTaskCount,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                120542,
                0,
                0,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings,
                new HashSet(List.of(120542)));

        assertThat(location.getShipmentSize().getWeightKg()).isEqualTo(expectedShipmentSizeWeightKg);

        assertThat(location.getPenalty().getDrop()).isEqualTo(expectedDropValue);
        assertThat(location.getPenalty().getOutOfTime().getFixed()).isEqualTo(expectedOutOfTimeFixed);
        assertThat(location.getPenalty().getOutOfTime().getMinute()).isEqualTo(expectedOutOfTimeMinute);
        assertThat(location.getHardWindow()).isEqualTo(hardWindow);
    }

    @Test
    void shouldNotOverrideHardWindowIfItIsDisabled() {
        Mockito.when(depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(DepotSettings.FORCE_HARD_WINDOW_FOR_DROPSHIP_ENABLED, 1L))
                .thenReturn(false);

        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.DROPSHIP,
                "m_123_456",
                1,
                "1",
                new RoutingAddress("address1", RoutingGeoPointGenerator.generateLonLat()),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + "m_123_456",
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                120542,
                0,
                0,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);
        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings,
                new HashSet<>(List.of(120542)));

        assertThat(location.getHardWindow()).isFalse();

    }

    @Test
    void shouldMapClientReturn() {
        var regionId = 120542;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT_RETURN,
                "cr_123_456",
                1,
                "1",
                new RoutingAddress("address1", RoutingGeoPointGenerator.generateLonLat()),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "clientReturnId=" + "cr_123_456",
                Set.of(),
                Set.of(),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                regionId,
                0,
                0,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);
        OrderLocation location = assertDoesNotThrow(() -> mvrpLocationMapper.mapDeliveryLocation(item, settings,
                new HashSet<>(List.of(120542))));

        assertThat(location.getOptionalTags()).hasSize(1);
        assertThat(location.getOptionalTags().get(0).getTag()).isEqualTo("reg" + 120542);
    }

    @Test
    void onlyPartnerTag() {
        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        eq(ONLY_PARTNER_REQUIRED_TAG_ENABLED), any()
                )
        ).thenReturn(true);
        Mockito.when(globalSettingsProvider.getValueAsStrings(
                GlobalSettings.SELF_EMPLOYED_ROUTING_TAG_BLACK_LIST
        )).thenReturn(
                Set.of(
                        RequiredRoutingTag.PVZ.getCode(),
                        RequiredRoutingTag.LOCKER.getCode(),
                        RequiredRoutingTag.POSTPAID.getCode(),
                        AdditionalTag.JEWELRY.getCode(),
                        RequiredRoutingTag.LAVKA.getCode(),
                        RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode(),
                        RequiredRoutingTag.DROPSHIP.getCode()
                )
        );

        Mockito.when(routingOrderTagProvider.findAllMapByTagName())
                .thenReturn(Map.of(
                        OptionalRoutingTag.CLIENT.getCode(),
                        new RoutingOrderTag(
                                OptionalRoutingTag.CLIENT.getCode(),
                                BigDecimal.ONE,
                                BigDecimal.ONE
                        ),
                        OptionalRoutingTag.REGULAR_CARGO.getCode(),
                        new RoutingOrderTag(
                                OptionalRoutingTag.REGULAR_CARGO.getCode(),
                                BigDecimal.ONE,
                                BigDecimal.ONE
                        )
                ));

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "1234567890";

        RoutingRequestItem itemForPartner = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(RequiredRoutingTag.POSTPAID.getCode(), RequiredRoutingTag.DELIVERY.getCode()),
                Set.of(),
                null,
                true,
                DimensionsClass.REGULAR_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        OrderLocation locationForPartner = mvrpLocationMapper.mapDeliveryLocation(
                itemForPartner,
                new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP),
                null
        );

        assertThat(locationForPartner.getRequiredTags()).containsExactlyInAnyOrder(
                RequiredRoutingTag.POSTPAID.getCode(),
                RequiredRoutingTag.DELIVERY.getCode(),
                RequiredRoutingTag.ONLY_PARTNER.getCode()
        );


        Set<String> optionalTags = StreamEx.of(locationForPartner.getOptionalTags())
                .map(OrderLocation.OptionalTag::getTag)
                .toSet();
        assertThat(optionalTags).containsExactlyInAnyOrder(
                OptionalRoutingTag.CLIENT.getCode(),
                OptionalRoutingTag.REGULAR_CARGO.getCode(),
                "reg" + 123
        );

        RoutingRequestItem itemNotForPartner = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(RequiredRoutingTag.PREPAID.getCode(), RequiredRoutingTag.DELIVERY.getCode()),
                Set.of(),
                null,
                true,
                DimensionsClass.REGULAR_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        OrderLocation locationNotForPartner = mvrpLocationMapper.mapDeliveryLocation(
                itemNotForPartner,
                new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP),
                null
        );

        assertThat(locationNotForPartner.getRequiredTags()).containsExactlyInAnyOrder(
                RequiredRoutingTag.PREPAID.getCode(),
                RequiredRoutingTag.DELIVERY.getCode()
        );


        Set<String> optionalTagsOfLocationNotForPartner =
                StreamEx.of(locationNotForPartner.getOptionalTags())
                        .map(OrderLocation.OptionalTag::getTag)
                        .toSet();
        assertThat(optionalTagsOfLocationNotForPartner).containsExactlyInAnyOrder(
                OptionalRoutingTag.CLIENT.getCode(),
                OptionalRoutingTag.REGULAR_CARGO.getCode(),
                "reg" + 123
        );
    }

    @ParameterizedTest
    @EnumSource(AdditionalTag.class)
    void onlyPartnerTagForAdditional(AdditionalTag additionalTag) {
        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        eq(ONLY_PARTNER_REQUIRED_TAG_ENABLED), any()
                )
        ).thenReturn(true);
        Mockito.when(globalSettingsProvider.getValueAsStrings(
                GlobalSettings.SELF_EMPLOYED_ROUTING_TAG_BLACK_LIST
        )).thenReturn(
                Set.of(
                        RequiredRoutingTag.PVZ.getCode(),
                        RequiredRoutingTag.LOCKER.getCode(),
                        RequiredRoutingTag.POSTPAID.getCode(),
                        RequiredRoutingTag.LAVKA.getCode(),
                        RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode(),
                        RequiredRoutingTag.DROPSHIP.getCode(),
                        AdditionalTag.B2B.getCode(),
                        AdditionalTag.JEWELRY.getCode(),
                        AdditionalTag.TRYING.getCode(),
                        AdditionalTag.SPECIAL_REQUEST.getCode(),
                        AdditionalTag.UNBOXING_AVAILABLE.getCode(),
                        AdditionalTag.R18.getCode(),
                        AdditionalTag.CLIENT_RETURN.getCode()
                )
        );

        Mockito.when(routingOrderTagProvider.findAllMapByTagName())
                .thenReturn(Map.of(
                        OptionalRoutingTag.CLIENT.getCode(),
                        new RoutingOrderTag(
                                OptionalRoutingTag.CLIENT.getCode(),
                                BigDecimal.ONE,
                                BigDecimal.ONE
                        ),
                        OptionalRoutingTag.REGULAR_CARGO.getCode(),
                        new RoutingOrderTag(
                                OptionalRoutingTag.REGULAR_CARGO.getCode(),
                                BigDecimal.ONE,
                                BigDecimal.ONE
                        ),
                        OptionalRoutingTag.LOCKER.getCode(),
                        new RoutingOrderTag(
                                OptionalRoutingTag.LOCKER.getCode(),
                                BigDecimal.ONE,
                                BigDecimal.ONE
                        )
                ));

        RoutingGeoPoint geoPoint = RoutingGeoPointGenerator.generateLonLat();
        String multiOrderId = "1234567890";

        RoutingRequestItem itemForPartner = new RoutingRequestItem(
                RoutingRequestItemType.LOCKER,
                multiOrderId,
                1,
                "1",
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(RequiredRoutingTag.PREPAID.getCode(), RequiredRoutingTag.DELIVERY.getCode()),
                Set.of(additionalTag.getCode()),
                null,
                true,
                DimensionsClass.REGULAR_CARGO,
                123,
                0,
                0,
                false,
                false
        );

        OrderLocation locationForPartner = mvrpLocationMapper.mapDeliveryLocation(
                itemForPartner,
                new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP),
                null
        );

        assertThat(locationForPartner.getRequiredTags()).containsExactlyInAnyOrder(
                RequiredRoutingTag.PREPAID.getCode(),
                RequiredRoutingTag.DELIVERY.getCode(),
                RequiredRoutingTag.ONLY_PARTNER.getCode()
        );

        Set<String> optionalTags = StreamEx.of(locationForPartner.getOptionalTags())
                .map(OrderLocation.OptionalTag::getTag)
                .toSet();
        assertThat(optionalTags).containsExactlyInAnyOrder(
                OptionalRoutingTag.LOCKER.getCode(),
                OptionalRoutingTag.REGULAR_CARGO.getCode(),
                "reg" + 123
        );
    }

    private static Stream<Arguments> provideItemTypes() {
        return Stream.of(
                Arguments.of(RoutingRequestItemType.CLIENT, 4, BigDecimal.ZERO, BigDecimal.valueOf(5000),
                        BigDecimal.valueOf(20), new BigDecimal("0.5"), Boolean.FALSE),
                Arguments.of(RoutingRequestItemType.DROPSHIP, 0, BigDecimal.ZERO, BigDecimal.valueOf(5000),
                        BigDecimal.valueOf(20), new BigDecimal("0.5"), Boolean.TRUE),
                Arguments.of(RoutingRequestItemType.LAVKA, 5, BigDecimal.valueOf(5), BigDecimal.valueOf(37500),
                        BigDecimal.valueOf(150), BigDecimal.valueOf(5), Boolean.FALSE),
                Arguments.of(RoutingRequestItemType.PVZ, 6, BigDecimal.valueOf(6), BigDecimal.valueOf(45000),
                        BigDecimal.valueOf(180), BigDecimal.valueOf(6), Boolean.FALSE),
                Arguments.of(RoutingRequestItemType.LOCKER, 7, BigDecimal.valueOf(7), BigDecimal.valueOf(52500),
                        BigDecimal.valueOf(210), BigDecimal.valueOf(7), Boolean.FALSE)
        );
    }

    @ComponentScan(
            value = "ru.yandex.market.tpl.core.external.routing.vrp.mapper",
            excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*TestConfig")
    )
    @MockBean(classes = {DepotSettingsProvider.class, GlobalSettingsProvider.class, RoutingOrderTagProvider.class})
    @Configuration
    @Profile("unit-test")
    public static class MvrpLocationMapperTestConfig {
        @Bean
        public Clock clock() {
            return Clock.systemDefaultZone();
        }
    }

}
