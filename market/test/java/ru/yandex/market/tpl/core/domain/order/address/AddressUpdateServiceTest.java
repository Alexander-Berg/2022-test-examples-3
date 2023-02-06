package ru.yandex.market.tpl.core.domain.order.address;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderAddressDto;
import ru.yandex.market.tpl.common.util.exception.TplException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.service.order.validator.OrderAddressValidator;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AddressUpdateServiceTest {

    private static final GeoPoint ORIGINAL_LOCATION = GeoPoint.ofLatLon(
            new BigDecimal("55.736306"),
            new BigDecimal("37.590497")
    );
    private static final GeoPoint CLOSE_LOCATION = GeoPoint.ofLatLon(
            new BigDecimal("55.736708"),
            new BigDecimal("37.589498")
    );
    private static final long DELIVERY_SERVICE_ID = 198L;
    private static final String CITY = "city";
    private static final String STREET = "street";
    private static final String HOUSE_BEFORE = "house";
    private static final String HOUSE_AFTER = "house after";
    private static final String BUILDING = "building";
    private static final String HOUSING = "housing";
    private static final String ENTRANCE = "entrance";
    private static final String FLOOR_BEFORE = "floor";
    private static final String FLOOR_AFTER = "floor after";
    private static final String APARTMENT = "apartment";
    private static final int REGION_ID = 117065;
    private static final PartnerOrderAddressDto NEW_ADDRESS_DTO = PartnerOrderAddressDto.builder()
            .city(CITY)
            .street(STREET)
            .house(HOUSE_AFTER)
            .entrance(ENTRANCE)
            .floor(FLOOR_BEFORE)
            .apartment(APARTMENT)
            .entryPhone(null)
            .build();
    private static final DeliveryAddress DELIVERY_ADDRESS_BEFORE = DeliveryAddress.builder()
            .geoPoint(ORIGINAL_LOCATION)
            .city(CITY)
            .street(STREET)
            .house(HOUSE_BEFORE)
            .building(BUILDING)
            .housing(HOUSING)
            .entrance(ENTRANCE)
            .floor(FLOOR_BEFORE)
            .apartment(APARTMENT)
            .entryPhone(null)
            .build();
    private static final DeliveryAddress DELIVERY_ADDRESS_AFTER = DeliveryAddress.builder()
            .geoPoint(ORIGINAL_LOCATION)
            .city(CITY)
            .street(STREET)
            .house(HOUSE_AFTER)
            .entrance(ENTRANCE)
            .floor(FLOOR_BEFORE)
            .apartment(APARTMENT)
            .entryPhone(null)
            .regionId(REGION_ID)
            .address("г. city, street, д. house after, подъезд entrance, кв. apartment, этаж floor")
            .build();
    private static final AddressString ADDRESS_STRING = AddressString.builder()
            .dropDefaultCityFromAddressString(true)
            .city(CITY)
            .street(STREET)
            .house(HOUSE_AFTER)
            .entrance(ENTRANCE)
            .floor(FLOOR_BEFORE)
            .apartment(APARTMENT)
            .entryPhone(null)
            .build();
    private static final Address NEW_ADDRESS = new Address(
            ADDRESS_STRING,
            CLOSE_LOCATION,
            REGION_ID
    );
    private static final DeliveryAddress NEW_DELIVERY_ADDRESS = DELIVERY_ADDRESS_AFTER.toBuilder()
            .geoPoint(CLOSE_LOCATION)
            .build();

    private static final PartnerOrderAddressDto OLD_ADDRESS_2_DTO = PartnerOrderAddressDto.builder()
            .city(CITY)
            .street(STREET)
            .house(HOUSE_BEFORE)
            .entrance(ENTRANCE)
            .floor(FLOOR_BEFORE)
            .apartment(APARTMENT)
            .entryPhone(null)
            .build();
    private static final DeliveryAddress DELIVERY_ADDRESS_2_BEFORE = DeliveryAddress.builder()
            .geoPoint(ORIGINAL_LOCATION)
            .city(CITY)
            .street(STREET)
            .house(HOUSE_BEFORE)
            .entrance(ENTRANCE)
            .floor(FLOOR_BEFORE)
            .apartment(APARTMENT)
            .entryPhone(null)
            .build();
    private static final DeliveryAddress NEW_DELIVERY_ADDRESS_2 = DeliveryAddress.builder()
            .city(CITY)
            .street(STREET)
            .house(HOUSE_BEFORE)
            .entrance(null)
            .floor(null)
            .apartment(null)
            .entryPhone(null)
            .build();
    private static final DeliveryAddress NEW_DELIVERY_ADDRESS_3 = DeliveryAddress.builder()
            .geoPoint(ORIGINAL_LOCATION)
            .city(CITY)
            .street(STREET)
            .house(HOUSE_BEFORE)
            .entrance(ENTRANCE)
            .floor(FLOOR_AFTER)
            .apartment(APARTMENT)
            .entryPhone(null)
            .regionId(REGION_ID)
            .address("г. city, street, д. house, подъезд entrance, кв. apartment, этаж floor after")
            .build();
    private static final DeliveryAddress NEW_DELIVERY_ADDRESS_4 = DeliveryAddress.builder()
            .city(CITY)
            .street(STREET)
            .house(HOUSE_BEFORE)
            .entrance(ENTRANCE)
            .floor(FLOOR_BEFORE)
            .apartment(null)
            .entryPhone(null)
            .build();
    private static final DeliveryAddress DELIVERY_ADDRESS_AFTER_2 = DeliveryAddress.builder()
            .geoPoint(ORIGINAL_LOCATION)
            .city(CITY)
            .street(STREET)
            .house(HOUSE_BEFORE)
            .entrance(ENTRANCE)
            .floor(FLOOR_AFTER)
            .apartment(APARTMENT)
            .entryPhone(null)
            .regionId(REGION_ID)
            .address("г. city, street, д. house, подъезд entrance, кв. apartment, этаж floor after")
            .build();
    private static final AddressString ADDRESS_STRING_2 = AddressString.builder()
            .dropDefaultCityFromAddressString(true)
            .city(CITY)
            .street(STREET)
            .house(HOUSE_BEFORE)
            .entrance(ENTRANCE)
            .floor(FLOOR_AFTER)
            .apartment(APARTMENT)
            .entryPhone(null)
            .build();

    @Spy
    private AddressFactory addressFactory;
    @Mock
    private AddressQueryService addressQueryService;
    @Mock
    private OrderAddressValidator orderAddressValidator;
    @Spy
    private AddressStringMapper addressStringMapper;
    @InjectMocks
    private AddressUpdateService addressUpdateService;

    @Test
    void getNewDeliveryAddressIsEmptyWhenAddressDtoIsNull() {
        //when
        Optional<DeliveryAddress> newDeliveryAddress = addressUpdateService.getNewDeliveryAddress(
                null,
                DeliveryAddress.builder().build()
        );
        //then
        then(newDeliveryAddress).isEmpty();
    }

    @Test
    void getNewDeliveryAddressIsEmptyWhenAddressTextIsTheSame() {
        //when
        Optional<DeliveryAddress> newDeliveryAddress = addressUpdateService.getNewDeliveryAddress(
                OLD_ADDRESS_2_DTO,
                DELIVERY_ADDRESS_2_BEFORE
        );
        //then
        then(newDeliveryAddress).isEmpty();
    }

    @Test
    void getNewDeliveryAddressThrowsWhenAddressWasNotFound() {
        //given
        given(addressQueryService.queryByAddressString(ADDRESS_STRING))
                .willReturn(Optional.empty());
        //then
        thenThrownBy(() -> addressUpdateService.getNewDeliveryAddress(
                NEW_ADDRESS_DTO,
                DELIVERY_ADDRESS_BEFORE
        )).isInstanceOf(TplException.class);
    }

    @Test
    void getNewDeliveryAddressIsPresentWhenGeoPointIsDifferent() {
        //given
        given(addressQueryService.queryByAddressString(ADDRESS_STRING))
                .willReturn(Optional.of(NEW_ADDRESS));
        //when
        Optional<DeliveryAddress> newDeliveryAddress = addressUpdateService.getNewDeliveryAddress(
                NEW_ADDRESS_DTO,
                DELIVERY_ADDRESS_BEFORE
        );
        //then
        then(newDeliveryAddress).contains(NEW_DELIVERY_ADDRESS);
    }

    @Test
    void getNewDeliveryAddressIsPresentWhenSupplementaryPartsAreDifferent() {
        //given
        given(addressQueryService.queryByAddressString(ADDRESS_STRING))
                .willReturn(Optional.of(NEW_ADDRESS));
        //when
        Optional<DeliveryAddress> newDeliveryAddress = addressUpdateService.getNewDeliveryAddress(
                NEW_ADDRESS_DTO,
                DELIVERY_ADDRESS_BEFORE
        );
        //then
        then(newDeliveryAddress).contains(NEW_DELIVERY_ADDRESS);
    }

    @Test
    void getNewDsDeliveryAddressIsEmptyWhenAddressDtoIsNull() {
        //when
        Optional<DeliveryAddress> newDeliveryAddress = addressUpdateService.getNewDeliveryAddress(
                null,
                DeliveryAddress.builder().build()
        );
        //then
        then(newDeliveryAddress).isEmpty();
    }

    @Test
    void getNewDsDeliveryAddressIsEmptyWhenAddressTextIsTheSame() {
        //when
        Optional<DeliveryAddress> newDeliveryAddress = addressUpdateService.getNewDsDeliveryAddress(
                DELIVERY_ADDRESS_2_BEFORE,
                DELIVERY_ADDRESS_2_BEFORE
        );
        //then
        then(newDeliveryAddress).isEmpty();
    }

    @Test
    void getNewDsDeliveryAddressIsEmptyWhenClarificationIsNull() {
        //when
        Optional<DeliveryAddress> newDeliveryAddress = addressUpdateService.getNewDsDeliveryAddress(
                NEW_DELIVERY_ADDRESS_2,
                DELIVERY_ADDRESS_2_BEFORE
        );
        //then
        then(newDeliveryAddress).isEmpty();
    }

    @Test
    void getNewDsDeliveryAddressIsPresentWhenClarificationIsPresent() {
        //when
        Optional<DeliveryAddress> newDeliveryAddress = addressUpdateService.getNewDsDeliveryAddress(
                NEW_DELIVERY_ADDRESS_3,
                DELIVERY_ADDRESS_2_BEFORE
        );
        //then
        then(newDeliveryAddress).contains(DELIVERY_ADDRESS_AFTER_2);
    }

    @Test
    void getNewDsDeliveryAddressIsEmptyWhenClarificationHasLessFields() {
        //when
        Optional<DeliveryAddress> newDeliveryAddress = addressUpdateService.getNewDsDeliveryAddress(
                NEW_DELIVERY_ADDRESS_4,
                DELIVERY_ADDRESS_2_BEFORE
        );
        //then
        then(newDeliveryAddress).isEmpty();
    }

    @Test
    void validateUpdatedAddressSuccess() {
        //given
        Mockito.when(orderAddressValidator.isGeoValid(ORIGINAL_LOCATION.getLongitude(),
                ORIGINAL_LOCATION.getLatitude(), DELIVERY_SERVICE_ID)).thenReturn(true);
        Mockito.when(orderAddressValidator.isEnabledForDS(DELIVERY_SERVICE_ID)).thenReturn(true);

        //then
        assertDoesNotThrow(() -> addressUpdateService.validateAddress(DELIVERY_ADDRESS_AFTER, DELIVERY_SERVICE_ID));
    }

    @Test
    void validateUpdatedAddressCityOutsideOfServiceArea() {
        //given
        Mockito.when(orderAddressValidator.isGeoValid(ORIGINAL_LOCATION.getLongitude(),
                ORIGINAL_LOCATION.getLatitude(), DELIVERY_SERVICE_ID)).thenReturn(false);
        Mockito.when(orderAddressValidator.isEnabledForDS(DELIVERY_SERVICE_ID)).thenReturn(true);


        //then
        thenThrownBy(() -> addressUpdateService.validateAddress(DELIVERY_ADDRESS_AFTER, DELIVERY_SERVICE_ID))
                .isInstanceOf(TplInvalidParameterException.class);
    }

}
