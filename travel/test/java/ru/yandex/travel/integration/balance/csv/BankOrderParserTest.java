package ru.yandex.travel.integration.balance.csv;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.junit.Test;

import ru.yandex.travel.integration.balance.model.csv.BankOrder;
import ru.yandex.travel.integration.balance.model.csv.BankOrderStatus;
import ru.yandex.travel.integration.balance.model.csv.OebsPaymentStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class BankOrderParserTest {

    @Test
    public void testParse() throws Exception {
        final BankOrderParser parser = new BankOrderParser(100500);

        final List<BankOrder> orders = parser.parse(getResourceAsStream("csv/get_payment_headers.csv"));

        assertThat(orders).hasSize(3);

        final BankOrder order = orders.get(0);

        assertThat(LocalDateTime.of(2020, Month.MAY, 21, 10, 47, 4)).isEqualTo(order.getTrantime());
        assertThat(LocalDate.of(2020, Month.MAY, 20)).isEqualTo(order.getEventtime());
        assertThat(BankOrderStatus.DONE).isEqualTo(order.getStatus());
        assertThat("18149").isEqualTo(order.getBankOrderId());
        assertThat("95973775").isEqualTo(order.getPaymentBatchId());
        assertThat(BigDecimal.valueOf(1980, 0)).isEqualTo(order.getSum());
        assertThat(OebsPaymentStatus.RECONCILED).isEqualTo(order.getOebsPaymentStatus());
        assertThat(order.getDescription()).isNotBlank();
    }

    private InputStream getResourceAsStream(String path) {
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }
}
