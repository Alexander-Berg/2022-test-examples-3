package steps;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.mockito.Mockito;
import steps.orderSteps.AddressSteps;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryLastMileChangeRequestPayload;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.UpdateLastMileDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileFromPickupToPickupRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileToCourierRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileToPickupRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.LastMileChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.integration.converter.AddressConverter;
import ru.yandex.market.delivery.mdbapp.integration.service.PersonalDataService;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.personal.PersonalClient;

@UtilityClass
public class UpdateLastMileSteps {
    public static final String FF_EXTERNAL_ID = "ff-external-id";
    public static final String PICKUP_EXTERNAL_ID = "pickup-external-id";
    public static final String NEW_PICKUP_EXTERNAL_ID = "new-pickup-external-id";
    public static final String COURIER_EXTERNAL_ID = "courier-external-id";
    public static final Long FF_PARTNER_ID = 172L;
    public static final Long PICKUP_PARTNER_ID = 12345L;
    public static final Long NEW_PICKUP_PARTNER_ID = 12346L;
    public static final Long COURIER_PARTNER_ID = 54321L;
    public static final Long PARCEL_ID = 333L;
    public static final Integer PICKUP_GEO_ID = 213;
    public static final Long PICKUP_OUTLET_ID = 123456789L;
    public static final String BARCODE = "234";
    public static final Long REQUEST_ID = 88586L;
    private final Integer regionId = 123;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AddressConverter addressConverter = new AddressConverter();
    private final PersonalDataService personalDataService = new PersonalDataService(
        new FeatureProperties().setFillPersonalDataValuesWithDefaultData(true),
        Mockito.mock(PersonalClient.class),
        new ru.yandex.market.logistics.personal.converter.AddressConverter()
    );

