package ru.yandex.direct.jobs.placements;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlockKey;
import ru.yandex.direct.core.testing.stub.GeosearchClientStub;
import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.geosearch.model.GeoObject;
import ru.yandex.direct.geosearch.model.Kind;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;

import static com.google.common.base.Preconditions.checkArgument;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;

@JobsTest
@ExtendWith(SpringExtension.class)
class GeoIdDetectorTest {

    private static final String COORDS_1 = "12.345,67.890";
    private static final String COORDS_REVERSE_1 = "67.890,12.345";

    private static final GeoObject MOSCOW_LOCALITY = new GeoObject.Builder()
            .withGeoId(Region.MOSCOW_REGION_ID)
            .withKind(Kind.LOCALITY)
            .build();

    private static final GeoObject MOSCOW_PROVINCE = new GeoObject.Builder()
            .withGeoId(Region.MOSCOW_REGION_ID)
            .withKind(Kind.PROVINCE)
            .build();

    private static final GeoObject SPB_LOCALITY = new GeoObject.Builder()
            .withGeoId(Region.SAINT_PETERSBURG_REGION_ID)
            .withKind(Kind.LOCALITY)
            .build();

    private static final GeoObject SPB_PROVINCE = new GeoObject.Builder()
            .withGeoId(Region.SAINT_PETERSBURG_REGION_ID)
            .withKind(Kind.PROVINCE)
            .build();

    private static final Long NOVOSIB_GEO_ID = 65L;
    private static final GeoObject NOVOSIB = new GeoObject.Builder()
            .withGeoId(NOVOSIB_GEO_ID)
            .withKind(Kind.LOCALITY)
            .build();

    private static final Long NOVOSIB_PROVINCE_GEO_ID = 11316L;
    private static final GeoObject NOVOSIB_PROVINCE = new GeoObject.Builder()
            .withGeoId(NOVOSIB_PROVINCE_GEO_ID)
            .withKind(Kind.PROVINCE)
            .build();


    @Autowired
    private GeoIdDetector geoIdDetector;

    @Autowired
    private GeosearchClient geosearchClientStub;

    @Autowired
    private GeoTreeFactory geoTreeFactory;
    private GeoTree geoTree;

    @BeforeEach
    void before() {
        ((GeosearchClientStub) geosearchClientStub).clearResultsForGetMostExactGeoObjectOfKind();
        geoTree = geoTreeFactory.getGlobalGeoTree();
    }

