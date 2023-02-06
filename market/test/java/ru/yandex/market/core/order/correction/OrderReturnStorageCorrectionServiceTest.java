package ru.yandex.market.core.order.correction;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

/**
 * Тесты для {@link OrderReturnStorageCorrectionService}
 */
class OrderReturnStorageCorrectionServiceTest extends FunctionalTest {

    @Autowired
    private OrderReturnStorageCorrectionService orderReturnStorageCorrectionService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Тест сохранения пустого списка корректировок")
    void testEmpty() {
        OrderReturnStorageCorrectionDao dao = Mockito.mock(OrderReturnStorageCorrectionDao.class);
        OrderReturnStorageCorrectionService correctionService =
                new OrderReturnStorageCorrectionService(dao, transactionTemplate);
        correctionService.persistOrderReturnStorageCorrections(List.of(), 1, "MBI-51");

        Mockito.verify(dao, Mockito.never()).persistOrderReturnStorageCorrections(Mockito.anyList());
    }

    @Test
    @DisplayName("Тест сохранения корректировок услуг хранения невыкупленных заказов")
    @DbUnitDataSet(
            before = "OrderReturnStorageCorrectionServiceTest.before.csv",
            after = "OrderReturnStorageCorrectionServiceTest.after.csv"
    )
    void testPersistOrderReturnStorageCorrections() {
        List<OrderReturnStorageCorrection> corrections = List.of(
                getCorrection(123, 456),
                getCorrection(124, 457),
                getCorrection(125, 458),
                getCorrection(126, 459),
                getCorrection(127, 460)
        );
        orderReturnStorageCorrectionService.persistOrderReturnStorageCorrections(corrections, 1, "MBI-51");
    }

    private OrderReturnStorageCorrection getCorrection(long orderId, long supplierId) {
        return OrderReturnStorageCorrection.builder()
                .setOrderId(orderId)
                .setSupplierId(supplierId)
                .setAmount(1L)
                .setCorrectionTimestamp(Instant.now())
                .setNote("MBI-51")
                .setLogin("yndx-test")
                .build();
    }

}
