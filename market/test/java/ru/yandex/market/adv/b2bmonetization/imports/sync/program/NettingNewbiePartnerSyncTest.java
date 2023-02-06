package ru.yandex.market.adv.b2bmonetization.imports.sync.program;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.program.NettingNewbiePartner;
import ru.yandex.market.adv.b2bmonetization.properties.yt.YtNettingNewbieTableProperties;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

class NettingNewbiePartnerSyncTest extends AbstractMonetizationTest {

    @Autowired
    private YtNettingNewbieTableProperties properties;

    @Autowired
    @Qualifier("nettingNewbiePartnerSyncExecutor")
    private Executor nettingNewbiePartnerSyncExecutor;

    @DisplayName("Успешно выгрузили в Yt новых партнеров на взаимозачете без бонусов (при несуществующей YT-таблице " +
            "и пустой таблице sync_info)")
    @DbUnitDataSet(
            before = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_tableEmpty_success.before.csv",
            after = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_tableEmpty_success.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_tableEmpty_success/latest",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_tableEmpty_success.after.json",
            create = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_tableEmpty_success/2021-10-21",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_tableEmpty_success.after.json",
            create = false
    )
    @Test
    void exportNettingNewbie_tableEmpty_success() {
        check("exportNettingNewbie_tableEmpty_success", nettingNewbiePartnerSyncExecutor);
    }

    @DisplayName("Успешно обновился линк со старой таблицы на новую")
    @DbUnitDataSet(
            before = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_linkUpdated_success.before.csv",
            after = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_linkUpdated_success.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_linkUpdated_success/latest",
                    isDynamic = false
            ),
            before = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_linkUpdated_success.before.json",
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_linkUpdated_success.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_linkUpdated_success/2021-10-21",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_linkUpdated_success.after.json",
            create = false
    )
    @Test
    void exportNettingNewbie_linkUpdated_success() {
        check("exportNettingNewbie_linkUpdated_success", nettingNewbiePartnerSyncExecutor);
    }

    @DisplayName("Успешно выгрузили в Yt новых партнеров на взаимозачете без бонусов (при непустой таблице sync_info)")
    @DbUnitDataSet(
            before = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_tableNotEmpty_success.before.csv",
            after = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_tableNotEmpty_success.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_tableNotEmpty_success/latest",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_tableNotEmpty_success.after.json",
            create = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_tableNotEmpty_success/2021-10-21",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_tableNotEmpty_success.after.json",
            create = false
    )
    @Test
    void exportNettingNewbie_tableNotEmpty_success() {
        check("exportNettingNewbie_tableNotEmpty_success", nettingNewbiePartnerSyncExecutor);
    }

    @DisplayName("Успешно не выгрузили в Yt нового партнера, когда его бизнесу уже начисляли бонус")
    @DbUnitDataSet(
            before = "NettingNewbiePartnerSyncTest/csv/" +
                    "exportNettingNewbie_existBonusForBusiness_success.before.csv",
            after = "NettingNewbiePartnerSyncTest/csv/" +
                    "exportNettingNewbie_existBonusForBusiness_success.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_existBonusForBusiness_success/latest",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_existBonusForBusiness_success.after.json",
            create = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/" +
                            "exportNettingNewbie_existBonusForBusiness_success/2021-10-21",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_existBonusForBusiness_success.after.json",
            create = false
    )
    @Test
    void exportNettingNewbie_existBonusForBusiness_success() {
        check("exportNettingNewbie_existBonusForBusiness_success", nettingNewbiePartnerSyncExecutor);
    }

    @DisplayName("Успешно выгрузили в Yt новых партнеров после предыдущей неудачной попытки")
    @DbUnitDataSet(
            before = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_retrySync_success.before.csv",
            after = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_retrySync_success.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_retrySync_success/latest",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_retrySync_success.after.json",
            create = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_retrySync_success/2021-10-21",
                    isDynamic = false
            ),
            before = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_retrySync_success.before.json",
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_retrySync_success.after.json"
    )
    @Test
    void exportNettingNewbie_retrySync_success() {
        check("exportNettingNewbie_retrySync_success", nettingNewbiePartnerSyncExecutor);
    }

    @DisplayName("Успешно обработали случай, когда успешная выгрузка за сегодняшний день уже была")
    @DbUnitDataSet(
            before = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_alreadySynced_success.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_alreadySynced_success/latest",
                    isDynamic = false
            ),
            before = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_alreadySynced_success.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_alreadySynced_success/2021-10-21",
                    isDynamic = false
            ),
            before = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_alreadySynced_success.before.json"
    )
    @Test
    void exportNettingNewbie_alreadySynced_success() {
        check("exportNettingNewbie_alreadySynced_success", nettingNewbiePartnerSyncExecutor);
    }

    @DisplayName("Успешно не выгрузили в Yt тех партнеров, у которых нет кампании с end_date = null")
    @DbUnitDataSet(
            before = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_campaignInfoNotExist_success.before.csv",
            after = "NettingNewbiePartnerSyncTest/csv/exportNettingNewbie_campaignInfoNotExist_success.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_campaignInfoNotExist_success/latest",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_campaignInfoNotExist_success.after.json",
            create = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = NettingNewbiePartner.class,
                    path = "//tmp/adv_unittest/exportNettingNewbie_campaignInfoNotExist_success/2021-10-21",
                    isDynamic = false
            ),
            after = "NettingNewbiePartnerSyncTest/json/exportNettingNewbie_campaignInfoNotExist_success.after.json",
            create = false
    )
    @Test
    void exportNettingNewbie_campaignInfoNotExist_success() {
        check("exportNettingNewbie_campaignInfoNotExist_success", nettingNewbiePartnerSyncExecutor);
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
