package ru.yandex.market.tpl.tms.service.external.sqs.mapper;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.les.dto.TplAddressChangedSource;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderDelivery;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.order.producer.OrderDeliveryAddressUpdatePayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AddressUpdateEventMapperTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AddressUpdateEventMapper mapper;

    @Test
    void testMapper() {
        // given
        var deliveryAddress = DeliveryAddress.builder()
                .country("country")
                .federalDistrict("federalDistrict")
                .region("region")
                .subRegion("subRegion")
                .address("address")
                .latitude(BigDecimal.ONE)
                .longitude(BigDecimal.TEN)
                .city("city")
                .settlement("settlement")
                .street("street")
                .house("house")
                .building("building")
                .housing("housing")
                .entrance("entrance")
                .apartment("apartment")
                .floor("2")
                .entryPhone("entryPhone")
                .zipCode("zipCode")
                .metro("metro")
                .originalRegionId(3L)
                .addressPersonalId("personal-address-id")
                .gpsPersonalId("personal-gps-id")
                .build();
        var payload = setupTest(deliveryAddress);

        // when
        var event = mapper.map(payload).orElseThrow();

        // then
        assertThat(event.getOrderId()).isEqualTo("externalOrderId");
        assertThat(event.getSource()).isEqualTo(TplAddressChangedSource.OPERATOR);
        assertThat(event.getAddressDto()).isNotNull();
        assertThat(event.getAddressDto().getCombinedAddress()).isEqualTo("address");
        var location = event.getAddressDto().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getCountry()).isEqualTo("country");
        assertThat(location.getFederalDistrict()).isEqualTo("federalDistrict");
        assertThat(location.getRegion()).isEqualTo("region");
        assertThat(location.getSubRegion()).isEqualTo("subRegion");
        assertThat(location.getLocality()).isEqualTo("city");
        assertThat(location.getSettlement()).isEqualTo("settlement");
        assertThat(location.getStreet()).isEqualTo("street");
        assertThat(location.getHouse()).isEqualTo("house");
        assertThat(location.getBuilding()).isEqualTo("building");
        assertThat(location.getHousing()).isEqualTo("housing");
        assertThat(location.getPorch()).isEqualTo("entrance");
        assertThat(location.getRoom()).isEqualTo("apartment");
        assertThat(location.getFloor()).isEqualTo(2);
        assertThat(location.getIntercom()).isEqualTo("entryPhone");
        assertThat(location.getMetro()).isEqualTo("metro");
        assertThat(location.getZipCode()).isEqualTo("zipCode");
        assertThat(location.getLat()).isEqualTo(BigDecimal.ONE.floatValue());
        assertThat(location.getLng()).isEqualTo(BigDecimal.TEN.floatValue());
        assertThat(location.getLocationId()).isEqualTo(3);
        assertThat(event.getComment()).isEqualTo("comment");
        assertThat(event.getPersonalAddressDto().getPersonalAddressId()).isEqualTo("personal-address-id");
        assertThat(event.getPersonalAddressDto().getPersonalGpsId()).isEqualTo("personal-gps-id");
    }

    @Test
    void testNotNullFieldsCoalesce() {
        // given
        var deliveryAddress = DeliveryAddress.builder()
                .address(null)
                .country(null)
                .region(null)
                .city(null)
                .house(null)
                .build();
        var payload = setupTest(deliveryAddress);

        // when
        var event = mapper.map(payload).orElseThrow();

        // then
        assertThat(event.getAddressDto()).isNotNull();
        var location = event.getAddressDto().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getCountry()).isNotNull();
        assertThat(location.getRegion()).isNotNull();
        assertThat(location.getLocality()).isNotNull();
        assertThat(location.getHouse()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1-4", "КПП", " "})
    void testNotIntegerFloor(String floor) {
        // given
        var deliveryAddress = DeliveryAddress.builder()
                .address("address")
                .country("country")
                .region("region")
                .city("city")
                .floor(floor)
                .build();
        var payload = setupTest(deliveryAddress);

        // when
        var event = mapper.map(payload).orElseThrow();

        // then
        assertThat(event.getAddressDto()).isNotNull();
        var location = event.getAddressDto().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getFloor()).isNull();
    }

    private OrderDeliveryAddressUpdatePayload setupTest(DeliveryAddress deliveryAddress) {
        var order = Mockito.mock(Order.class);
        var delivery = Mockito.mock(OrderDelivery.class);
        doReturn(deliveryAddress)
                .when(delivery)
                .getDeliveryAddress();
        doReturn(delivery)
                .when(order)
                .getDelivery();
        doReturn("comment")
                .when(order)
                .getRecipientNotes();
        doReturn(Optional.of(order))
                .when(orderRepository)
                .findByExternalOrderId(eq("externalOrderId"));
        return new OrderDeliveryAddressUpdatePayload(
                "requestId",
                1L,
                "externalOrderId",
                "eventId",
                Source.OPERATOR
        );
    }
}
