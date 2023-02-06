package ru.yandex.market.logistics.nesu.model;

import java.util.List;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;

@UtilityClass
public class GeoSearchFactory {
    @Nonnull
    public GeoObject geoObject(
        Kind kind,
        String geoId,
        String point,
        List<Component> components
    ) {
        return SimpleGeoObject.newBuilder()
            .withToponymInfo(
                ToponymInfo.newBuilder()
                    .withKind(kind)
                    .withPoint(point)
                    .withGeoid(geoId)
                    .withComponents(components)
                    .build()
            )
            .withAddressInfo(
                AddressInfo.newBuilder()
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }
}