    @Test
    void detectGeoIds_EmptyList_EmptyResult() {
        Map<PlacementBlockKey, Long> result = geoIdDetector.detectGeoIds(emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    void detectGeoIds_OneBlockInUndetectedLocalityAndUndetectedProvince_EmptyResult() {
        OutdoorBlock outdoorBlock = outdoorBlockWithOneSize(1L, 2L, now(), COORDS_1);

        returnLocalityFromGeocoderClient(COORDS_REVERSE_1, null);
        returnProvinceFromGeocoderClient(COORDS_REVERSE_1, null);

        Map<PlacementBlockKey, Long> result = geoIdDetector.detectGeoIds(singletonList(outdoorBlock));
        assertThat(result).isEmpty();
    }

    @Test
    void detectGeoIds_OneBlockInUndetectedLocalityAndDetectedMoscowProvince_MoscowGeoId() {
        OutdoorBlock outdoorBlock = outdoorBlockWithOneSize(1L, 2L, now(), COORDS_1);

        returnLocalityFromGeocoderClient(COORDS_REVERSE_1, null);
        returnProvinceFromGeocoderClient(COORDS_REVERSE_1, MOSCOW_PROVINCE);

        Map<PlacementBlockKey, Long> result = geoIdDetector.detectGeoIds(singletonList(outdoorBlock));
        assertThat(result)
                .hasSize(1)
                .containsKey(PlacementBlockKey.of(1L, 2L))
                .containsValues(Region.MOSCOW_REGION_ID);
    }

    @Test
    void detectGeoIds_OneBlockInMoscowLocality_MoscowGeoId() {
        OutdoorBlock outdoorBlock = outdoorBlockWithOneSize(1L, 2L, now(), COORDS_1);

        returnLocalityFromGeocoderClient(COORDS_REVERSE_1, MOSCOW_LOCALITY);

        Map<PlacementBlockKey, Long> result = geoIdDetector.detectGeoIds(singletonList(outdoorBlock));
        assertThat(result)
                .hasSize(1)
                .containsKey(PlacementBlockKey.of(1L, 2L))
                .containsValues(Region.MOSCOW_REGION_ID);
    }

    @Test
    void detectGeoIds_OneBlockInLocalityInMoscowProvinceThatExistsInGeoTree_MoscowGeoId() {
        Long odintsovoGeoId = 10743L;
        Long parentProvinceId = geoTree.upRegionToType(odintsovoGeoId, Region.REGION_TYPE_PROVINCE);
        checkArgument(parentProvinceId.equals(Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID));

        GeoObject odintsovo = new GeoObject.Builder()
                .withGeoId(odintsovoGeoId)
                .withKind(Kind.LOCALITY)
                .build();

        OutdoorBlock outdoorBlock = outdoorBlockWithOneSize(1L, 2L, now(), COORDS_1);

        returnLocalityFromGeocoderClient(COORDS_REVERSE_1, odintsovo);

        Map<PlacementBlockKey, Long> result = geoIdDetector.detectGeoIds(singletonList(outdoorBlock));
        assertThat(result)
                .hasSize(1)
                .containsKey(PlacementBlockKey.of(1L, 2L))
                .containsValues(Region.MOSCOW_REGION_ID);
    }

    @Test
    void detectGeoIds_OneBlockInLocalityInSpbProvinceThatDoesNotExistInGeoTree_SpbGeoId() {
        Long sestroretskGeoId = 102557L;
        checkArgument(!geoTree.hasRegion(sestroretskGeoId));
        GeoObject sestroretsk = new GeoObject.Builder()
                .withGeoId(sestroretskGeoId)
                .withKind(Kind.LOCALITY)
                .build();

        OutdoorBlock outdoorBlock = outdoorBlockWithOneSize(1L, 2L, now(), COORDS_1);

        returnLocalityFromGeocoderClient(COORDS_REVERSE_1, sestroretsk);
        returnProvinceFromGeocoderClient(COORDS_REVERSE_1, SPB_PROVINCE);

        Map<PlacementBlockKey, Long> result = geoIdDetector.detectGeoIds(singletonList(outdoorBlock));
        assertThat(result)
                .hasSize(1)
                .containsKey(PlacementBlockKey.of(1L, 2L))
                .containsValues(Region.SAINT_PETERSBURG_REGION_ID);
    }

    @Test
    void detectGeoIds_OneBlockInLocalityOutOfSpecialProvincesThatExistsInGeoTree_ThatLocalityGeoId() {
        checkArgument(geoTree.hasRegion(NOVOSIB_GEO_ID));

        OutdoorBlock outdoorBlock = outdoorBlockWithOneSize(1L, 2L, now(), COORDS_1);

        returnLocalityFromGeocoderClient(COORDS_REVERSE_1, NOVOSIB);

        Map<PlacementBlockKey, Long> result = geoIdDetector.detectGeoIds(singletonList(outdoorBlock));
        assertThat(result)
                .hasSize(1)
                .containsKey(PlacementBlockKey.of(1L, 2L))
                .containsValues(NOVOSIB_GEO_ID);
    }

    @Test
    void detectGeoIds_OneBlockInLocalityOutOfSpecialProvincesThatDoesNotExistInGeoTree_ProvinceGeoId() {
        Long unknownLocalityGeoId = 123456789L;
        checkArgument(!geoTree.hasRegion(unknownLocalityGeoId));

        GeoObject unknownLocality = new GeoObject.Builder()
                .withGeoId(unknownLocalityGeoId)
                .withKind(Kind.LOCALITY)
                .build();

        OutdoorBlock outdoorBlock = outdoorBlockWithOneSize(1L, 2L, now(), COORDS_1);

        returnLocalityFromGeocoderClient(COORDS_REVERSE_1, unknownLocality);
        returnProvinceFromGeocoderClient(COORDS_REVERSE_1, NOVOSIB_PROVINCE);

        Map<PlacementBlockKey, Long> result = geoIdDetector.detectGeoIds(singletonList(outdoorBlock));
        assertThat(result)
                .hasSize(1)
                .containsKey(PlacementBlockKey.of(1L, 2L))
                .containsValues(NOVOSIB_PROVINCE_GEO_ID);
    }

    @Test
    void detectGeoIds_MixedTest() {
        String coords2 = "23.34, 56.78";
        String coordsReversed2 = "56.78,23.34";
        String coords3 = "34.56, 78.90";
        String coordsReversed3 = "78.90,34.56";

        OutdoorBlock outdoorBlock1 = outdoorBlockWithOneSize(1L, 2L, now(), COORDS_1);
        OutdoorBlock outdoorBlock2 = outdoorBlockWithOneSize(2L, 3L, now(), coords2);
        OutdoorBlock outdoorBlock3 = outdoorBlockWithOneSize(3L, 4L, now(), coords3);

        returnLocalityFromGeocoderClient(COORDS_REVERSE_1, SPB_LOCALITY);
        returnLocalityFromGeocoderClient(coordsReversed2, null);
        returnLocalityFromGeocoderClient(coordsReversed3, NOVOSIB);

        Map<PlacementBlockKey, Long> result =
                geoIdDetector.detectGeoIds(asList(outdoorBlock1, outdoorBlock2, outdoorBlock3));
        assertThat(result).hasSize(2)
                .contains(entry(PlacementBlockKey.of(1L, 2L), Region.SAINT_PETERSBURG_REGION_ID))
                .contains(entry(PlacementBlockKey.of(3L, 4L), NOVOSIB_GEO_ID));
    }

    private void returnLocalityFromGeocoderClient(String coordinates, GeoObject geoObject) {
        ((GeosearchClientStub) geosearchClientStub)
                .putResultForGetMostExactGeoObjectOfKind(coordinates, Kind.LOCALITY, geoObject);
    }

    private void returnProvinceFromGeocoderClient(String coordinates, GeoObject geoObject) {
        ((GeosearchClientStub) geosearchClientStub)
                .putResultForGetMostExactGeoObjectOfKind(coordinates, Kind.PROVINCE, geoObject);
    }
}
