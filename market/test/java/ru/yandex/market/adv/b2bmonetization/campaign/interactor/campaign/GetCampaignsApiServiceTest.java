package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.Campaign;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

class GetCampaignsApiServiceTest extends AbstractMonetizationTest {

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignsGet_correctPartnerId_campaignsInfo_adv_campaign"
            ),
            before = "GetCampaignApiService/json/yt/Campaign/" +
                    "v1AdvCampaignsGet_correctPartnerId_campaignsInfo.before.json"
    )
    @DisplayName("Получения информации о кампаниях с корректными id партнера")
    @Test
    void v1AdvCampaignsGet_correctPartnerId_campaignsInfo() {
        run("v1AdvCampaignsGet_correctPartnerId_campaignsInfo_",
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.GET,
                                new URIBuilder()
                                        .setPath("/v1/adv/campaigns")
                                        .addParameters(List.of(
                                                new BasicNameValuePair("uid", "1"),
                                                new BasicNameValuePair("partner_id", "1"),
                                                new BasicNameValuePair("color", "WHITE")
                                        ))
                                        .build()
                                        .toString(),
                                HttpStatus.OK_200,
                                "GetCampaignApiService/json/response/" +
                                        "v1AdvCampaignsGet_correctPartnerId_campaignsInfo.json",
                                null,
                                true
                        );
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }
                }
        );
    }
}
