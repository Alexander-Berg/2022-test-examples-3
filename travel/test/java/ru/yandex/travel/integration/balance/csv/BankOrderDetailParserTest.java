package ru.yandex.travel.integration.balance.csv;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.Test;

import ru.yandex.travel.integration.balance.model.csv.BankOrderDetail;
import ru.yandex.travel.integration.balance.model.csv.BillingCurrency;
import ru.yandex.travel.integration.balance.model.csv.PaymentType;
import ru.yandex.travel.integration.balance.model.csv.TransactionType;

import static org.assertj.core.api.Assertions.assertThat;

public class BankOrderDetailParserTest {

    @Test
    public void testParse() throws Exception {
        final BankOrderDetailParser parser = new BankOrderDetailParser("100500");

        final List<BankOrderDetail> details = parser.parse(getResourceAsStream("csv/get_payment_batch_details.csv"));

        assertThat(details).hasSize(2);

        final BankOrderDetail orderDetail = details.get(0);

        assertThat("100500").isEqualTo(orderDetail.getPaymentBatchId());
        assertThat(orderDetail.getTrustRefundId()).isNull();
        assertThat("00000000000000000000000000002998").isEqualTo(orderDetail.getTrustPaymentId());
        assertThat(orderDetail.getServiceOrderId()).isNull();
        assertThat(BigDecimal.valueOf(1593.15)).isEqualTo(orderDetail.getSum());
        assertThat(BillingCurrency.RUR).isEqualTo(orderDetail.getCurrency());
        assertThat(PaymentType.COST).isEqualTo(orderDetail.getPaymentType());
        assertThat(TransactionType.PAYMENT).isEqualTo(orderDetail.getTransactionType());
        assertThat(LocalDate.of(2020, Month.JUNE, 1)).isEqualTo(orderDetail.getHandlingTime());
        assertThat(LocalDate.of(2020, Month.JUNE, 1)).isEqualTo(orderDetail.getPaymentTime());
        assertThat(1636737L).isEqualTo(orderDetail.getContractId());
        assertThat(BigDecimal.valueOf(0)).isEqualTo(orderDetail.getAgencyCommission());
    }

    private InputStream getResourceAsStream(String path) {
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }

}
