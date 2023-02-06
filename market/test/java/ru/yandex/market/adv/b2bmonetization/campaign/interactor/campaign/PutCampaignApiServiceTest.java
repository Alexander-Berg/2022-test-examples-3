package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.Nonnull;
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
import ru.yandex.mj.generated.server.model.Color;

@DisplayName("Тесты на endpoint PUT /v1/adv/campaign/activation/{campaign_id}.")
@ParametersAreNonnullByDefault
class PutCampaignApiServiceTest extends AbstractMonetizationTest {

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_campaignDoesNotBelongToPartner_throwException_" +
                            "adv_campaign"
            ),
            before = "PutCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_campaignDoesNotBelongToPartner_throwException.before.json",
            after = "PutCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_campaignDoesNotBelongToPartner_throwException.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_campaignDoesNotBelongToPartner_throwException_" +
                            "adv_campaign_history"
            ),
            before = "PutCampaignApiService/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_campaignDoesNotBelongToPartner_throwException.before.json",
            after = "PutCampaignApiService/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_campaignDoesNotBelongToPartner_throwException.after.json"
    )
    @DisplayName("Запрос на изменение кампании не принадлежащей партнеру")
    @Test
    void v1AdvCampaignActivationCampaignIdPut_campaignDoesNotBelongToPartner_throwException() {
        run("v1AdvCampaignActivationCampaignIdPut_campaignDoesNotBelongToPartner_throwException_",
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.PUT,
                                getUrlTemplate(10L, 2L, 2L, Color.valueOf("WHITE"), true),
                                HttpStatus.NOT_FOUND_404,
                                "PutCampaignApiService/json/response/v1AdvCampaignActivationCampaignIdPut" +
                                        "_campaignDoesNotBelongToPartner_throwException.json",
                                null,
                                true
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
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_excelFileNotExisted_notFound_" +
                            "adv_campaign"
            ),
            before = "PutCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_excelFileNotExisted_notFound.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_excelFileNotExisted_notFound_" +
                            "adv_campaign_history"
            ),
            before = "PutCampaignApiService/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_excelFileNotExisted_notFound.before.json"
    )
    @DisplayName("Запрос на изменение кампании, по которой не найден excel файл")
    @Test
    void v1AdvCampaignActivationCampaignIdPut_excelFileNotExisted_notFound() {
        run("v1AdvCampaignActivationCampaignIdPut_excelFileNotExisted_notFound_",
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.PUT,
                                getUrlTemplate(11L, 1L, 1L, Color.valueOf("WHITE"), true),
                                HttpStatus.NOT_FOUND_404,
                                "PutCampaignApiService/json/response/" +
                                        "v1AdvCampaignActivationCampaignIdPut_excelFileNotExisted_notFound.json",
                                null,
                                true
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
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_activating_updateFields_" +
                            "adv_campaign"
            ),
            before = "PutCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_activating_updateFields.before.json",
            after = "PutCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_activating_updateFields.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_activating_updateFields_" +
                            "adv_campaign_history",
                    ignoreColumns = {"actionId"}
            ),
            before = "PutCampaignApiService/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_activating_updateFields.before.json",
            after = "PutCampaignApiService/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_activating_updateFields.after.json"
    )
    @DbUnitDataSet(
            before = "PutCampaignApiService/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_activating_updateFields.before.csv",
            after = "PutCampaignApiService/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_activating_updateFields.after.csv"
    )
    @DisplayName("Изменение записи о рекламной кампании при активации")
    @Test
    void v1AdvCampaignActivationCampaignIdPut_activating_updateFields() {
        putOk("v1AdvCampaignActivationCampaignIdPut_activating_updateFields_",
                10, 1, 1, Color.valueOf("WHITE"), true);
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields_" +
                            "adv_campaign"
            ),
            before = "PutCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields.before.json",
            after = "PutCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields_" +
                            "adv_campaign_history",
                    ignoreColumns = {"actionId"}
            ),
            before = "PutCampaignApiService/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields.before.json",
            after = "PutCampaignApiService/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields.after.json"
    )
    @DbUnitDataSet(
            before = "PutCampaignApiService/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields.before.csv",
            after = "PutCampaignApiService/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields.after.csv"
    )
    @DisplayName("Изменение записи о рекламной кампании при деактивации")
    @Test
    void v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields() {
        putOk("v1AdvCampaignActivationCampaignIdPut_deactivating_updateFields_",
                12, 2, 2, Color.valueOf("BLUE"), false);
    }

    private void putOk(String tablePrefix, long campaignId, long uid, long partnerId,
                       Color color, boolean active) {
        run(tablePrefix,
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.PUT,
                                getUrlTemplate(campaignId, uid, partnerId, color, active),
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

    /**
     * Собираем шаблон url
     *
     * @param campaignId идентификатор кампании
     * @param uid        идентификатор пользователя
     * @param partnerId  идентификатор партнера
     * @param color      цвет
     * @param active     флаг активации
     * @return url
     */
    @Nonnull
    private String getUrlTemplate(long campaignId, long uid, long partnerId,
                                  Color color, boolean active) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setPath(String.format("/v1/adv/campaign/activation/%d", campaignId));
        builder.addParameters(List.of(
                new BasicNameValuePair("uid", Long.toString(uid)),
                new BasicNameValuePair("partner_id", Long.toString(partnerId)),
                new BasicNameValuePair("color", color.toString()),
                new BasicNameValuePair("active", Boolean.toString(active))
        ));
        return builder.build().toString();
    }
}
