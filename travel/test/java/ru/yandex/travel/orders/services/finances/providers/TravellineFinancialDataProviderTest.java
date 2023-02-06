package ru.yandex.travel.orders.services.finances.providers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.money.Monetary;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.hotels.administrator.export.proto.HotelAgreement;
import ru.yandex.travel.hotels.common.orders.BaseRate;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.hotels.common.orders.RefundInfo;
import ru.yandex.travel.hotels.common.orders.TravellineHotelItinerary;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.orders.commons.proto.EVat;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.HotelOrderItem;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.entities.PaymentSchedule;
import ru.yandex.travel.orders.entities.PaymentScheduleItem;
import ru.yandex.travel.orders.entities.PendingInvoice;
import ru.yandex.travel.orders.entities.PendingInvoiceItem;
import ru.yandex.travel.orders.entities.TravellineOrderItem;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.FinancialEventType;
import ru.yandex.travel.orders.repository.FinancialEventRepository;
import ru.yandex.travel.orders.services.finances.HotelAgreementService;
import ru.yandex.travel.orders.workflow.payments.proto.EPaymentState;
import ru.yandex.travel.utils.ClockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.entities.finances.FinancialEventType.PAYMENT;
import static ru.yandex.travel.orders.entities.finances.FinancialEventType.REFUND;
import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

@RunWith(SpringRunner.class)
public class TravellineFinancialDataProviderTest {

    private static final String HOTEL_ID = "123";
    private static final long FINANCIAL_CLIENT_ID = 11;
    private static final long FINANCIAL_CONTRACT_ID = 3764624323132131231L;
    private static final String PRETTY_ID = "PrettyId";
    private static final Instant CURRENT_MOMENT = Instant.parse("2019-11-22T13:15:30.00Z");
    private static final String HOTEL_INN = "123321";

    @Mock
    private HotelAgreementService agreementService;

    @Mock
    private FinancialEventRepository financialEventRepository;

    @Before
    public void setup() {
        HotelAgreement agreement = HotelAgreement.newBuilder()
                .setId(0L)
                .setHotelId(HOTEL_ID)
                .setPartnerId(EPartnerId.PI_TRAVELLINE)
                .setAgreementStartDate(CURRENT_MOMENT.toEpochMilli())
                .setInn(HOTEL_INN)
                .setVatType(EVat.VAT_18)
                .setAgreementEndDate(CURRENT_MOMENT.plus(10, ChronoUnit.DAYS).toEpochMilli())
                .setFinancialClientId(FINANCIAL_CLIENT_ID)
                .setFinancialContractId(FINANCIAL_CONTRACT_ID)
                .setOrderConfirmedRate("0.14")
                .setOrderRefundedRate("0.08")
                .setEnabled(true)
                .setSendEmptyOrdersReport(true)
                .build();
        when(agreementService.getAgreementForTimestamp(eq(HOTEL_ID), eq(EPartnerId.PI_TRAVELLINE), any(Instant.class)))
                .thenReturn(agreement);
    }

    public void mockFullyPaidEvents(Boolean enablePromoFee) {
        FullMoneySplitCalculator calculator = new FullMoneySplitCalculator();
        when(financialEventRepository.findAllByOrderItem(any())).thenAnswer(a -> {
            List<FinancialEvent> result = new ArrayList<>(1);
            HotelOrderItem orderItem = a.getArgument(0);
            MoneySplit split = ProviderHelper.splitMoney(orderItem.getHotelItinerary().getFiscalPrice(),
                    BigDecimal.valueOf(0.14));
            FullMoneySplit fullSplit = calculator.calculatePaymentWithPromoMoney(split,
                    orderItem.getTotalDiscount());
            FinancialEvent event = FinancialEvent.builder()
                    .type(PAYMENT)
                    .build();
            ProviderHelper.setPaymentMoney(event, fullSplit, enablePromoFee);
            result.add(event);
            return result;
        });
    }

    public void mockEventsWithList(List<FinancialEvent> listToReturn) {
        when(financialEventRepository.findAllByOrderItem(any())).thenReturn(listToReturn);
    }


