package ru.yandex.travel.orders.services.promo.taxi2020;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.hotels.common.orders.TravellineHotelItinerary;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.HotelOrderItem;
import ru.yandex.travel.orders.entities.TravellineOrderItem;
import ru.yandex.travel.orders.entities.promo.taxi2020.Taxi2020PromoCode;
import ru.yandex.travel.orders.entities.promo.taxi2020.Taxi2020PromoOrder;
import ru.yandex.travel.orders.entities.promo.taxi2020.Taxi2020PromoOrderStatus;
import ru.yandex.travel.orders.repository.HotelOrderRepository;
import ru.yandex.travel.orders.repository.promo.taxi2020.Taxi2020PromoCodeRepository;
import ru.yandex.travel.orders.repository.promo.taxi2020.Taxi2020PromoOrderRepository;
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState;
import ru.yandex.travel.testing.time.SettableClock;
import ru.yandex.travel.workflow.single_operation.SingleOperationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.entities.promo.taxi2020.Taxi2020PromoOrderStatus.ELIGIBLE;
import static ru.yandex.travel.orders.entities.promo.taxi2020.Taxi2020PromoOrderStatus.EMAIL_SCHEDULED;
import static ru.yandex.travel.orders.entities.promo.taxi2020.Taxi2020PromoOrderStatus.NOT_ELIGIBLE;
import static ru.yandex.travel.orders.services.OperationTypes.TAXI_2020_PROMO_CODE_EMAIL_SENDER;
import static ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState.OS_CANCELLED;
import static ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState.OS_CONFIRMED;
import static ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState.OS_REFUNDED;
import static ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState.OS_WAITING_CONFIRMATION;

public class Taxi2020PromoServiceTest {
    private Taxi2020PromoServiceProperties properties;
    private Taxi2020PromoService service;
    private Taxi2020PromoOrderRepository promoOrderRepository;
    private Taxi2020PromoCodeRepository promoCodeRepository;
    private HotelOrderRepository hotelOrderRepository;
    private SettableClock settableClock;
    private SingleOperationService singleOperationService;

    @Before
    public void init() {
        properties = Taxi2020PromoServiceProperties.builder()
                .startsAt(Instant.parse("2020-08-17T00:00:00Z"))
                .endsAt(Instant.parse("2020-11-17T00:00:00Z"))
                .maxCheckInDate(LocalDate.parse("2021-03-31"))
                .minPriceCurrency("RUB")
                .minPriceValue(new BigDecimal("5000.0"))
                .maxProcessOrdersBatch(100)
                .emailScheduledAtOffset(Duration.ofHours(0))
                .smsScheduledAtOffset(Duration.ofSeconds(15))
                .smsTextTemplate("Your promo is %s")
                .defaultHotelTimeZoneId(ZoneId.of("UTC"))
                .build();
        promoOrderRepository = Mockito.mock(Taxi2020PromoOrderRepository.class);
        promoCodeRepository = Mockito.mock(Taxi2020PromoCodeRepository.class);
        hotelOrderRepository = Mockito.mock(HotelOrderRepository.class);
        settableClock = new SettableClock();
        singleOperationService = Mockito.mock(SingleOperationService.class);
        service = new Taxi2020PromoService(properties, promoOrderRepository, promoCodeRepository,
                hotelOrderRepository, settableClock, singleOperationService);
    }

    @Test
    public void isActive() {
        assertThat(service.isPromoActive(Instant.parse("2020-08-16T23:59:59Z"))).isFalse();
        assertThat(service.isPromoActive(Instant.parse("2020-08-17T00:00:00Z"))).isTrue();
        assertThat(service.isPromoActive(Instant.parse("2020-11-16T23:59:59Z"))).isTrue();
        assertThat(service.isPromoActive(Instant.parse("2020-11-17T00:00:00Z"))).isFalse();
    }