    @Nonnull
    public UpdateLastMileDto createUpdateLastMileDto() {
        AddressImpl address = createAddress();
        return UpdateLastMileDto.builder()
            .address(
                addressConverter.fromCheckouter(
                    address,
                    createLocation(),
                    personalDataService
                )
            )
            .comment(address.getNotes())
            .deliveryType(DeliveryType.DELIVERY)
            .route(objectMapper.valueToTree(ParcelSteps.ROUTE))
            .orderId(UpdateRecipientSteps.ORDER_ID)
            .checkouterChangeRequestId(REQUEST_ID)
            .deliveryServiceId(UpdateRecipientSteps.PARTNER_ID)
            .dateFrom(LocalDate.of(2022, 3, 25))
            .dateTo(LocalDate.of(2022, 3, 26))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(18, 0))
            .build();
    }

    @Nonnull
    public LastMileChangeRequestDto createLastMileChangeRequestDto() {
        return LastMileChangeRequestDto.builder()
            .lomOrderId(UpdateRecipientSteps.ORDER_ID)
            .changeRequestId(REQUEST_ID)
            .build();
    }

    @Nonnull
    public ChangeLastMileToCourierRequestDto createChangeLastMileToCourierRequestDto() {
        return ChangeLastMileToCourierRequestDto.builder()
            .lomOrderId(UpdateRecipientSteps.ORDER_ID)
            .changeRequestId(REQUEST_ID)
            .build();
    }

    @Nonnull
    public ChangeLastMileToPickupRequestDto createChangeLastMileToPickupRequestDto() {
        return ChangeLastMileToPickupRequestDto.builder()
            .lomOrderId(UpdateRecipientSteps.ORDER_ID)
            .changeRequestId(REQUEST_ID)
            .build();
    }

    @Nonnull
    public ChangeLastMileFromPickupToPickupRequestDto createChangeLastMileFromPickupToPickupRequestDto() {
        return ChangeLastMileFromPickupToPickupRequestDto.builder()
            .lomOrderId(UpdateRecipientSteps.ORDER_ID)
            .changeRequestId(REQUEST_ID)
            .build();
    }

    @Nonnull
    public OrderDto createLomOrderDto(
        ChangeOrderRequestStatus status,
        @Nullable ChangeOrderRequestReason reason,
        JsonNode payload,
        ChangeOrderRequestType requestType
    ) {
        return new OrderDto()
            .setId(UpdateRecipientSteps.ORDER_ID)
            .setBarcode(BARCODE)
            .setExternalId(BARCODE)
            .setChangeOrderRequests(List.of(
                ChangeOrderRequestDto.builder()
                    .id(REQUEST_ID)
                    .requestType(requestType)
                    .reason(reason)
                    .status(status)
                    .payloads(Set.of(
                        ChangeOrderRequestPayloadDto.builder()
                            .id(1L)
                            .payload(payload)
                            .status(ChangeOrderRequestStatus.INFO_RECEIVED)
                            .build()
                    ))
                    .build()
            ));
    }

    @Nonnull
    public OrderDto createLomOrderDtoAfterLastMileChangeToCourier(
        ChangeOrderRequestStatus status,
        JsonNode payload
    ) {
        OrderDto lomOrderDto = createLomOrderDto(
            status,
            null,
            payload,
            ChangeOrderRequestType.CHANGE_LAST_MILE_TO_PICKUP
        );
        lomOrderDto.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.FULFILLMENT)
                .partnerType(PartnerType.FULFILLMENT)
                .externalId(FF_EXTERNAL_ID)
                .partnerId(FF_PARTNER_ID)
                .build(),
            WaybillSegmentDto.builder().build(),
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.COURIER)
                .externalId(COURIER_EXTERNAL_ID)
                .partnerId(COURIER_PARTNER_ID)
                .build()
        ));
        return lomOrderDto;
    }

    @Nonnull
    public OrderDto createLomOrderDtoAfterLastMileChangeToPickup(
        ChangeOrderRequestStatus status,
        JsonNode payload
    ) {
        OrderDto lomOrderDto = createLomOrderDto(
            status,
            null,
            payload,
            ChangeOrderRequestType.CHANGE_LAST_MILE_TO_PICKUP
        );
        lomOrderDto.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.FULFILLMENT)
                .partnerType(PartnerType.FULFILLMENT)
                .externalId(FF_EXTERNAL_ID)
                .partnerId(FF_PARTNER_ID)
                .build(),
            WaybillSegmentDto.builder().build(),
            WaybillSegmentDto.builder().build(),
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.PICKUP)
                .externalId(PICKUP_EXTERNAL_ID)
                .partnerId(PICKUP_PARTNER_ID)
                .build()
        ));
        return lomOrderDto;
    }

    @Nonnull
    public OrderDto createLomOrderDtoAfterLastMileChangeFromPickupToPickup(
        ChangeOrderRequestStatus status,
        @Nullable ChangeOrderRequestReason reason,
        JsonNode payload
    ) {
        OrderDto lomOrderDto = createLomOrderDto(
            status,
            reason,
            payload,
            ChangeOrderRequestType.CHANGE_LAST_MILE_FROM_PICKUP_TO_PICKUP
        );
        WaybillSegmentDto.ShipmentDto pickupShipment = WaybillSegmentDto.ShipmentDto.builder()
            .locationTo(LocationDto.builder()
                .address(AddressDto.builder().geoId(PICKUP_GEO_ID).build())
                .warehouseId(PICKUP_OUTLET_ID)
                .build())
            .build();
        lomOrderDto.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.FULFILLMENT)
                .partnerType(PartnerType.FULFILLMENT)
                .externalId(FF_EXTERNAL_ID)
                .partnerId(FF_PARTNER_ID)
                .build(),
            WaybillSegmentDto.builder().build(),
            WaybillSegmentDto.builder().build(),
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.PICKUP)
                .shipment(pickupShipment)
                .externalId(NEW_PICKUP_EXTERNAL_ID)
                .partnerId(NEW_PICKUP_PARTNER_ID)
                .build()
        ));
        return lomOrderDto;
    }

    @Nonnull
    public Order createCheckouterOrder() {
        Order order = new Order();
        Long orderId = Long.valueOf(BARCODE);
        order.setId(orderId);
        order.setChangeRequests(List.of(
            createChangeRequest()
        ));
        Delivery delivery = new Delivery();
        Parcel parcel = new Parcel();
        parcel.setId(PARCEL_ID);
        parcel.setStatus(ParcelStatus.READY_TO_SHIP);
        Track ffTrack = new Track(FF_EXTERNAL_ID, FF_PARTNER_ID);
        ffTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        parcel.setTracks(List.of(
            ffTrack,
            new Track("movement-external-id", 1234L)
        ));
        delivery.setParcels(List.of(parcel));
        order.setDelivery(delivery);
        return order;
    }

    @Nonnull
    public ChangeRequest createChangeRequest() {
        DeliveryLastMileChangeRequestPayload payload = new DeliveryLastMileChangeRequestPayload();
        payload.setAddress(createAddress());

        return new ChangeRequest(
            REQUEST_ID,
            Long.valueOf(BARCODE),
            payload,
            ChangeRequestStatus.PROCESSING,
            Instant.now(),
            null,
            ClientRole.SYSTEM
        );
    }

    @Nonnull
    public AddressImpl createAddress() {
        AddressImpl address = AddressSteps.getAddress();
        address.setNotes("Комментарий");
        return address;
    }

    @Nonnull
    private Location createLocation() {
        return new Location()
            .setId(regionId)
            .setCountry("country")
            .setFederalDistrict("federal-district")
            .setRegion("region")
            .setSubRegion("subregion");
    }
}
