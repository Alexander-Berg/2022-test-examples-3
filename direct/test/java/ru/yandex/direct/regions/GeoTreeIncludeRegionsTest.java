package ru.yandex.direct.regions;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.regions.utils.TestGeoTrees;

import static java.util.List.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@RunWith(Parameterized.class)
public class GeoTreeIncludeRegionsTest {
    private static final long RUSSIA = RUSSIA_REGION_ID;
    private static final long SIBERIA = 59L;
    private static final long MOSCOW = MOSCOW_REGION_ID;
    private static final long MOSCOW_AND_MOSCOW_REGION = MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
    private static final long RYAZAN_REGION = 10776L;
    private static final long RYAZAN = 11L;
    private static final long SAINT_PETERSBURG = 2L;
    private static final long CENTRAL_FEDERAL_DISTRICT = 3L;
    private static final long NORTHWESTERN_FEDERAL_DISTRICT = 17L;
    private static final long SAINT_PETERSBURG_AND_LENINGRAD_REGION = 10174L;
    private static final long AFRICA = 241L;
    private static final long UKRAINE = UKRAINE_REGION_ID;
    private static final long KIEV = 143L;
    private static final long CIS = 166L;
    private static final long CRIMEA = CRIMEA_REGION_ID;

    @Parameterized.Parameter
    public GeoTree geoTree;

    @Parameterized.Parameter(1)
    public List<Long> geoTargeting;

    @Parameterized.Parameter(2)
    public List<Long> regionsToInclude;

    @Parameterized.Parameter(3)
    public List<Long> effectiveGeo;

