package ru.yandex.market.ocrm.module.payment;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.pay.PagedPayments;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubstatus;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.crm.util.Dates;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.ocrm.module.order.ModuleOrderTestConfiguration;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration(classes = PaymentListTest.TestConfiguration.class)
public class PaymentListTest {

    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private PaymentHistorySource paymentHistorySource;
    @Inject
    private TrustPaymentSource trustPaymentSource;

    @Test
    public void simple() {
        Order order = orderTestUtils.createOrder();

        final String balanceId = "balanceId123";

        final ru.yandex.market.checkout.checkouter.pay.Payment samplePayment = getSamplePayment(balanceId);
        setupGetPaymentHistory(order.getOrderId(), samplePayment);
        final TrustPayment sampleTrustPayment = getSampleTrustPayment();
        setupGetTrustPayment(samplePayment.getId(), sampleTrustPayment);

        List<Payment> payments = getPayments(order.getOrderId());

        Assertions.assertEquals(1, payments.size());
        final Payment actualPayment = payments.get(0);

        assertPayment(actualPayment, samplePayment);

        Assertions.assertEquals(actualPayment.getBalanceId(), balanceId);

        Assertions.assertEquals(actualPayment.getRrn(), sampleTrustPayment.getRrn());
        Assertions.assertNotNull(sampleTrustPayment.getRrn());
    }

    private void assertPayment(Payment payment,
                               ru.yandex.market.checkout.checkouter.pay.Payment samplePayment) {
        Assertions.assertEquals(payment.getTotalAmount(), samplePayment.getTotalAmount());
        Assertions.assertNotNull(samplePayment.getTotalAmount());

        Assertions.assertEquals(payment.getCurrency().getCode(), samplePayment.getCurrency().name());

        Assertions.assertEquals(payment.getStatus().getCode(), samplePayment.getStatus().name());

        Assertions.assertEquals(payment.getSubStatus().getCode(), samplePayment.getSubstatus().name());

        Assertions.assertEquals(payment.getFailReason(), samplePayment.getFailReason());
        Assertions.assertNotNull(samplePayment.getFailReason());

        Assertions.assertEquals(payment.getFailDescription(), samplePayment.getFailDescription());
        Assertions.assertNotNull(samplePayment.getFailDescription());

        final OffsetDateTime expectedDateTime = OffsetDateTime.ofInstant(samplePayment.getCreationDate().toInstant(),
                Dates.MOSCOW_ZONE);
        Assertions.assertEquals(payment.getCreationDate(), expectedDateTime);
    }

    private TrustPayment getSampleTrustPayment() {
        final TrustPayment payment = new TrustPayment();
        payment.setRrn("rrn123");
        return payment;
    }

    private void setupGetTrustPayment(Long paymentId,
                                      TrustPayment trustPayment) {
        Mockito.when(trustPaymentSource.getPayment(Mockito.eq(paymentId), Mockito.eq(false)))
                .thenReturn(Optional.of(trustPayment));
    }

    private ru.yandex.market.checkout.checkouter.pay.Payment getSamplePayment(String balanceId) {
        final ru.yandex.market.checkout.checkouter.pay.Payment payment =
                new ru.yandex.market.checkout.checkouter.pay.Payment();
        payment.setId(112345L);
        payment.setTotalAmount(BigDecimal.valueOf(234));
        payment.setCurrency(Currency.RUR);
        payment.setStatus(PaymentStatus.CLEARED);
        payment.setSubstatus(PaymentSubstatus.HOLD_FAILED);
        payment.setCreationDate(
                Date.from(LocalDateTime.of(2020, 1, 21,
                        12, 1, 2, 0)
                        .toInstant(ZoneOffset.UTC)));
        payment.setFailReason("failReason123");
        payment.setBasketId(balanceId);
        payment.setFake(false);
        payment.setFailDescription("failDescription456");
        return payment;
    }

    public void setupGetPaymentHistory(Long orderId,
                                       ru.yandex.market.checkout.checkouter.pay.Payment payment) {
        final PagedPayments result = new PagedPayments();

        final Pager pager = new Pager();
        pager.setPagesCount(1);
        result.setPager(pager);

        result.setItems(List.of(payment));

        Mockito.when(paymentHistorySource.getPayments(
                Mockito.eq(orderId),
                Mockito.eq(false),
                Mockito.any(),
                Mockito.anyInt(),
                Mockito.anyInt()
        )).thenReturn(result);
    }

    private List<Payment> getPayments(Object order) {
        Query query = Query.of(Payment.FQN)
                .withFilters(Filters.eq(Payment.ORDERS, order));
        return entityStorageService.list(query);
    }

    @Import({ModulePaymentConfiguration.class, ModuleOrderTestConfiguration.class})
    public static class TestConfiguration {

        @Bean
        public PaymentHistorySource paymentHistorySource() {
            return Mockito.mock(PaymentHistorySource.class);
        }

        @Bean
        @Primary
        public TrustPaymentSource paymentSource() {
            return Mockito.mock(TrustPaymentSource.class);
        }

    }

}