    private DirectHotelBillingFinancialDataProvider providerWithClockFixed(String currentTimeUtc) {
        Clock clock = Clock.fixed(Instant.parse(currentTimeUtc), ZoneId.of("UTC"));
        return new DirectHotelBillingFinancialDataProvider(ClockService.create(clock), new FullMoneySplitCalculator(),
                financialEventRepository);
    }

    private TravellineOrderItem createOrderItem(String nowStr, String checkoutDate) {
        return createOrderItem(nowStr, checkoutDate, null);
    }

    private TravellineOrderItem createOrderItem(String nowStr, String checkoutDate, Double discount) {
        return createOrderItem(nowStr, checkoutDate, discount, 2000);
    }

    private TravellineOrderItem createOrderItem(String nowStr, String checkoutDate, Number discount, Number penalty) {
        Instant now = Instant.parse(nowStr);
        TravellineOrderItem orderItem = new TravellineOrderItem();
        HotelOrder order = new HotelOrder();
        order.setCurrency(ProtoCurrencyUnit.RUB);
        order.setPrettyId(PRETTY_ID);
        orderItem.setOrder(order);
        TravellineHotelItinerary hotelItinerary = new TravellineHotelItinerary();
        hotelItinerary.setOrderDetails(OrderDetails.builder()
                .originalId(HOTEL_ID)
                .checkoutDate(LocalDate.parse(checkoutDate))
                .build());
        hotelItinerary.setFiscalPrice(Money.of(10_000, "RUB"));
        var refund = hotelItinerary.getFiscalPrice().subtract(Money.of(penalty, "RUB")).getNumber();

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setPenalty(new BaseRate(penalty.toString(), "RUB"));
        refundInfo.setRefund(new BaseRate(refund.toString(), "RUB"));
        hotelItinerary.setRefundInfo(refundInfo);
        orderItem.setItinerary(hotelItinerary);
        orderItem.setConfirmedAt(now.plusSeconds(10));
        orderItem.setRefundedAt(now.plusSeconds(20));
        orderItem.setAgreement(agreementService.getAgreementForTimestamp(HOTEL_ID, EPartnerId.PI_TRAVELLINE,
                orderItem.getConfirmedAt()));

        orderItem.addFiscalItem(FiscalItem.builder()
                .moneyAmount(hotelItinerary.getFiscalPrice())
                .build());
        if (discount != null) {
            orderItem.getFiscalItems().get(0).applyDiscount(Money.of(discount, "RUB"));
        }

        return orderItem;
    }

    @Test
    public void onConfirmationEventStructure() {
        var enablePromoFee = false;
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-26");
        List<FinancialEvent> events = providerWithClockFixed("2019-11-12T13:15:30.00Z").onConfirmation(orderItem, enablePromoFee);
        assertThat(events.size()).isEqualTo(1);
        var event = events.get(0);
        assertThat(event.getOrderItem()).isNotNull();
        assertThat(event.getOrder()).isNotNull();
        assertThat(event.getOrderPrettyId()).isEqualTo(PRETTY_ID);
        assertThat(event.getType()).isEqualTo(PAYMENT);
        assertThat(event.getBillingClientId()).isEqualTo(FINANCIAL_CLIENT_ID);
        assertThat(event.getBillingContractId()).isEqualTo(FINANCIAL_CONTRACT_ID);
        assertThat(event.getAccrualAt()).isEqualTo(Instant.parse("2019-11-12T13:15:30.00Z"));
        assertThat(event.getPayoutAt()).isEqualTo(mskDtToUtcInstant("2019-11-25T00:00:00"));
        assertThat(event.getAccountingActAt()).isEqualTo(mskDtToUtcInstant("2019-11-26T00:00:00"));
        // @ MSK
        assertThat(event.getPartnerAmount()).isEqualTo(Money.of(8_600, "RUB"));
        assertThat(event.getFeeAmount()).isEqualTo(Money.of(1_400, "RUB"));
        assertThat(event.getPartnerRefundAmount()).isNull();
        assertThat(event.getFeeRefundAmount()).isNull();
    }

