package ru.yandex.market.accrual;

import org.junit.jupiter.api.Test;
import ru.yandex.market.core.oebs.OperatingUnit;
import ru.yandex.market.core.order.payment.AccrualProductType;
import ru.yandex.market.core.order.payment.EntityType;
import ru.yandex.market.core.order.payment.PaymentOrderCurrency;
import ru.yandex.market.core.order.payment.PayoutStatus;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.TransactionType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link AccrualYtDao}
 */
class AccrualYtDaoTest {

    private static final Accrual ACCRUAL = Accrual.builder()
            .setId(15678304L)
            .setEntityId(141625601L)
            .setAccrualProductType(AccrualProductType.PARTNER_PAYMENT)
            .setPaysysType(PaysysTypeCc.ACC_GOOGLE_PAY)
            .setCheckouterId(97394510L)
            .setTransactionType(TransactionType.PAYMENT)
            .setOrderId(82445221L)
            .setPartnerId(1096429L)
            .setTrantime(
                    LocalDateTime.of(2021, 12, 15, 8, 32, 41)
                            .atZone(ZoneId.systemDefault()).toInstant()
            )
            .setAmount(149000L)
            .setExportedToTlog(true)
            .setEntityType(EntityType.ITEM)
            .setPaysysPartnerId(null)
            .setCurrency(PaymentOrderCurrency.RUB)
            .setCreatedAt(
                    LocalDateTime.of(2021, 12, 15, 8, 45, 9)
                            .atZone(ZoneId.systemDefault()).toInstant()
            )
            .setOrgId(OperatingUnit.YANDEX_MARKET)
            .setPayoutStatus(PayoutStatus.NEW)
            .build();

    @Test
    void test() throws SQLException {
        ResultSet rs = prepareResultSet();
        Accrual accrual = AccrualYtDao.mapRow(rs);
        assertEquals(ACCRUAL.getId(), accrual.getId());
        assertEquals(ACCRUAL.getEntityId(), accrual.getEntityId());
        assertEquals(ACCRUAL.getAccrualProductType(), accrual.getAccrualProductType());
        assertEquals(ACCRUAL.getPaysysType(), accrual.getPaysysType());
        assertEquals(ACCRUAL.getCheckouterId(), accrual.getCheckouterId());
        assertEquals(ACCRUAL.getTransactionType(), accrual.getTransactionType());
        assertEquals(ACCRUAL.getOrderId(), accrual.getOrderId());
        assertEquals(ACCRUAL.getPartnerId(), accrual.getPartnerId());
        assertEquals(ACCRUAL.getTrantime(), accrual.getTrantime());
        assertEquals(ACCRUAL.getAmount(), accrual.getAmount());
        assertEquals(ACCRUAL.isExportedToTlog(), accrual.isExportedToTlog());
        assertEquals(ACCRUAL.getEntityType(), accrual.getEntityType());
        assertEquals(ACCRUAL.getPaysysPartnerId(), accrual.getPaysysPartnerId());
        assertEquals(ACCRUAL.getCurrency(), accrual.getCurrency());
        assertEquals(ACCRUAL.getCreatedAt(), accrual.getCreatedAt());
        assertEquals(ACCRUAL.getOrgId(), accrual.getOrgId());
        assertEquals(ACCRUAL.getPayoutStatus(), accrual.getPayoutStatus());
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(15678304L);
        when(rs.getLong("entity_id")).thenReturn(141625601L);
        when(rs.getString("product")).thenReturn("partner_payment");
        when(rs.getString("paysys_type_cc")).thenReturn("acc_google_pay");
        when(rs.getLong("checkouter_id")).thenReturn(97394510L);
        when(rs.getString("transaction_type")).thenReturn("payment");
        when(rs.getLong("order_id")).thenReturn(82445221L);
        when(rs.getLong("partner_id")).thenReturn(1096429L);
        when(rs.getString("trantime")).thenReturn("2021-12-15 08:32:41.0");
        when(rs.getLong("amount")).thenReturn(149000L);
        when(rs.getBoolean("exported_to_tlog")).thenReturn(true);
        when(rs.getString("entity_type")).thenReturn("item");
        //для поля paysys_partner_id (т.к. оно null)
        when(rs.wasNull()).thenReturn(true);
        when(rs.getString("currency")).thenReturn("RUB");
        when(rs.getString("created_at")).thenReturn("2021-12-15 08:45:09.0");
        when(rs.getLong("org_id")).thenReturn(64554L);
        when(rs.getString("payout_status")).thenReturn("new");
        return rs;
    }

}