    @Parameterized.Parameters(name = "geoTree, geoTargeting = {1}, regionsToInclude = {2}, effectiveGeo = {3}")
    public static Collection getParameters() throws IOException {
        GeoTree globalGeoTree = TestGeoTrees.loadGlobalTree();
        GeoTree apiGeoTree = TestGeoTrees.loadApiTree();
        GeoTree ruGeoTree = TestGeoTrees.loadRussianTree();

        return of(new Object[][]{
                // 1 вариант исходного дерева с различными добавлениями
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(SAINT_PETERSBURG),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(NORTHWESTERN_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW)},

                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(RYAZAN),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, RYAZAN, MOSCOW_AND_MOSCOW_REGION, -MOSCOW)},

                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(RYAZAN_REGION),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, RYAZAN_REGION, MOSCOW_AND_MOSCOW_REGION,
                                -MOSCOW)},

                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(MOSCOW),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(MOSCOW, RYAZAN),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, RYAZAN, MOSCOW_AND_MOSCOW_REGION)},

                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(MOSCOW_AND_MOSCOW_REGION),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(MOSCOW_AND_MOSCOW_REGION, RYAZAN),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(MOSCOW_AND_MOSCOW_REGION, RYAZAN_REGION),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, RYAZAN_REGION)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(MOSCOW_AND_MOSCOW_REGION, RYAZAN_REGION, CENTRAL_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID)},

                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(CENTRAL_FEDERAL_DISTRICT, RYAZAN),
                        of(GLOBAL_REGION_ID)},

                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW),
                        of(GLOBAL_REGION_ID),
                        of(GLOBAL_REGION_ID)},

                // 2 вариант исходного дерева с различными добавлениями
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(RYAZAN),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, RYAZAN, MOSCOW_AND_MOSCOW_REGION, -MOSCOW)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(RYAZAN_REGION),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, RYAZAN_REGION, MOSCOW_AND_MOSCOW_REGION,
                                -MOSCOW)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(MOSCOW),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(MOSCOW_AND_MOSCOW_REGION),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(MOSCOW_AND_MOSCOW_REGION, RYAZAN_REGION),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, RYAZAN_REGION)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(RUSSIA),
                        of(GLOBAL_REGION_ID)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(NORTHWESTERN_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN)},


                // 3 вариант исходного дерева с различными добавлениями
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(RUSSIA),
                        of(GLOBAL_REGION_ID)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(MOSCOW_AND_MOSCOW_REGION),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID, -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(SAINT_PETERSBURG),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                SAINT_PETERSBURG, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(NORTHWESTERN_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(NORTHWESTERN_FEDERAL_DISTRICT, CENTRAL_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN),
                        of(GLOBAL_REGION_ID),
                        of(GLOBAL_REGION_ID)},

                // 4 вариант исходного дерева с различными добавлениями
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN, SAINT_PETERSBURG),
                        of(GLOBAL_REGION_ID),
                        of(GLOBAL_REGION_ID)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN, SAINT_PETERSBURG),
                        of(SAINT_PETERSBURG),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                SAINT_PETERSBURG, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN, SAINT_PETERSBURG),
                        of(SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN, SAINT_PETERSBURG),
                        of(NORTHWESTERN_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN, SAINT_PETERSBURG),
                        of(NORTHWESTERN_FEDERAL_DISTRICT, CENTRAL_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID)},
                {globalGeoTree,
                        of(GLOBAL_REGION_ID, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                                MOSCOW_AND_MOSCOW_REGION, -MOSCOW, RYAZAN, SAINT_PETERSBURG),
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(GLOBAL_REGION_ID, -SAINT_PETERSBURG_AND_LENINGRAD_REGION, SAINT_PETERSBURG)},

                // ниже переделанные кейсы из exclude тестов
                {globalGeoTree,
                        of(GLOBAL_REGION_ID),
                        of(),
                        of(GLOBAL_REGION_ID)},
                {globalGeoTree,
                        of(MOSCOW, KIEV),
                        of(UKRAINE),
                        of(MOSCOW, UKRAINE)},
                {globalGeoTree,
                        of(MOSCOW, KIEV),
                        of(RUSSIA, UKRAINE),
                        of(RUSSIA, UKRAINE)},
                {globalGeoTree,
                        of(RUSSIA, UKRAINE),
                        of(UKRAINE),
                        of(RUSSIA, UKRAINE)},
                {globalGeoTree,
                        of(RUSSIA, UKRAINE),
                        of(RUSSIA),
                        of(UKRAINE, RUSSIA)},
                {globalGeoTree,
                        of(CIS),
                        of(UKRAINE),
                        of(CIS)},
                {globalGeoTree,
                        of(CIS, -UKRAINE),
                        of(UKRAINE),
                        of(CIS)},
                {globalGeoTree,
                        of(RUSSIA),
                        of(MOSCOW),
                        of(RUSSIA)},
                {globalGeoTree,
                        of(RUSSIA, -MOSCOW),
                        of(MOSCOW),
                        of(RUSSIA)},
                {globalGeoTree,
                        of(RUSSIA),
                        of(MOSCOW, SAINT_PETERSBURG),

                        of(RUSSIA)},
                {globalGeoTree,
                        of(RUSSIA, -MOSCOW),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(RUSSIA)},
                {globalGeoTree,
                        of(RUSSIA, -SAINT_PETERSBURG),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(RUSSIA)},
                {globalGeoTree,
                        of(RUSSIA, -SAINT_PETERSBURG, -MOSCOW),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(RUSSIA)},

                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT),
                        of(MOSCOW),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -MOSCOW),
                        of(MOSCOW),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -MOSCOW),
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(RUSSIA)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -MOSCOW),
                        of(RUSSIA),
                        of(RUSSIA)},

                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(RUSSIA, -SAINT_PETERSBURG)},

                {globalGeoTree,
                        of(),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(MOSCOW, SAINT_PETERSBURG)},
                {globalGeoTree,
                        of(MOSCOW),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(MOSCOW, SAINT_PETERSBURG)},
                {globalGeoTree,
                        of(MOSCOW),
                        of(SAINT_PETERSBURG),
                        of(MOSCOW, SAINT_PETERSBURG)},

                {globalGeoTree,
                        of(SIBERIA),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(SIBERIA, MOSCOW, SAINT_PETERSBURG)},

                {globalGeoTree,
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(CENTRAL_FEDERAL_DISTRICT, SAINT_PETERSBURG)},
                {globalGeoTree,
                        of(CENTRAL_FEDERAL_DISTRICT, -MOSCOW),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(CENTRAL_FEDERAL_DISTRICT, SAINT_PETERSBURG)},

                {globalGeoTree,
                        of(NORTHWESTERN_FEDERAL_DISTRICT),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(NORTHWESTERN_FEDERAL_DISTRICT, MOSCOW)},
                {globalGeoTree,
                        of(NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(MOSCOW,
                                SAINT_PETERSBURG),
                        of(NORTHWESTERN_FEDERAL_DISTRICT, MOSCOW)},
                {globalGeoTree,
                        of(NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(MOSCOW),
                        of(NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG, MOSCOW)},
                {globalGeoTree,
                        of(NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(SAINT_PETERSBURG),
                        of(NORTHWESTERN_FEDERAL_DISTRICT)},

                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW, NORTHWESTERN_FEDERAL_DISTRICT)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW, NORTHWESTERN_FEDERAL_DISTRICT)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(SAINT_PETERSBURG),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(NORTHWESTERN_FEDERAL_DISTRICT),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT)},

                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(MOSCOW),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW, NORTHWESTERN_FEDERAL_DISTRICT,
                                -SAINT_PETERSBURG)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(NORTHWESTERN_FEDERAL_DISTRICT),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG),
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(RUSSIA, NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG)},

                {globalGeoTree,
                        of(MOSCOW_AND_MOSCOW_REGION),
                        of(MOSCOW, SAINT_PETERSBURG),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG)},

                {globalGeoTree,
                        of(RUSSIA),
                        of(MOSCOW_AND_MOSCOW_REGION),
                        of(RUSSIA)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT),
                        of(MOSCOW_AND_MOSCOW_REGION),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION)},
                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT),
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(RUSSIA)},

                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION)},

                {globalGeoTree,
                        of(),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION)},

                {globalGeoTree,
                        of(SIBERIA),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(SIBERIA, MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION)},

                {globalGeoTree,
                        of(NORTHWESTERN_FEDERAL_DISTRICT),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(NORTHWESTERN_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION)},

                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW_AND_MOSCOW_REGION, NORTHWESTERN_FEDERAL_DISTRICT)},

                {globalGeoTree,
                        of(RUSSIA),
                        of(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(RUSSIA)},

                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT),
                        of(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, MOSCOW)},

                {globalGeoTree,
                        of(CENTRAL_FEDERAL_DISTRICT),
                        of(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(CENTRAL_FEDERAL_DISTRICT, SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree,
                        of(NORTHWESTERN_FEDERAL_DISTRICT),
                        of(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(NORTHWESTERN_FEDERAL_DISTRICT, MOSCOW)},

                {globalGeoTree,
                        of(MOSCOW_AND_MOSCOW_REGION),
                        of(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION)},

                {globalGeoTree,
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -NORTHWESTERN_FEDERAL_DISTRICT),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -NORTHWESTERN_FEDERAL_DISTRICT,
                                MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION)},

                {globalGeoTree,
                        of(AFRICA, RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -NORTHWESTERN_FEDERAL_DISTRICT),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(AFRICA, RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -NORTHWESTERN_FEDERAL_DISTRICT,
                                MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION)},

                {globalGeoTree,
                        of(AFRICA),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(AFRICA, MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION)},

                {globalGeoTree,
                        of(RUSSIA, MOSCOW, SAINT_PETERSBURG),
                        of(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        of(RUSSIA)},

                // translocality
                {globalGeoTree, of(RUSSIA), of(CRIMEA), of(RUSSIA, CRIMEA)},
                {apiGeoTree, of(RUSSIA), of(CRIMEA), of(RUSSIA, CRIMEA)},
                {ruGeoTree, of(RUSSIA), of(CRIMEA), of(RUSSIA)},
                {globalGeoTree, of(UKRAINE), of(CRIMEA), of(UKRAINE)},
                {apiGeoTree, of(UKRAINE), of(CRIMEA), of(UKRAINE, CRIMEA)},
                {ruGeoTree, of(UKRAINE), of(CRIMEA), of(UKRAINE, CRIMEA)},
        });
    }

    @Test
    public void test() {
        assertThat(geoTree.includeRegions(geoTargeting, regionsToInclude), equalTo(effectiveGeo));
    }
}
