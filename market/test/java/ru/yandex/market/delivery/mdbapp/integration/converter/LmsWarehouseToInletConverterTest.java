package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.delivery.mdbapp.util.GeoTestUtils;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class LmsWarehouseToInletConverterTest {

    private static final String SETTLEMENT = "хутор";

    private GeoInfo geoInfo = GeoTestUtils.prepareGeoInfo();
    private LmsWarehouseToInletConverter lmsWarehouseToInletConverter = new LmsWarehouseToInletConverter(geoInfo);

    @Test
    public void testSettlementFallbackWhenCityNotFound() {
        LogisticsPoint outlet = lmsWarehouseToInletConverter.convert(buildLogisticPoint());

        assertThat(outlet.getAddress().getCity()).isEqualTo(SETTLEMENT);
    }

    private LogisticsPointResponse buildLogisticPoint() {
        return LogisticsPointResponse.newBuilder()
            .id(1L)
            .address(getAddress())
            .phones(Collections.emptySet())
            .schedule(Collections.emptySet())
            .build();
    }

    private Address getAddress() {
        return Address.newBuilder()
            .locationId(98580)
            .settlement(SETTLEMENT)
            .postCode("555666")
            .latitude(new BigDecimal("100"))
            .longitude(new BigDecimal("200"))
            .street("Октябрьская")
            .house("5")
            .housing("3")
            .building("2")
            .apartment("1")
            .comment("comment")
            .region("region")
            .addressString("Строка адреса")
            .shortAddressString("Строка адреса")
            .build();
    }
}
