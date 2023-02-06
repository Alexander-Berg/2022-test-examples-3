package ru.yandex.market.ff.model;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.model.dbqueue.CancelRequestPayload;
import ru.yandex.market.ff.model.dbqueue.RequestDailyReportByWarehousePayload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbQueueTaskShopRequestRelationUtilsTest {

    @Test
    void isRelatedToShopRequest() {
        assertTrue(DbQueueTaskShopRequestRelationUtils.isRelatedToShopRequest(new CancelRequestPayload(123L)));
        assertFalse(DbQueueTaskShopRequestRelationUtils.isRelatedToShopRequest(
                new RequestDailyReportByWarehousePayload(123L, SupplierType.FIRST_PARTY, LocalDate.now())));
    }

    @Test
    void getShopRequestIdTest() {
        assertEquals(123L,
                DbQueueTaskShopRequestRelationUtils.getShopRequestId(new CancelRequestPayload(123L)));
    }
}