    @Test
    public void onConfirmation_payoutAfterCheckout() {
        var enablePromoFee = false;
        // payment & act this month
        String now = "2019-11-27T13:15:30.00Z";
        List<FinancialEvent> events = providerWithClockFixed(now).onConfirmation(createOrderItem(now, "2019-11-30"), enablePromoFee);
        assertThat(events.size()).isEqualTo(1);
        var e1 = events.get(0);
        assertThat(e1.getPayoutAt()).isEqualTo(Instant.parse("2019-11-29T21:00:00.00Z")); // 30 Nov, 00:00 @ MSK
        assertThat(e1.getAccountingActAt()).isEqualTo(Instant.parse("2019-11-29T21:00:00.00Z")); // same last day of
        // month

        // payment & act next month
        now = "2019-11-28T13:15:30.00Z";
        List<FinancialEvent> events2 = providerWithClockFixed(now).onConfirmation(createOrderItem(now, "2019-11-30"), enablePromoFee);
        assertThat(events2.size()).isEqualTo(1);
        var e2 = events2.get(0);
        assertThat(e2.getPayoutAt()).isEqualTo(mskDtToUtcInstant("2019-12-01T00:00:00"));
        assertThat(e2.getAccountingActAt()).isEqualTo(mskDtToUtcInstant("2019-12-01T00:00:00"));
    }

    @Test
    public void onRefundEventStructure() {
        var enablePromoFee = false;
        mockFullyPaidEvents(enablePromoFee);
        List<FinancialEvent> events = providerWithClockFixed("2019-11-12T13:15:30.00Z")
                .onRefund(createOrderItem(CURRENT_MOMENT.toString(), "2019-11-26"), enablePromoFee);
        assertThat(events).hasSize(1);
        FinancialEvent event = events.get(0);
        assertThat(event.getOrderItem()).isNotNull();
        assertThat(event.getOrder()).isNotNull();
        assertThat(event.getOrderPrettyId()).isEqualTo(PRETTY_ID);
        assertThat(event.getType()).isEqualTo(FinancialEventType.REFUND);
        assertThat(event.getBillingClientId()).isEqualTo(FINANCIAL_CLIENT_ID);
        assertThat(event.getBillingContractId()).isEqualTo(FINANCIAL_CONTRACT_ID);
        assertThat(event.getAccrualAt()).isEqualTo(Instant.parse("2019-11-12T13:15:30.00Z"));
        assertThat(event.getPayoutAt()).isEqualTo(Instant.parse("2019-11-24T21:00:00.00Z")); // 25 Nov, 00:00 @ MSK
        assertThat(event.getAccountingActAt()).isEqualTo(mskDtToUtcInstant("2019-11-26T00:00:00"));
        // @ MSK
        assertThat(event.getPartnerAmount()).isNull();
        assertThat(event.getFeeAmount()).isNull();
        // Confirmation money = partner + 14% fee   -> 10 000 = 8 600 + 1 400
        // Penalty money = partner + 8% fee         ->  2 000 = 1 840 +   160
        // Refund money = confirmed - penalty       ->  6 760 = 8 600 - 1 840   for partner money
        //                                              1 240 = 1 400 -   160   for fee money
        assertThat(event.getPartnerRefundAmount()).isEqualTo(Money.of(6_760, "RUB"));
        assertThat(event.getFeeRefundAmount()).isEqualTo(Money.of(1_240, "RUB"));
    }

