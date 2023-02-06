package ru.yandex.travel.orders.test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.javamoney.moneta.Money;

import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotCarrier;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotSegment;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotSegmentNode;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotServicePayload;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTotalOffer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.model.booking.ClientInfo;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.hotels.common.orders.ConfirmationInfo;
import ru.yandex.travel.hotels.common.orders.ExpediaHotelItinerary;
import ru.yandex.travel.hotels.common.orders.Guest;
import ru.yandex.travel.hotels.common.orders.HotelItinerary;
import ru.yandex.travel.hotels.common.orders.HotelUserInfo;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.hotels.common.orders.TravellineHotelItinerary;
import ru.yandex.travel.hotels.common.orders.promo.AppliedPromoCampaigns;
import ru.yandex.travel.hotels.common.orders.promo.YandexPlusApplication;
import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.commons.proto.EPaymentOutcome;
import ru.yandex.travel.orders.commons.proto.TPaymentTestContext;
import ru.yandex.travel.orders.entities.AeroflotOrder;
import ru.yandex.travel.orders.entities.AeroflotOrderItem;
import ru.yandex.travel.orders.entities.ExpediaOrderItem;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.FiscalItemType;
import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.HotelOrderItem;
import ru.yandex.travel.orders.entities.Order;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.entities.SuburbanOrderItem;
import ru.yandex.travel.orders.entities.TravellineOrderItem;
import ru.yandex.travel.orders.entities.VatType;
import ru.yandex.travel.orders.integration.train.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.workflow.hotels.expedia.proto.EExpediaItemState;
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState;
import ru.yandex.travel.orders.workflow.hotels.travelline.proto.ETravellineItemState;
import ru.yandex.travel.orders.workflow.order.aeroflot.proto.EAeroflotOrderState;
import ru.yandex.travel.orders.workflow.order.generic.proto.EOrderState;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.suburban.model.ImReservation;
import ru.yandex.travel.suburban.model.SuburbanReservation;
import ru.yandex.travel.suburban.partners.SuburbanCarrier;
import ru.yandex.travel.suburban.partners.SuburbanProvider;
import ru.yandex.travel.workflow.entities.Workflow;

/**
 * @see ru.yandex.travel.orders.integration.train.factories.TrainOrderItemFactory
 */
@UtilityClass
public class TestOrderFactory {
    public static HotelOrder createHotelOrder() {
        return createHotelOrder(createExpediaOrderItem());
    }

    public static ExpediaOrderItem createExpediaOrderItem() {
        var orderItem = createOrderItem(ExpediaOrderItem.class);
        orderItem.setItinerary(createOrderItinerary(ExpediaHotelItinerary.class));
        orderItem.setState(EExpediaItemState.IS_CONFIRMED);
        return orderItem;
    }

    public static TravellineOrderItem createTravellineOrderItem() {
        var orderItem = createOrderItem(TravellineOrderItem.class);
        orderItem.setItinerary(createOrderItinerary(TravellineHotelItinerary.class));
        orderItem.setState(ETravellineItemState.IS_CONFIRMED);
        return orderItem;
    }

    public static HotelOrder createHotelOrder(HotelOrderItem orderItem) {
        var order = new HotelOrder();
        setCommonOrderData(order);
        order.setState(EHotelOrderState.OS_CONFIRMED);
        order.setDisplayType(EDisplayOrderType.DT_HOTEL);
        order.setPaymentTestContext(TPaymentTestContext.newBuilder()
                .setPaymentOutcome(EPaymentOutcome.PO_SUCCESS)
                .build());
        order.addOrderItem(orderItem);
        return order;
    }

    @SneakyThrows
    public static <SomeHotelItinerary extends HotelItinerary> SomeHotelItinerary createOrderItinerary(
            Class<SomeHotelItinerary> itineraryClass) {
        SomeHotelItinerary itinerary = itineraryClass.getDeclaredConstructor().newInstance();

        Guest guest = new Guest();
        guest.setFirstName("Test");
        guest.setLastName("Testov");
        itinerary.setGuests(List.of(guest));
        itinerary.setCustomerEmail("user1@unit.test");
        itinerary.setCustomerPhone("71234567890");
        itinerary.setOrderDetails(OrderDetails.builder()
                .hotelName("hotel")
                // scheduling it into the past to trigger the single operation task processor without any delays
                .checkinDate(LocalDate.parse("2021-07-03"))
                .checkoutDate(LocalDate.parse("2021-07-10"))
                .hotelPhone("12345678")
                .ratePlanDetails("специальная инструкция")
                .permalink(1L)
                .build());
        ConfirmationInfo confirmation = new ConfirmationInfo();
        confirmation.setHotelConfirmationId("confirmationId");
        itinerary.setConfirmation(confirmation);
        itinerary.setFiscalPrice(Money.of(1000, "RUB"));
        itinerary.setAppliedPromoCampaigns(AppliedPromoCampaigns.builder()
                .yandexPlus(YandexPlusApplication.builder()
                        .mode(YandexPlusApplication.Mode.TOPUP)
                        .points(1234)
                        .build())
                .build());
        itinerary.setUserInfo(HotelUserInfo.builder()
                .plusBalance(100)
                .build());

        return itinerary;
    }

