package ru.yandex.market.tpl.core.external.routing.vrp.mapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.routing.tag.RoutingOrderTagType;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.base.property.BooleanDefinition;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.routing.movement.MovementsRequestItemsCollector;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTagQueryService;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.DimensionsClass;
import ru.yandex.market.tpl.core.external.routing.api.RoutingAddress;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.external.routing.vrp.mapper.location.DurationCalculator;
import ru.yandex.market.tpl.core.external.routing.vrp.mapper.location.MvrpLocationMapper;
import ru.yandex.market.tpl.core.external.routing.vrp.model.OrderLocation;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.HardcodedVrpSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.VrpSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettingsProvider;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.partner.SortingCenter.DEFAULT_SC_ID;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.OPTIONAL_TAG_HIGH_ROUTE_COST;
import static ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings.OPTIONAL_TAG_ROUTE_COST;

@RequiredArgsConstructor
public class MvrpLocationMapperTest extends TplAbstractTest {

    private final MvrpLocationMapper mvrpLocationMapper;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final SortingCenterRepository sortingCenterRepository;
    private final GlobalSettingsProvider globalSettingsProvider;

    @MockBean
    private RoutingOrderTagQueryService routingOrderTagQueryService;

    @Test
    void multiplyServiceDurationForMultiOrder() {
        when(globalSettingsProvider.getValueAsInteger(OPTIONAL_TAG_ROUTE_COST)).thenReturn(java.util.Optional.of(10));
        when(globalSettingsProvider.getValueAsInteger(OPTIONAL_TAG_HIGH_ROUTE_COST)).thenReturn(java.util.Optional.of(1000));

        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
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
        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());

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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
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

