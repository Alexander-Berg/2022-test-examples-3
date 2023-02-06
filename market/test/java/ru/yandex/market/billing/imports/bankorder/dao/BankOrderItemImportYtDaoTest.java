package ru.yandex.market.billing.imports.bankorder.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.imports.bankorder.model.BankOrderItem;
import ru.yandex.market.core.currency.Currency;
import ru.yandex.market.core.payment.TransactionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для {@link BankOrderItemImportYtDao}
 */
class BankOrderItemImportYtDaoTest extends FunctionalTest {

    @Autowired
    BankOrderItemImportYtDao bankOrderItemImportYtDao;

    private static final BankOrderItem BANK_ORDER_ITEM = getBankOrderItem("123");

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

    private static BankOrderItem getBankOrderItem(String paymentBatchId) {
        return BankOrderItem.builder()
                .setPaymentBatchId(paymentBatchId)
                .setTransactionType(TransactionType.PAYMENT)
                .setTrustId("-1")
                .setServiceOrderId("payment_order-12345")
                .setOrderId(null)
                .setOrderItemId(null)
                .setReturnId(null)
                .setPaymentOrderId(12345L)
                .setSum(9000L)
                .setCurrency(Currency.RUR)
                .setPaymentType("partner_payment")
                .setHandlingTime(
                        LocalDateTime.of(2020, 11, 10, 10, 30, 0)
                                .atZone(ZoneId.systemDefault()).toInstant())
                .setPaymentTime(
                        LocalDateTime.of(2020, 11, 10, 10, 30, 0)
                                .atZone(ZoneId.systemDefault()).toInstant())
                .setContractId(123L)
                .setPartnerId(234L)
                .setAgencyCommission(0)
                .build();
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getString("PAYMENT_BATCH_ID")).thenReturn("123");
        Mockito.when(resultSet.getString("SERVICE_ORDER_ID")).thenReturn("payment_order-12345");
        Mockito.when(resultSet.getString("TRANSACTION_TYPE")).thenReturn("payment");
        Mockito.when(resultSet.getDouble("SUM")).thenReturn(9000d);
        Mockito.when(resultSet.getLong("CONTRACT_ID")).thenReturn(123L);
        Mockito.when(resultSet.getString("CURRENCY")).thenReturn("RUR");
        Mockito.when(resultSet.getString("PAYMENT_TYPE")).thenReturn("partner_payment");
        Mockito.when(resultSet.getString("HANDLING_TIME")).thenReturn("2020-11-10 10:30:00");
        Mockito.when(resultSet.getString("PAYMENT_TIME")).thenReturn("2020-11-10 10:30:00");
        Mockito.when(resultSet.getDouble("AGENCY_COMMISSION")).thenReturn(0d);
        return resultSet;
    }

}
