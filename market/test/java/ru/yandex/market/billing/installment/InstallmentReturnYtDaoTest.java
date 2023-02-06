package ru.yandex.market.billing.installment;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.yandex.market.core.billing.model.InstallmentReturnBilledAmount;
import ru.yandex.market.core.billing.model.InstallmentType;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.model.ValueType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link InstallmentReturnYtDao}.
 */
class InstallmentReturnYtDaoTest {

    private static final InstallmentReturnBilledAmount RETURN_BILLED_AMOUNT = InstallmentReturnBilledAmount.builder()
            .setOrderId(78559164L)
            .setOrderItemId(136510647L)
            .setPartnerId(1L)
            .setServiceType(BillingServiceType.INSTALLMENT_RETURN_CANCELLATION)
            .setInstallmentType(InstallmentType.INSTALLMENT_12)
            .setReturnItemId(2)
            .setTrantime(LocalDateTime.of(LocalDate.of(2021, 11, 25), LocalTime.of(2, 30))
                    .atZone(ZoneId.systemDefault())
                    .toInstant())
            .setCount(1)
            .setTariffValue(1200)
            .setTariffValueType(ValueType.RELATIVE)
            .setExportedToTlog(true)
            .setRawAmount(-304880L)
            .setAmount(-304880L)
            .build();

    @Test
    public void test() throws SQLException {
        ResultSet rs = prepareResultSet();
        InstallmentReturnBilledAmount result = InstallmentReturnYtDao.mapRow(rs);
        assertEquals(RETURN_BILLED_AMOUNT.getAmount(), result.getAmount());
        assertEquals(RETURN_BILLED_AMOUNT.getOrderId(), result.getOrderId());
        assertEquals(RETURN_BILLED_AMOUNT.getOrderItemId(), result.getOrderItemId());
        assertEquals(RETURN_BILLED_AMOUNT.getPartnerId(), result.getPartnerId());
        assertEquals(RETURN_BILLED_AMOUNT.getServiceType(), result.getServiceType());
        assertEquals(RETURN_BILLED_AMOUNT.getInstallmentType(), result.getInstallmentType());
        assertEquals(RETURN_BILLED_AMOUNT.getReturnItemId(), result.getReturnItemId());
        assertEquals(RETURN_BILLED_AMOUNT.getTrantime(), result.getTrantime());
        assertEquals(RETURN_BILLED_AMOUNT.getCount(), result.getCount());
        assertEquals(RETURN_BILLED_AMOUNT.getTariffValue(), result.getTariffValue());
        assertEquals(RETURN_BILLED_AMOUNT.getTariffValueType(), result.getTariffValueType());
        assertEquals(RETURN_BILLED_AMOUNT.getRawAmount(), result.getRawAmount());
        assertEquals(RETURN_BILLED_AMOUNT.isExportedToTlog(), result.isExportedToTlog());
        assertEquals(RETURN_BILLED_AMOUNT.getAmount(), result.getAmount());
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.getLong("order_id")).thenReturn(78559164L);
        when(resultSet.getLong("order_item_id")).thenReturn(136510647L);
        when(resultSet.getLong("partner_id")).thenReturn(1L);
        when(resultSet.getString("service_type")).thenReturn("installment_return_cancellation");
        when(resultSet.getString("installment_type")).thenReturn("installment_12");
        when(resultSet.getLong("return_item_id")).thenReturn(2L);
        when(resultSet.getString("trantime")).thenReturn("2021-11-25 02:30:00.000000");
        when(resultSet.getInt("count")).thenReturn(1);
        when(resultSet.getInt("tariff_value")).thenReturn(1200);
        when(resultSet.getString("tariff_value_type")).thenReturn("relative");
        when(resultSet.getInt("exported_to_tlog")).thenReturn(1);
        when(resultSet.getLong("raw_amount")).thenReturn(-304880L);
        when(resultSet.getLong("amount")).thenReturn(-304880L);
        return resultSet;
    }

}
