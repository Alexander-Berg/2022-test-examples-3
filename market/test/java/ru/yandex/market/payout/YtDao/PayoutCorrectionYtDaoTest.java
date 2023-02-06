package ru.yandex.market.payout.YtDao;

import org.junit.jupiter.api.Test;
import ru.yandex.market.core.order.payment.EntityType;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.ProductType;
import ru.yandex.market.core.order.payment.TransactionType;
import ru.yandex.market.payout.model.PayoutCorrection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link PayoutCorrectionYtDao}
 */
public class PayoutCorrectionYtDaoTest {

    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 8, 31);

    private static final PayoutCorrection PAYOUT_CORRECTION = PayoutCorrection.builder()
            .setPayoutId(1L)
            .setEntityId(2L)
            .setCheckouterId(3L)
            .setTransactionType(TransactionType.PAYMENT)
            .setProductType(ProductType.PARTNER_PAYMENT)
            .setPaysysTypeCc(PaysysTypeCc.ACC_SBERBANK)
            .setOrderId(4L)
            .setPartnerId(5L)
            .setTrantime(IMPORT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
            .setAmount(0L)
            .setUuid("123456")
            .setLogin("test")
            .setStTicket("MARKETBILLING-666")
            .setPayoutGroupId(7L)
            .setEntityType(EntityType.ITEM)
            .setPaysysPartnerId(8L)
            .build();

    @Test
    void test() throws SQLException {
        ResultSet rs = prepareResultSet();
        PayoutCorrection payoutCorrection = PayoutCorrectionYtDao.ROW_MAPPER.mapRow(rs, 1);
        assertEquals(PAYOUT_CORRECTION.getPayoutId(), payoutCorrection.getPayoutId());
        assertEquals(PAYOUT_CORRECTION.getEntityId(), payoutCorrection.getEntityId());
        assertEquals(PAYOUT_CORRECTION.getCheckouterId(), payoutCorrection.getCheckouterId());
        assertEquals(PAYOUT_CORRECTION.getTransactionType(), payoutCorrection.getTransactionType());
        assertEquals(PAYOUT_CORRECTION.getProductType(), payoutCorrection.getProductType());
        assertEquals(PAYOUT_CORRECTION.getPaysysTypeCc(), payoutCorrection.getPaysysTypeCc());
        assertEquals(PAYOUT_CORRECTION.getOrderId(), payoutCorrection.getOrderId());
        assertEquals(PAYOUT_CORRECTION.getPartnerId(), payoutCorrection.getPartnerId());
        assertEquals(PAYOUT_CORRECTION.getTrantime(), payoutCorrection.getTrantime());
        assertEquals(PAYOUT_CORRECTION.getAmount(), payoutCorrection.getAmount());
        assertEquals(PAYOUT_CORRECTION.getUuid(), payoutCorrection.getUuid());
        assertEquals(PAYOUT_CORRECTION.getLogin(), payoutCorrection.getLogin());
        assertEquals(PAYOUT_CORRECTION.getStTicket(), payoutCorrection.getStTicket());
        assertEquals(PAYOUT_CORRECTION.getPayoutGroupId(), payoutCorrection.getPayoutGroupId());
        assertEquals(PAYOUT_CORRECTION.getEntityType(), payoutCorrection.getEntityType());
        assertEquals(PAYOUT_CORRECTION.getPaysysPartnerId(), payoutCorrection.getPaysysPartnerId());
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("payout_id")).thenReturn(1L);
        when(rs.getLong("entity_id")).thenReturn(2L);
        when(rs.getLong("checkouter_id")).thenReturn(3L);
        when(rs.getString("transaction_type")).thenReturn("payment");
        when(rs.getString("product")).thenReturn("partner_payment");
        when(rs.getString("paysys_type_cc")).thenReturn("acc_sberbank");
        when(rs.getLong("order_id")).thenReturn(4L);
        when(rs.getLong("partner_id")).thenReturn(5L);
        when(rs.getString("trantime")).thenReturn("2021-08-31 00:00:00.0");
        when(rs.getLong("amount")).thenReturn(0L);
        when(rs.getString("uuid")).thenReturn("123456");
        when(rs.getString("login")).thenReturn("test");
        when(rs.getString("st_ticket")).thenReturn("MARKETBILLING-666");
        when(rs.getLong("payout_group_id")).thenReturn(7L);
        when(rs.getString("entity_type")).thenReturn("item");
        when(rs.getLong("paysys_partner_id")).thenReturn(8L);
        return rs;
    }
}
