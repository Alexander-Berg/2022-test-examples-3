package ru.yandex.market.accrual;

import org.junit.jupiter.api.Test;
import ru.yandex.market.core.oebs.OperatingUnit;
import ru.yandex.market.core.order.payment.AccrualProductType;
import ru.yandex.market.core.order.payment.EntityType;
import ru.yandex.market.core.order.payment.PaymentOrderCurrency;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.TransactionType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link AccrualCorrectionYtDao}
 */
class AccrualCorrectionYtDaoTest {

    private static final AccrualCorrection ACCRUAL_CORRECTION = AccrualCorrection.builder()
            .setId(15678304L)
            .setEntityId(141625601L)
            .setAccrualProductType(AccrualProductType.PARTNER_PAYMENT)
            .setPaysysType(PaysysTypeCc.ACC_TINKOFF_CREDIT)
            .setCheckouterId(97394510L)
            .setTransactionType(TransactionType.REFUND)
            .setOrderId(82445221L)
            .setPartnerId(1096429L)
            .setTrantime(
                    LocalDateTime.of(2021, 12, 15, 8, 32, 41)
                            .atZone(ZoneId.systemDefault()).toInstant()
            )
            .setAmount(-819000L)
            .setExportedToTlog(true)
            .setEntityType(EntityType.ITEM)
            .setPaysysPartnerId(null)
            .setCurrency(PaymentOrderCurrency.RUB)
            .setOrgId(OperatingUnit.YANDEX_MARKET)
            .setUuid("123")
            .setLogin("234")
            .setStTicket("345")
            .build();

    @Test
    void test() throws SQLException {
        ResultSet rs = prepareResultSet();
        AccrualCorrection accrualCorrection = AccrualCorrectionYtDao.mapRow(rs);
        assertEquals(ACCRUAL_CORRECTION.getId(), accrualCorrection.getId());
        assertEquals(ACCRUAL_CORRECTION.getEntityId(), accrualCorrection.getEntityId());
        assertEquals(ACCRUAL_CORRECTION.getAccrualProductType(), accrualCorrection.getAccrualProductType());
        assertEquals(ACCRUAL_CORRECTION.getPaysysType(), accrualCorrection.getPaysysType());
        assertEquals(ACCRUAL_CORRECTION.getCheckouterId(), accrualCorrection.getCheckouterId());
        assertEquals(ACCRUAL_CORRECTION.getTransactionType(), accrualCorrection.getTransactionType());
        assertEquals(ACCRUAL_CORRECTION.getOrderId(), accrualCorrection.getOrderId());
        assertEquals(ACCRUAL_CORRECTION.getPartnerId(), accrualCorrection.getPartnerId());
        assertEquals(ACCRUAL_CORRECTION.getTrantime(), accrualCorrection.getTrantime());
        assertEquals(ACCRUAL_CORRECTION.getAmount(), accrualCorrection.getAmount());
        assertEquals(ACCRUAL_CORRECTION.getExportedToTlog(), accrualCorrection.getExportedToTlog());
        assertEquals(ACCRUAL_CORRECTION.getEntityType(), accrualCorrection.getEntityType());
        assertEquals(ACCRUAL_CORRECTION.getPaysysPartnerId(), accrualCorrection.getPaysysPartnerId());
        assertEquals(ACCRUAL_CORRECTION.getCurrency(), accrualCorrection.getCurrency());
        assertEquals(ACCRUAL_CORRECTION.getOrgId(), accrualCorrection.getOrgId());
        assertEquals(ACCRUAL_CORRECTION.getUuid(), accrualCorrection.getUuid());
        assertEquals(ACCRUAL_CORRECTION.getLogin(), accrualCorrection.getLogin());
        assertEquals(ACCRUAL_CORRECTION.getStTicket(), accrualCorrection.getStTicket());
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(15678304L);
        when(rs.getLong("entity_id")).thenReturn(141625601L);
        when(rs.getString("product")).thenReturn("partner_payment");
        when(rs.getString("paysys_type_cc")).thenReturn("acc_tinkoff_credit");
        when(rs.getLong("checkouter_id")).thenReturn(97394510L);
        when(rs.getString("transaction_type")).thenReturn("refund");
        when(rs.getLong("order_id")).thenReturn(82445221L);
        when(rs.getLong("partner_id")).thenReturn(1096429L);
        when(rs.getString("trantime")).thenReturn("2021-12-15 08:32:41.0");
        when(rs.getLong("amount")).thenReturn(-819000L);
        when(rs.getBoolean("exported_to_tlog")).thenReturn(true);
        when(rs.getString("entity_type")).thenReturn("item");
        //для поля paysys_partner_id (т.к. оно null)
        when(rs.wasNull()).thenReturn(true);
        when(rs.getString("currency")).thenReturn("RUB");
        when(rs.getLong("org_id")).thenReturn(64554L);
        when(rs.getString("uuid")).thenReturn("123");
        when(rs.getString("login")).thenReturn("234");
        when(rs.getString("st_ticket")).thenReturn("345");
        return rs;
    }

}
