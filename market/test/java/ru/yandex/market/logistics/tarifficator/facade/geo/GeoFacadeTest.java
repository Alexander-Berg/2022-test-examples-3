package ru.yandex.market.logistics.tarifficator.facade.geo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistics.tarifficator.AbstractUnitTest;
import ru.yandex.market.logistics.tarifficator.model.pricelist.raw.LocationRaw;
import ru.yandex.market.logistics.tarifficator.service.geo.GeoService;
import ru.yandex.market.logistics.tarifficator.service.geo.GeoServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Unit-тест фасада GeoFacade")
@ParametersAreNonnullByDefault
class GeoFacadeTest extends AbstractUnitTest {

    private static final AtomicInteger REGION_ID_SEQUENCE = new AtomicInteger(0);
    private static final AtomicInteger GEO_ID_SEQUENCE = new AtomicInteger(0);

    private static final Set<Region> REGIONS = Arrays.stream(RegionType.values())
        .map(type -> new Region(
            REGION_ID_SEQUENCE.addAndGet(1),
            "Локация " + type.getDescription(),
            type,
            null
        ))
        .collect(Collectors.toSet());

    private static final Set<GeoObject> GEO_OBJECTS = Arrays.stream(Kind.values())
        .map(
            kind -> SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid(String.valueOf(GEO_ID_SEQUENCE.addAndGet(1)))
                    .withKind(kind)
                    .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .withAddressLine("Локация " + kind.name().toLowerCase())
                    .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build()
        )
        .collect(Collectors.toSet());

    @SuppressWarnings("unchecked")
    private final RegionTree<Region> regionTree = (RegionTree<Region>) mock(RegionTree.class);
    private final GeoClient geoClient = mock(GeoClient.class);
    private final RegionService regionService = mock(RegionService.class);
    private final GeoService geoService = new GeoServiceImpl(geoClient, regionService);
    private final GeoFacade geoFacade = new GeoFacadeImpl(geoService, regionService);

