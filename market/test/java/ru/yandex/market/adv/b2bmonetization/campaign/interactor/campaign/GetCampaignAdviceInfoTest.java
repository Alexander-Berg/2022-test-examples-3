package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.PriceRecommendation;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;


@DisplayName("Тесты на endpoint GET /v1/adv/campaign/{campaignId}/advice/info.")
@ParametersAreNonnullByDefault
public class GetCampaignAdviceInfoTest extends AbstractMonetizationTest {

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/getAdviceCalculationTime_recommendationsExist_timeReturned_price_recommendations"
            ),
            before = "GetCampaignAdviceCalculationTime/json/yt/" +
                    "getAdviceCalculationTime_recommendationsExist_timeReturned_recommendations.json"
    )
    @DisplayName("Возвращает время, в которое были рассчитаны рекомендации для кампании")
    @Test
    void getRecommendationInfo_recommendationsExist_timeReturned() {
        String testName = "getAdviceCalculationTime_recommendationsExist_timeReturned";

        run(testName + "_",
                () -> {
                    try {
                        mvcPerform(
                                HttpMethod.GET,
                                new URIBuilder()
                                        .setPath("/v1/adv/campaign/1/advice/info")
                                        .addParameters(List.of(
                                                new BasicNameValuePair("uid", "1"),
                                                new BasicNameValuePair("partner_id", "1"),
                                                new BasicNameValuePair("color", "WHITE"),
                                                new BasicNameValuePair("campaign_version", "PL")
                                        ))
                                        .build()
                                        .toString(),
                                HttpStatus.OK_200,
                                "GetCampaignAdviceCalculationTime/json/response/" + testName + "_ok.json",
                                null,
                                true
                        );

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/adv_unittest/price_recommendations_grouped_diff/2022-07-21T12:00:18.02"
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/adv_unittest/price_recommendations_grouped_diff/2022-07-22T06:00:11.157"
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/adv_unittest/price_recommendations_grouped_diff/latest"
            )
    )
    @DisplayName("Возвращает время, в которое были рассчитаны рекомендации для кампании по diff таблицам")
    @Test
    void getRecommendationInfo_recommendationsExistWithDiff_timeReturned() {
        try {
            mvcPerform(
                    HttpMethod.GET,
                    new URIBuilder()
                            .setPath("/v1/adv/campaign/1/advice/info")
                            .addParameters(List.of(
                                    new BasicNameValuePair("uid", "1"),
                                    new BasicNameValuePair("partner_id", "1"),
                                    new BasicNameValuePair("color", "WHITE"),
                                    new BasicNameValuePair("campaign_version", "PL")
                            ))
                            .build()
                            .toString(),
                    HttpStatus.OK_200,
                    "GetCampaignAdviceCalculationTime/json/response/" +
                            "getRecommendationInfo_recommendationsExistWithDiff_timeReturned.json",
                    null,
                    true
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
