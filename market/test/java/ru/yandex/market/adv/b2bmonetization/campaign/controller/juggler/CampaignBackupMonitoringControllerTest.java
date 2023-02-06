package ru.yandex.market.adv.b2bmonetization.campaign.controller.juggler;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.properties.monitoring.backup.CampaignBackupProperties;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.Campaign;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 07.07.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
@DisplayName("Тесты на endpoint GET juggler/campaign/backup/status.")
class CampaignBackupMonitoringControllerTest extends AbstractMonetizationTest {

    @Autowired
    private CampaignBackupProperties campaignBackupProperties;

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/checkCampaignBackup_today_ok_2021-10-21",
                    isDynamic = false
            ),
            before = "CampaignBackupMonitoringController/json/yt/checkCampaignBackup_today_ok.json"
    )
    @DisplayName("Есть backup рекламных кампаний за сегодня.")
    @Test
    void checkCampaignBackup_today_ok() {
        checkCampaignBackup("checkCampaignBackup_today_ok", status().isOk());
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/checkCampaignBackup_tomorrow_ok_2021-10-20",
                    isDynamic = false
            ),
            before = "CampaignBackupMonitoringController/json/yt/checkCampaignBackup_tomorrow_ok.json"
    )
    @DisplayName("Есть backup рекламных кампаний за вчера.")
    @Test
    void checkCampaignBackup_tomorrow_ok() {
        checkCampaignBackup("checkCampaignBackup_tomorrow_ok", status().isOk());
    }

    @DisplayName("Нет backup рекламных кампаний.")
    @Test
    void checkCampaignBackup_nothing_error() {
        checkCampaignBackup("checkCampaignBackup_nothing_error", status().isExpectationFailed());
    }

    private void checkCampaignBackup(String methodName, ResultMatcher resultMatcher) {
        String oldPath = campaignBackupProperties.getPath();
        campaignBackupProperties.setPath("//tmp/" + methodName + "_");
        try {
            mvc.perform(
                            get("/juggler/campaign/backup/status")
                                    .contentType(MediaType.TEXT_PLAIN_VALUE)
                    )
                    .andExpect(resultMatcher)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                    .andExpect(content().string(
                                    loadFile("CampaignBackupMonitoringController/json/response/" + methodName + ".txt")
                                            .trim()
                            )
                    );
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            campaignBackupProperties.setPath(oldPath);
        }
    }
}
