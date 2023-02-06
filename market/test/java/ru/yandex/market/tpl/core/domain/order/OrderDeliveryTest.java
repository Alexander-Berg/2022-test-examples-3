package ru.yandex.market.tpl.core.domain.order;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.tpl.core.domain.order.address.AddressString;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderDeliveryTest {

    @Test
    void getRoutePointAddressReturnsCorrectEntity() {
        var lon = BigDecimal.valueOf(43.333333);
        var lat = BigDecimal.valueOf(33.333333);
        var precise = BigDecimal.valueOf(6);
        String addressString = "address string";
        String house = "25";
        String building = "44";
        String housing = "9";
        var orderDelivery = new OrderDelivery();
        orderDelivery.setDeliveryAddress(new DeliveryAddress(
                addressString, lon, lat, precise, precise,
                "Rossiya", "federalDistrict", "region", "subRegion",
                "Moskva", "settlement", "big street", house,
                building, housing, "2", "43", "4",
                "1234511", "zip", "metro", false, 1, 1L,
                "1111", "2222"
        ));

        RoutePointAddress result = orderDelivery.getRoutePointAddress();
        assertThat(result).isEqualTo(
                RoutePointAddress.builder()
                        .addressString(
                                AddressString.fromDeliveryAddress(orderDelivery.getDeliveryAddress()).toHouseString()
                        )
                        .house(house)
                        .building(building)
                        .longitude(lon)
                        .latitude(lat)
                        .housing(housing)
                        .build()
        );

    }

}
