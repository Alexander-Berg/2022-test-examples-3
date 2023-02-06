package ru.yandex.market.logistics.management.service.geobase;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.geobase.beans.GeobaseRegionData;
import ru.yandex.market.logistics.management.AbstractTest;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.management.util.TestRegions.BALTIC_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.MOSCOW_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.NEVSKY_PROSPEKT_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.NORTH_DISTRICT_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.RIGA_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.SPB_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;

@ExtendWith(MockitoExtension.class)
class GeoBaseAcceptedRegionsServiceTest extends AbstractTest {

    private static final int NOVOSIBIRSK_ID = 30;
    private static final int SIBIRSKAYA_ID = 31;

    private static final GeobaseRegionData NOVOSIBIRSK = geobaseRegion(NOVOSIBIRSK_ID, RegionType.CITY);
    private static final GeobaseRegionData SIBIRSKAYA =
        geobaseRegion(SIBIRSKAYA_ID, RegionType.METRO_STATION, NOVOSIBIRSK_ID);

    @Mock
    RegionService regionService;
    @Mock
    GeoBaseClientService geoBaseClientService;

    @InjectMocks
    GeoBaseAcceptedRegionsService service;

    private final ArrayList<Runnable> verifications = new ArrayList<>();

    @BeforeEach
    void setUp() {
        doReturn(buildRegionTree())
            .when(regionService).get();
    }

    @AfterEach
    void tearDown() {
        verifications.forEach(Runnable::run);
        verifications.clear();
        verifyNoMoreInteractions(geoBaseClientService);
        verifyNoMoreInteractions(regionService);
    }

    @Test
    void shouldFoundViaRegionService() {
        // when:
        var actual = service.findAcceptedParentRegionId(MOSCOW_ID);

        // then:
        softly.assertThat(actual).isEqualTo(MOSCOW_ID);
    }

    @Test
    void shouldFoundParentRegionIdViaRegionService_whenRegionTypeIsNotAccepted() {
        // when:
        var actual = service.findAcceptedParentRegionId(NEVSKY_PROSPEKT_ID);

        // then:
        softly.assertThat(actual).isEqualTo(SPB_ID);
    }

    @Test
    void shouldFoundLocalityPrecisionRegionIdViaRegionService_whenRegionTypeIsNotAccepted() {
        // when:
        var actual = service.getLocalityPrecisionRegionId(NORTH_DISTRICT_ID);

        // then:
        softly.assertThat(actual).isEqualTo(RIGA_ID);
    }

    @Test
    void shouldFoundViaGeoBase_whenDontFoundInRegionService() {
        // given:
        mockGeobaseGetRegion(NOVOSIBIRSK_ID, NOVOSIBIRSK);

        // when:
        var actual = service.findAcceptedParentRegionId(NOVOSIBIRSK_ID);

        // then:
        softly.assertThat(actual).isEqualTo(NOVOSIBIRSK_ID);
    }

    @Test
    void shouldFoundViaGeoBase_whenDontFoundInRegionServiceForLocalityPrecision() {
        // given:
        mockGeobaseGetRegion(NOVOSIBIRSK_ID, NOVOSIBIRSK);
        mockGeobaseGetRegion(SIBIRSKAYA_ID, SIBIRSKAYA);

        // when:
        var actual = service.getLocalityPrecisionRegionId(SIBIRSKAYA_ID);

        // then:
        softly.assertThat(actual).isEqualTo(NOVOSIBIRSK_ID);
    }
    @Test
    void shouldFoundParentRegionIdViaGeoBase_whenDontFoundInRegionService() {
        // given:
        mockGeobaseGetRegion(NOVOSIBIRSK_ID, NOVOSIBIRSK);
        mockGeobaseGetRegion(SIBIRSKAYA_ID, SIBIRSKAYA);

        // when:
        var actual = service.findAcceptedParentRegionId(SIBIRSKAYA_ID);

        // then:
        softly.assertThat(actual).isEqualTo(NOVOSIBIRSK_ID);
    }

    @Test
    void shouldReturnNull_whenNotFoundViaGeoBase() {
        // given:
        mockGeobaseGetRegion(NOVOSIBIRSK_ID, null);

        // when:
        var actual = service.findAcceptedParentRegionId(NOVOSIBIRSK_ID);

        // then:
        softly.assertThat(actual).isNull();
    }

    @Test
    void shouldReturnNull_whenNotFoundViaGeoBaseForLocalityPrecision() {
        // given:
        mockGeobaseGetRegion(SIBIRSKAYA_ID, null);

        // when:
        var actual = service.getLocalityPrecisionRegionId(SIBIRSKAYA_ID);

        // then:
        softly.assertThat(actual).isNull();
    }

    @Test
    void shouldReturnNull_whenNotAcceptedRegionHasNoParent() {
        // given:
        mockGeobaseGetRegion(SIBIRSKAYA_ID, geobaseRegion(SIBIRSKAYA_ID, RegionType.METRO_STATION));

        // when:
        var actual = service.findAcceptedParentRegionId(SIBIRSKAYA_ID);

        // then:
        softly.assertThat(actual).isNull();
    }

    @Test
    void shouldReturnNull_whenNoLocalityPrecisionParentExist() {
        // when:
        var actual = service.getLocalityPrecisionRegionId(BALTIC_ID);

        // then:
        softly.assertThat(actual).isNull();
    }

    private static GeobaseRegionData geobaseRegion(int id, RegionType type, int parentId) {
        GeobaseRegionData geobaseRegion = geobaseRegion(id, type);
        geobaseRegion.setParentId(parentId);
        return geobaseRegion;
    }

    private static GeobaseRegionData geobaseRegion(int id, RegionType type) {
        GeobaseRegionData geobaseRegion = new GeobaseRegionData();
        geobaseRegion.setId(id);
        geobaseRegion.setType(type.getCode());
        return geobaseRegion;
    }

    private void mockGeobaseGetRegion(int regionId, GeobaseRegionData geobaseRegion) {
        doReturn(geobaseRegion)
            .when(geoBaseClientService).getRegion(regionId);
        verifications.add(() -> verify(geoBaseClientService).getRegion(regionId));
    }
}
