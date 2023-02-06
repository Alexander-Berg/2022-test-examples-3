package ru.yandex.market.delivery.rupostintegrationapp.service.component.getreferencepickuppoints.updater;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.RussianPostPickupPoint;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickupPointAddressEnricherTest extends BaseTest {

    @InjectMocks
    protected PickupPointAddressEnricher enricher;

    @Mock
    protected GeoClient client;

    @Test
    void testEnrichDisabledEmtryAddress() {
        RussianPostPickupPoint point = new RussianPostPickupPoint();

        softly.assertThat(point.getEnabled()).isTrue();

        // go
        enricher.enrich(point);

        point.setAddress("");

        enricher.enrich(point);

        softly.assertThat(point.getEnabled()).isFalse();
    }

    @Test
    void testEnrichDisabledFailedClient() {
        RussianPostPickupPoint point = new RussianPostPickupPoint();
        point.setAddress("address");
        softly.assertThat(point.getEnabled()).isTrue();

        // go
        enricher.enrich(point);

        softly.assertThat(point.getEnabled()).isFalse();
    }

    @Test
    void testEnrichDisabledFailedArea() {

        RussianPostPickupPoint point = spy(new RussianPostPickupPoint());
        point.setAddress("address");

        GeoObject geo = SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder().build())
            .withAddressInfo(AddressInfo.newBuilder()
                .withCountryInfo(CountryInfo.newBuilder().build())
                .withAreaInfo(AreaInfo.newBuilder().build())
                .withLocalityInfo(LocalityInfo.newBuilder().build())
                .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();

        when(client.findFirst(anyString())).thenReturn(Optional.of(geo));

        softly.assertThat(point.getEnabled()).isTrue();

        // go
        enricher.enrich(point);

        softly.assertThat(point.getEnabled()).isFalse();
        verify(point, never()).getArea();
    }

    @Test
    void testEnrichDisabledFailedLocality() {

        RussianPostPickupPoint point = spy(new RussianPostPickupPoint());
        point.setAddress("address");

        GeoObject geo = SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder().build())
            .withAddressInfo(AddressInfo.newBuilder()
                .withCountryInfo(CountryInfo.newBuilder().build())
                .withAreaInfo(AreaInfo.newBuilder()
                    .withAdministrativeAreaName("area")
                    .build()
                )
                .withLocalityInfo(LocalityInfo.newBuilder().build())
                .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();

        softly.assertThat(geo.getAdministrativeAreaName()).isEqualTo("area");

        when(client.findFirst(anyString())).thenReturn(Optional.of(geo));

        softly.assertThat(point.getEnabled()).isTrue();

        // go
        enricher.enrich(point);

        softly.assertThat(point.getEnabled()).isFalse();
        verify(point, never()).getArea();
    }

    @Test
    void testEnrich() {

        RussianPostPickupPoint point = new RussianPostPickupPoint();
        point.setAddress("address");

        GeoObject geo = SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder().build())
            .withAddressInfo(AddressInfo.newBuilder()
                .withCountryInfo(CountryInfo.newBuilder().build())
                .withAreaInfo(AreaInfo.newBuilder()
                    .withAdministrativeAreaName("area")
                    .withSubAdministrativeAreaName("SubAdminArea")
                    .build()
                )
                .withLocalityInfo(LocalityInfo.newBuilder()
                    .withLocalityName("locality")
                    .withThoroughfareName("street")
                    .withPremiseNumber("16")
                    .build()
                )
                .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();

        softly.assertThat(geo.getAdministrativeAreaName()).isEqualTo("area");
        softly.assertThat(geo.getLocalityName()).isEqualTo("locality");

        when(client.findFirst(anyString())).thenReturn(Optional.of(geo));

        softly.assertThat(point.getEnabled()).isTrue();

        // go
        enricher.enrich(point);

        softly.assertThat(point.getEnabled()).isTrue();

        softly.assertThat(point.getArea()).isEqualTo("area");
        softly.assertThat(point.getLocality()).isEqualTo("locality");

        softly.assertThat(point.getStreet()).isEqualTo("street");
        softly.assertThat(point.getHouse()).isEqualTo("16");
    }

}