    @BeforeEach
    void init() {
        when(regionService.getRegionTree()).thenReturn(regionTree);
        REGIONS.forEach(region -> when(regionTree.getRegion(eq(region.getId()), eq(false))).thenReturn(region));
        GEO_OBJECTS.forEach(
            geoObject -> when(geoClient.find(eq(geoObject.getAddressLine()), any(GeoSearchParams.class)))
                .thenReturn(List.of(geoObject))
        );
    }

    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(false, false),
            Arguments.of(false, true),
            Arguments.of(true, false),
            Arguments.of(true, true)
        );
    }

    @DisplayName("Обогащение локаций гео-данными по geoId не зависит от флагов фильтрации")
    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource("arguments")
    void enrichLocationByGeoWithoutFiltration(boolean filterType, boolean extendedLocations) throws Exception {
        Map<LocationRaw, LocationRaw> enrichedLocations = geoFacade.enrichGeoInfo(
            locationsWithGeoId(), filterType, extendedLocations
        ).get();

        Map<LocationRaw, LocationRaw> locations = ImmutableMap.<LocationRaw, LocationRaw>builder()
            .put(location(1), foundByGeoId(1, "Локация континент"))
            .put(location(2), foundByGeoId(2, "Локация регион"))
            .put(location(3), foundByGeoId(3, "Локация страна"))
            .put(location(4), foundByGeoId(4, "Локация федеральный округ"))
            .put(location(5), foundByGeoId(5, "Локация субъект федерации"))
            .put(location(6), foundByGeoId(6, "Локация город"))
            .put(location(7), foundByGeoId(7, "Локация село"))
            .put(location(8), foundByGeoId(8, "Локация район города"))
            .put(location(9), foundByGeoId(9, "Локация станция метро"))
            .put(location(10), foundByGeoId(10, "Локация район субъекта федерации"))
            .put(location(11), foundByGeoId(11, "Локация пригород"))
            .put(location(12), foundByGeoId(12, "Локация заморская территория"))
            .put(location(13), foundByGeoId(13, "Локация район города второго уровня"))
            .put(location(14), foundByGeoId(14, "Локация станция монорельса"))
            .put(location(15), foundByGeoId(15, "Локация поселение"))
            .put(location(16), foundByGeoId(16, "Локация Прочее"))
            .put(location(17), foundByGeoId(17, "Локация универсальное"))
            .put(location(18), foundByGeoId(18, "Локация Скрытые"))
            .build();

        softly.assertThat(enrichedLocations).isEqualTo(locations);
    }

    @Test
    @DisplayName("Обогащение локаций гео-данными по адресу с фильтрацией по типам")
    void enrichLocationAddressWithFiltration() throws Exception {
        Map<LocationRaw, LocationRaw> enrichedLocations = geoFacade.enrichGeoInfo(
            locationsWithAddress(), true, false
        ).get();
        softly.assertThat(enrichedLocations).isEqualTo(
            ImmutableMap.<LocationRaw, LocationRaw>builder()
                .put(location("Локация house"), notFound("Локация house"))
                .put(location("Локация street"), notFound("Локация street"))
                .put(location("Локация metro"), notFound("Локация metro"))
                .put(location("Локация district"), foundByAddress(4, "Локация district"))
                .put(location("Локация locality"), foundByAddress(5, "Локация locality"))
                .put(location("Локация area"), notFound("Локация area"))
                .put(location("Локация province"), notFound("Локация province"))
                .put(location("Локация country"), notFound("Локация country"))
                .put(location("Локация hydro"), notFound("Локация hydro"))
                .put(location("Локация railway"), notFound("Локация railway"))
                .put(location("Локация route"), notFound("Локация route"))
                .put(location("Локация vegetation"), notFound("Локация vegetation"))
                .put(location("Локация cemetery"), notFound("Локация cemetery"))
                .put(location("Локация bridge"), notFound("Локация bridge"))
                .put(location("Локация km"), notFound("Локация km"))
                .put(location("Локация airport"), notFound("Локация airport"))
                .put(location("Локация other"), notFound("Локация other"))
                .put(location("Локация region"), notFound("Локация region"))
                .put(location("Локация station"), notFound("Локация station"))
                .put(location("Локация entrance"), notFound("Локация entrance"))
                .put(location("Локация unknown"), notFound("Локация unknown"))
                .build()
        );
    }

    @Test
    @DisplayName("Обогащение локаций гео-данными по адресу с фильтрацией по расширенным типам")
    void enrichLocationAddressWithExtendedFiltration() throws Exception {
        Map<LocationRaw, LocationRaw> enrichedLocations = geoFacade.enrichGeoInfo(
            locationsWithAddress(), true, true
        ).get();
        softly.assertThat(enrichedLocations).isEqualTo(
            ImmutableMap.<LocationRaw, LocationRaw>builder()
                .put(location("Локация house"), notFound("Локация house"))
                .put(location("Локация street"), notFound("Локация street"))
                .put(location("Локация metro"), notFound("Локация metro"))
                .put(location("Локация district"), foundByAddress(4, "Локация district"))
                .put(location("Локация locality"), foundByAddress(5, "Локация locality"))
                .put(location("Локация area"), foundByAddress(6, "Локация area"))
                .put(location("Локация province"), notFound("Локация province"))
                .put(location("Локация country"), notFound("Локация country"))
                .put(location("Локация hydro"), notFound("Локация hydro"))
                .put(location("Локация railway"), notFound("Локация railway"))
                .put(location("Локация route"), notFound("Локация route"))
                .put(location("Локация vegetation"), notFound("Локация vegetation"))
                .put(location("Локация cemetery"), notFound("Локация cemetery"))
                .put(location("Локация bridge"), notFound("Локация bridge"))
                .put(location("Локация km"), notFound("Локация km"))
                .put(location("Локация airport"), notFound("Локация airport"))
                .put(location("Локация other"), notFound("Локация other"))
                .put(location("Локация region"), notFound("Локация region"))
                .put(location("Локация station"), notFound("Локация station"))
                .put(location("Локация entrance"), notFound("Локация entrance"))
                .put(location("Локация unknown"), notFound("Локация unknown"))
                .build()
        );
    }

    @Test
    @DisplayName("Обогащение локаций гео-данными по адресу без фильтрации по типам")
    void enrichLocationAddressWithoutFiltration() throws Exception {
        Map<LocationRaw, LocationRaw> enrichedLocations = geoFacade.enrichGeoInfo(
            locationsWithAddress(), false, false
        ).get();

        Map<LocationRaw, LocationRaw> enrichedLocationsExtended = geoFacade.enrichGeoInfo(
            locationsWithAddress(), false, true
        ).get();

        Map<LocationRaw, LocationRaw> locations = ImmutableMap.<LocationRaw, LocationRaw>builder()
            .put(location("Локация house"), foundByAddress(1, "Локация house"))
            .put(location("Локация street"), foundByAddress(2, "Локация street"))
            .put(location("Локация metro"), foundByAddress(3, "Локация metro"))
            .put(location("Локация district"), foundByAddress(4, "Локация district"))
            .put(location("Локация locality"), foundByAddress(5, "Локация locality"))
            .put(location("Локация area"), foundByAddress(6, "Локация area"))
            .put(location("Локация province"), foundByAddress(7, "Локация province"))
            .put(location("Локация country"), foundByAddress(8, "Локация country"))
            .put(location("Локация hydro"), foundByAddress(9, "Локация hydro"))
            .put(location("Локация railway"), foundByAddress(10, "Локация railway"))
            .put(location("Локация route"), foundByAddress(11, "Локация route"))
            .put(location("Локация vegetation"), foundByAddress(12, "Локация vegetation"))
            .put(location("Локация cemetery"), foundByAddress(13, "Локация cemetery"))
            .put(location("Локация bridge"), foundByAddress(14, "Локация bridge"))
            .put(location("Локация km"), foundByAddress(15, "Локация km"))
            .put(location("Локация airport"), foundByAddress(16, "Локация airport"))
            .put(location("Локация other"), foundByAddress(17, "Локация other"))
            .put(location("Локация region"), foundByAddress(18, "Локация region"))
            .put(location("Локация station"), foundByAddress(19, "Локация station"))
            .put(location("Локация entrance"), foundByAddress(20, "Локация entrance"))
            .put(location("Локация unknown"), foundByAddress(21, "Локация unknown"))
            .build();

        softly.assertThat(enrichedLocations).isEqualTo(locations);
        softly.assertThat(enrichedLocationsExtended).isEqualTo(enrichedLocations);
    }

    @Nonnull
    private Set<LocationRaw> locationsWithGeoId() {
        return REGIONS.stream()
            .map(region -> foundByGeoId(region.getId(), null))
            .collect(Collectors.toSet());
    }

    @Nonnull
    private Set<LocationRaw> locationsWithAddress() {
        return GEO_OBJECTS.stream()
            .map(geoObject -> LocationRaw.builder().address(geoObject.getAddressLine()).build())
            .collect(Collectors.toSet());
    }

    @Nonnull
    private static LocationRaw location(Integer geoId) {
        return foundByGeoId(geoId, null);
    }

    @Nonnull
    private static LocationRaw location(String address) {
        return LocationRaw.builder()
            .address(address)
            .build();
    }

    @Nonnull
    private static LocationRaw foundByGeoId(@Nullable Integer geoId, @Nullable String locationAddress) {
        return LocationRaw.builder()
            .geoId(geoId)
            .locationAddress(locationAddress)
            .build();
    }

    @Nonnull
    private static LocationRaw foundByAddress(@Nullable Integer geoId, String address) {
        return LocationRaw.builder()
            .geoId(geoId)
            .address(address)
            .locationAddress(address)
            .build();
    }

    @Nonnull
    private static LocationRaw notFound() {
        return LocationRaw.builder().build();
    }

    @Nonnull
    private static LocationRaw notFound(String address) {
        return location(address);
    }
}
