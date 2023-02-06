package ru.yandex.market.admin.service.remote;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.order.UIOrderReturnStorageCorrection;
import ru.yandex.market.admin.ui.model.supplier.UIBillingCorrectionCommonInfo;
import ru.yandex.market.admin.ui.model.supplier.UIRawCorrection;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.admin.ui.model.supplier.UIServiceType.ORDER_RETURN_STORAGE_CORRECTION;

/**
 * Тесты для {@link RemoteOrderReturnStorageCorrectionUIService}
 */
class RemoteOrderReturnStorageCorrectionUIServiceTest extends FunctionalTest {

    @Autowired
    private RemoteOrderReturnStorageCorrectionUIService remoteOrderReturnStorageCorrectionUIService;

    @Test
    @DisplayName("Должны вернуть ошибку, если csv содержит некорректное кол-во колонок.")
    void testParseAndValidateIncorrectColumnNumber() {
        UIRawCorrection uiRawCorrection = new UIRawCorrection("123,456.-200", "MBI-51", ORDER_RETURN_STORAGE_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteOrderReturnStorageCorrectionUIService.parseAndValidate(uiRawCorrection));
        assertEquals("Неверное число столбцов в строке '1'.", exception.getMessage());
    }

    @Test
    @DisplayName("Должны вернуть ошибку, если csv is empty")
    void testParseAndValidateEmptyCsvData() {
        UIRawCorrection uiRawCorrection = new UIRawCorrection("", "MBI-51", ORDER_RETURN_STORAGE_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteOrderReturnStorageCorrectionUIService.parseAndValidate(uiRawCorrection));
        assertEquals("Corrections shouldn't be empty!", exception.getMessage());
    }

    @Test
    @DisplayName("Должны вернуть ошибку, если amount = 0")
    void testParseAndValidateAmountIsZero() {
        UIRawCorrection uiRawCorrection = new UIRawCorrection("123,456,0", "MBI-51", ORDER_RETURN_STORAGE_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteOrderReturnStorageCorrectionUIService.parseAndValidate(uiRawCorrection));
        assertEquals("Amount must be non-zero", exception.getMessage());
    }

    @Test
    @DisplayName("Должны вернуть ошибку, если orderId + supplierId не уникальны")
    void testParseAndValidateNonUnique() {
        UIRawCorrection uiRawCorrection = new UIRawCorrection("123,456,1\n123,456,2", "MBI-51", ORDER_RETURN_STORAGE_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteOrderReturnStorageCorrectionUIService.parseAndValidate(uiRawCorrection));
        assertEquals("Duplicate record for orderId: 123, supplierId: 456", exception.getMessage());
    }

    @Test
    @DisplayName("Должны вернуть ошибку, если orderId + supplierId не существуют")
    void testParseAndValidateOrderNotExist() {
        UIRawCorrection uiRawCorrection = new UIRawCorrection("123,456,1", "MBI-51", ORDER_RETURN_STORAGE_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteOrderReturnStorageCorrectionUIService.parseAndValidate(uiRawCorrection));
        assertEquals("OrderId: 123 with supplierId: 456 not found", exception.getMessage());
    }

    @Test
    @DisplayName("Должны пройти проверку валидации: данные корректны")
    @DbUnitDataSet(
            before = "RemoteOrderReturnStorageCorrectionUIServiceTest.before.csv"
    )
    void testParseAndValidatePositive() {
        UIRawCorrection uiRawCorrection = new UIRawCorrection("123,456,1", "MBI-51", ORDER_RETURN_STORAGE_CORRECTION);
        assertDoesNotThrow(() -> remoteOrderReturnStorageCorrectionUIService.parseAndValidate(uiRawCorrection));
    }

    @Test
    @DisplayName("Должны сохранить корректировку, данные валидные")
    @DbUnitDataSet(
            before = "RemoteOrderReturnStorageCorrectionUIServiceTest.before.csv",
            after = "RemoteOrderReturnStorageCorrectionUIServiceTest.after.csv"
    )
    void testPersist() {
        UIBillingCorrectionCommonInfo info = getInfo();
        remoteOrderReturnStorageCorrectionUIService.persist(List.of(getUICorrection()), info);
    }

    private UIOrderReturnStorageCorrection getUICorrection() {
        return new UIOrderReturnStorageCorrection(123, 456, 1, "MBI-51");
    }

    private UIBillingCorrectionCommonInfo getInfo() {
        return new UIBillingCorrectionCommonInfo(1L, "yndx-test", ORDER_RETURN_STORAGE_CORRECTION, "MBI-51");
    }
}
