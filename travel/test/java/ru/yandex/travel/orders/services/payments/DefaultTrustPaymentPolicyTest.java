package ru.yandex.travel.orders.services.payments;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.hotels.common.orders.ExpediaHotelItinerary;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.hotels.common.refunds.RefundRule;
import ru.yandex.travel.hotels.common.refunds.RefundRules;
import ru.yandex.travel.hotels.common.refunds.RefundType;
import ru.yandex.travel.orders.entities.ExpediaOrderItem;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.factories.SuburbanOrderItemEnvProviderFactory;
import ru.yandex.travel.orders.services.promo.mir2020.Mir2020PromoService;
import ru.yandex.travel.orders.services.suburban.environment.SuburbanOrderItemEnvProvider;
import ru.yandex.travel.testing.time.SettableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultTrustPaymentPolicyTest {

    private final SettableClock clock = new SettableClock();
    private DefaultTrustPaymentPolicy subject;
    private TrustPaymentPolicyProperties properties;
    private Mir2020PromoService mir2020PromoService;
    private SuburbanOrderItemEnvProvider suburbanEnvProvider;

    @Before
    public void init() {
        properties = new TrustPaymentPolicyProperties();
        properties.setHotels(new TrustPaymentPolicyProperties.HotelProperties());
        properties.getHotels().setNoThreeDs(new TrustPaymentPolicyProperties.HotelProperties.NoThreeDsProperties());
        properties.getHotels().getNoThreeDs().setDaysBeforeCheckin(3);
        properties.getHotels().getNoThreeDs().setDaysBeforePenalty(3);
        properties.getHotels().getNoThreeDs().setMaxPrice(BigDecimal.valueOf(12_000_000));
        mir2020PromoService = mock(Mir2020PromoService.class);
        suburbanEnvProvider = SuburbanOrderItemEnvProviderFactory.createEnvProvider();
        subject = new DefaultTrustPaymentPolicy(clock, properties, mir2020PromoService, suburbanEnvProvider);
    }

    @Test
    public void testNo3dsForceForTrainOrder() {
        assertThat(subject.forceThreeDs(new TrainOrder())).isFalse();
        assertThat(subject.eligibleForMir(new TrainOrder())).isFalse();
    }

    @Test
    public void test3dsForceForHotelOrdersOnException() {
        assertThat(subject.forceThreeDs(new HotelOrder())).isTrue();
        assertThat(subject.eligibleForMir(new TrainOrder())).isFalse();
    }

    @Test
    public void testNo3dsForceForHotelOrders() {
        LocalDate currentTime = LocalDate.of(2020, 9, 1);
        clock.setCurrentTime(currentTime.atStartOfDay(ZoneId.systemDefault()).toInstant());
        HotelOrder order = createHotelOrder(LocalDate.of(2020, 9, 10), 1000L);
        assertThat(subject.forceThreeDs(order)).isFalse();
        assertThat(subject.eligibleForMir(new TrainOrder())).isFalse();
    }

    @Test
    public void testNo3dsForceByFlag() {
        properties.getHotels().setForceEnableThreeDs(true);
        LocalDate currentTime = LocalDate.of(2020, 9, 1);
        clock.setCurrentTime(currentTime.atStartOfDay(ZoneId.systemDefault()).toInstant());
        HotelOrder order = createHotelOrder(LocalDate.of(2020, 9, 10), 1000L);
        assertThat(subject.forceThreeDs(order)).isTrue();
        assertThat(subject.eligibleForMir(new TrainOrder())).isFalse();
    }

    @Test
    public void test3dsForceForHotelOrdersWithMinAmountGreater() {
        LocalDate currentTime = LocalDate.of(2020, 9, 1);
        clock.setCurrentTime(currentTime.atStartOfDay(ZoneId.systemDefault()).toInstant());
        HotelOrder order = createHotelOrder(LocalDate.of(2020, 9, 10), 13_000_000);
        assertThat(subject.forceThreeDs(order)).isTrue();
        assertThat(subject.eligibleForMir(new TrainOrder())).isFalse();
    }

    @Test
    public void test3dsForceForHotelOrdersWithCheckinNear() {
        LocalDate currentTime = LocalDate.of(2020, 9, 1);
        clock.setCurrentTime(currentTime.atStartOfDay(ZoneId.systemDefault()).toInstant());
        HotelOrder order = createHotelOrder(currentTime.plus(1, ChronoUnit.DAYS), 1000L);
        assertThat(subject.forceThreeDs(order)).isTrue();
        assertThat(subject.eligibleForMir(new TrainOrder())).isFalse();
    }

    @Test
    public void testMirPromoForEligibleHotelOrder() {
        when(mir2020PromoService.checkInitialEligibility(any())).thenReturn(true);
        assertThat(subject.eligibleForMir(createHotelOrder(LocalDate.of(2020, 9, 10), 1000L))).isTrue();
    }

    private HotelOrder createHotelOrder(LocalDate checkinDate, long price) {
        ZoneId zoneId = ZoneId.of("UTC+8");
        ExpediaOrderItem orderItem = new ExpediaOrderItem();
        orderItem.setItinerary(new ExpediaHotelItinerary());

        RefundRules rules = RefundRules.builder()
                .rule(RefundRule.builder()
                        .type(RefundType.FULLY_REFUNDABLE)
                        .startsAt(LocalDate.of(2020, 9, 3).atStartOfDay(zoneId).toInstant())
                        .endsAt(LocalDate.of(2020, 9, 5).atStartOfDay(zoneId).toInstant())
                        .build())
                .rule(RefundRule.builder()
                        .type(RefundType.REFUNDABLE_WITH_PENALTY)
                        .startsAt(LocalDate.of(2020, 9, 5).atStartOfDay(zoneId).toInstant())
                        .endsAt(LocalDate.of(2020, 9, 8).atStartOfDay(zoneId).toInstant())
                        .build())
                .build();

        var builder = OrderDetails.builder()
                .hotelTimeZoneId(zoneId)
                .checkinDate(checkinDate);

        orderItem.getHotelItinerary().setOrderDetails(builder.build());
        orderItem.getHotelItinerary().setRefundRules(rules);
        orderItem.addFiscalItem(new FiscalItem());
        orderItem.getFiscalItems().get(0).setMoneyAmount(Money.of(price, ProtoCurrencyUnit.RUB));

        HotelOrder order = new HotelOrder();
        order.addOrderItem(orderItem);
        return order;
    }
}
