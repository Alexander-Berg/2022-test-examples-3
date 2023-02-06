package ru.yandex.market.billing.marketing.service;

import java.time.LocalDate;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DbUnitDataSet(
        before = "../PromoOrderItemImport.enable.csv"
)
@ParametersAreNonnullByDefault
class PromoOrderItemImportServiceTest extends FunctionalTest {
    @Autowired
    PromoOrderItemImportService promoOrderItemImportService;

    @Test
    @DisplayName("Получение промозаказов из чекаутера, доставленных в заданную дату")
    @DbUnitDataSet(
            before = {"PromoOrderItemImportServiceTest_oldAnaplanIdFormat.before.csv",
                    "../PromoOrderItemImport.useOriginalTable.csv"},
            after = "PromoOrderItemImportServiceTest_oldAnaplanIdFormat.after.csv"
    )
    void successImport_oldAnaplanIdFormat() {
        promoOrderItemImportService.process(LocalDate.parse("2021-06-30"));
    }

    @Test
    @DisplayName("Получение промозаказов, доставленных в заданную дату, с новым форматом anaplan_id")
    @DbUnitDataSet(
            before = {"PromoOrderItemImportServiceTest.before.csv",
                    "../PromoOrderItemImport.useOriginalTable.csv"},
            after = "PromoOrderItemImportServiceTest.after.csv"
    )
    void successImport() {
        promoOrderItemImportService.process(LocalDate.parse("2022-06-08"));
    }

    @Test
    @DisplayName("Получение промозаказов, доставленных в заданную дату, с учетом игнор-листа заказов")
    @DbUnitDataSet(
            before = {
                    "PromoOrderItemImportServiceTest_oldAnaplanIdFormat.before.csv",
                    "PromoOrderItemImportServiceTest_ignoredOrders.before.csv",
                    "../PromoOrderItemImport.useOriginalTable.csv"
            },
            after = "PromoOrderItemImportServiceTest_ignoredOrders.after.csv"
    )
    void import_withIgnoredOrders() {
        promoOrderItemImportService.process(LocalDate.parse("2021-06-30"));
    }

    @Test
    @DisplayName("Переимпорт промозаказов, доставленных в заданную дату, с учетом игнор-листа")
    @DbUnitDataSet(
            before = {
                    // Данные, которые были в базе до первичного импорта, допустим они не изменились
                    "PromoOrderItemImportServiceTest_oldAnaplanIdFormat.before.csv",
                    // После первичного импорта создались записи в promo_order_items
                    "PromoOrderItemImportServiceTest_oldAnaplanIdFormat.after.csv",
                    // Часть заказов хотим проигнорить при переимпорте
                    "PromoOrderItemImportServiceTest_ignoredOrders.before.csv",
                    "../PromoOrderItemImport.useOriginalTable.csv"
            },
            // В результате должны получить то же, что получили бы при первичном импорте с игнором определенных заказов
            after = "PromoOrderItemImportServiceTest_ignoredOrders.after.csv"
    )
    void reimport_withIgnoredOrders() {
        promoOrderItemImportService.process(LocalDate.parse("2021-06-30"));
    }

    @Test
    @DisplayName("При явно не включенной переменной писать во временную табличку")
    @DbUnitDataSet(
            before = "PromoOrderItemImportServiceTest.before.csv",
            after = "PromoOrderItemImportServiceTest.successTmpTableImport.after.csv"
    )
    void successTmpTableImport() {
        promoOrderItemImportService.process(LocalDate.parse("2022-06-08"));
    }
}
