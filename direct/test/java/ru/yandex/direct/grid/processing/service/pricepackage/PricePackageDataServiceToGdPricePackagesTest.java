package ru.yandex.direct.grid.processing.service.pricepackage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.testing.data.TestPlacements;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.pricepackage.GdPricePackage;
import ru.yandex.direct.grid.processing.model.pricepackage.GdPricePackageForClient;
import ru.yandex.direct.grid.processing.service.campaign.RegionDescriptionLocalizer;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.testPackagesGoals;
import static ru.yandex.direct.regions.Region.AFRICA_REGION_ID;
import static ru.yandex.direct.regions.Region.ASIA_REGION_ID;
import static ru.yandex.direct.regions.Region.AUSTRALIA_AND_OCEANIA_REGION_ID;
import static ru.yandex.direct.regions.Region.CENTRAL_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.EUROPE_REGION_ID;
import static ru.yandex.direct.regions.Region.FAR_EASTERN_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.NORTHWESTERN_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.NORTH_AMERICA_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_CONTINENT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_PROVINCE;
import static ru.yandex.direct.regions.Region.REGION_TYPE_TOWN;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.SIBERIAN_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.SOUTH_AMERICA_REGION_ID;
import static ru.yandex.direct.regions.Region.SOUTH_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.VOLGA_FEDERAL_DISTRICT_REGION_ID;

@GridProcessingTest
@RunWith(Parameterized.class)
public class PricePackageDataServiceToGdPricePackagesTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();
    @Autowired
    private PricePackageDataService service;
    @Autowired
    private PricePackageService pricePackageService;
    @Autowired
    private RegionDescriptionLocalizer localizer;
    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;
    @Parameterized.Parameter(0)
    public List<Long> geo;
    @Parameterized.Parameter(1)
    public Integer geoType;
    @Parameterized.Parameter(2)
    public List<Long> expectedPriorityRegionIds;

    @Parameterized.Parameters(name = "geo{0} geoType {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        List.of(GLOBAL_REGION_ID),
                        REGION_TYPE_CONTINENT,
                        List.of(EUROPE_REGION_ID,
                                ASIA_REGION_ID,
                                AFRICA_REGION_ID,
                                NORTH_AMERICA_REGION_ID,
                                SOUTH_AMERICA_REGION_ID,
                                AUSTRALIA_AND_OCEANIA_REGION_ID)},
                {
                        List.of(GLOBAL_REGION_ID),
                        2,
                        List.of(166L) // СНГ (исключая Россию)
                },
                {
                        List.of(GLOBAL_REGION_ID),
                        REGION_TYPE_COUNTRY,
                        List.of(RUSSIA_REGION_ID)
                },
                {
                        List.of(RUSSIA_REGION_ID),
                        REGION_TYPE_DISTRICT,
                        List.of(CENTRAL_FEDERAL_DISTRICT_REGION_ID,
                                NORTHWESTERN_FEDERAL_DISTRICT_REGION_ID,
                                SOUTH_FEDERAL_DISTRICT_REGION_ID,
                                VOLGA_FEDERAL_DISTRICT_REGION_ID,
                                SIBERIAN_FEDERAL_DISTRICT_REGION_ID,
                                FAR_EASTERN_FEDERAL_DISTRICT_REGION_ID)
                },
                {
                        List.of(CENTRAL_FEDERAL_DISTRICT_REGION_ID),
                        REGION_TYPE_PROVINCE,
                        List.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)
                },
                {
                        List.of(NORTHWESTERN_FEDERAL_DISTRICT_REGION_ID),
                        REGION_TYPE_PROVINCE,
                        List.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)
                },
                {
                        List.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        REGION_TYPE_TOWN,
                        List.of(MOSCOW_REGION_ID)
                },
        });
    }

    @Test
    public void testTargetingsFixed_GeoExpandedOrder_Ru() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));
        testTargetingsFixed_GeoExpandedOrder();
    }

    @Test
    public void testTargetingsFixed_GeoExpandedOrder_En() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        testTargetingsFixed_GeoExpandedOrder();
    }

    private void testTargetingsFixed_GeoExpandedOrder() {
        PricePackage pricePackage = defaultPricePackage();
        pricePackage.getTargetingsFixed()
                .withGeo(geo)
                .withGeoType(geoType)
                .withGeoExpanded(pricePackageService.getGeoTreeConverter().expandGeo(geo, geoType));

        Map<Long, Placement> pageIdToPlacement = pricePackage.getAllowedPageIds().stream().collect(toMap(e -> e,
                TestPlacements::gradusnikPlacement));

        List<GdPricePackage> gdPricePackages = service.toGdPricePackages(List.of(pricePackage), pageIdToPlacement);
        checkInnerOrder(gdPricePackages.get(0).getTargetingsFixed().getGeoExpanded(), expectedPriorityRegionIds);
        GdPricePackageForClient gdPricePackageForClient = service.toGdPricePackageForClient(pricePackage, 1L);
        checkInnerOrder(gdPricePackageForClient.getTargetingsFixed().getGeoExpanded(), expectedPriorityRegionIds);
    }

    @Test
    public void testTargetingsCustom_GeoExpandedOrder_Ru() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));
        testTargetingsCustom_GeoExpandedOrder();
    }

    @Test
    public void testTargetingsCustom_GeoExpandedOrder_En() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        testTargetingsCustom_GeoExpandedOrder();
    }

    private void testTargetingsCustom_GeoExpandedOrder() {
        PricePackage pricePackage = defaultPricePackage();
        pricePackage.getTargetingsCustom()
                .withGeo(geo)
                .withGeoType(geoType)
                .withGeoExpanded(pricePackageService.getGeoTreeConverter().expandGeo(geo, geoType));

        Map<Long, Placement> pageIdToPlacement = pricePackage.getAllowedPageIds().stream().collect(toMap(e -> e,
                TestPlacements::gradusnikPlacement));

        List<GdPricePackage> gdPricePackages = service.toGdPricePackages(List.of(pricePackage),
                pageIdToPlacement);

        checkInnerOrder(gdPricePackages.get(0).getTargetingsCustom().getGeoExpanded(), expectedPriorityRegionIds);
        GdPricePackageForClient gdPricePackageForClient = service.toGdPricePackageForClient(pricePackage, 1L);
        checkInnerOrder(gdPricePackageForClient.getTargetingsCustom().getGeoExpanded(), expectedPriorityRegionIds);
    }

    private void checkInnerOrder(List<Long> geoExpanded, List<Long> firstElements) {
        var iterator = geoExpanded.iterator();
        for (var expectedElementId : firstElements) {
            assertThat(expectedElementId).isEqualTo(iterator.next());
        }
        if (!iterator.hasNext()) {
            return;
        }
        Long previousId = iterator.next();
        String prevousName = localizer.localize(previousId, pricePackageService.getGeoTree()).getName();
        while (iterator.hasNext()) {
            String nextName = localizer.localize(iterator.next(), pricePackageService.getGeoTree()).getName();
            int actual = nextName.compareTo(prevousName);
            assertThat(actual).isGreaterThan(0);
            prevousName = nextName;
        }
    }

    @Before
    public void initTestPackagesGoals() {
        testCryptaSegmentRepository.clean();
        testCryptaSegmentRepository.addAll(testPackagesGoals());
    }
}
