package ru.yandex.market.delivery.mdbapp.components.geo;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.common.util.region.RegionType.CITY;
import static ru.yandex.common.util.region.RegionType.CITY_DISTRICT;
import static ru.yandex.common.util.region.RegionType.COUNTRY;
import static ru.yandex.common.util.region.RegionType.COUNTRY_DISTRICT;
import static ru.yandex.common.util.region.RegionType.SECONDARY_DISTRICT;
import static ru.yandex.common.util.region.RegionType.SUBJECT_FEDERATION;
import static ru.yandex.common.util.region.RegionType.SUBJECT_FEDERATION_DISTRICT;
import static ru.yandex.common.util.region.RegionType.VILLAGE;

@RunWith(Parameterized.class)
public class GeoInfoTest {
    private static final int REGION_ID = 666;
    private final RegionService regionService = mock(RegionService.class);
    private final Location expectedLocation;

    public GeoInfoTest(String msg, EnumMap<RegionType, String> regionNames, Location expectedLocation) {
        this.expectedLocation = expectedLocation;

        RegionTree regionTree = mock(RegionTree.class);
        when(regionService.getRegionTree()).thenReturn(regionTree);
        when(regionTree.getRegion(REGION_ID))
                .thenReturn(new Region(REGION_ID, "name shouldn't be null", RegionType.UNIVERSAL, null));
        regionNames.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> when(regionTree.getTypedParent(REGION_ID, entry.getKey()))
                        .thenReturn(new Region(Integer.MAX_VALUE, entry.getValue(), entry.getKey(), null)));
    }

    @Parameterized.Parameters(name = "{0}")
    @Nonnull
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"City locality instead of village", buildRegionNames("city", "village"), buildLocation("city")},
                {"City locality", buildRegionNames("city", null), buildLocation("city")},
                {"Village locality", buildRegionNames(null, "village"), buildLocation("village")},
                {"Null subregion, set city", buildRegionNamesWithNoSubregion("city"), buildLocation("city", "city")},
                {
                    "Null subregion, null city, set secondaryDistrict",
                    buildRegionNamesWithNoSubregionAndNoCity(),
                    buildLocation("city", "SecondaryDistrict")
                },
                {"Null locality", buildRegionNames(null, null), buildLocation(null)}
        });
    }

    @Nonnull
    private static EnumMap<RegionType, String> buildRegionNamesWithNoSubregionAndNoCity() {
        var regions = buildRegionNames("city", null);
        regions.put(SUBJECT_FEDERATION_DISTRICT, null);
        regions.put(SECONDARY_DISTRICT, "SecondaryDistrict");
        return regions;
    }

    @Nonnull
    private static EnumMap<RegionType, String> buildRegionNamesWithNoSubregion(String city) {
         var regions = buildRegionNames(city, null);
         regions.put(CITY_DISTRICT, "city");
         regions.put(SUBJECT_FEDERATION_DISTRICT, null);
        return regions;
    }

    @Nonnull
    private static EnumMap<RegionType, String> buildRegionNames(String city, String village) {
        EnumMap<RegionType, String> regionNames = new EnumMap<>(RegionType.class);
        regionNames.put(COUNTRY, "Parcel Land");
        regionNames.put(COUNTRY_DISTRICT, "shipment");
        regionNames.put(SUBJECT_FEDERATION, "shipment?");
        regionNames.put(CITY, city);
        regionNames.put(VILLAGE, village);
        regionNames.put(SUBJECT_FEDERATION_DISTRICT, "yet another shipment");

        return regionNames;
    }

    @Nonnull
    private static Location buildLocation(String locality) {
        return buildLocation(locality, "yet another shipment");
    }

    @Nonnull
    private static Location buildLocation(String locality, String subRegion) {
        return new Location()
            .setId(REGION_ID)
            .setCountry("Parcel Land")
            .setFederalDistrict("shipment")
            .setRegion("shipment?")
            .setSubRegion(subRegion)
            .setLocality(locality);
    }

    @Test
    public void getLocationTest() {
        assertThat(new GeoInfo(regionService).getLocation(REGION_ID))
            .as("Wrong location from GeoInfo")
            .isEqualTo(expectedLocation);
    }
}