    @Test
    public void onRefund() {
        var enablePromoFee = false;
        // payment & act this month
        String now = "2019-11-27T13:15:30.00Z";
        String refundInADay = "2019-11-28T10:15:25.00Z";
        Instant expectedPaymentDt = Instant.parse("2019-11-29T21:00:00.00Z");

        OrderItem item = createOrderItem(now, "2019-11-30");
        List<FinancialEvent> events = providerWithClockFixed(now).onConfirmation(item, enablePromoFee);
        assertThat(events.size()).isEqualTo(1);
        var e1 = events.get(0);
        assertThat(e1.getPayoutAt()).isEqualTo(expectedPaymentDt); // 30 Nov, 00:00 @ MSK
        assertThat(e1.getAccountingActAt()).isEqualTo(Instant.parse("2019-11-29T21:00:00.00Z")); // same last day of
        mockFullyPaidEvents(enablePromoFee);
        List<FinancialEvent> refundEvents = providerWithClockFixed(refundInADay).onRefund(item, enablePromoFee);
        assertThat(refundEvents).hasSize(1);
        FinancialEvent e2 = refundEvents.get(0);
        // refund is scheduled the same day as payment
        assertThat(e2.getType()).isEqualTo(REFUND);
        assertThat(e2.getPayoutAt()).isEqualTo(expectedPaymentDt); // 30 Nov, 00:00 @ MSK
        assertThat(e2.getAccountingActAt()).isEqualTo(Instant.parse("2019-11-29T21:00:00.00Z")); // same last day of
    }

    @Test
    public void onConfirmation_promoMoney() {
        var enablePromoFee = false;
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-26", 3000.0);
        List<FinancialEvent> events = providerWithClockFixed("2019-11-12T13:15:30.00Z").onConfirmation(orderItem, enablePromoFee);
        assertThat(events.size()).isEqualTo(1);
        var event = events.get(0);
        assertThat(event.getType()).isEqualTo(PAYMENT);
        assertThat(event.getBillingClientId()).isEqualTo(FINANCIAL_CLIENT_ID);
        assertThat(event.getPartnerAmount()).isEqualTo(Money.of(5_600, "RUB"));
        assertThat(event.getFeeAmount()).isEqualTo(Money.of(1_400, "RUB"));
        assertThat(event.getPromoCodePartnerAmount()).isEqualTo(Money.of(3_000, "RUB"));
        assertThat(event.getPromoCodeFeeAmount()).isNull();
        assertThat(event.getPartnerRefundAmount()).isNull();
        assertThat(event.getFeeRefundAmount()).isNull();
        assertThat(event.getPromoCodePartnerRefundAmount()).isNull();
        assertThat(event.getPromoCodeFeeRefundAmount()).isNull();
    }

    @Test
    public void onConfirmation_promoMoney100Percent() {
        var enablePromoFee = true;
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-26", 9_999.0);
        List<FinancialEvent> events = providerWithClockFixed("2019-11-12T13:15:30.00Z").onConfirmation(orderItem, enablePromoFee);
        assertThat(events.size()).isEqualTo(1);
        var event = events.get(0);
        assertThat(event.getType()).isEqualTo(PAYMENT);
        assertThat(event.getBillingClientId()).isEqualTo(FINANCIAL_CLIENT_ID);
        assertThat(event.getPartnerAmount()).isEqualTo(Money.of(0, "RUB"));
        assertThat(event.getFeeAmount()).isEqualTo(Money.of(1, "RUB"));
        assertThat(event.getPromoCodePartnerAmount()).isEqualTo(Money.of(8_600, "RUB"));
        assertThat(event.getPromoCodeFeeAmount()).isEqualTo(Money.of(1_399, "RUB"));
        assertThat(event.getPartnerRefundAmount()).isNull();
        assertThat(event.getFeeRefundAmount()).isNull();
        assertThat(event.getPromoCodePartnerRefundAmount()).isNull();
        assertThat(event.getPromoCodeFeeRefundAmount()).isNull();
    }

