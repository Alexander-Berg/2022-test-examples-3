package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Location;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Address;
import ru.yandex.market.logistics.lom.entity.embedded.PickupPoint;
import ru.yandex.market.logistics.lom.entity.embedded.Recipient;
import ru.yandex.market.logistics.lom.entity.enums.LocationType;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.LastMileLocationEnricher;

@DisplayName("Валидация обогащения локации последней мили")
class LastMileLocationEnricherTest extends AbstractTest {

    public static final long PARTNER_ID = 55L;

    private final LastMileLocationEnricher validator = new LastMileLocationEnricher();
    private final ValidateAndEnrichContext context = new ValidateAndEnrichContext();

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("lastMileEnrichSource")
    void lastMileEnrich(@SuppressWarnings("unused") String caseName, Order order, Location expectedLocation) {
        context.setPartnerTypeById(Map.of(PARTNER_ID, PartnerType.DELIVERY));
        ValidateAndEnrichResults results = validator.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();

        Order modifiedOrder = results.getOrderModifier().apply(order);
        Location location = modifiedOrder.getWaybill().stream()
            .findFirst()
            .map(WaybillSegment::getWaybillShipment)
            .map(WaybillSegment.WaybillShipment::getLocationTo)
            .orElseThrow();

        softly.assertThat(location)
            .isEqualToComparingFieldByFieldRecursively(expectedLocation);
    }

    @Nonnull
    private static Stream<Arguments> lastMileEnrichSource() {
        return Stream.of(
            Arguments.of(
                "Recipient address",
                new Order()
                    .setWaybill(List.of(createShipment(SegmentType.COURIER)))
                    .setRecipient(new Recipient().setAddress(
                        new Address().setCountry("cntry").setLocality("lclty").setRegion("rgn")
                    )),
                new Location()
                    .setType(LocationType.RECIPIENT)
                    .setAddress(new Address().setCountry("cntry").setLocality("lclty").setRegion("rgn"))
            ),
            Arguments.of(
                "Pickup address",
                new Order()
                    .setWaybill(List.of(createShipment(SegmentType.PICKUP)))
                    .setPickupPoint(new PickupPoint().setPickupPointId(22L)),
                new Location().setType(LocationType.PICKUP).setWarehouseId(22L)
            )
        );
    }

    @Nonnull
    private static WaybillSegment createShipment(SegmentType segmentType) {
        return new WaybillSegment()
            .setId(1L)
            .setPartnerId(PARTNER_ID)
            .setSegmentType(segmentType)
            .setWaybillShipment(new WaybillSegment.WaybillShipment().setLocationTo(null));
    }
}
