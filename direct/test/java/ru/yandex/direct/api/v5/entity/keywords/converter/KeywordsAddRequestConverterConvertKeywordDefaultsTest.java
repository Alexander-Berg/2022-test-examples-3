package ru.yandex.direct.api.v5.entity.keywords.converter;

import java.math.BigDecimal;
import java.util.Arrays;

import com.yandex.direct.api.v5.keywords.KeywordAddItem;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;

import static org.mockito.Mockito.mock;


@RunWith(Parameterized.class)
public class KeywordsAddRequestConverterConvertKeywordDefaultsTest {
    private static final long ADGROUP_ID = 22L;
    private static final String PHRASE = "bbb";
    private static final String PARAM1 = "333";
    private static final String PARAM2 = "444";

    private Keyword keyword;

    @Parameterized.Parameter
    public boolean isAutobudget;

    @Parameterized.Parameter(1)
    public boolean isDifferentPlaces;

    @Parameterized.Parameter(2)
    public CampaignsPlatform platform;

    @Parameterized.Parameter(3)
    public boolean priceIsNull;

    @Parameterized.Parameter(4)
    public boolean priceContextIsNull;

    @Parameterized.Parameters(name = "{index}: isAutobudget={0}, isDifferentPlaces={1}, platform={2}, "
            + "priceIsNull={3}, priceContextIsNull={4}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {false, false, CampaignsPlatform.BOTH, false, true},
                {false, false, CampaignsPlatform.SEARCH, false, true},
                {false, false, CampaignsPlatform.CONTEXT, true, true},
                {false, true, CampaignsPlatform.BOTH, false, false},
                {false, true, CampaignsPlatform.SEARCH, false, true},
                {false, true, CampaignsPlatform.CONTEXT, true, false},
                {true, false, CampaignsPlatform.BOTH, true, true},
                {true, false, CampaignsPlatform.SEARCH, true, true},
                {true, false, CampaignsPlatform.CONTEXT, true, true},
                {true, true, CampaignsPlatform.BOTH, true, true},
                {true, true, CampaignsPlatform.SEARCH, true, true},
                {true, true, CampaignsPlatform.CONTEXT, true, true},
        });
    }

    @Before
    public void before() {
        KeywordsAddRequestConverter converter =
                new KeywordsAddRequestConverter(mock(ClientService.class), mock(AdGroupService.class),
                        mock(FeatureService.class));

        KeywordAddItem item = new KeywordAddItem()
                .withAdGroupId(ADGROUP_ID)
                .withKeyword(PHRASE)
                .withStrategyPriority(null)
                .withBid(null)
                .withContextBid(null)
                .withUserParam1(PARAM1)
                .withUserParam2(PARAM2);
        item.setContextBid(null);

        DbStrategy strategy = new DbStrategy();
        if (isAutobudget) {
            strategy.withAutobudget(CampaignsAutobudget.YES);
        } else {
            strategy.withAutobudget(CampaignsAutobudget.NO);
        }
        if (isDifferentPlaces) {
            strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        } else {
            strategy.setStrategy(null);
        }
        strategy.withPlatform(platform);

        keyword = converter.convertKeywordItem(item, strategy, BigDecimal.TEN).getKeyword();
    }

    @Test
    public void check() {
        SoftAssertions.assertSoftly(softly -> {
            if (priceIsNull) {
                softly.assertThat(keyword.getPrice())
                        .as("Price must not be set")
                        .isNull();
            } else {
                softly.assertThat(keyword.getPrice())
                        .as("Default value must be set for price")
                        .isNotNull();
            }
            if (priceContextIsNull) {
                softly.assertThat(keyword.getPriceContext())
                        .as("PriceContext must not be set")
                        .isNull();
            } else {
                softly.assertThat(keyword.getPriceContext())
                        .as("Default value must be set for priceContext")
                        .isNotNull();
            }
            softly.assertThat(keyword.getAutobudgetPriority())
                    .as("Default value must be set for autobudgetPriority")
                    .isNotNull();
        });
    }
}