    @SneakyThrows
    public static <SomeHotelOrderItem extends HotelOrderItem> SomeHotelOrderItem createOrderItem(
            Class<SomeHotelOrderItem> oiClass) {
        SomeHotelOrderItem item = oiClass.getDeclaredConstructor().newInstance();
        item.setId(UUID.randomUUID());

        var fiscalItem = new FiscalItem();
        fiscalItem.setMoneyAmount(Money.of(1000, ProtoCurrencyUnit.RUB));
        fiscalItem.setType(FiscalItemType.EXPEDIA_HOTEL);
        fiscalItem.setVatType(VatType.VAT_20);
        item.addFiscalItem(fiscalItem);

        return item;
    }

    public static AeroflotOrder createAeroflotOrder() {
        var order = new AeroflotOrder();
        order.setState(EAeroflotOrderState.OS_CONFIRMED);
        setCommonOrderData(order);

        AeroflotOrderItem item = new AeroflotOrderItem();
        item.setPayload(AeroflotServicePayload.builder()
                .variant(
                        AeroflotVariant.builder()
                                .segments(List.of(aeroflotSegment()))
                                .offer(AeroflotTotalOffer.builder()
                                        .totalPrice(Money.of(1000, ProtoCurrencyUnit.RUB))
                                        .build())
                                .build()
                )
                .partnerId("partner")
                .travellers(List.of())
                .preliminaryCost(Money.of(1, ProtoCurrencyUnit.RUB))
                .clientInfo(ClientInfo.builder()
                        .email("email")
                        .phone("phone")
                        .userIp("1.1.1.1")
                        .userAgent("userAgent")
                        .build())
                .build());
        order.addOrderItem(item);

        return order;
    }

    private static void setCommonOrderData(Order order) {
        order.setId(UUID.randomUUID());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setPrettyId("Pretty");
        order.setCurrency(ProtoCurrencyUnit.RUB);
    }

    private static AeroflotSegment aeroflotSegment() {
        return AeroflotSegment.builder()
                .id(UUID.randomUUID().toString())
                .originDestinationId("SVX")
                .arrival(AeroflotSegmentNode.builder()
                        .airportCode("SVX")
                        .date("2021-10-10")
                        .time("04:20")
                        .build())
                .marketingCarrier(AeroflotCarrier.builder()
                        .airlineId("?")
                        .flightNumber("1")
                        .build())
                .operatingCarrier(AeroflotCarrier.builder()
                        .airlineId("?")
                        .flightNumber("1")
                        .build())
                .aircraftCode("1")
                .flightDuration("1")
                .departure(AeroflotSegmentNode.builder()
                        .airportCode("SVX")
                        .date("2021-10-10")
                        .time("04:20")
                        .build())

                .build();
    }

    public static GenericOrder createGenericOrder(EDisplayOrderType type, Supplier<? extends OrderItem> orderItemProvider) {
        GenericOrder order = new GenericOrder();
        order.setDisplayType(type);
        setCommonOrderData(order);
        order.setState(EOrderState.OS_CONFIRMED);


        order.addOrderItem(orderItemProvider.get());

        order.setWorkflow(Workflow.createWorkflowForEntity(order));

        return order;
    }

    public static GenericOrder createGenericTrainOrder() {
        TrainOrderItemFactory factory = new TrainOrderItemFactory();
        return createGenericOrder(EDisplayOrderType.DT_TRAIN, factory::createTrainOrderItem);
    }

    public static GenericOrder createGenericSuburbanOrder() {
        return createGenericOrder(EDisplayOrderType.DT_SUBURBAN, () -> {
            SuburbanOrderItem item = new SuburbanOrderItem();
            item.setState(EOrderItemState.IS_CONFIRMED);
            item.setId(UUID.randomUUID());
            item.setReservation(SuburbanReservation.builder()
                    .price(Money.of(10, ProtoCurrencyUnit.RUB))
                    .stationFrom(SuburbanReservation.Station.builder()
                            .id(1)
                            .titleDefault("point A")
                            .build())
                    .stationTo(SuburbanReservation.Station.builder()
                            .id(2)
                            .titleDefault("point B")
                            .build())

                    .provider(SuburbanProvider.IM)
                    .carrier(SuburbanCarrier.SZPPK)
                    .imReservation(ImReservation.builder()
                            .orderId(1)
                            .date(LocalDate.EPOCH)
                            .build())

                    .build());
            return item;
        });
    }
}
