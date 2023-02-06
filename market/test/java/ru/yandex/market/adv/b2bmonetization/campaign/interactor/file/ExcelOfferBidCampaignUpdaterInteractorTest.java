package ru.yandex.market.adv.b2bmonetization.campaign.interactor.file;

import java.io.UncheckedIOException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.Campaign;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.CampaignHistory;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.OfferBids;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.OfferBidsHistory;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

/**
 * Date: 04.03.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
class ExcelOfferBidCampaignUpdaterInteractorTest extends AbstractMonetizationTest {

    @Autowired
    @Qualifier("tmsOfferBidCampaignUpdaterExecutor")
    private Executor tmsOfferBidCampaignUpdaterExecutor;

    @DisplayName("Проверка работоспособности job tmsOfferBidCampaignUpdaterExecutor.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBidsHistory.class,
                    path = "//tmp/update_findFirstFourRow_allSuccess_" +
                            "offer_bids_history",
                    ignoreColumns = "actionId"
            ),
            before = "ExcelOfferBidCampaignUpdaterInteractor/json/offerBidsHistory/" +
                    "update_findFirstFourRow_allSuccess.before.json",
            after = "ExcelOfferBidCampaignUpdaterInteractor/json/offerBidsHistory/" +
                    "update_findFirstFourRow_allSuccess.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBids.class,
                    path = "//tmp/update_findFirstFourRow_allSuccess_" +
                            "offer_bids"
            ),
            before = "ExcelOfferBidCampaignUpdaterInteractor/json/offerBids/" +
                    "update_findFirstFourRow_allSuccess.before.json",
            after = "ExcelOfferBidCampaignUpdaterInteractor/json/offerBids/" +
                    "update_findFirstFourRow_allSuccess.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/update_findFirstFourRow_allSuccess_" +
                            "adv_campaign"
            ),
            before = "ExcelOfferBidCampaignUpdaterInteractor/json/Campaign/" +
                    "update_findFirstFourRow_allSuccess.before.json",
            after = "ExcelOfferBidCampaignUpdaterInteractor/json/Campaign/" +
                    "update_findFirstFourRow_allSuccess.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/update_findFirstFourRow_allSuccess_" +
                            "adv_campaign_history",
                    ignoreColumns = {"actionId"}
            ),
            before = "ExcelOfferBidCampaignUpdaterInteractor/json/CampaignHistory/" +
                    "update_findFirstFourRow_allSuccess.before.json",
            after = "ExcelOfferBidCampaignUpdaterInteractor/json/CampaignHistory/" +
                    "update_findFirstFourRow_allSuccess.after.json"
    )
    @DbUnitDataSet(
            before = "ExcelOfferBidCampaignUpdaterInteractor/csv/" +
                    "update_findFirstFourRow_allSuccess.before.csv",
            after = "ExcelOfferBidCampaignUpdaterInteractor/csv/" +
                    "update_findFirstFourRow_allSuccess.after.csv"
    )
    void update_findFirstFourRow_allSuccess() {
        run("update_findFirstFourRow_allSuccess_",
                () -> tmsOfferBidCampaignUpdaterExecutor.doJob(mockContext())
        );
    }

    @DisplayName("Проверка работоспособности job tmsOfferBidCampaignUpdaterExecutor в случае исключения.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBidsHistory.class,
                    path = "//tmp/update_firstFourRowAndException_throwException_" +
                            "offer_bids_history",
                    ignoreColumns = "actionId"
            ),
            before = "ExcelOfferBidCampaignUpdaterInteractor/json/offerBidsHistory/" +
                    "update_firstFourRowAndException_throwException.before.json",
            after = "ExcelOfferBidCampaignUpdaterInteractor/json/offerBidsHistory/" +
                    "update_firstFourRowAndException_throwException.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBids.class,
                    path = "//tmp/update_firstFourRowAndException_throwException_" +
                            "offer_bids"
            ),
            before = "ExcelOfferBidCampaignUpdaterInteractor/json/offerBids/" +
                    "update_firstFourRowAndException_throwException.before.json",
            after = "ExcelOfferBidCampaignUpdaterInteractor/json/offerBids/" +
                    "update_firstFourRowAndException_throwException.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/update_firstFourRowAndException_throwException_" +
                            "adv_campaign"
            ),
            before = "ExcelOfferBidCampaignUpdaterInteractor/json/Campaign/" +
                    "update_firstFourRowAndException_throwException.before.json",
            after = "ExcelOfferBidCampaignUpdaterInteractor/json/Campaign/" +
                    "update_firstFourRowAndException_throwException.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/update_firstFourRowAndException_throwException_" +
                            "adv_campaign_history",
                    ignoreColumns = {"actionId"}
            ),
            before = "ExcelOfferBidCampaignUpdaterInteractor/json/CampaignHistory/" +
                    "update_firstFourRowAndException_throwException.before.json",
            after = "ExcelOfferBidCampaignUpdaterInteractor/json/CampaignHistory/" +
                    "update_firstFourRowAndException_throwException.after.json"
    )
    @DbUnitDataSet(
            before = "ExcelOfferBidCampaignUpdaterInteractor/csv/" +
                    "update_firstFourRowAndException_throwException.before.csv",
            after = "ExcelOfferBidCampaignUpdaterInteractor/csv/" +
                    "update_firstFourRowAndException_throwException.after.csv"
    )
    void update_firstFourRowAndException_throwException() {
        run("update_firstFourRowAndException_throwException_",
                () -> Assertions.assertThatThrownBy(() -> tmsOfferBidCampaignUpdaterExecutor.doJob(mockContext()))
                        .isInstanceOf(UncheckedIOException.class)
        );
    }
}
