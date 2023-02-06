package ru.yandex.market.cpa;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.core.currency.Currency;
import ru.yandex.market.core.order.payment.BankOrderItem;
import ru.yandex.market.core.order.payment.TransactionType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тест для {@link BankOrderItemImportYtDao}
 */
class BankOrderItemImportYtDaoTest extends FunctionalTest {

    @Autowired BankOrderItemImportYtDao bankOrderItemImportYtDao;

    private final BankOrderItem BANK_ORDER_ITEM = getBankOrderItem("123");

    @Test
    void testMapRow() throws SQLException {
        ResultSet rs = prepareResultSet();
        HashMap<Long, Long> contractIdToPartnerId = new HashMap<>();
        contractIdToPartnerId.put(123L, 234L);
        BankOrderItem bankOrderItem = BankOrderItemImportYtDao.mapRow(rs, contractIdToPartnerId);
        assertEquals(BANK_ORDER_ITEM.getPaymentBatchId(), bankOrderItem.getPaymentBatchId());
        assertEquals(BANK_ORDER_ITEM.getTransactionType(), bankOrderItem.getTransactionType());
        assertEquals(BANK_ORDER_ITEM.getTrustId(), bankOrderItem.getTrustId());
        assertEquals(BANK_ORDER_ITEM.getServiceOrderId(), bankOrderItem.getServiceOrderId());
        assertEquals(BANK_ORDER_ITEM.getOrderId(), bankOrderItem.getOrderId());
        assertEquals(BANK_ORDER_ITEM.getOrderItemId(), bankOrderItem.getOrderItemId());
        assertEquals(BANK_ORDER_ITEM.getReturnId(), bankOrderItem.getReturnId());
        assertEquals(BANK_ORDER_ITEM.getPaymentOrderId(), bankOrderItem.getPaymentOrderId());
        assertEquals(BANK_ORDER_ITEM.getSum(), bankOrderItem.getSum());
        assertEquals(BANK_ORDER_ITEM.getCurrency(), bankOrderItem.getCurrency());
        assertEquals(BANK_ORDER_ITEM.getPaymentType(), bankOrderItem.getPaymentType());
        assertEquals(BANK_ORDER_ITEM.getHandlingTime(), bankOrderItem.getHandlingTime());
        assertEquals(BANK_ORDER_ITEM.getPaymentTime(), bankOrderItem.getPaymentTime());
        assertEquals(BANK_ORDER_ITEM.getContractId(), bankOrderItem.getContractId());
        assertEquals(BANK_ORDER_ITEM.getPartnerId(), bankOrderItem.getPartnerId());
        assertEquals(BANK_ORDER_ITEM.getAgencyCommission(), bankOrderItem.getAgencyCommission());
    }

    @Test
    public void testNeedAcceptItemsEmpty() {
        ArrayList<BankOrderItem> bankOrderItems = new ArrayList<>();
        assertFalse(bankOrderItemImportYtDao.needAcceptItems(bankOrderItems, BANK_ORDER_ITEM, 5000));
    }

    @Test
    public void testNeedAcceptItemsOneElements() {
        ArrayList<BankOrderItem> bankOrderItems = new ArrayList<>();
        bankOrderItems.add(BANK_ORDER_ITEM);
        assertFalse(bankOrderItemImportYtDao.needAcceptItems(bankOrderItems, BANK_ORDER_ITEM, 5000));
    }

    @Test
    public void testNeedAcceptItemsBatchSizeFalse() {
        ArrayList<BankOrderItem> bankOrderItems = new ArrayList<>();
        bankOrderItems.add(BANK_ORDER_ITEM);
        bankOrderItems.add(BANK_ORDER_ITEM);
        assertFalse(bankOrderItemImportYtDao.needAcceptItems(bankOrderItems, getBankOrderItem("124"), 3));
    }

    @Test
    public void testNeedAcceptItemsBatchSizeTrue() {
        ArrayList<BankOrderItem> bankOrderItems = new ArrayList<>();
        bankOrderItems.add(BANK_ORDER_ITEM);
        bankOrderItems.add(BANK_ORDER_ITEM);
        assertTrue(bankOrderItemImportYtDao.needAcceptItems(bankOrderItems, getBankOrderItem("124"), 2));
    }

    private BankOrderItem getBankOrderItem(String paymentBatchId) {
        return BankOrderItem.builder()
                .setPaymentBatchId(paymentBatchId)
                .setTransactionType(TransactionType.PAYMENT)
                .setTrustId("-1")
                .setServiceOrderId("35624363-item-35624364")
                .setOrderId(35624363L)
                .setOrderItemId(35624364L)
                .setReturnId(null)
                .setPaymentOrderId(null)
                .setSum(9000L)
                .setCurrency(Currency.RUR)
                .setPaymentType("spasibo")
                .setHandlingTime(LocalDateTime.of(2020, 11, 10, 10, 30, 0).atZone(ZoneId.systemDefault()).toInstant())
                .setPaymentTime(LocalDateTime.of(2020, 11, 10, 10, 30, 0).atZone(ZoneId.systemDefault()).toInstant())
                .setContractId(123L)
                .setPartnerId(234L)
                .setAgencyCommission(10)
                .build();
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getString("PAYMENT_BATCH_ID")).thenReturn("123");
        Mockito.when(resultSet.getString("SERVICE_ORDER_ID")).thenReturn("35624363-item-35624364");
        Mockito.when(resultSet.getString("TRANSACTION_TYPE")).thenReturn("payment");
        Mockito.when(resultSet.getDouble("SUM")).thenReturn(9000d);
        Mockito.when(resultSet.getLong("CONTRACT_ID")).thenReturn(123L);
        Mockito.when(resultSet.getString("CURRENCY")).thenReturn("RUR");
        Mockito.when(resultSet.getString("PAYMENT_TYPE")).thenReturn("spasibo");
        Mockito.when(resultSet.getString("HANDLING_TIME")).thenReturn("2020-11-10 10:30:00");
        Mockito.when(resultSet.getString("PAYMENT_TIME")).thenReturn("2020-11-10 10:30:00");
        Mockito.when(resultSet.getDouble("AGENCY_COMMISSION")).thenReturn(10d);
        return resultSet;
    }

}
