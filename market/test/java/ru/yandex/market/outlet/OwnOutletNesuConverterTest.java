package ru.yandex.market.outlet;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.delivery.DeliveryRule;
import ru.yandex.market.core.delivery.DeliveryServiceInfo;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.OutletVisibility;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;
import ru.yandex.market.logistics.nesu.client.enums.ShopPickupPointStatus;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointAddressDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointMetaRequest;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointPhoneDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointRequest;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointScheduleDayDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointTariffRequest;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link OwnOutletNesuConverter}.
 * @author Vladislav Bauer
 */
class OwnOutletNesuConverterTest extends FunctionalTest {

    @Autowired
    private OwnOutletNesuConverter converter;

    @Test
    void testConvertType() {
        Stream.of(OutletType.values())
                .filter(type -> type != OutletType.NOT_DEFINED)
                .forEach(type -> assertThat(converter.convertType(type)).isNotNull());
    }

    @Test
    void testConvertPhoneNumber() {
        final ShopPickupPointPhoneDto actual = converter.convertPhoneNumber(
                PhoneNumber.builder()
                        .setBasedOnString("+7(123)4567#89")
                        .build());

        assertThat(actual.getPhoneNumber()).isEqualTo("+7-123-4567");
        assertThat(actual.getInternalNumber()).isEqualTo("89");
    }

    @Test
    void testConvertAddress() {
        final Address address = Address.builder()
                .setCity("city")
                .setStreet("street")
                .setBlock("block")
                .setOther("other")
                .setBuilding("building")
                .setEstate("estate")
                .setPostCode("postcode")
                .setFlat("flat")
                .setNumber("number")
                .setOther("other")
                .setLaneKM(3)
                .build();

        final Coordinates coordinates = new Coordinates(1, 2);
        final GeoInfo geoInfo = new GeoInfo();
        geoInfo.setRegionId(123L);
        geoInfo.setGpsCoordinates(coordinates);

        final OutletInfo outletInfo = createOutletInfo();
        outletInfo.setAddress(address);
        outletInfo.setGeoInfo(geoInfo);

        final ShopPickupPointAddressDto pointAddress = converter.convertAddress(outletInfo);
        assertThat(pointAddress.getRegion()).isNull();
        assertThat(pointAddress.getSubRegion()).isNull();
        assertThat(pointAddress.getLocality()).isEqualTo(address.getCity());
        assertThat(pointAddress.getStreet()).isEqualTo(address.getStreet());
        assertThat(pointAddress.getApartment()).isEqualTo(address.getFlat());
        assertThat(pointAddress.getBuilding()).isEqualTo(address.getBuilding());
        assertThat(pointAddress.getEstate()).isEqualTo(address.getEstate());
        assertThat(pointAddress.getHouse()).isEqualTo(address.getNumber());
        assertThat(pointAddress.getHousing()).isEqualTo(address.getBlock());
        assertThat(pointAddress.getComment()).isEqualTo(address.getAddrAdditional());
        assertThat(pointAddress.getPostalCode()).isEqualTo(address.getPostCode());
        assertThat(pointAddress.getKm()).isEqualTo(address.getKm());
        assertThat(pointAddress.getLocationId()).isEqualTo(geoInfo.getRegionId().intValue());
        assertThat(pointAddress.getLatitude()).isEqualTo(BigDecimal.valueOf(coordinates.getLat()));
        assertThat(pointAddress.getLongitude()).isEqualTo(BigDecimal.valueOf(coordinates.getLon()));
    }

    @Test
    void testConvertSchedule() {
        final Schedule schedule = new Schedule(1L, List.of(
                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 15, 15),
                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 1, 45, 60)
        ));

        final List<ShopPickupPointScheduleDayDto> pointSchedule = converter.convertSchedule(schedule);
        assertThat(pointSchedule)
                .hasSize(3)
                .usingRecursiveComparison()
                .isEqualTo(
                        List.of(
                                converter.createScheduleDay(1, LocalTime.of(0, 15), LocalTime.of(0, 30)),
                                converter.createScheduleDay(3, LocalTime.of(0, 45), LocalTime.of(1, 45)),
                                converter.createScheduleDay(4, LocalTime.of(0, 45), LocalTime.of(1, 45))
                        )
                );
    }

    @Test
    void testConvertOutletTariffInfo() {
        final DeliveryRule deliveryRule = createDeliveryRule();
        final OutletInfo outletInfo = createOutletInfo();
        outletInfo.addDeliveryRules(List.of(deliveryRule));

        final ShopPickupPointTariffRequest tariff = converter.convertOutletTariffInfo(outletInfo);
        assertThat(tariff.getDaysFrom()).isEqualTo(deliveryRule.getMinDeliveryDays());
        assertThat(tariff.getDaysTo()).isEqualTo(deliveryRule.getMaxDeliveryDays());
        assertThat(tariff.getOrderBeforeHour()).isEqualTo(deliveryRule.getDateSwitchHour());
    }

    @Test
    void testConvertOutletMetaInfo() {
        final long shipperId = 301;
        final DeliveryRule deliveryRule = createDeliveryRule();
        deliveryRule.setDeliveryServiceInfo(new DeliveryServiceInfo(shipperId, "shipper-name"));

        final OutletInfo outletInfo = createOutletInfo();
        outletInfo.setMain(true);
        outletInfo.addDeliveryRule(deliveryRule);

        final ShopPickupPointMetaRequest request = converter.convertOutletMetaInfo(outletInfo);
        assertThat(request.getMbiId()).isEqualTo(outletInfo.getId());

        final ShopPickupPointRequest pickupPoint = request.getPickupPoint();
        assertThat(pickupPoint.getExternalId()).isEqualTo(outletInfo.getShopOutletId());
        assertThat(pickupPoint.getName()).isEqualTo(outletInfo.getName());
        assertThat(pickupPoint.getOwnerPartnerId()).isEqualTo(shipperId);
        assertThat(pickupPoint.getIsMain()).isEqualTo(outletInfo.isMain());
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("testConvertStatusData")
    void testConvertStatus(final OutletVisibility visibility, final ShopPickupPointStatus status) {
        assertThat(converter.convertStatus(visibility)).isEqualTo(status);
    }


    private static Stream<Arguments> testConvertStatusData() {
        return Stream.of(
                Arguments.of(OutletVisibility.VISIBLE, ShopPickupPointStatus.ACTIVE),
                Arguments.of(OutletVisibility.HIDDEN, ShopPickupPointStatus.INACTIVE),
                Arguments.of(OutletVisibility.UNKNOWN, ShopPickupPointStatus.INACTIVE),
                Arguments.of(null, ShopPickupPointStatus.INACTIVE)
        );
    }

    private OutletInfo createOutletInfo() {
        return new OutletInfo(1L, 2L, OutletType.POST, "outlet-name", true, "shop-outlet-id");
    }

    private DeliveryRule createDeliveryRule() {
        return new DeliveryRule(
                BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE, 0, 3, true, 13, false, null);
    }

}
