package ru.yandex.direct.geobasehelper;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.regions.SimpleGeoTreeFactory;
import ru.yandex.geobase.CrimeaStatus;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

public class GeoBaseHelperTest {
    private static final long UNSUPPORTED_REGION_ID = 155L;
    public static final long KOZYLKA_ID = 100997L;
    private static final int KRASNOYARSK_ID = 62;
    private static final long MOSCOW_ID = 213L;
    private static final long MOSCOW_OBLAST_ID = 1L;
    private static final long ZELENOGRAD_ID = 216L;
    private static final long TROITSK_ID = 20674L;

    private GeoTreeFactory geoTreeFactory;
    private GeoTree geoTree;

    @Before
    public void before() {
        geoTree = Mockito.mock(GeoTree.class);
        Mockito.when(geoTree.hasRegion(UNSUPPORTED_REGION_ID)).thenReturn(false);
        Mockito.when(geoTree.hasRegion(Region.BY_REGION_ID)).thenReturn(true);
        geoTreeFactory = new SimpleGeoTreeFactory(Map.of(GeoTreeType.GLOBAL, geoTree));
    }

    @Test
    public void convertToDirectRegionId_NullId() {
        GeoBaseHelper geoBaseHelper = new GeoBaseHelperStub(geoTreeFactory);
        Optional<Long> directRegionId = geoBaseHelper.convertToDirectRegionId(null);
        assertThat(directRegionId.isPresent()).isFalse();
    }

    @Test
    public void convertToDirectRegionId_SupportedId() {
        GeoBaseHelper geoBaseHelper = new GeoBaseHelperStub(geoTreeFactory);
        Optional<Long> directRegionId = geoBaseHelper.convertToDirectRegionId(Region.BY_REGION_ID);
        assertThat(directRegionId.isPresent()).isTrue();
        assertThat(directRegionId.get()).isEqualTo(Region.BY_REGION_ID);
    }

    @Test
    public void shouldUseConvertToDirectRegionIdToMoveUpInTree() {
        var helper = new GeoBaseHelperStub(geoTreeFactory);
        helper.addRegionWithParent(KOZYLKA_ID, singletonList(KRASNOYARSK_ID));
        helper.addRegionWithCoordinates(0, 0, KOZYLKA_ID);
        given(geoTree.hasRegion(eq(KOZYLKA_ID))).willReturn(false);
        given(geoTree.hasRegion(eq((long) KRASNOYARSK_ID))).willReturn(true);

        var result = helper.getNearestDirectRegionId(0, 0, CrimeaStatus.RU);

        assertThat(result).isEqualTo(Optional.of((long) KRASNOYARSK_ID));
    }

    @Test
    public void shouldGoDeepUntilChildrenIsNotEmpty() {
        var helper = new GeoBaseHelperStub(geoTreeFactory);
        helper.addRegionWithCoordinates(0, 0, MOSCOW_OBLAST_ID);
        helper.addCoordinatesByRegionId(2, 2, ZELENOGRAD_ID);
        helper.addCoordinatesByRegionId(1, 1, TROITSK_ID);
        helper.addCoordinatesWithDistance(0, 0, 0, 0, 3);
        helper.addCoordinatesWithDistance(0, 0, 1, 1, 1.4);
        helper.addCoordinatesWithDistance(0, 0, 2, 2, 2.5);

        given(geoTree.hasRegion(eq(MOSCOW_OBLAST_ID))).willReturn(true);
        given(geoTree.getChildren(eq(MOSCOW_OBLAST_ID))).willReturn(singleton(MOSCOW_ID));
        given(geoTree.getChildren(eq(MOSCOW_ID))).willReturn(Set.of(ZELENOGRAD_ID, TROITSK_ID));
        given(geoTree.getChildren(eq(TROITSK_ID))).willReturn(emptySet());

        var result = helper.getNearestDirectRegionId(0, 0, CrimeaStatus.RU);

        assertThat(result).isEqualTo(Optional.of(TROITSK_ID));
    }

    @Test
    public void shouldReturnParentIfParentIsNearerThanChildren() {
        var helper = new GeoBaseHelperStub(geoTreeFactory);
        helper.addRegionWithCoordinates(0, 0, MOSCOW_OBLAST_ID);
        helper.addCoordinatesByRegionId(2, 2, ZELENOGRAD_ID);
        helper.addCoordinatesByRegionId(1, 1, TROITSK_ID);
        helper.addCoordinatesWithDistance(0, 0, 0, 0, 0);
        helper.addCoordinatesWithDistance(0, 0, 1, 1, 1.4);
        helper.addCoordinatesWithDistance(0, 0, 2, 2, 2.5);

        given(geoTree.hasRegion(eq(MOSCOW_OBLAST_ID))).willReturn(true);
        given(geoTree.getChildren(eq(MOSCOW_OBLAST_ID))).willReturn(singleton(MOSCOW_ID));
        given(geoTree.getChildren(eq(MOSCOW_ID))).willReturn(Set.of(ZELENOGRAD_ID, TROITSK_ID));

        var result = helper.getNearestDirectRegionId(0, 0, CrimeaStatus.RU);

        assertThat(result).isEqualTo(Optional.of(MOSCOW_ID));
    }

    @Test
    @Ignore
    //ходит в реальную геобазу
    public void convertToDirectRegionId_WithHttpApi_UnsupportedId() {
        GeoBaseHelper geoBaseHelper = new GeoBaseHttpApiHelper(geoTreeFactory);
        Optional<Long> directRegionId = geoBaseHelper.convertToDirectRegionId(UNSUPPORTED_REGION_ID);
        assertThat(directRegionId.isPresent()).isTrue();
        assertThat(directRegionId.get()).isEqualTo(Region.BY_REGION_ID);
    }
}