    @Test
    public void onRefund_promoMoneyWithCorrection() {
        var enablePromoFee = false;
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-26", 7000.0);
        mockFullyPaidEvents(enablePromoFee);
        List<FinancialEvent> events = providerWithClockFixed("2019-11-12T13:15:30.00Z").onRefund(orderItem, enablePromoFee);
        assertThat(events).hasSize(2);

        FinancialEvent refundEvent = events.stream().filter(e -> e.getType() == REFUND).findAny().orElse(null);
        assertThat(refundEvent).isNotNull();
        assertThat(refundEvent.getType()).isEqualTo(REFUND);
        assertThat(refundEvent.getBillingClientId()).isEqualTo(FINANCIAL_CLIENT_ID);
        assertThat(refundEvent.getPartnerAmount()).isNull();
        assertThat(refundEvent.getFeeAmount()).isNull();
        assertThat(refundEvent.getPromoCodePartnerAmount()).isNull();
        assertThat(refundEvent.getPromoCodeFeeAmount()).isNull();
        assertThat(refundEvent.getPartnerRefundAmount()).isEqualTo(Money.of(0, "RUB"));
        assertThat(refundEvent.getFeeRefundAmount()).isEqualTo(Money.of(1_240, "RUB"));
        assertThat(refundEvent.getPromoCodePartnerRefundAmount()).isEqualTo(Money.of(7_000, "RUB"));
        assertThat(refundEvent.getPromoCodeFeeRefundAmount()).isNull();

        FinancialEvent correctionEvent = events.stream().filter(e -> e.getType() == PAYMENT).findAny().orElse(null);
        assertThat(correctionEvent).isNotNull();
        assertThat(correctionEvent.getType()).isEqualTo(PAYMENT);
        assertThat(correctionEvent.getBillingClientId()).isEqualTo(refundEvent.getBillingClientId());
        assertThat(correctionEvent.getPayoutAt()).isEqualTo(refundEvent.getPayoutAt());
        assertThat(correctionEvent.getAccountingActAt()).isEqualTo(refundEvent.getAccountingActAt());
        assertThat(correctionEvent.getPartnerAmount()).isEqualTo(Money.of(240, "RUB"));
        assertThat(correctionEvent.getFeeAmount()).isEqualTo(Money.of(0, "RUB"));
        assertThat(correctionEvent.getPromoCodePartnerAmount()).isNull();
        assertThat(correctionEvent.getPromoCodeFeeAmount()).isNull();
        assertThat(correctionEvent.getPartnerRefundAmount()).isNull();
        assertThat(correctionEvent.getFeeRefundAmount()).isNull();
        assertThat(correctionEvent.getPromoCodePartnerRefundAmount()).isNull();
        assertThat(correctionEvent.getPromoCodeFeeRefundAmount()).isNull();
    }

    @Test
    public void onRefund_promoMoneyPenalty() {
        var enablePromoFee = false;
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-26", 7000, 5000);
        mockFullyPaidEvents(enablePromoFee);
        List<FinancialEvent> events = providerWithClockFixed("2019-11-12T13:15:30.00Z").onRefund(orderItem, enablePromoFee);
        assertThat(events).hasSize(2);

        FinancialEvent refundEvent = events.stream().filter(e -> e.getType() == REFUND).findAny().orElse(null);
        assertThat(refundEvent).isNotNull();
        assertThat(refundEvent.getFeeRefundAmount()).isEqualTo(rub(1_000));
        assertThat(refundEvent.getPromoCodePartnerRefundAmount()).isEqualTo(rub(5_000));
        assertThat(refundEvent.getTotalAmount()).isEqualTo(rub(6_000));
        refundEvent.ensureNoNegativeValues();

        FinancialEvent correctionEvent = events.stream().filter(e -> e.getType() == PAYMENT).findAny().orElse(null);
        assertThat(correctionEvent).isNotNull();
        assertThat(correctionEvent.getPartnerAmount()).isEqualTo(rub(1_000));
        assertThat(correctionEvent.getTotalAmount()).isEqualTo(rub(1_000));
        correctionEvent.ensureNoNegativeValues();
    }

