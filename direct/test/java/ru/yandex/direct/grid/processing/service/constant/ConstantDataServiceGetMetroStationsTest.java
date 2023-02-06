package ru.yandex.direct.grid.processing.service.constant;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.model.constants.GdMetroCity;
import ru.yandex.direct.grid.processing.model.constants.GdMetroStation;
import ru.yandex.direct.grid.processing.model.constants.GdMetroStationsContainer;
import ru.yandex.direct.grid.processing.model.constants.GdMetroStationsFilter;
import ru.yandex.direct.grid.processing.service.offlinereport.OfflineReportValidationService;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.Metro;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
public class ConstantDataServiceGetMetroStationsTest {

    private static ClientId operatorClientId;
    private static Map<Long, Metro> metroMap;

    private GdMetroStationsContainer input;

    @Mock
    private GeoTree geoTree;

    @Mock
    private ClientGeoService clientGeoService;

    @SuppressWarnings("unused")
    @Mock
    private AgencyOfflineReportParametersService agencyOfflineReportParametersService;

    @SuppressWarnings("unused")
    @Mock
    private OfflineReportValidationService offlineReportValidationService;

    @InjectMocks
    private ConstantDataService constantDataService;

    @BeforeClass
    public static void beforeClass() {
        operatorClientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());

        Region moscowRegion = mock(Region.class);
        doReturn(GdMetroCity.MOSCOW.getRegionId())
                .when(moscowRegion).getId();
        Region spbRegion = mock(Region.class);
        doReturn(GdMetroCity.SAINT_PETERSBURG.getRegionId())
                .when(spbRegion).getId();
        Region kievRegion = mock(Region.class);
        doReturn(GdMetroCity.KYIV.getRegionId())
                .when(kievRegion).getId();

        metroMap = ImmutableMap.<Long, Metro>builder()
                .put(GdMetroCity.MOSCOW.getRegionId(), new Metro(1L, moscowRegion, "Парк культуры"))
                .put(GdMetroCity.SAINT_PETERSBURG.getRegionId(), new Metro(2L, spbRegion, "Невский проспект"))
                .put(GdMetroCity.KYIV.getRegionId(), new Metro(3L, kievRegion, "Крещатик"))
                .build();
    }

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);

        doReturn(metroMap)
                .when(geoTree).getMetroMap();
        doReturn(geoTree)
                .when(clientGeoService).getClientTranslocalGeoTree(operatorClientId);

        input = new GdMetroStationsContainer()
                .withFilter(new GdMetroStationsFilter());
    }


    @Test
    public void getAllMetroStations_WhenFilterIsEmpty() {
        List<GdMetroStation> metroStations = constantDataService.getMetroStations(operatorClientId, input);

        List<GdMetroStation> expectedMetroStations = getExpectedMetroStations(metroMap.values());
        assertThat(metroStations)
                .is(matchedBy(beanDiffer(expectedMetroStations)));
    }

    @Test
    public void getMetroStations_ByFilter() {
        input.getFilter().setMetroCityIn(Collections.singleton(GdMetroCity.SAINT_PETERSBURG));
        List<GdMetroStation> metroStations = constantDataService.getMetroStations(operatorClientId, input);

        Metro expectedMetro = metroMap.get(GdMetroCity.SAINT_PETERSBURG.getRegionId());
        List<GdMetroStation> expectedMetroStations = getExpectedMetroStations(Collections.singletonList(expectedMetro));
        assertThat(metroStations)
                .is(matchedBy(beanDiffer(expectedMetroStations)));
    }


    private static List<GdMetroStation> getExpectedMetroStations(Collection<Metro> metroStations) {
        return StreamEx.of(metroStations)
                .map(ConstantsConverter::toGdMetroStation)
                .sorted(Comparator.comparing(GdMetroStation::getMetroStationName))
                .toList();
    }

}
