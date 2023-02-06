package ru.yandex.market.adv.b2bmonetization.imports.tasks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.program.YtCampaignInfo;
import ru.yandex.market.adv.b2bmonetization.properties.yt.YtMstatTableProperties;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

@DisplayName("Тест на импорт campaign_info")
class CampaignInfoImportTest extends AbstractMonetizationTest {

    @Autowired
    private YtMstatTableProperties properties;

    @Qualifier("importCampaignInfo")
    @Autowired
    private Executor executor;

    @DisplayName("Импорт в пустую таблицу прошел успешно")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtCampaignInfo.class,
                    path = "//tmp/syncCampaignInfo_emptyTable_success/dictionaries/campaign_info/latest",
                    isDynamic = false
            ),
            before = "CampaignInfoImportTest/json/syncCampaignInfo_emptyTable_success.json"
    )
    @DbUnitDataSet(
            before = "CampaignInfoImportTest/csv/syncCampaignInfo_emptyTable_success.before.csv",
            after = "CampaignInfoImportTest/csv/syncCampaignInfo_emptyTable_success.after.csv"
    )
    @Test
    void syncCampaignInfo_emptyTable_success() {
        check("syncCampaignInfo_emptyTable_success", executor);
    }

    @DisplayName("Импорт в непустую таблицу прошел успешно")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtCampaignInfo.class,
                    path = "//tmp/syncCampaignInfo_existData_success/dictionaries/campaign_info/latest",
                    isDynamic = false
            ),
            before = "CampaignInfoImportTest/json/syncCampaignInfo_existData_success.json"
    )
    @DbUnitDataSet(
            before = "CampaignInfoImportTest/csv/syncCampaignInfo_existData_success.before.csv",
            after = "CampaignInfoImportTest/csv/syncCampaignInfo_existData_success.after.csv"
    )
    @Test
    void syncCampaignInfo_existData_success() {
        check("syncCampaignInfo_existData_success", executor);
    }

    private void check(String prefix, Executor executor) {
        String oldPrefix = properties.getPrefix();
        try {
            properties.setPrefix("//tmp/" + prefix + "/");
            executor.doJob(mockContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            properties.setPrefix(oldPrefix);
        }
    }
}