    @Test
    public void onConfirmationWhenOrderHasDeferredPayment() {
        var enablePromoFee = false;
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-27", 1000.0);
        var paymentMoment = CURRENT_MOMENT.plus(1, ChronoUnit.DAYS); //2019-11-23T13:15:30.00Z

        orderItem.getOrder().setPaymentSchedule(PaymentSchedule.builder()
                .state(EPaymentState.PS_PARTIALLY_PAID).build());
        DirectHotelBillingFinancialDataProvider provider = providerWithClockFixed("2019-11-12T13:15:30.00Z");
        List<FinancialEvent> events = provider.onConfirmation(orderItem, enablePromoFee);
        assertThat(events).hasSize(0);  // no events of confirmation - order is not fully paid yet
        PaymentSchedule schedule = getFullyPaidSchedule(paymentMoment);

        events = provider.onPaymentScheduleFullyPaid(orderItem, schedule, enablePromoFee);
        assertThat(events).hasSize(1);
        var event = events.get(0);
        assertThat(event.getAccrualAt()).isEqualTo(Instant.parse("2019-11-12T13:15:30.00Z"));
        assertThat(event.getPayoutAt()).isEqualTo(mskDtToUtcInstant("2019-11-26T00:00:00"));
        assertThat(event.getAccountingActAt()).isEqualTo(mskDtToUtcInstant("2019-11-27T00:00:00"));
        // @ MSK
        assertThat(event.getTotalAmount()).isEqualTo(Money.of(10_000, "RUB"));
        assertThat(event.getPartnerAmount()).isEqualTo(Money.of(7_600, "RUB"));
        assertThat(event.getPromoCodePartnerAmount()).isEqualTo(Money.of(1_000, "RUB"));
        assertThat(event.getFeeAmount()).isEqualTo(Money.of(1_400, "RUB"));
        assertThat(event.getPartnerRefundAmount()).isNull();
        assertThat(event.getFeeRefundAmount()).isNull();
    }

    private PaymentSchedule getFullyPaidSchedule(Instant paymentMoment) {
        return PaymentSchedule.builder()
                .state(EPaymentState.PS_FULLY_PAID)
                .items(List.of(PaymentScheduleItem.builder()
                        .pendingInvoice(PendingInvoice.builder()
                                .state(EPaymentState.PS_FULLY_PAID)
                                .closedAt(paymentMoment)
                                .build())
                        .build()))
                .build();
    }

    @Test
    public void onRefundWhenOrderHadDeferredPaymentAndWasNotPaid() {
        var enablePromoFee = false;
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-27", 1000.0);
        orderItem.getOrder().setPaymentSchedule(PaymentSchedule.builder()
                .state(EPaymentState.PS_PARTIALLY_PAID).build());
        DirectHotelBillingFinancialDataProvider provider = providerWithClockFixed("2019-11-12T13:15:30.00Z");
        var events = provider.onRefund(orderItem, enablePromoFee);
        assertThat(events).hasSize(1);
        assertThat(events.get(0))
                .hasFieldOrPropertyWithValue("type", PAYMENT)  // payment of penalty
                .hasFieldOrPropertyWithValue("totalAmount", Money.of(2000, "RUB"))
                .hasFieldOrPropertyWithValue("promoCodePartnerAmount", null)  // promo code was not used
                .hasFieldOrPropertyWithValue("feeAmount", Money.of(160, "RUB")) // refund rate is 8%, so 2000* 0.08 = 160
                .hasFieldOrPropertyWithValue("partnerAmount", Money.of(1840, "RUB"))
                .hasFieldOrPropertyWithValue("payoutAt", mskDtToUtcInstant("2019-11-25T00:00:00")); // reservation moment + 3 days

    }

    @Test
    public void onRefundWhenOrderHadDeferredPaymentAndWasFullyPaid() {
        var enablePromoFee = false;
        List<FinancialEvent> eventRepo = new ArrayList<>();
        mockEventsWithList(eventRepo);
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-27", 1000.0);
        orderItem.getOrder().setPaymentSchedule(PaymentSchedule.builder().state(EPaymentState.PS_PARTIALLY_PAID).build());
        DirectHotelBillingFinancialDataProvider provider = providerWithClockFixed("2019-11-12T13:15:30.00Z");
        var paymentEvents = provider.onPaymentScheduleFullyPaid(orderItem, getFullyPaidSchedule(CURRENT_MOMENT), enablePromoFee);
        assertThat(paymentEvents).hasSize(1);
        eventRepo.addAll(paymentEvents);
        ServiceBalance balance = new ServiceBalance(eventRepo, Monetary.getCurrency("RUB"));
        assertThat(balance.getOverallBalance())
                .hasFieldOrPropertyWithValue("totalPartner", Money.of(8600, "RUB"))
                .hasFieldOrPropertyWithValue("totalFee", Money.of(1400, "RUB"))
                .hasFieldOrPropertyWithValue("totalPromo", Money.of(1000, "RUB"));

        var refundEvents = provider.onRefund(orderItem, enablePromoFee);
        assertThat(refundEvents).hasSize(1);
        assertThat(refundEvents.get(0))
                .hasFieldOrPropertyWithValue("type", REFUND)  // refund of all the sum minus penalty
                .hasFieldOrPropertyWithValue("totalAmount", Money.of(8000, "RUB"))
                .hasFieldOrPropertyWithValue("payoutAt", mskDtToUtcInstant("2019-11-25T00:00:00")); // reservation moment + 3 days
        eventRepo.addAll(refundEvents);
        balance = new ServiceBalance(eventRepo, Monetary.getCurrency("RUB"));
        assertThat(balance.getOverallBalance())
                .hasFieldOrPropertyWithValue("totalPartner", Money.of(1840, "RUB"))
                .hasFieldOrPropertyWithValue("totalFee", Money.of(160, "RUB"))
                .hasFieldOrPropertyWithValue("totalPromo", Money.of(0, "RUB"));
    }

