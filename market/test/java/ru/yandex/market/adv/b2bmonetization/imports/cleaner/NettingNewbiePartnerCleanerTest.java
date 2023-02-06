package ru.yandex.market.adv.b2bmonetization.imports.cleaner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.program.NettingNewbiePartner;
import ru.yandex.market.adv.b2bmonetization.properties.yt.YtNettingNewbieTableProperties;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.tms.quartz2.model.Executor;

class NettingNewbiePartnerCleanerTest extends AbstractMonetizationTest {

    @Autowired
    private YtNettingNewbieTableProperties properties;

    @Autowired
    @Qualifier("nettingNewbiePartnerCleanerExecutor")
    private Executor nettingNewbiePartnerClearExecutor;

    @DisplayName("Успешно удалили старые таблицы")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_deleteOldTables_success/latest",
                    isDynamic = false
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_deleteOldTables_success/2021-10-18",
                    isDynamic = false
            ),
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_deleteOldTables_success/2021-10-19",
                    isDynamic = false
            ),
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_deleteOldTables_success/2021-10-20",
                    isDynamic = false
            )
    )
    @Test
    void exportNettingNewbie_deleteOldTables_success() {
        check("exportNettingNewbie_deleteOldTables_success", nettingNewbiePartnerClearExecutor);
    }

    @DisplayName("Успешная отработка джобы, когда нет таблиц для удаления")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_noTablesForDelete_success/latest",
                    isDynamic = false
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_noTablesForDelete_success/2021-10-20",
                    isDynamic = false
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_noTablesForDelete_success/2021-10-21",
                    isDynamic = false
            )
    )
    @Test
    void exportNettingNewbie_noTablesForDelete_success() {
        check("exportNettingNewbie_noTablesForDelete_success", nettingNewbiePartnerClearExecutor);
    }

    private void check(String prefix, Executor executor) {
        String oldPrefix = properties.getPrefix();
        try {
            properties.setPrefix("//tmp/adv_unittest/" + prefix + "/");
            executor.doJob(mockContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            properties.setPrefix(oldPrefix);
        }
    }
}
