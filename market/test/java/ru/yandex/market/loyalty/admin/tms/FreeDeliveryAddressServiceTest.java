package ru.yandex.market.loyalty.admin.tms;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.Precision;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.juggler.JugglerEvent;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.delivery.GeoPoint;
import ru.yandex.market.loyalty.core.config.Juggler;
import ru.yandex.market.loyalty.core.model.delivery.AddressType;
import ru.yandex.market.loyalty.core.model.delivery.FreeDeliveryAddress;
import ru.yandex.market.loyalty.core.monitor.CoreMonitorType;
import ru.yandex.market.loyalty.core.service.FreeDeliveryAddressService;
import ru.yandex.market.loyalty.monitoring.beans.JugglerEventsPushExecutor;
import ru.yandex.market.loyalty.test.TestFor;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.utils.JugglerTestUtils.assertSingleJugglerEventPushed;
import static ru.yandex.market.loyalty.core.utils.JugglerTestUtils.assertZeroJugglerEventPushed;
import static ru.yandex.market.loyalty.core.utils.JugglerTestUtils.mockOkeyJugglerResponse;

@TestFor({FreeDeliveryAddressValidateProcessor.class})
public class FreeDeliveryAddressServiceTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String TEST_ADDRESS = "г. Москва, ул. Садовническая, 82с2";
    private static final GeoObject geoObject;
    private static final GeoObject wrongGeoObject;

    static {
        geoObject = SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                        .withPrecision(Precision.EXACT)
                        .withPoint("37.642474 55.73552")
                        .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder().build())
                        .withAreaInfo(AreaInfo.newBuilder().build())
                        .withLocalityInfo(LocalityInfo.newBuilder().build())
                        .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build();
        wrongGeoObject = SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                        .withPrecision(Precision.EXACT)
                        .withPoint("37.644474 55.73552")
                        .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder().build())
                        .withAreaInfo(AreaInfo.newBuilder().build())
                        .withLocalityInfo(LocalityInfo.newBuilder().build())
                        .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build();
    }

    @Autowired
    @Juggler
    private HttpClient jugglerHttpClient;
    @Autowired
    private JugglerEventsPushExecutor jugglerEventsPushExecutor;
    @Autowired
    private GeoClient geoClient;
    @Autowired
    private FreeDeliveryAddressValidateProcessor freeDeliveryAddressValidateProcessor;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FreeDeliveryAddressService freeDeliveryAddressService;


    @Before
    public void setup() {
        when(geoClient.find(TEST_ADDRESS)).thenReturn(Collections.singletonList(geoObject));
    }

    @Test
    public void shouldFreeDeliveryAddressesLoaded() throws IOException {
        freeDeliveryAddressValidateProcessor.validateAddressGeocoding();

        mockOkeyJugglerResponse(jugglerHttpClient);
        jugglerEventsPushExecutor.jugglerPushEvents();

        checkAddresses(TEST_ADDRESS);

        assertZeroJugglerEventPushed(jugglerHttpClient, objectMapper);
    }

    @Test
    public void shouldMonitorFireWhenAddressGPSIncorrect() throws IOException {
        given(geoClient.find(TEST_ADDRESS)).willReturn(Collections.singletonList(wrongGeoObject));

        freeDeliveryAddressService.refreshFreeDeliveryAddresses();
        freeDeliveryAddressValidateProcessor.validateAddressGeocoding();

        mockOkeyJugglerResponse(jugglerHttpClient);
        jugglerEventsPushExecutor.jugglerPushEvents();

        checkAddresses(TEST_ADDRESS);

        assertSingleJugglerEventPushed(
                CoreMonitorType.FREE_DELIVERY_ADDRESS_MONITOR,
                JugglerEvent.Status.WARN,
                jugglerHttpClient,
                objectMapper
        );
    }


    @Test
    public void shouldMonitorFireWhenGeocodingError() throws IOException {
        when(geoClient.find(TEST_ADDRESS)).thenReturn(Collections.emptyList());

        freeDeliveryAddressService.refreshFreeDeliveryAddresses();
        freeDeliveryAddressValidateProcessor.validateAddressGeocoding();

        mockOkeyJugglerResponse(jugglerHttpClient);
        jugglerEventsPushExecutor.jugglerPushEvents();

        checkAddresses(TEST_ADDRESS);

        assertSingleJugglerEventPushed(
                CoreMonitorType.FREE_DELIVERY_ADDRESS_MONITOR,
                JugglerEvent.Status.WARN,
                jugglerHttpClient,
                objectMapper
        );
    }

    private void checkAddresses(String addressString) {
        Collection<FreeDeliveryAddress> addresses = freeDeliveryAddressService.getFreeDeliveryAddresses();
        assertNotNull(addresses);
        assertThat(addresses, hasSize(1));
        FreeDeliveryAddress address = addresses.iterator().next();
        assertEquals(addressString, address.getAddress());
        assertTrue(new GeoPoint(37.642474, 55.735520).isNear(address.getGps(), 0.0001));
        assertEquals(AddressType.YANDEX, address.getAddressType());
    }
}
