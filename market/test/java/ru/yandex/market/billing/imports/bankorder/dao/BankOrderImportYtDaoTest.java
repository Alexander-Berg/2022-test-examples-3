package ru.yandex.market.billing.imports.bankorder.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.billing.imports.bankorder.model.BankOrder;
import ru.yandex.market.billing.imports.bankorder.model.BankOrderStatus;
import ru.yandex.market.billing.order.payment.OebsPaymentStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест для {@link BankOrderImportYtDao}
 */
class BankOrderImportYtDaoTest {

    private static final BankOrder BANK_ORDER = BankOrder.builder()
            .setServiceId(1100L)
            .setPaymentBatchId("1111")
            .setTrantime(LocalDateTime.of(2021, 11, 2, 10, 9, 59).atZone(ZoneId.systemDefault()).toInstant())
            .setEventtime(LocalDate.of(2021, 11, 1))
            .setStatus(BankOrderStatus.DONE)
            .setBankOrderId("666")
            .setSum(1000L)
            .setOebsPaymentStatus(OebsPaymentStatus.RECONCILED)
            .build();

    @Test
    void test() throws SQLException {
        ResultSet rs = prepareResultSet();
        BankOrder bankOrder = BankOrderImportYtDao.mapRow(rs);
        assertEquals(BANK_ORDER.getServiceId(), bankOrder.getServiceId());
        assertEquals(BANK_ORDER.getPaymentBatchId(), bankOrder.getPaymentBatchId());
        assertEquals(BANK_ORDER.getTrantime(), bankOrder.getTrantime());
        assertEquals(BANK_ORDER.getEventtime(), bankOrder.getEventtime());
        assertEquals(BANK_ORDER.getStatus(), bankOrder.getStatus());
        assertEquals(BANK_ORDER.getBankOrderId(), bankOrder.getBankOrderId());
        assertEquals(BANK_ORDER.getSum(), bankOrder.getSum());
        assertEquals(BANK_ORDER.getOebsPaymentStatus(), bankOrder.getOebsPaymentStatus());
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getDouble("SERVICE_ID")).thenReturn(1100d);
        Mockito.when(resultSet.getLong("PAYMENT_BATCH_ID")).thenReturn(1111L);
        Mockito.when(resultSet.getString("TRANTIME")).thenReturn("2021-11-02 10:09:59");
        Mockito.when(resultSet.getString("EVENTTIME")).thenReturn("2021-11-01 00:00:00");
        Mockito.when(resultSet.getString("STATUS")).thenReturn("done");
        Mockito.when(resultSet.getString("BANK_ORDER_ID")).thenReturn("666");
        Mockito.when(resultSet.getDouble("SUM")).thenReturn(1000d);
        Mockito.when(resultSet.getString("OEBS_STATUS")).thenReturn("reconciled");
        return resultSet;
    }

}
