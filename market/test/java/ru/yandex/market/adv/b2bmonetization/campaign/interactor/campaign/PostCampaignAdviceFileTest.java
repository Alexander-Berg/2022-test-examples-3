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

/**
 * Date: 04.07.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint POST /v1/adv/campaign/advice/file.")
@Disabled
@ParametersAreNonnullByDefault
public class PostCampaignAdviceFileTest extends AbstractMonetizationTest {

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/getAdviceFile_dbs_correctFile_" +
                            "shop_offer"
            ),
            before = "PostCampaignAdviceFile/json/yt/shopOffer/" +
                    "getAdviceFile_dbs_correctFile.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/getAdviceFile_dbs_correctFile_" +
                            "price_recommendations"
            ),
            before = "PostCampaignAdviceFile/json/yt/priceRecommendations/" +
                    "getAdviceFile_dbs_correctFile.json"
    )
    @DbUnitDataSet(
            after = "PostCampaignAdviceFile/csv/getAdviceFile_dbs_correctFile.after.csv"
    )
    @DisplayName("Файл с рекомендациями для DBS партнера по всем кампании вернулся успешно.")
    @Test
    void getAdviceFile_dbs_correctFile() {
        getAdviceFile("getAdviceFile_dbs_correctFile", "523", "WHITE");
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/getAdviceFile_fb_correctFile_" +
                            "blue_shop_offer"
            ),
            before = "PostCampaignAdviceFile/json/yt/blueShopOffer/" +
                    "getAdviceFile_fb_correctFile.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/getAdviceFile_fb_correctFile_" +
                            "price_recommendations"
            ),
            before = "PostCampaignAdviceFile/json/yt/priceRecommendations/" +
                    "getAdviceFile_fb_correctFile.json"
    )
    @DbUnitDataSet(
            after = "PostCampaignAdviceFile/csv/getAdviceFile_fb_correctFile.after.csv"
    )
    @DisplayName("Файл с рекомендациями для FB* партнера по всем кампании вернулся успешно.")
    @Test
    void getAdviceFile_fb_correctFile() {
        getAdviceFile("getAdviceFile_fb_correctFile", "524", "BLUE");
    }

    private void getAdviceFile(String testName, String partnerId, String color) {
        run(testName + "_",
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.POST,
                                new URIBuilder()
                                        .setPath("/v1/adv/campaign/advice/file")
                                        .addParameters(
                                                List.of(
                                                        new BasicNameValuePair("uid", "904"),
                                                        new BasicNameValuePair("partner_id", partnerId),
                                                        new BasicNameValuePair("color", color)
                                                )
                                        )
                                        .build()
                                        .toString(),
                                HttpStatus.OK_200,
                                "PostCampaignAdviceFile/json/response/" + testName + ".json",
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