    @Test
    public void onFullyPaidThanExtraPaymentThanRefund() {
        var enablePromoFee = false;
        List<FinancialEvent> eventRepo = new ArrayList<>();
        mockEventsWithList(eventRepo);
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-27", 1000.0);
        DirectHotelBillingFinancialDataProvider provider = providerWithClockFixed("2019-11-12T13:15:30.00Z");
        List<FinancialEvent> confirmationEvents = provider.onConfirmation(orderItem, enablePromoFee);
        eventRepo.addAll(confirmationEvents);
        ServiceBalance balance = new ServiceBalance(eventRepo, Monetary.getCurrency("RUB"));
        assertThat(balance.getOverallBalance())
                .hasFieldOrPropertyWithValue("totalPartner", Money.of(8600, "RUB"))
                .hasFieldOrPropertyWithValue("totalFee", Money.of(1400, "RUB"))
                .hasFieldOrPropertyWithValue("totalPromo", Money.of(1000, "RUB"));
        PendingInvoice extraInvoice = PendingInvoice.builder()
                .order(orderItem.getOrder())
                .pendingInvoiceItems(List.of(PendingInvoiceItem.builder()
                        .price(Money.of(800, "RUB"))
                        .build()))
                .state(EPaymentState.PS_FULLY_PAID)
                .closedAt(CURRENT_MOMENT.plus(1, ChronoUnit.DAYS))
                .build();
        orderItem.addFiscalItem(FiscalItem.builder()
                .moneyAmount(extraInvoice.getTotalAmount())
                .build());
        orderItem.getHotelItinerary().addExtra(extraInvoice.getTotalAmount());
        List<FinancialEvent> extraEvents = provider.onExtraPayment(orderItem, extraInvoice, enablePromoFee);
        assertThat(extraEvents).hasSize(1);
        eventRepo.addAll(extraEvents);
        balance = new ServiceBalance(eventRepo, Monetary.getCurrency("RUB"));
        assertThat(balance.getOverallBalance())
                .hasFieldOrPropertyWithValue("total", Money.of(10800, "RUB"))
                .hasFieldOrPropertyWithValue("totalPartner", Money.of(9288, "RUB"))
                .hasFieldOrPropertyWithValue("totalFee", Money.of(1512, "RUB"))
                .hasFieldOrPropertyWithValue("totalPromo", Money.of(1000, "RUB"));
        List<FinancialEvent> refundEvents = provider.onRefund(orderItem, enablePromoFee);
        assertThat(refundEvents).hasSize(1);
        eventRepo.addAll(refundEvents);
        balance = new ServiceBalance(eventRepo, Monetary.getCurrency("RUB"));
        assertThat(balance.getOverallBalance())
                .hasFieldOrPropertyWithValue("total", Money.of(2000, "RUB"))
                .hasFieldOrPropertyWithValue("totalPartner", Money.of(1840, "RUB"))
                .hasFieldOrPropertyWithValue("totalFee", Money.of(160, "RUB"))
                .hasFieldOrPropertyWithValue("totalPromo", Money.of(0, "RUB"));
    }

