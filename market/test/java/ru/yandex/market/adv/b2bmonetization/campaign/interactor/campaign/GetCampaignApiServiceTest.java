package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.Campaign;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class GetCampaignApiServiceTest extends AbstractMonetizationTest {

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignCampaignIdGet_correctPartnerIdAndCampaignId_campaignInfo_adv_campaign"
            ),
            before = "GetCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignCampaignIdGet_correctPartnerIdAndCampaignId_campaignInfo.before.json"
    )
    @DbUnitDataSet(
            before = "GetCampaignApiService/csv/" +
                    "v1AdvCampaignCampaignIdGet_correctPartnerIdAndCampaignId_campaignInfo.csv"
    )
    @DisplayName("Получения информации о кампании с корректными id партнера и принадлежащей ему кампании")
    @Test
    void v1AdvCampaignCampaignIdGet_correctPartnerIdAndCampaignId_campaignInfo() {
        run("v1AdvCampaignCampaignIdGet_correctPartnerIdAndCampaignId_campaignInfo_",
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.GET,
                                new URIBuilder()
                                        .setPath(String.format("/v1/adv/campaign/%d", 10L))
                                        .addParameters(List.of(
                                                new BasicNameValuePair("uid", "1"),
                                                new BasicNameValuePair("partner_id", "1"),
                                                new BasicNameValuePair("color", "WHITE")
                                        ))
                                        .build()
                                        .toString(),
                                HttpStatus.OK_200,
                                "GetCampaignApiService/json/response/" +
                                        "v1AdvCampaignCampaignIdGet_correctPartnerIdAndCampaignId_campaignInfo.json",
                                null,
                                true
                        )
                                .andExpect(
                                        jsonPath("$.data.file.url")
                                                .value(Matchers.endsWith("ru/yandex/market/adv/b2bmonetization/1.xlsm"))
                                );
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }
                }
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignCampaignIdGet_campaignDoesNotBelongToPartner_throwException_adv_campaign"
            ),
            before = "GetCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignCampaignIdGet_campaignDoesNotBelongToPartner_throwException.before.json"
    )
    @DisplayName("Получения информации о кампании с корректными id партнера, но не принадлежащей ему кампании")
    @Test
    void v1AdvCampaignCampaignIdGet_campaignDoesNotBelongToPartner_throwException() {
        run("v1AdvCampaignCampaignIdGet_campaignDoesNotBelongToPartner_throwException_",
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.GET,
                                new URIBuilder()
                                        .setPath(String.format("/v1/adv/campaign/%d", 12L))
                                        .addParameters(List.of(
                                                new BasicNameValuePair("uid", "1"),
                                                new BasicNameValuePair("partner_id", "1"),
                                                new BasicNameValuePair("color", "WHITE")
                                        ))
                                        .build()
                                        .toString(),
                                HttpStatus.NOT_FOUND_404,
                                "GetCampaignApiService/json/response/" +
                                        "v1AdvCampaignCampaignIdGet_campaignDoesNotBelongToPartner_throwException.json",
                                null,
                                true
                        );
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
        );
    }
}
