package ru.yandex.direct.core.entity.pricepackage.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.util.GeoTreeConverter;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.utils.TestGeoTrees;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTHWESTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.SAINT_PETERSBURG_PROVINCE;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_PROVINCE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(Parameterized.class)
public class PricePackageGeoProcessorTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static GeoTree geoTreeGlobal = TestGeoTrees.loadGlobalTree();

    private static GeoTree geoTreeRussian = TestGeoTrees.loadRussianTree();

    @Parameterized.Parameter(0)
    public GeoTree geoTree;

    @Parameterized.Parameter(1)
    public Integer geoType;

    @Parameterized.Parameter(2)
    public List<Long> geo;

    @Parameterized.Parameter(3)
    public List<Long> geoExpanded;

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{geoTreeGlobal, REGION_TYPE_DISTRICT, List.of(RUSSIA),
                        List.of(NORTHWESTERN_DISTRICT, CENTRAL_DISTRICT)},
                new Object[]{geoTreeGlobal, REGION_TYPE_DISTRICT, List.of(RUSSIA, -NORTHWESTERN_DISTRICT),
                        List.of(CENTRAL_DISTRICT)},
                new Object[]{geoTreeGlobal, REGION_TYPE_PROVINCE, List.of(RUSSIA, -CENTRAL_DISTRICT),
                        List.of(SAINT_PETERSBURG_PROVINCE)},
                new Object[]{geoTreeRussian, REGION_TYPE_PROVINCE, List.of(RUSSIA),
                        List.of(CRIMEA_REGION_ID)}
        );
    }


    @Test
    public void test() {
        var geoProcessor = new PricePackageGeoProcessor(new GeoTreeConverter(geoTree));
        var targetings = new TargetingsFixed()
                .withGeo(geo)
                .withGeoType(geoType);

        geoProcessor.expandGeo(targetings);

        var expectedAfterPrepare = new TargetingsFixed()
                .withGeo(geo)
                .withGeoType(geoType)
                .withGeoExpanded(geoExpanded);
        assertThat(targetings).is(matchedBy(beanDiffer(expectedAfterPrepare)));
    }

}
