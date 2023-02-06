package ru.yandex.market.ocrm.module.payment;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.pay.PagedRefunds;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundSubstatus;
import ru.yandex.market.checkout.checkouter.pay.TrustRefundKey;
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
@ContextConfiguration(classes = RefundListTest.TestConfiguration.class)
public class RefundListTest {

    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private RefundSource refundSource;

    @Test
    public void simple() {
        Order order = orderTestUtils.createOrder();

        final String balanceId = "balanceId123";

        final ru.yandex.market.checkout.checkouter.pay.Refund sampleRefund = getSampleRefund(balanceId);
        setupGetRefund(order.getOrderId(), sampleRefund);

        List<Refund> refunds = getRefunds(order.getOrderId());

        Assertions.assertEquals(1, refunds.size());
        final Refund actualRefund = refunds.get(0);

        assertRefund(actualRefund, sampleRefund);

        Assertions.assertEquals(actualRefund.getBalanceId(), balanceId);
    }

    private void assertRefund(Refund refund,
                              ru.yandex.market.checkout.checkouter.pay.Refund sampleRefund) {

        Assertions.assertEquals(refund.getAmount(), sampleRefund.getAmount());
        Assertions.assertNotNull(sampleRefund.getAmount());

        Assertions.assertEquals(refund.getCurrency().getCode(), sampleRefund.getCurrency().name());

        Assertions.assertEquals(refund.getStatus().getCode(), sampleRefund.getStatus().name());

        Assertions.assertEquals(refund.getSubStatus().getCode(), sampleRefund.getSubstatus().name());

        final OffsetDateTime expectedCreationDate = OffsetDateTime.ofInstant(sampleRefund.getCreationDate().toInstant(),
                Dates.MOSCOW_ZONE);
        Assertions.assertEquals(refund.getCreationDate(), expectedCreationDate);

        final OffsetDateTime expectedUpdateDate = OffsetDateTime.ofInstant(sampleRefund.getUpdateDate().toInstant(),
                Dates.MOSCOW_ZONE);
        Assertions.assertEquals(refund.getUpdateDate(), expectedUpdateDate);

        final OffsetDateTime expectedStatusUpdateDate =
                OffsetDateTime.ofInstant(sampleRefund.getStatusUpdateDate().toInstant(),
                        Dates.MOSCOW_ZONE);
        Assertions.assertEquals(refund.getStatusUpdateDate(), expectedStatusUpdateDate);

        Assertions.assertEquals(refund.getComment(), sampleRefund.getComment());
        Assertions.assertNotNull(sampleRefund.getComment());

        Assertions.assertEquals(refund.getClientRole().getCode(), sampleRefund.getCreatedByRole().name());

        Assertions.assertEquals(refund.getClientUid(), String.valueOf(sampleRefund.getShopManagerId()));
        Assertions.assertNotNull(sampleRefund.getShopManagerId());

    }


    private ru.yandex.market.checkout.checkouter.pay.Refund getSampleRefund(String balanceId) {
        final ru.yandex.market.checkout.checkouter.pay.Refund refund =
                new ru.yandex.market.checkout.checkouter.pay.Refund();
        refund.setId(112345L);
        refund.setAmount(BigDecimal.valueOf(234));
        refund.setCurrency(Currency.RUR);
        refund.setStatus(RefundStatus.ACCEPTED);
        refund.setSubstatus(RefundSubstatus.REFUND_EXPIRED);
        refund.setCreationDate(
                Date.from(LocalDateTime.of(2020, 1, 21,
                        12, 1, 2, 0)
                        .toInstant(ZoneOffset.UTC)));
        refund.setUpdateDate(
                Date.from(LocalDateTime.of(2020, 1, 21,
                        12, 1, 3, 0)
                        .toInstant(ZoneOffset.UTC)));
        refund.setStatusUpdateDate(
                Date.from(LocalDateTime.of(2020, 1, 21,
                        12, 1, 4, 0)
                        .toInstant(ZoneOffset.UTC)));

        refund.setTrustRefundKey(new TrustRefundKey(balanceId));
        refund.setComment("comment123");
        refund.setCreatedByRole(ClientRole.SHOP_USER);
        refund.setShopManagerId(1234567L);

        return refund;
    }

    public void setupGetRefund(Long orderId,
                               ru.yandex.market.checkout.checkouter.pay.Refund refund) {
        final PagedRefunds result = new PagedRefunds();

        final Pager pager = new Pager();
        pager.setPagesCount(1);
        result.setPager(pager);

        result.setItems(List.of(refund));

        Mockito.when(refundSource.getRefunds(
                Mockito.eq(orderId),
                Mockito.eq(false),
                Mockito.anyInt(),
                Mockito.anyInt()
        ))
                .thenReturn(result);
    }

    private List<Refund> getRefunds(Object order) {
        Query query = Query.of(Refund.FQN)
                .withFilters(Filters.eq(Payment.ORDERS, order));
        return entityStorageService.list(query);
    }

    @Import({ModulePaymentConfiguration.class, ModuleOrderTestConfiguration.class})
    public static class TestConfiguration {

        @Bean
        public RefundSource refundSource() {
            return Mockito.mock(RefundSource.class);
        }

    }

}