    @Test
    public void testMatcher() {
        // ok order
        String okUpdatedAt = "2020-09-01T14:43:56Z";
        EHotelOrderState okState = OS_CONFIRMED;
        String okCheckIn = "2020-12-15";
        double okPrice = 14000;
        assertThat(service.promoTermsMatch(order(okUpdatedAt, okState, okCheckIn, okPrice))).isTrue();

        // states
        assertThat(service.promoTermsMatch(order(okUpdatedAt, OS_WAITING_CONFIRMATION, okCheckIn, okPrice))).isFalse();
        assertThat(service.promoTermsMatch(order(okUpdatedAt, OS_CANCELLED, okCheckIn, okPrice))).isFalse();
        assertThat(service.promoTermsMatch(order(okUpdatedAt, OS_REFUNDED, okCheckIn, okPrice))).isFalse();

        // checkIn
        assertThat(service.promoTermsMatch(order(okUpdatedAt, okState, "2020-08-01", okPrice))).isTrue();
        assertThat(service.promoTermsMatch(order(okUpdatedAt, okState, "2020-08-17", okPrice))).isTrue();
        assertThat(service.promoTermsMatch(order(okUpdatedAt, okState, "2021-03-31", okPrice))).isTrue();
        assertThat(service.promoTermsMatch(order(okUpdatedAt, okState, "2021-04-01", okPrice))).isFalse();

        // price
        assertThat(service.promoTermsMatch(order(okUpdatedAt, okState, okCheckIn, 4999.99))).isFalse();
        assertThat(service.promoTermsMatch(order(okUpdatedAt, okState, okCheckIn, 5000))).isTrue();
        assertThat(service.promoTermsMatch(order(okUpdatedAt, okState, okCheckIn, 1005000))).isTrue();
    }

    @Test
    public void registerConfirmedOrder_inactive() {
        settableClock.setCurrentTime(Instant.parse("2020-08-13T00:00:00Z"));
        service.registerConfirmedOrder(order("2020-08-13T00:00:00Z", OS_CONFIRMED, "2021-02-21", 7000));
        verify(promoCodeRepository, times(0)).save(any());
    }

    @Test
    public void registerConfirmedOrder_active() {
        settableClock.setCurrentTime(Instant.parse("2020-08-18T00:00:00Z"));
        HotelOrder order = order("2020-08-18T00:00:00Z", OS_CONFIRMED, "2020-11-21", 8000);
        service.registerConfirmedOrder(order);

        ArgumentCaptor<Taxi2020PromoOrder> promoOrderCaptor = ArgumentCaptor.forClass(Taxi2020PromoOrder.class);
        verify(promoOrderRepository, times(1)).save(promoOrderCaptor.capture());
        Taxi2020PromoOrder promoOrder = promoOrderCaptor.getValue();
        assertThat(promoOrder).isNotNull();
        assertThat(promoOrder.getOrderId()).isEqualTo(order.getId());
        assertThat(promoOrder.getStatus()).isEqualTo(Taxi2020PromoOrderStatus.ELIGIBLE);
        assertThat(promoOrder.getEmail()).isEqualTo("some@example.com");
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2020-11-21T00:00:00Z"));
    }

    @Test
    public void registerConfirmedOrder_activeNotEligible() {
        settableClock.setCurrentTime(Instant.parse("2020-08-18T00:00:00Z"));
        HotelOrder order = order("2020-08-18T00:00:00Z", OS_CONFIRMED, "2020-11-21", 4000);
        service.registerConfirmedOrder(order);

        ArgumentCaptor<Taxi2020PromoOrder> promoOrderCaptor = ArgumentCaptor.forClass(Taxi2020PromoOrder.class);
        verify(promoOrderRepository, times(1)).save(promoOrderCaptor.capture());
        Taxi2020PromoOrder promoOrder = promoOrderCaptor.getValue();
        assertThat(promoOrder).isNotNull();
        assertThat(promoOrder.getOrderId()).isEqualTo(order.getId());
        assertThat(promoOrder.getStatus()).isEqualTo(NOT_ELIGIBLE);
        assertThat(promoOrder.getEmail()).isNull();
        assertThat(promoOrder.getEmailScheduledAt()).isNull();
    }

