package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.Campaign;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.CampaignHistory;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 17.03.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
@DisplayName("Тесты на endpoint DELETE /v1/adv/campaign/{campaignId}.")
class DeleteAdvCampaignTest extends AbstractMonetizationTest {

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignCampaignIdDelete_correctCampaign_noContent_" +
                            "adv_campaign"
            ),
            before = "DeleteAdvCampaignTest/json/yt/Campaign/" +
                    "v1AdvCampaignCampaignIdDelete_correctCampaign_noContent.before.json",
            after = "DeleteAdvCampaignTest/json/yt/Campaign/" +
                    "v1AdvCampaignCampaignIdDelete_correctCampaign_noContent.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignCampaignIdDelete_correctCampaign_noContent_" +
                            "adv_campaign_history",
                    ignoreColumns = "actionId"
            ),
            before = "DeleteAdvCampaignTest/json/yt/CampaignHistory/" +
                    "v1AdvCampaignCampaignIdDelete_correctCampaign_noContent.before.json",
            after = "DeleteAdvCampaignTest/json/yt/CampaignHistory/" +
                    "v1AdvCampaignCampaignIdDelete_correctCampaign_noContent.after.json"
    )
    @DbUnitDataSet(
            before = "DeleteAdvCampaignTest/csv/" +
                    "v1AdvCampaignCampaignIdDelete_correctCampaign_noContent.before.csv",
            after = "DeleteAdvCampaignTest/csv/" +
                    "v1AdvCampaignCampaignIdDelete_correctCampaign_noContent.after.csv"
    )
    @DisplayName("Запрос на удаление рекламной кампании партнера завершился успехом.")
    @Test
    public void v1AdvCampaignCampaignIdDelete_correctCampaign_noContent() {
        run("v1AdvCampaignCampaignIdDelete_correctCampaign_noContent_",
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.DELETE,
                                new URIBuilder()
                                        .setPath("/v1/adv/campaign/412")
                                        .addParameters(
                                                List.of(
                                                        new BasicNameValuePair("uid", "142"),
                                                        new BasicNameValuePair("partner_id", "543"),
                                                        new BasicNameValuePair("color", "WHITE")
                                                )
                                        )
                                        .build()
                                        .toString(),
                                HttpStatus.NO_CONTENT_204,
                                null,
                                null,
                                false
                        );
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignCampaignIdDelete_wrongCampaign_badRequest_" +
                            "adv_campaign"
            ),
            before = "DeleteAdvCampaignTest/json/yt/Campaign/" +
                    "v1AdvCampaignCampaignIdDelete_wrongCampaign_badRequest.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignCampaignIdDelete_wrongCampaign_badRequest_" +
                            "adv_campaign_history"
            ),
            before = "DeleteAdvCampaignTest/json/yt/CampaignHistory/" +
                    "v1AdvCampaignCampaignIdDelete_wrongCampaign_badRequest.before.json"
    )
    @DisplayName("Не нашли рекламную кампанию у партнера, вернулось 404.")
    @Test
    void v1AdvCampaignCampaignIdDelete_wrongCampaign_badRequest() {
        run("v1AdvCampaignCampaignIdDelete_wrongCampaign_badRequest_",
                () -> post("412", "412", 404, "v1AdvCampaignCampaignIdDelete_wrongCampaign_badRequest")
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignCampaignIdDelete_wrongFile_badRequest_" +
                            "adv_campaign"
            ),
            before = "DeleteAdvCampaignTest/json/yt/Campaign/" +
                    "v1AdvCampaignCampaignIdDelete_wrongFile_badRequest.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignCampaignIdDelete_wrongFile_badRequest_" +
                            "adv_campaign_history"
            ),
            before = "DeleteAdvCampaignTest/json/yt/CampaignHistory/" +
                    "v1AdvCampaignCampaignIdDelete_wrongFile_badRequest.json"
    )
    @DisplayName("Не нашли excel файл по рекламной кампании, вернулось 404.")
    @Test
    void v1AdvCampaignCampaignIdDelete_wrongFile_badRequest() {
        run("v1AdvCampaignCampaignIdDelete_wrongFile_badRequest_",
                () -> post("412", "543", 404, "v1AdvCampaignCampaignIdDelete_wrongFile_badRequest")
        );
    }

    @DisplayName("При неверных входных параметрах, вернулось 400.")
    @Test
    void v1AdvCampaignCampaignIdDelete_wrongParameters_badRequest() {
        run("v1AdvCampaignCampaignIdDelete_wrongParameters_badRequest_",
                () -> post("10", "gs", 400, "v1AdvCampaignCampaignIdDelete_wrongParameters_badRequest")
        );
    }

    private void post(String campaignId, String partnerId, int status, String testName) {
        try {
            mvcPerform(
                    HttpMethod.DELETE,
                    new URIBuilder()
                            .setPath("/v1/adv/campaign/" + campaignId)
                            .addParameters(
                                    List.of(
                                            new BasicNameValuePair("uid", "4182"),
                                            new BasicNameValuePair("partner_id", partnerId),
                                            new BasicNameValuePair("color", "WHITE")
                                    )
                            )
                            .build()
                            .toString(),
                    status,
                    "DeleteAdvCampaignTest/json/response/" + testName + ".json",
                    null,
                    true
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
