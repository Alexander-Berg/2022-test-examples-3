package ru.yandex.market.abo.core.outlet.maps;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.market.abo.core.outlet.maps.model.MapsOutlet;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.OutletInfo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 29.05.18.
 */
public class OutletAddressesEqualsTest {
    private static final String MBI_ADDRESS = "mbi address";
    private static final String MAPS_ADDRESS = "maps address";

    @InjectMocks
    private OutletParamsComparator outletParamsComparator;

    @Mock
    private GeoClient geoClient;

    @Mock
    private GeoObject mbiGeoObject;

    @Mock
    private GeoObject mapsGeoObject;

    @Mock
    private OutletInfo mbiOutlet;

    @Mock
    private Address address;

    @Mock
    private MapsOutlet mapsOutlet;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(mapsOutlet.getAddress()).thenReturn(MAPS_ADDRESS);
        when(mbiOutlet.getAddress()).thenReturn(address);
        when(address.searchableStringWithNoAddAddr()).thenReturn(MBI_ADDRESS);

        when(geoClient.findFirst(MBI_ADDRESS)).thenReturn(Optional.of(mbiGeoObject));
        when(geoClient.findFirst(MAPS_ADDRESS)).thenReturn(Optional.of(mapsGeoObject));
    }

    @Test
    public void addressesEqual() {
        String address = "NeverLand";
        when(mbiGeoObject.getAddressLine()).thenReturn(address);
        when(mapsGeoObject.getAddressLine()).thenReturn(address);

        assertTrue(outletParamsComparator.addressLineEqual(mbiOutlet, mapsOutlet));
    }

    @Test
    public void addressesDiff() {
        when(mbiGeoObject.getAddressLine()).thenReturn("foo");
        when(mapsGeoObject.getAddressLine()).thenReturn("bar");
        assertFalse(outletParamsComparator.addressLineEqual(mbiOutlet, mapsOutlet));
    }

    /**
     * forgive outlets if geoCoder fails.
     */
    @Test
    public void geoCoderFailWithNull() {
        when(geoClient.findFirst(any())).thenReturn(null);
        assertTrue(outletParamsComparator.addressLineEqual(mbiOutlet, mapsOutlet));
    }
}
