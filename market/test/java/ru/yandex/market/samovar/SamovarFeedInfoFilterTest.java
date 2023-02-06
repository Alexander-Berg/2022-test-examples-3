package ru.yandex.market.samovar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Date: 16.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(before = "SamovarFeedInfoFilterTest.before.csv")
class SamovarFeedInfoFilterTest extends FunctionalTest {

    @Autowired
    private SamovarFeedInfoFilter samovarFeedInfoFilter;

    @SuppressWarnings({"unused", "checkstyle:parameterNumber"})
    @ParameterizedTest(name = "{0}")
    @DisplayName("Проверка фида на валидность.")
    @CsvFileSource(resources = "csv/test.source.csv", numLinesToSkip = 1)
    void test(String name, boolean expected, long feedId, long partnerId, String url,
              CampaignType campaignType, EnvironmentType environmentType, boolean own) {
        assertThat(
                samovarFeedInfoFilter.test(
                        createSamovarFeedInfo(feedId, partnerId, url, campaignType, environmentType, own)
                )
        ).isEqualTo(expected);
    }

    private SamovarFeedInfo createSamovarFeedInfo(long feedId, long partnerId, String url, CampaignType campaignType,
                                                  EnvironmentType environmentType, boolean own) {
        return new SamovarFeedInfo.Builder()
                .setOrigin(environmentType)
                .setOwn(own)
                .setCampaignType(campaignType)
                .setFeedInfo(SamovarContextOuterClass.FeedInfo.newBuilder()
                        .setFeedId(feedId)
                        .setUrl(url)
                        .setShopId(partnerId)
                        .build())
                .build();
    }
}