    @Test
    public void processOrdersBatch() {
        Taxi2020PromoOrder promoOrder1 = promoOrder(uuid(1), NOT_ELIGIBLE);
        Taxi2020PromoOrder promoOrder2 = promoOrder(uuid(2), ELIGIBLE);
        when(promoOrderRepository.findOrdersToCheck(any(), any())).thenReturn(List.of(promoOrder1, promoOrder2));
        HotelOrder order1 = order(uuid(1), "2020-11-18T00:00:00Z", OS_CONFIRMED, "2020-12-22", 7000);
        HotelOrder order2 = order(uuid(2), "2020-11-19T00:00:00Z", OS_REFUNDED, "2020-12-22", 7000);
        when(hotelOrderRepository.findAllById(any())).thenReturn(List.of(order1, order2));

        service.processOrdersBatch(Taxi2020PromoService.SINGLETON_ORDERS_WAITING_FOR_PROCESSING_TASK);

        // both are processed
        assertThat(promoOrder1.getStatus()).isEqualTo(ELIGIBLE);
        assertThat(promoOrder1.getStatusUpdatedAt()).isEqualTo("2020-11-18T00:00:00Z");
        assertThat(promoOrder1.getEmail()).isEqualTo("some@example.com");
        assertThat(promoOrder1.getEmailScheduledAt()).isEqualTo(Instant.parse("2020-12-22T00:00:00Z"));
        assertThat(promoOrder2.getStatus()).isEqualTo(NOT_ELIGIBLE);
        assertThat(promoOrder2.getStatusUpdatedAt()).isEqualTo("2020-11-19T00:00:00Z");
        assertThat(promoOrder2.getEmail()).isNull();
        assertThat(promoOrder2.getEmailScheduledAt()).isNull();
    }

    @Test
    public void processPendingOrder_notEligibleBecomesScheduled() {
        HotelOrder order = order(uuid(1), "2020-09-01T14:43:56Z", OS_CONFIRMED, "2021-02-21", 6000);
        Taxi2020PromoOrder promoOrder = promoOrder(uuid(1), NOT_ELIGIBLE);
        Taxi2020PromoCode promoCode = Taxi2020PromoCode.builder()
                .code("code1").expiresAt(Instant.parse("4021-02-21T00:00:00Z")).build();
        Instant currentTime = Instant.parse("2021-02-21T00:00:00.001Z");
        settableClock.setCurrentTime(currentTime);
        when(promoCodeRepository.findAnyByUsedAtIsNull()).thenReturn(promoCode);
        String taxi2020opType = TAXI_2020_PROMO_CODE_EMAIL_SENDER.getValue();
        when(singleOperationService.runOperation(any(), eq(taxi2020opType), any()))
                .thenReturn(UUID.randomUUID());

        service.processPendingOrder(promoOrder, order);

        assertThat(promoOrder.getStatus()).isEqualTo(EMAIL_SCHEDULED);
        assertThat(promoOrder.getEmail()).isEqualTo("some@example.com");
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-21T00:00:00Z"));
        assertThat(promoOrder.getPromoCode()).isEqualTo("code1");
        assertThat(promoOrder.getSendEmailOperationId()).isNotNull();
        assertThat(promoCode.getUsedAt()).isEqualTo(currentTime);
    }

    @Test
    public void processPendingOrder_offset() {
        HotelOrder order = order(uuid(1), "2020-09-01T14:43:56Z", OS_CONFIRMED, "2021-02-21", 6000);
        Taxi2020PromoOrder promoOrder = promoOrder(uuid(1), ELIGIBLE);

        properties.setEmailScheduledAtOffset(Duration.ofHours(-8));
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getStatus()).isEqualTo(ELIGIBLE);
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-20T16:00:00Z"));

        properties.setEmailScheduledAtOffset(Duration.ofHours(0));
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-21T00:00:00Z"));