        generated = GeoPointGenerator.generateLonLat();
        geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                false,
                false
        );

        OrderLocation location2 = mvrpLocationMapper.mapDeliveryLocation(item2, settings, null);

        assertThat(location2.getOptionalTags()).isNotEmpty();
    }

    @Test
    void optionalTagForMultiOrderWithHighCost() {
        when(globalSettingsProvider.getValueAsInteger(OPTIONAL_TAG_ROUTE_COST)).thenReturn(java.util.Optional.of(10));
        when(globalSettingsProvider.getValueAsInteger(OPTIONAL_TAG_HIGH_ROUTE_COST)).thenReturn(java.util.Optional.of(1000));

        SortingCenter sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.REGION_OPTIONAL_TAG_ROUTE_COST,
                1000L
        );

        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT,
                multiOrderId,
                subTaskCount,
                "" + sortingCenter.getId(),
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                120542,
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
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
            BooleanDefinition property,
            boolean isEnable,
            RoutingRequestItemType routingRequestItemType,
            String expectedTimeWindow
    ) {
        SortingCenter sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                property,
                isEnable
        );

        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                routingRequestItemType,
                multiOrderId,
                subTaskCount,
                "" + sortingCenter.getId(),
                new RoutingAddress("address1", geoPoint),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=" + multiOrderId,
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                120542,
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
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
                        SortingCenterProperties.WIDE_INTERVAL_FOR_CLIENT_ORDER_ENABLED,
                        true,
                        RoutingRequestItemType.CLIENT,
                        "09:00:00-23:00:00"
                ),
                Arguments.of(
                        SortingCenterProperties.WIDE_INTERVAL_FOR_CLIENT_ORDER_ENABLED,
                        false,
                        RoutingRequestItemType.CLIENT,
                        "10:00:00-14:00:00"
                ),
                Arguments.of(
                        SortingCenterProperties.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        true,
                        RoutingRequestItemType.PVZ,
                        "10:00:00-18:00:00"
                ),
                Arguments.of(
                        SortingCenterProperties.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        false,
                        RoutingRequestItemType.PVZ,
                        "10:00:00-14:00:00"
                ),
                Arguments.of(
                        SortingCenterProperties.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        true,
                        RoutingRequestItemType.LAVKA,
                        "10:00:00-18:00:00"
                ),
                Arguments.of(
                        SortingCenterProperties.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        false,
                        RoutingRequestItemType.LAVKA,
                        "10:00:00-14:00:00"
                ),
                Arguments.of(
                        SortingCenterProperties.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        true,
                        RoutingRequestItemType.LOCKER,
                        "10:00:00-18:00:00"
                ),
                Arguments.of(
                        SortingCenterProperties.WIDE_INTERVAL_FOR_PICKUP_POINT_ORDER_ENABLED,
                        false,
                        RoutingRequestItemType.LOCKER,
                        "10:00:00-14:00:00"
                )
        );
    }

    @Test
    void customServiceDurationForBulkyCargoOrder() {
        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getServiceDurationS()).isNotNull();
        assertThat(location.getOptionalTags()).isNotEmpty();

        Long expectedServiceDuration = DurationCalculator.BULKY_CARGO_SERVICE_DURATION_IN_SECONDS;
        Long expectedSharedServiceDuration = MvrpLocationMapper.BULKY_CARGO_SHARED_SERVICE_DURATION_IN_SECONDS;
        assertThat(location.getServiceDurationS()).isEqualTo(expectedServiceDuration);
        assertThat(location.getSharedServiceDurationS()).isEqualTo(expectedSharedServiceDuration);
        assertThat(location.getRequiredTags()).isNullOrEmpty();
    }

    @Test
    void bulkyCargoMultyOrderTest() {
        var sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.CARGO_TYPE_REQUIRED_TAG_ENABLED,
                true
        );

        RoutingOrderTag tag = new RoutingOrderTag("bulky_cargo", "КГТ", BigDecimal.valueOf(400L), BigDecimal.ONE,
                RoutingOrderTagType.ORDER_TYPE, Set.of());
        when(routingOrderTagQueryService.findAllMapByTagName())
                .thenReturn(Map.of("bulky_cargo", tag));

        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                true,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);
        assertThat(location.getRequiredTags()).isNullOrEmpty();

        generated = GeoPointGenerator.generateLonLat();
        geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                false,
                false
        );

        settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getRequiredTags()).contains("bulky_cargo");
    }

    @Test
    void noBulkyCargoLockerOrderTest() {
        var sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.CARGO_TYPE_REQUIRED_TAG_ENABLED,
                true
        );

        RoutingOrderTag tag = new RoutingOrderTag("bulky_cargo", "КГТ", BigDecimal.valueOf(400L), BigDecimal.ONE,
                RoutingOrderTagType.ORDER_TYPE, Set.of());
        when(routingOrderTagQueryService.findAllMapByTagName())
                .thenReturn(Map.of("bulky_cargo", tag));

        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getRequiredTags()).isEmpty();

    }

    @Test
    void noBulkyCargoForOtherScTest() {
        var sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.CARGO_TYPE_REQUIRED_TAG_ENABLED,
                true
        );

        RoutingOrderTag tag = new RoutingOrderTag("bulky_cargo", "КГТ", BigDecimal.valueOf(400L), BigDecimal.ONE,
                RoutingOrderTagType.ORDER_TYPE, Set.of());
        when(routingOrderTagQueryService.findAllMapByTagName())
                .thenReturn(Map.of("bulky_cargo", tag));

        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);
        assertThat(location.getRequiredTags().get(0)).isEqualTo("bulky_cargo");

        tag = new RoutingOrderTag("bulky_cargo", "КГТ", BigDecimal.valueOf(400L), BigDecimal.ONE,
                RoutingOrderTagType.ORDER_TYPE, Set.of());
        when(routingOrderTagQueryService.findAllMapByTagName()).thenReturn(Map.of("bulky_cargo", tag));

        generated = GeoPointGenerator.generateLonLat();
        geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                false,
                false
        );

        settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);

        assertThat(location.getRequiredTags()).isEmpty();

    }

    @Test
    void pickupTypeForClientReturnItem() {
        //given
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.CLIENT_RETURN,
                "multiOrderId",
                0,
                "1",
                new RoutingAddress("address1", new RoutingGeoPoint(BigDecimal.ONE, BigDecimal.ONE)),
                RelativeTimeInterval.valueOf("10:00-14:00"),
                "orderId=multiOrderId",
                Set.of(),
                Set.of("reg120542"),
                null,
                false,
                DimensionsClass.REGULAR_CARGO,
                120542,
                140,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                false,
                false
        );

        //when
        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);
        OrderLocation orderLocation = mvrpLocationMapper.mapDeliveryLocation(item, settings, Set.of());

        //then
        Assertions.assertThat(orderLocation.getType()).isEqualTo(RoutingLocationType.pickup.name());
    }

    @Test
    void calculatedAdditionalTimeForSurvey() {
        when(globalSettingsProvider.getValueAsInteger(OPTIONAL_TAG_ROUTE_COST)).thenReturn(java.util.Optional.of(10));
        when(globalSettingsProvider.getValueAsInteger(OPTIONAL_TAG_HIGH_ROUTE_COST)).thenReturn(java.util.Optional.of(1000));

        SortingCenter sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.REGION_OPTIONAL_TAG_ROUTE_COST,
                1000L
        );

        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
        String multiOrderId = "m_123_456";
        int subTaskCount = 2;
        RoutingRequestItem item = new RoutingRequestItem(
                RoutingRequestItemType.PVZ,
                multiOrderId,
                subTaskCount,
                "" + sortingCenter.getId(),
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
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
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
        var sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.CARGO_TYPE_REQUIRED_TAG_ENABLED,
                true
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.BULKY_CARGO_VEHICLE_EXECUTE_ONLY_BULKY_CARGO_LOCATION_ENABLED,
                true
        );

        RoutingOrderTag tag = new RoutingOrderTag("bulky_cargo", "КГТ", BigDecimal.valueOf(400L), BigDecimal.ONE,
                RoutingOrderTagType.ORDER_TYPE, Set.of());
        when(routingOrderTagQueryService.findAllMapByTagName())
                .thenReturn(Map.of("bulky_cargo", tag));

        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                false,
                false
        );

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);

        OrderLocation location = mvrpLocationMapper.mapDeliveryLocation(item, settings, null);
        assertThat(location.getRequiredTags()).contains("bulky_cargo");
        assertThat(location.getRequiredTags()).doesNotContain("no_bulky_cargo");


        generated = GeoPointGenerator.generateLonLat();
        geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
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
        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
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
        GeoPoint generated = GeoPointGenerator.generateLonLat();
        RoutingGeoPoint geoPoint = RoutingGeoPoint.ofLatLon(generated.getLatitude(), generated.getLongitude());
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
                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
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

}
