package ru.yandex.market.payment.YtDao;

import org.junit.jupiter.api.Test;
import ru.yandex.market.core.order.payment.PaymentOrderCurrency;
import ru.yandex.market.core.order.payment.PaymentOrderFactoring;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.ProductType;
import ru.yandex.market.core.order.payment.TransactionType;
import ru.yandex.market.payment.model.PaymentOrder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link PaymentOrderYtDao}
 */
public class PaymentOrderYtDaoTest {

    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 8, 30);

    private static final PaymentOrder PAYMENT_ORDERS = PaymentOrder.builder()
            .setId(1L)
            .setClientId(2L)
            .setContractId(3L)
            .setServiceId(610L)
            .setTransactionType(TransactionType.PAYMENT)
            .setSecuredPayment(false)
            .setFactoring(PaymentOrderFactoring.MARKET)
            .setProductType(ProductType.PARTNER_PAYMENT)
            .setPaysysTypeCc(PaysysTypeCc.PARTNER_PAYMENT)
            .setTrantime(IMPORT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
            .setAmount(2000L)
            .setCurrency(PaymentOrderCurrency.RUB)
            .setExportedToTlog(false)
            .build();


    @Test
    void test() throws SQLException {
        ResultSet rs = prepareResultSet();
        PaymentOrder paymentOrder = PaymentOrderYtDao.ROW_MAPPER.mapRow(rs, 1);
        assertEquals(PAYMENT_ORDERS.getId(), paymentOrder.getId());
        assertEquals(PAYMENT_ORDERS.getClientId(), paymentOrder.getClientId());
        assertEquals(PAYMENT_ORDERS.getContractId(), paymentOrder.getContractId());
        assertEquals(PAYMENT_ORDERS.getServiceId(), paymentOrder.getServiceId());
        assertEquals(PAYMENT_ORDERS.getTransactionType(), paymentOrder.getTransactionType());
        assertEquals(PAYMENT_ORDERS.getSecuredPayment(), paymentOrder.getSecuredPayment());
        assertEquals(PAYMENT_ORDERS.getFactoring(), paymentOrder.getFactoring());
        assertEquals(PAYMENT_ORDERS.getProductType(), paymentOrder.getProductType());
        assertEquals(PAYMENT_ORDERS.getPaysysTypeCc(), paymentOrder.getPaysysTypeCc());
        assertEquals(PAYMENT_ORDERS.getTrantime(), paymentOrder.getTrantime());
        assertEquals(PAYMENT_ORDERS.getAmount(), paymentOrder.getAmount());
        assertEquals(PAYMENT_ORDERS.getCurrency(), paymentOrder.getCurrency());
        assertEquals(PAYMENT_ORDERS.getExportedToTlog(), paymentOrder.getExportedToTlog());
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getLong("client_id")).thenReturn(2L);
        when(rs.getLong("contract_id")).thenReturn(3L);
        when(rs.getLong("service_id")).thenReturn(610L);
        when(rs.getString("transaction_type")).thenReturn("payment");
        when(rs.getBoolean("secured_payment")).thenReturn(false);
        when(rs.getString("factoring")).thenReturn("market");
        when(rs.getString("product")).thenReturn("partner_payment");
        when(rs.getString("paysys_type_cc")).thenReturn("partner_payment");
        when(rs.getString("trantime")).thenReturn("2021-08-30 00:00:00.000000");
        when(rs.getLong("amount")).thenReturn(2000L);
        when(rs.getString("currency")).thenReturn("RUB");
        when(rs.getBoolean("exported_to_tlog")).thenReturn(false);
        return rs;
    }

}
