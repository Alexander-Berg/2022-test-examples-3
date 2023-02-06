package ru.yandex.market.abo.core.outlet.maps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.outlet.maps.model.MapsOutlet;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 26.04.18.
 * надеюсь, я ничего не нарушаю, посягаясь на координаты внутреннего дворика Морозова =)
 */
public class OutletCoordinatesEqualsTest {
    private static final double YA_LAT = 55.734010;
    private static final double YA_LON = 37.587615;

    private static final double YA_LAT_CLOSE = 55.734053;
    private static final double YA_LON_CLOSE = 37.587609;

    private static final double YA_LAT_FAR = 55.735062;
    private static final double YA_LON_FAR = 37.585503;

    @Mock
    private OutletInfo mbiOutlet;
    @Mock
    private MapsOutlet mapsOutlet;
    @Mock
    private GeoInfo mbiGeoInfo;
    @Mock
    private Coordinates mbiCoordinates;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void coordinatesEquals() {
        when(mbiOutlet.getGeoInfo()).thenReturn(mbiGeoInfo);
        when(mbiGeoInfo.getGpsCoordinates()).thenReturn(mbiCoordinates);
        when(mbiCoordinates.getLat()).thenReturn(YA_LAT);
        when(mbiCoordinates.getLon()).thenReturn(YA_LON);

        when(mapsOutlet.getLatitude()).thenReturn(YA_LAT_CLOSE);
        when(mapsOutlet.getLongitude()).thenReturn(YA_LON_CLOSE);

        assertTrue(OutletParamsComparator.coordinatesEqual(mbiOutlet, mapsOutlet));

        when(mapsOutlet.getLatitude()).thenReturn(YA_LAT_FAR);
        when(mapsOutlet.getLongitude()).thenReturn(YA_LON_FAR);

        assertFalse(OutletParamsComparator.coordinatesEqual(mbiOutlet, mapsOutlet));
    }
}
