package ru.yandex.market.logistics.management.service.geobase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.geobase.beans.GeobaseRegionData;
import ru.yandex.market.logistics.management.AbstractTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GeoBaseClientServiceTest extends AbstractTest {

    private static final double LATITUDE = 100;
    private static final double LONGITUDE = 200;
    private static final int REGION_ID = 12345;

    @Mock
    HttpGeobase httpGeobase;

    @InjectMocks
    GeoBaseClientService service;

    @Test
    void getRegionId__shouldReturnRegionId() {
        // given:
        doReturn(REGION_ID)
            .when(httpGeobase).getRegionId(anyDouble(), anyDouble());

        // when:
        int actual = service.getRegionId(LATITUDE, LONGITUDE);

        // then:
        softly.assertThat(actual).isEqualTo(REGION_ID);
        verify(httpGeobase).getRegionId(LATITUDE, LONGITUDE);
    }

    @Test
    void getRegionId__shouldThrowGeoBaseClientException_whenHttpGeobaseThrowAnyException() {
        // given:
        doThrow(new RuntimeException())
            .when(httpGeobase).getRegionId(anyDouble(), anyDouble());

        // expect:
        assertThrows(GeoBaseClientException.class, () -> service.getRegionId(LATITUDE, LONGITUDE));
    }

    @Test
    void getRegion__shouldReturnRegion() {
        // given:
        doReturn(region())
            .when(httpGeobase).getRegion(anyInt());

        // when:
        GeobaseRegionData actual = service.getRegion(REGION_ID);

        // then:
        softly.assertThat(actual).isEqualToComparingFieldByField(region());
        verify(httpGeobase).getRegion(REGION_ID);
    }

    @Test
    void getRegion__shouldThrowGeoBaseClientException_whenHttpGeobaseThrowAnyException() {
        // given:
        doThrow(new RuntimeException())
            .when(httpGeobase).getRegion(anyInt());

        // expect:
        assertThrows(GeoBaseClientException.class, () -> service.getRegion(REGION_ID));
    }

    private GeobaseRegionData region() {
        GeobaseRegionData region = new GeobaseRegionData();
        region.setId(REGION_ID);
        return region;
    }
}
