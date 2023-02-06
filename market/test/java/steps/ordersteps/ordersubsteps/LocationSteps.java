package steps.ordersteps.ordersubsteps;

import java.math.BigDecimal;

import ru.yandex.market.delivery.entities.common.Location;

public class LocationSteps {

    private LocationSteps() {
        throw new UnsupportedOperationException();
    }

    public static Location getLocation() {
        Location location = new Location();

        location.setCountry("Российская Федерация");
        location.setFederalDistrict("Москва и московская область");
        location.setRegion("Москва");
        location.setLocality("Москва");
        location.setStreet("Льва Толстого");
        location.setHouse("16");
        location.setBuilding("1");
        location.setHousing("1");
        location.setRoom("9217");
        location.setZipCode("630090");
        location.setPorch("123");
        location.setFloor(2);
        location.setLat(BigDecimal.valueOf(53));
        location.setLng(BigDecimal.valueOf(55));
        location.setLocationId(213L);

        return location;
    }
}
