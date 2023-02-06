package ru.yandex.market.adv.b2bmonetization.imports.tasks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.program.YtShop;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

@DisplayName("Тест на импорт shop")
class ShopImportTest extends AbstractMonetizationTest {

    @Qualifier("importShop")
    @Autowired
    private Executor executor;

    @DisplayName("Импорт в пустую таблицу прошел успешно")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtShop.class,
                    path = "//tmp/syncShop_emptyTable_success/shop"
            ),
            before = "ShopImportTest/json/syncShop_emptyTable_success.json"
    )
    @DbUnitDataSet(
            before = "ShopImportTest/csv/syncShop_emptyTable_success.before.csv",
            after = "ShopImportTest/csv/syncShop_emptyTable_success.after.csv"
    )
    @Test
    void syncShop_emptyTable_success() {
        run("syncShop_emptyTable_success/", () -> executor.doJob(mockContext()));
    }

    @DisplayName("Импорт в непустую таблицу прошел успешно")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtShop.class,
                    path = "//tmp/syncShop_existData_success/shop"
            ),
            before = "ShopImportTest/json/syncShop_existData_success.json"
    )
    @DbUnitDataSet(
            before = "ShopImportTest/csv/syncShop_existData_success.before.csv",
            after = "ShopImportTest/csv/syncShop_existData_success.after.csv"
    )
    @Test
    void syncShop_existData_success() {
        run("syncShop_existData_success/", () -> executor.doJob(mockContext()));
    }
}
