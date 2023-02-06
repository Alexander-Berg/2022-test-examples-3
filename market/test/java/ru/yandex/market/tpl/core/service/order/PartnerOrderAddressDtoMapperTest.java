package ru.yandex.market.tpl.core.service.order;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderAddressDto;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;

import static org.assertj.core.api.BDDAssertions.then;

@ExtendWith(SpringExtension.class)
class PartnerOrderAddressDtoMapperTest {

    private static final GeoPoint ORIGINAL_LOCATION = GeoPoint.ofLatLon(
            new BigDecimal("55.736306"),
            new BigDecimal("37.590497")
    );
    private static final String CITY = "city";
    private static final String STREET = "street";
    private static final String HOUSE = "1";
    private static final String BUILDING = "2";
    private static final String HOUSING = "3";
    private static final String ENTRANCE = "entrance";
    private static final String FLOOR = "floor";
    private static final String APARTMENT = "apartment";
    private static final String ENTRY_PHONE = "entry phone";
    private static final long DELIVERY_SERVICE_ID = 198L;
    private static final DeliveryAddress DELIVERY_ADDRESS = DeliveryAddress.builder()
            .geoPoint(ORIGINAL_LOCATION)
            .city(CITY)
            .street(STREET)
            .house(HOUSE)
            .building(BUILDING)
            .housing(HOUSING)
            .entrance(ENTRANCE)
            .floor(FLOOR)
            .apartment(APARTMENT)
            .entryPhone(ENTRY_PHONE)
            .build();
    private static final String COMBO_HOUSE = "д. 1, стр. 2, к. 3";

    private PartnerOrderAddressDtoMapper partnerOrderAddressDtoMapper;

    @BeforeEach
    void init() {
        partnerOrderAddressDtoMapper = new PartnerOrderAddressDtoMapper();
    }

    @Test
    void fromDeliveryAddressWhenCityIsNotFoundInDSAreaService() {
        //when
        PartnerOrderAddressDto orderAddressDto = partnerOrderAddressDtoMapper
                .fromDeliveryAddress(DELIVERY_ADDRESS);
        //then
        then(orderAddressDto).isEqualTo(PartnerOrderAddressDto.builder()
                .city(CITY)
                .street(STREET)
                .house(COMBO_HOUSE)
                .entrance(ENTRANCE)
                .floor(FLOOR)
                .apartment(APARTMENT)
                .entryPhone(ENTRY_PHONE)
                .build()
        );
    }

}