    @Test
    public void onDeferredPaymentThanFullyPaidThanExtraThanRefund() {
        var enablePromoFee = false;
        List<FinancialEvent> eventRepo = new ArrayList<>();
        mockEventsWithList(eventRepo);
        TravellineOrderItem orderItem = createOrderItem(CURRENT_MOMENT.toString(), "2019-11-27", 1000.0);
        PaymentSchedule paymentSchedule = PaymentSchedule.builder()
                .state(EPaymentState.PS_PARTIALLY_PAID)
                .build();
        orderItem.getOrder().setPaymentSchedule(paymentSchedule);
        DirectHotelBillingFinancialDataProvider provider = providerWithClockFixed("2019-11-12T13:15:30.00Z");
        List<FinancialEvent> confirmationEvents = provider.onConfirmation(orderItem, false);
        assertThat(confirmationEvents).hasSize(0);

        List<FinancialEvent> fullyPaidEvents = provider.onPaymentScheduleFullyPaid(orderItem,
                getFullyPaidSchedule(CURRENT_MOMENT.plus(2, ChronoUnit.DAYS)), false);
        paymentSchedule.setState(EPaymentState.PS_FULLY_PAID);

        assertThat(fullyPaidEvents).hasSize(1);
        eventRepo.addAll(fullyPaidEvents);

        ServiceBalance balance = new ServiceBalance(eventRepo, Monetary.getCurrency("RUB"));
        assertThat(balance.getOverallBalance())
                .hasFieldOrPropertyWithValue("total", Money.of(10000, "RUB"))
                .hasFieldOrPropertyWithValue("totalPartner", Money.of(8600, "RUB"))
                .hasFieldOrPropertyWithValue("totalFee", Money.of(1400, "RUB"))
                .hasFieldOrPropertyWithValue("totalPromo", Money.of(1000, "RUB"));

        PendingInvoice extraInvoice = PendingInvoice.builder()
                .order(orderItem.getOrder())
                .pendingInvoiceItems(List.of(PendingInvoiceItem.builder()
                        .price(Money.of(800, "RUB"))
                        .build()))
                .state(EPaymentState.PS_FULLY_PAID)
                .closedAt(CURRENT_MOMENT.plus(1, ChronoUnit.DAYS))
                .build();
        orderItem.addFiscalItem(FiscalItem.builder()
                .moneyAmount(extraInvoice.getTotalAmount())
                .build());
        orderItem.getHotelItinerary().addExtra(extraInvoice.getTotalAmount());
        List<FinancialEvent> extraEvents = provider.onExtraPayment(orderItem, extraInvoice, enablePromoFee);
        assertThat(extraEvents).hasSize(1);
        eventRepo.addAll(extraEvents);
        balance = new ServiceBalance(eventRepo, Monetary.getCurrency("RUB"));
        assertThat(balance.getOverallBalance())
                .hasFieldOrPropertyWithValue("total", Money.of(10800, "RUB"))
                .hasFieldOrPropertyWithValue("totalPartner", Money.of(9288, "RUB"))
                .hasFieldOrPropertyWithValue("totalFee", Money.of(1512, "RUB"))
                .hasFieldOrPropertyWithValue("totalPromo", Money.of(1000, "RUB"));

        List<FinancialEvent> refundEvents = provider.onRefund(orderItem, enablePromoFee);
        assertThat(refundEvents).hasSize(1);
        eventRepo.addAll(refundEvents);
        balance = new ServiceBalance(eventRepo, Monetary.getCurrency("RUB"));
        assertThat(balance.getOverallBalance())
                .hasFieldOrPropertyWithValue("total", Money.of(2000, "RUB"))
                .hasFieldOrPropertyWithValue("totalPartner", Money.of(1840, "RUB"))
                .hasFieldOrPropertyWithValue("totalFee", Money.of(160, "RUB"))
                .hasFieldOrPropertyWithValue("totalPromo", Money.of(0, "RUB"));
    }

    private Instant mskDtToUtcInstant(String mskDt) {
        return LocalDateTime.parse(mskDt).atZone(ZoneId.of("Europe/Moscow")).toInstant();
    }

}
