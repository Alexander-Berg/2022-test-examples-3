package ru.yandex.market.tpl.core.domain.order.address;

import java.util.EnumMap;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.tpl.core.domain.region.TplRegionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressEnrichServiceTest {

    private static final int REGION_ID = 666;
    private static final String COUNTRY = "Russia";
    private static final String FEDERAL_DISTRICT = "Country District";
    private static final String REGION = "Subject Federation";
    private static final String CITY = "city";
    private static final String SECONDARY_DISTRICT = "SecondaryDistrict";
    private static final String VILLAGE = "village";
    public static final String SUB_REGION = "Subject Federation District";

    @Mock
    private TplRegionService tplRegionService;

    @InjectMocks
    private AddressEnrichService service;

    @Nonnull
    public static Stream<Arguments> getParameters() {
        return Stream.of(
                arguments("City locality", buildRegionNames(CITY, null), buildRegionInfo(CITY)),
                arguments("City locality instead of village",
                        buildRegionNames(CITY, VILLAGE),
                        buildRegionInfo(CITY)),
                arguments("Village locality",
                        buildRegionNames(null, VILLAGE),
                        buildRegionInfo(VILLAGE)),
                arguments("Null subregion, set city",
                        buildRegionNamesWithNoSubregion(CITY),
                        buildRegionInfo(CITY, CITY)),
                arguments("Null subregion, null city, set secondaryDistrict",
                        buildRegionNamesWithNoSubregionAndNoCity(),
                        buildRegionInfo(CITY, SECONDARY_DISTRICT)),
                arguments("Null locality", buildRegionNames(null, null), buildRegionInfo(null))
        );
    }

    @Nonnull
    private static EnumMap<RegionType, String> buildRegionNamesWithNoSubregionAndNoCity() {
        var regions = buildRegionNames(CITY, null);
        regions.put(RegionType.SUBJECT_FEDERATION_DISTRICT, null);
        regions.put(RegionType.SECONDARY_DISTRICT, SECONDARY_DISTRICT);
        return regions;
    }

    @Nonnull
    private static EnumMap<RegionType, String> buildRegionNamesWithNoSubregion(String city) {
        var regions = buildRegionNames(city, null);
        regions.put(RegionType.CITY_DISTRICT, CITY);
        regions.put(RegionType.SUBJECT_FEDERATION_DISTRICT, null);
        return regions;
    }

    @Nonnull
    private static EnumMap<RegionType, String> buildRegionNames(String city, String village) {
        EnumMap<RegionType, String> regionNames = new EnumMap<>(RegionType.class);
        regionNames.put(RegionType.COUNTRY, COUNTRY);
        regionNames.put(RegionType.COUNTRY_DISTRICT, FEDERAL_DISTRICT);
        regionNames.put(RegionType.SUBJECT_FEDERATION, REGION);
        regionNames.put(RegionType.CITY, city);
        regionNames.put(RegionType.VILLAGE, village);
        regionNames.put(RegionType.SUBJECT_FEDERATION_DISTRICT, SUB_REGION);
        return regionNames;
    }

    @Nonnull
    private static RegionInfo buildRegionInfo(String locality) {
        return buildRegionInfo(locality, SUB_REGION);
    }

    @Nonnull
    private static RegionInfo buildRegionInfo(String locality, String subRegion) {
        return RegionInfo.builder()
                .id(REGION_ID)
                .country(COUNTRY)
                .federalDistrict(FEDERAL_DISTRICT)
                .region(REGION)
                .subRegion(subRegion)
                .locality(locality)
                .build();
    }

    void initMocks(EnumMap<RegionType, String> regionNames) {
        var regionTree = mock(RegionTree.class);
        when(tplRegionService.getRegionTree()).thenReturn(regionTree);
        when(tplRegionService.getOrBuildRegionTree()).thenReturn(regionTree);
        when(regionTree.getRegion(REGION_ID))
                .thenReturn(new Region(REGION_ID, "name shouldn't be null", RegionType.UNIVERSAL, null));
        regionNames.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> lenient().when(regionTree.getTypedParent(REGION_ID, entry.getKey()))
                        .thenReturn(new Region(Integer.MAX_VALUE, entry.getValue(), entry.getKey(), null)));
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void doTest(String msg, EnumMap<RegionType, String> regionNames, RegionInfo expectedRegionInfo) {
        // given
        initMocks(regionNames);
        // when
        var regionInfo = service.getRegionInfo(REGION_ID);
        // then
        assertThat(regionInfo).isEqualTo(expectedRegionInfo);
    }

    @Nonnull
    public static Stream<Arguments> testEnrichDeliveryAddressWhenRegionIdIs() {
        return Stream.of(
                arguments("Enrich when regionId", REGION_ID, (long) REGION_ID),
                arguments("Enrich when regionId and original null", REGION_ID, null),
                arguments("Enrich when originalRegionId", -1, (long) REGION_ID),
                arguments("Enrich when originalRegionId and id null", null, (long) REGION_ID)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testEnrichDeliveryAddressWhenRegionIdIs(String msg, Integer regionId, Long originalRegionId) {
        // given
        initMocks(buildRegionNames(CITY, null));
        var deliveryAddress = DeliveryAddress.builder()
                .regionId(regionId)
                .originalRegionId(originalRegionId)
                .build();
        // when
        service.enrichWithRegionInfo(deliveryAddress);
        // then
        assertThat(deliveryAddress.getCountry()).isEqualTo(COUNTRY);
        assertThat(deliveryAddress.getFederalDistrict()).isEqualTo(FEDERAL_DISTRICT);
        assertThat(deliveryAddress.getRegion()).isEqualTo(REGION);
        assertThat(deliveryAddress.getSubRegion()).isEqualTo(SUB_REGION);
        assertThat(deliveryAddress.getCity()).isEqualTo(CITY);
    }

    @Test
    void testEnrichDeliveryAddressWhenNoRegions() {
        // given
        var deliveryAddress = DeliveryAddress.builder()
                .regionId(null)
                .originalRegionId(null)
                .build();
        // when
        service.enrichWithRegionInfo(deliveryAddress);
        // then
        assertThat(deliveryAddress.getCountry()).isNull();
        assertThat(deliveryAddress.getFederalDistrict()).isNull();
        assertThat(deliveryAddress.getRegion()).isNull();
        assertThat(deliveryAddress.getSubRegion()).isNull();
        assertThat(deliveryAddress.getCity()).isNull();
    }

    @Test
    void testEnrichDeliveryAddressWhenRegionIsNotFound() {
        // given
        var regionTree = mock(RegionTree.class);
        when(tplRegionService.getOrBuildRegionTree()).thenReturn(regionTree);
        when(regionTree.getRegion(REGION_ID)).thenReturn(null);
        var deliveryAddress = DeliveryAddress.builder()
                .regionId(REGION_ID)
                .build();
        // when
        service.enrichWithRegionInfo(deliveryAddress);
        // then
        assertThat(deliveryAddress.getCountry()).isNull();
        assertThat(deliveryAddress.getFederalDistrict()).isNull();
        assertThat(deliveryAddress.getRegion()).isNull();
        assertThat(deliveryAddress.getSubRegion()).isNull();
        assertThat(deliveryAddress.getCity()).isNull();
    }

    @Test
    void testEnrichDeliveryAddressDontSetCityIfPresent() {
        // given
        initMocks(buildRegionNames(CITY, null));
        var deliveryAddress = DeliveryAddress.builder()
                .regionId(REGION_ID)
                .originalRegionId((long) REGION_ID)
                .city("Another city")
                .build();
        // when
        service.enrichWithRegionInfo(deliveryAddress);
        // then
        assertThat(deliveryAddress.getCountry()).isEqualTo(COUNTRY);
        assertThat(deliveryAddress.getFederalDistrict()).isEqualTo(FEDERAL_DISTRICT);
        assertThat(deliveryAddress.getRegion()).isEqualTo(REGION);
        assertThat(deliveryAddress.getSubRegion()).isEqualTo(SUB_REGION);
        assertThat(deliveryAddress.getCity()).isEqualTo("Another city");
    }

}
