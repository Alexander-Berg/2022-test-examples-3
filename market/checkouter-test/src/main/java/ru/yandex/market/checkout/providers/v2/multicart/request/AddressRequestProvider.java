package ru.yandex.market.checkout.providers.v2.multicart.request;

import java.math.BigDecimal;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.AddressRequest;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.LocationRequest;

public class AddressRequestProvider {

    private AddressRequestProvider() {
    }

    public static AddressRequest fromAddress(Address address) {
        if (address == null) {
            return null;
        }
        return AddressRequest.builder()
                .withCountry(address.getCountry())
                .withPostcode(address.getPostcode())
                .withCity(address.getCity())
                .withDistrict(address.getDistrict())
                .withStreet(address.getStreet())
                .withHouse(address.getHouse())
                .withBlock(address.getBlock())
                .withFloor(address.getFloor())
                .withApartment(address.getApartment())
                .withPreciseRegionId(address.getPreciseRegionId())
                .withLocation(fromGps(address.getGps()))
                .withPersonalGpsId(address.getPersonalGpsId())
                .withPersonalAddressId(address.getPersonalAddressId())
                .build();
    }

    private static LocationRequest fromGps(String gps) {
        if (gps == null) {
            return null;
        }
        String[] parts = gps.split(",");
        return LocationRequest.builder()
                .withLongitude(new BigDecimal(parts[0]))
                .withLatitude(new BigDecimal(parts[1]))
                .build();
    }

}