        properties.setEmailScheduledAtOffset(Duration.ofHours(8));
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-21T08:00:00Z"));
    }

    @Test
    public void processPendingOrder_hotelTimeZone() {
        HotelOrder order = order(uuid(1), "2020-09-01T14:43:56Z", OS_CONFIRMED, "2021-02-21", 6000);
        Taxi2020PromoOrder promoOrder = promoOrder(uuid(1), ELIGIBLE);

        properties.setDefaultHotelTimeZoneId(ZoneId.of("UTC"));
        assertThat(promoOrder.getStatus()).isEqualTo(ELIGIBLE);
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-21T00:00:00Z"));

        properties.setDefaultHotelTimeZoneId(ZoneId.of("Europe/Moscow"));
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-20T21:00:00Z"));

        properties.setDefaultHotelTimeZoneId(ZoneId.of("America/Los_Angeles"));
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-21T08:00:00Z"));

        properties.setDefaultHotelTimeZoneId(ZoneId.of("UTC"));
        ((HotelOrderItem) order.getOrderItems().get(0)).getHotelItinerary().getOrderDetails()
                .setHotelTimeZoneId(ZoneId.of("Asia/Yekaterinburg"));
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-20T19:00:00Z"));
    }

    @Test
    public void processPendingOrder_updatedOrdersSync() {
        HotelOrder order = order(uuid(1), "2020-08-24T17:43:56Z", OS_CONFIRMED, "2021-02-21", 6000);
        Taxi2020PromoOrder promoOrder = promoOrder(uuid(1), ELIGIBLE);

        // base state: eligible order is updated, nothing changes
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getStatus()).isEqualTo(ELIGIBLE);
        assertThat(promoOrder.getEmail()).isEqualTo("some@example.com");
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-21T00:00:00Z"));

        // email scheduling related data changed
        order.setEmail("some-other@example.com");
        ((HotelOrderItem) order.getOrderItems().get(0)).getHotelItinerary().getOrderDetails()
                .setCheckinDate(LocalDate.parse("2021-02-23"));
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getStatus()).isEqualTo(ELIGIBLE);
        assertThat(promoOrder.getEmail()).isEqualTo("some-other@example.com");
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-23T00:00:00Z"));

        // order that used to satisfy promo terms is changed but we age still going to grant a promo code
        ((HotelOrderItem) order.getOrderItems().get(0)).getHotelItinerary().setFiscalPrice(Money.of(4000, "RUB"));
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getStatus()).isEqualTo(ELIGIBLE);
        assertThat(promoOrder.getEmail()).isEqualTo("some-other@example.com");
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-23T00:00:00Z"));

        // order is cancelled, now is the time to revoke the code
        order.setState(OS_REFUNDED);
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getStatus()).isEqualTo(NOT_ELIGIBLE);
        assertThat(promoOrder.getEmail()).isNull();
        assertThat(promoOrder.getEmailScheduledAt()).isNull();

        // not eligible confirmed orders don't get promo codes until they satisfy all promo terms
        order.setState(OS_CONFIRMED);
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getStatus()).isEqualTo(NOT_ELIGIBLE);
        assertThat(promoOrder.getEmail()).isNull();
        assertThat(promoOrder.getEmailScheduledAt()).isNull();

        // but once they start satisfying the terms we will issue promo codes for such orders
        ((HotelOrderItem) order.getOrderItems().get(0)).getHotelItinerary().setFiscalPrice(Money.of(7000, "RUB"));
        service.processPendingOrder(promoOrder, order);
        assertThat(promoOrder.getStatus()).isEqualTo(ELIGIBLE);
        assertThat(promoOrder.getEmail()).isEqualTo("some-other@example.com");
        assertThat(promoOrder.getEmailScheduledAt()).isEqualTo(Instant.parse("2021-02-23T00:00:00Z"));
    }

    private static HotelOrder order(String updatedAt, EHotelOrderState state, String checkIn, double price) {
        return order(UUID.randomUUID(), updatedAt, state, checkIn, price);
    }

    private static HotelOrder order(UUID id, String currentTimestamp, EHotelOrderState state, String checkIn,
                                    double price) {
        HotelOrder order = new HotelOrder();
        order.setId(id);
        order.setCreatedAt(Instant.parse(currentTimestamp));
        order.setUpdatedAt(Instant.parse(currentTimestamp));
        order.setState(state);
        order.setCurrency(ProtoCurrencyUnit.RUB);
        order.setEmail("some@example.com");
        TravellineHotelItinerary itinerary = new TravellineHotelItinerary();
        itinerary.setFiscalPrice(Money.of(price, "RUB"));
        itinerary.setOrderDetails(OrderDetails.builder()
                .checkinDate(LocalDate.parse(checkIn))
                .checkoutDate(LocalDate.parse("4020-08-11"))
                .build());
        TravellineOrderItem orderItem = new TravellineOrderItem();
        orderItem.setUpdatedAt(Instant.parse(currentTimestamp));
        orderItem.setItinerary(itinerary);
        order.addOrderItem(orderItem);
        return order;
    }

    private static Taxi2020PromoOrder promoOrder(UUID orderId, Taxi2020PromoOrderStatus status) {
        return Taxi2020PromoOrder.builder()
                .orderId(orderId)
                .status(status)
                .build();
    }

    private static UUID uuid(int shortId) {
        return UUID.fromString("0-0-0-0-" + shortId);
    }
}
