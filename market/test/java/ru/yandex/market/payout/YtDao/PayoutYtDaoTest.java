package ru.yandex.market.payout.YtDao;

import org.junit.jupiter.api.Test;
import ru.yandex.market.core.order.payment.EntityType;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.ProductType;
import ru.yandex.market.core.order.payment.TransactionType;
import ru.yandex.market.payout.model.Payout;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link PayoutYtDao}
 */
public class PayoutYtDaoTest {

    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 8, 31);

    private static final Payout PAYOUT = Payout.builder()
            .setPayoutId(1L)
            .setEntityId(2L)
            .setCheckouterId(3L)
            .setTransactionType(TransactionType.REFUND)
            .setProductType(ProductType.YANDEX_ACCOUNT_WITHDRAW)
            .setPaysysTypeCc(PaysysTypeCc.YAMARKETPLUS)
            .setOrderId(4L)
            .setPartnerId(5L)
            .setTrantime(IMPORT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
            .setAmount(1000L)
            .setPayoutGroupId(6L)
            .setEntityType(EntityType.ITEM)
            .setPaysysPartnerId(7L)
            .build();

    @Test
    void test() throws SQLException {
        ResultSet rs = prepareResultSet();
        Payout payout = PayoutYtDao.mapRow(rs);
        assertEquals(PAYOUT.getPayoutId(), payout.getPayoutId());
        assertEquals(PAYOUT.getEntityId(), payout.getEntityId());
        assertEquals(PAYOUT.getCheckouterId(), payout.getCheckouterId());
        assertEquals(PAYOUT.getTransactionType(), payout.getTransactionType());
        assertEquals(PAYOUT.getProductType(), payout.getProductType());
        assertEquals(PAYOUT.getPaysysTypeCc(), payout.getPaysysTypeCc());
        assertEquals(PAYOUT.getOrderId(), payout.getOrderId());
        assertEquals(PAYOUT.getPartnerId(), payout.getPartnerId());
        assertEquals(PAYOUT.getTrantime(), payout.getTrantime());
        assertEquals(PAYOUT.getAmount(), payout.getAmount());
        assertEquals(PAYOUT.getPayoutGroupId(), payout.getPayoutGroupId());
        assertEquals(PAYOUT.getEntityType(), payout.getEntityType());
        assertEquals(PAYOUT.getPaysysPartnerId(), payout.getPaysysPartnerId());
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("payout_id")).thenReturn(1L);
        when(rs.getLong("entity_id")).thenReturn(2L);
        when(rs.getLong("checkouter_id")).thenReturn(3L);
        when(rs.getString("transaction_type")).thenReturn("refund");
        when(rs.getString("product")).thenReturn("yandex_account_withdraw");
        when(rs.getString("paysys_type_cc")).thenReturn("yamarketplus");
        when(rs.getLong("order_id")).thenReturn(4L);
        when(rs.getLong("partner_id")).thenReturn(5L);
        when(rs.getString("trantime")).thenReturn("2021-08-31 00:00:00.0");
        when(rs.getLong("amount")).thenReturn(1000L);
        when(rs.getLong("payout_group_id")).thenReturn(6L);
        when(rs.getString("entity_type")).thenReturn("item");
        when(rs.getLong("paysys_partner_id")).thenReturn(7L);
        return rs;
    }

}
