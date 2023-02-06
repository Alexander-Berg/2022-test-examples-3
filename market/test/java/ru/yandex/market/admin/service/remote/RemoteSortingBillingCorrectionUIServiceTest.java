package ru.yandex.market.admin.service.remote;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.magic.passport.model.PassportInfo;
import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.order.UISortingCorrection;
import ru.yandex.market.admin.ui.model.supplier.UIRawCorrection;
import ru.yandex.market.admin.ui.model.supplier.UIServiceType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для {@link RemoteSortingBillingCorrectionUIService}.
 */
@ParametersAreNonnullByDefault
class RemoteSortingBillingCorrectionUIServiceTest extends FunctionalTest {

    @Autowired
    private RemoteSortingBillingCorrectionUIService remoteSortingBillingCorrectionUIService;

    @Test
    @DisplayName("Должны сохранить корректировку, если пришли валидные данные.")
    @DbUnitDataSet(
            before = "RemoteSortingBillingCorrectionUIServiceTest.before.csv",
            after = "RemoteSortingBillingCorrectionUIServiceTest.after.csv")
    void test_shouldSaveCorrections() {
        UIRawCorrection rawData = new UIRawCorrection("1,intake,2019-12-31,60000", "MBI-123",
                UIServiceType.SORTING_CORRECTION);
        List<UISortingCorrection> uiSortingCorrections =
                remoteSortingBillingCorrectionUIService.parseAndValidate(rawData);
        remoteSortingBillingCorrectionUIService.applyCorrections(uiSortingCorrections, getFakePassportInfo());
    }

    @Test
    @DisplayName("Должны вернуть ошибку, если csv содержит некорректное кол-во колонок.")
    void test_shouldThrowExceptionWhenUnexpectedColumnsGiven() {
        UIRawCorrection rawData = new UIRawCorrection("1,intake,2019-12-31,60000,unexpected_column", "MBI-123",
                UIServiceType.SORTING_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteSortingBillingCorrectionUIService.parseAndValidate(rawData));
        assertEquals("Введено некорректное количество колонок, ожидается 4 колонки.", exception.getMessage());
    }

    @Test
    @DisplayName("Должны вернуть ошибку, если некорректный id поставщика.")
    void test_shouldThrowExceptionWhenBadSupplierIdGiven() {
        UIRawCorrection rawData = new UIRawCorrection("1,intake,2019-12-31,60000", "MBI-123",
                UIServiceType.SORTING_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteSortingBillingCorrectionUIService.parseAndValidate(rawData));
        assertEquals("Найдены несуществующие id поставщиков: [1]", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "RemoteSortingBillingCorrectionUIServiceTest.before.csv")
    @DisplayName("Должны вернуть ошибку, если некорректный intake type.")
    void test_shouldThrowExceptionWhenBadIntakeTypeGiven() {
        UIRawCorrection rawData = new UIRawCorrection("1,incorrect_value,2019-12-31,60000", "MBI-123",
                UIServiceType.SORTING_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteSortingBillingCorrectionUIService.parseAndValidate(rawData));
        assertEquals("Некорректный тип привоза! Допустимые типы: intake, self_delivery.", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "RemoteSortingBillingCorrectionUIServiceTest.before.csv")
    @DisplayName("Должны вернуть ошибку, если некорректная корректируемая дата.")
    void test_shouldThrowExceptionWhenBadCorrectedDateGiven() {
        UIRawCorrection rawData = new UIRawCorrection("1,intake,3019-12-31,60000", "MBI-123",
                UIServiceType.SORTING_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteSortingBillingCorrectionUIService.parseAndValidate(rawData));
        assertEquals("Корректируемая дата должна быть в предыдущем периоде (месяце)!", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "RemoteSortingBillingCorrectionUIServiceTest.before.csv")
    @DisplayName("Должны вернуть ошибку, если некорректная сумма корректировки.")
    void test_shouldThrowExceptionWhenBadAmountGiven() {
        UIRawCorrection rawData = new UIRawCorrection("1,intake,2019-12-31,0", "MBI-123",
                UIServiceType.SORTING_CORRECTION);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> remoteSortingBillingCorrectionUIService.parseAndValidate(rawData));
        assertEquals("Сумма корректироки должна быть ненулевой!", exception.getMessage());
    }

    @Nonnull
    private PassportInfo getFakePassportInfo() {
        return new PassportInfo(123L, "user", "nickname", "login");
    }
}