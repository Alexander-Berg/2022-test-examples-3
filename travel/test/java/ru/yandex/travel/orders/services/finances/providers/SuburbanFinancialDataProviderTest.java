package ru.yandex.travel.orders.services.finances.providers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.Invoice;
import ru.yandex.travel.orders.entities.SuburbanOrderItem;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.FinancialEventPaymentScheme;
import ru.yandex.travel.orders.entities.finances.FinancialEventType;
import ru.yandex.travel.orders.entities.partners.SuburbanBillingPartnerAgreement;
import ru.yandex.travel.orders.factories.SuburbanOrderItemEnvProviderFactory;
import ru.yandex.travel.orders.services.suburban.environment.SuburbanOrderItemEnvProvider;
import ru.yandex.travel.orders.workflows.orderitem.suburban.SuburbanProperties;
import ru.yandex.travel.suburban.model.SuburbanReservation;
import ru.yandex.travel.suburban.partners.SuburbanCarrier;
import ru.yandex.travel.suburban.partners.SuburbanProvider;
import ru.yandex.travel.utils.ClockService;

import static org.assertj.core.api.Assertions.assertThat;


public class SuburbanFinancialDataProviderTest {
    @Test
    public void onConfirmation() {
        var enablePromoFee = false;
        Instant now = Instant.parse("2021-03-01T13:15:30.00Z");
        SuburbanOrderItem orderItem = createOrderItem(now, SuburbanProvider.MOVISTA, SuburbanCarrier.CPPK);
        SuburbanFinancialDataProvider provider = createFinancialProvider(now.plusSeconds(3));
        List<FinancialEvent> events = provider.onConfirmation(orderItem, enablePromoFee);

        assertThat(events.size()).isEqualTo(1);
        FinancialEvent event = events.get(0);

        FinancialEvent expectedEvent = FinancialEvent.builder()
                .orderItem(orderItem)
                .order(orderItem.getOrder())
                .orderPrettyId("pretty")
                .paymentScheme(FinancialEventPaymentScheme.SUBURBAN)
                .type(FinancialEventType.PAYMENT)
                .billingClientId(4242L)
                .accrualAt(Instant.parse("2021-03-01T13:15:33.00Z"))
                .payoutAt(now)
                .accountingActAt(now)
                .partnerAmount(Money.of(100.2, "RUB"))
                .feeAmount(Money.of(2.04, "RUB"))
                .build();

        assertThat(event).isEqualTo(expectedEvent);
    }

    @Test
    public void onRefund() {
        var enablePromoFee = false;
        Instant now = Instant.parse("2021-03-01T13:15:30.00Z");
        SuburbanOrderItem orderItem = createOrderItem(now, SuburbanProvider.MOVISTA, SuburbanCarrier.CPPK);
        SuburbanFinancialDataProvider provider = createFinancialProvider(now.plusSeconds(3));
        List<FinancialEvent> events = provider.onRefund(orderItem, null, enablePromoFee);

        assertThat(events.size()).isEqualTo(1);
        FinancialEvent event = events.get(0);

        FinancialEvent expectedEvent = FinancialEvent.builder()
                .orderItem(orderItem)
                .order(orderItem.getOrder())
                .orderPrettyId("pretty")
                .paymentScheme(FinancialEventPaymentScheme.SUBURBAN)
                .type(FinancialEventType.REFUND)
                .billingClientId(4242L)
                .accrualAt(Instant.parse("2021-03-01T13:15:33.00Z"))
                .payoutAt(now)
                .accountingActAt(now)
                .partnerRefundAmount(Money.of(100.2, "RUB"))
                .feeRefundAmount(Money.of(0.36, "RUB"))
                .build();

        assertThat(event).isEqualTo(expectedEvent);
    }

    private SuburbanFinancialDataProvider createFinancialProvider(Instant now) {

        SuburbanProperties props = SuburbanProperties.builder().providers(
                SuburbanProperties.Providers.builder()
                        .movista(SuburbanProperties.MovistaProps.builder()
                                .common(SuburbanProperties.ProviderProps.builder()
                                        .financialEvents(SuburbanProperties.FinancialEventsProps.builder()
                                                .rewardFee(BigDecimal.valueOf(0.017))
                                                .rewardFeeRefund(BigDecimal.valueOf(0.003))
                                                .vat(BigDecimal.valueOf(0.2)).build())
                                        .build()).build()).build()).build();

        SuburbanOrderItemEnvProvider envProvider = SuburbanOrderItemEnvProviderFactory.createEnvProvider(props);
        Clock clock = Clock.fixed(now, ZoneId.of("UTC"));

        return new SuburbanFinancialDataProvider(ClockService.create(clock), envProvider);
    }

    private SuburbanOrderItem createOrderItem(Instant now, SuburbanProvider provider, SuburbanCarrier carrier) {
        var order = new GenericOrder();
        order.setId(UUID.randomUUID());
        order.setPrettyId("pretty");

        Invoice invoice = new TrustInvoice();
        invoice.setTrustPaymentId("trust_payment_id");
        order.addInvoice(invoice);

        var orderItem = new SuburbanOrderItem();
        orderItem.setId(UUID.randomUUID());

        Money price = Money.of(100.2, ProtoCurrencyUnit.RUB);
        var payload = SuburbanReservation.builder()
                .price(price)
                .provider(provider)
                .stationFrom(SuburbanReservation.Station.builder().build())
                .stationTo(SuburbanReservation.Station.builder().build())
                .carrier(carrier)
                .build();
        orderItem.setReservation(payload);
        orderItem.setBillingPartnerAgreement(new SuburbanBillingPartnerAgreement(4242L));

        orderItem.setConfirmedAt(now);
        order.addOrderItem(orderItem);
        return orderItem;
    }
}
