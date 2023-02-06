package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.exception.CampaignNotFoundException;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.Campaign;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.CampaignHistory;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * @author nongi
 */
@DisplayName("Тесты на репозиторий активации/деактивации рекламных кампаний")
@ParametersAreNonnullByDefault
class CampaignActivationInteractorTest extends AbstractMonetizationTest {

    private static final long CORRECT_PARTNER_ID = 1L;
    private static final long CORRECT_UID = 50L;
    private static final long CORRECT_CAMPAIGN_ID = 11L;
    private static final long WRONG_CAMPAIGN_ID = 99L;

    @Autowired
    private CampaignActivationInteractor campaignActivationInteractor;

    @DisplayName("Активация рекламных кампаний с корректным id.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/changeActivateStatus_existCampaignId_statusChanged_" +
                            "adv_campaign"
            ),
            before = "CampaignActivationInteractor/json/yt/Campaign/" +
                    "changeActivateStatus_existCampaignId_statusChanged.before.json",
            after = "CampaignActivationInteractor/json/yt/Campaign/" +
                    "changeActivateStatus_existCampaignId_statusChanged.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/changeActivateStatus_existCampaignId_statusChanged_" +
                            "adv_campaign_history"
            ),
            before = "CampaignActivationInteractor/json/yt/CampaignHistory/" +
                    "changeActivateStatus_existCampaignId_statusChanged.before.json",
            after = "CampaignActivationInteractor/json/yt/CampaignHistory/" +
                    "changeActivateStatus_existCampaignId_statusChanged.after.json"
    )
    @DbUnitDataSet(
            before = "CampaignActivationInteractor/csv/" +
                    "changeActivateStatus_existCampaignId_statusChanged.before.csv",
            after = "CampaignActivationInteractor/csv/" +
                    "changeActivateStatus_existCampaignId_statusChanged.after.csv"
    )
    @Test
    void changeActivateStatus_existCampaignId_statusChanged() {
        run("changeActivateStatus_existCampaignId_statusChanged_",
                () -> campaignActivationInteractor.changeActivateStatus(CORRECT_CAMPAIGN_ID,
                        CORRECT_PARTNER_ID, CORRECT_UID, true)
        );
    }

    @DisplayName("Активация рекламных кампаний выдаст исключение, так как campaignId нет в таблице.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/changeActivateStatus_notExistCampaignId_gotException_" +
                            "adv_campaign"
            ),
            before = "CampaignActivationInteractor/json/yt/Campaign/" +
                    "changeActivateStatus_notExistCampaignId_gotException.before.json",
            after = "CampaignActivationInteractor/json/yt/Campaign/" +
                    "changeActivateStatus_notExistCampaignId_gotException.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/changeActivateStatus_notExistCampaignId_gotException_" +
                            "adv_campaign_history"
            ),
            before = "CampaignActivationInteractor/json/yt/CampaignHistory/" +
                    "changeActivateStatus_notExistCampaignId_gotException.before.json",
            after = "CampaignActivationInteractor/json/yt/CampaignHistory/" +
                    "changeActivateStatus_notExistCampaignId_gotException.after.json"
    )
    @Test
    void changeActivateStatus_notExistCampaignId_gotException() {
        run("changeActivateStatus_notExistCampaignId_gotException_",
                () -> Assertions.assertThatThrownBy(() ->
                                campaignActivationInteractor.changeActivateStatus(WRONG_CAMPAIGN_ID,
                                        CORRECT_PARTNER_ID, CORRECT_UID, true)
                        )
                        .isInstanceOf(CampaignNotFoundException.class)
        );
    }
}
