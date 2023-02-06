package ru.yandex.market.core.samovar.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feed.model.FeedType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Date: 14.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(before = "SamovarFeedValidationServiceImplTest.before.csv")
class SamovarFeedValidationServiceImplTest extends FunctionalTest {

    @Autowired
    private SamovarFeedValidationService samovarFeedValidationService;

    @SuppressWarnings("unused")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Проверка фида на валидность.")
    @CsvFileSource(resources = "csv/test.source.csv", numLinesToSkip = 1)
    void validate(String name, boolean expected,
                  long feedId, long partnerId, String url,
                  CampaignType campaignType, FeedType feedType) {
        assertThat(samovarFeedValidationService.validate(feedId, partnerId, url, campaignType, feedType))
                .isEqualTo(expected);
    }
}
