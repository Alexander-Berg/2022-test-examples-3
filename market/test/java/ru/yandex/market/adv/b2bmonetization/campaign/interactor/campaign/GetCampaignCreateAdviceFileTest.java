package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.Matchers;
import org.hamcrest.number.OrderingComparison;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.PriceRecommendation;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.ShopOffer;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@DisplayName("Тесты на endpoint GET /v1/adv/campaign/{campaignId}/advice/file.")
@Disabled
@ParametersAreNonnullByDefault
class GetCampaignCreateAdviceFileTest extends AbstractMonetizationTest {

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/getAdviceFile_dbsAdv_correctFile_" +
                            "shop_offer"
            ),
            before = "GetCampaignCreateAdviceFile/json/yt/shopOffer/" +
                    "getAdviceFile_dbsAdv_correctFile.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/getAdviceFile_dbsAdv_correctFile_" +
                            "price_recommendations"
            ),
            before = "GetCampaignCreateAdviceFile/json/yt/priceRecommendations/" +
                    "getAdviceFile_dbsAdv_correctFile.json"
    )
    @DbUnitDataSet(
            after = "GetCampaignCreateAdviceFile/csv/getAdviceFile_dbsAdv_correctFile.after.csv"
    )
    @DisplayName("Файл с рекомендациями для DBS партнера по ADV кампании вернулся успешно.")
    @Test
    void getAdviceFile_dbsAdv_correctFile() {
        getAdviceFile("getAdviceFile_dbsAdv_correctFile", "523", "WHITE", "631", "ADV");
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/getAdviceFile_dbsPl_correctFile_" +
                            "shop_offer"
            ),
            before = "GetCampaignCreateAdviceFile/json/yt/shopOffer/" +
                    "getAdviceFile_dbsPl_correctFile.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/getAdviceFile_dbsPl_correctFile_" +
                            "price_recommendations"
            ),
            before = "GetCampaignCreateAdviceFile/json/yt/priceRecommendations/" +
                    "getAdviceFile_dbsPl_correctFile.json"
    )
    @DbUnitDataSet(
            after = "GetCampaignCreateAdviceFile/csv/getAdviceFile_dbsPl_correctFile.after.csv"
    )
    @DisplayName("Файл с рекомендациями для DBS партнера по PL кампании вернулся успешно.")
    @Test
    void getAdviceFile_dbsPl_correctFile() {
        getAdviceFile("getAdviceFile_dbsPl_correctFile", "523", "WHITE", "632", "PL");
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/getAdviceFile_fbAdv_correctFile_" +
                            "blue_shop_offer"
            ),
            before = "GetCampaignCreateAdviceFile/json/yt/blueShopOffer/" +
                    "getAdviceFile_fbAdv_correctFile.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/getAdviceFile_fbAdv_correctFile_" +
                            "price_recommendations"
            ),
            before = "GetCampaignCreateAdviceFile/json/yt/priceRecommendations/" +
                    "getAdviceFile_fbAdv_correctFile.json"
    )
    @DbUnitDataSet(
            after = "GetCampaignCreateAdviceFile/csv/getAdviceFile_fbAdv_correctFile.after.csv"
    )
    @DisplayName("Файл с рекомендациями для FB* партнера по ADV кампании вернулся успешно.")
    @Test
    void getAdviceFile_fbAdv_correctFile() {
        getAdviceFile("getAdviceFile_fbAdv_correctFile", "524", "BLUE", "631", "ADV");
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/getAdviceFile_fbPl_correctFile_" +
                            "blue_shop_offer"
            ),
            before = "GetCampaignCreateAdviceFile/json/yt/blueShopOffer/" +
                    "getAdviceFile_fbPl_correctFile.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/getAdviceFile_fbPl_correctFile_" +
                            "price_recommendations"
            ),
            before = "GetCampaignCreateAdviceFile/json/yt/priceRecommendations/" +
                    "getAdviceFile_fbPl_correctFile.json"
    )
    @DbUnitDataSet(
            after = "GetCampaignCreateAdviceFile/csv/getAdviceFile_fbPl_correctFile.after.csv"
    )
    @DisplayName("Файл с рекомендациями для FB* партнера по PL кампании вернулся успешно.")
    @Test
    void getAdviceFile_fbPl_correctFile() {
        getAdviceFile("getAdviceFile_fbPl_correctFile", "524", "BLUE", "632", "PL");
    }

    private void getAdviceFile(String testName, String partnerId, String color,
                               String campaignId, String campaignVersion) {
        run(testName + "_",
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.GET,
                                new URIBuilder()
                                        .setPath("/v1/adv/campaign/" + campaignId + "/advice/file")
                                        .addParameters(
                                                List.of(
                                                        new BasicNameValuePair("uid", "904"),
                                                        new BasicNameValuePair("partner_id", partnerId),
                                                        new BasicNameValuePair("color", color),
                                                        new BasicNameValuePair("campaign_version", campaignVersion)
                                                )
                                        )
                                        .build()
                                        .toString(),
                                HttpStatus.OK_200,
                                "GetCampaignCreateAdviceFile/json/response/" + testName + ".json",
                                null,
                                true
                        )
                                .andExpect(jsonPath("$.id")
                                        .value(OrderingComparison.greaterThanOrEqualTo(1))
                                )
                                .andExpect(jsonPath("$.url")
                                        .value(
                                                Matchers.endsWith("ru/yandex/market/adv/b2bmonetization/" +
                                                        "1-marketplace-recommended-prices-promocodes.xlsm")
                                        )
                                );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}
