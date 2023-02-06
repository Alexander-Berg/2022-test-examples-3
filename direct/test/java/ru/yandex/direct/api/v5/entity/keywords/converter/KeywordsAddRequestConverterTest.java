package ru.yandex.direct.api.v5.entity.keywords.converter;

import java.math.BigDecimal;

import com.yandex.direct.api.v5.general.AutotargetingCategoriesEnum;
import com.yandex.direct.api.v5.general.AutotargetingCategory;
import com.yandex.direct.api.v5.general.PriorityEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.keywords.KeywordAddItem;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.common.ConverterUtils.convertStrategyPriority;
import static ru.yandex.direct.api.v5.common.ConverterUtils.convertToDbPrice;

public class KeywordsAddRequestConverterTest {
    private static final long ADGROUP_ID = 22L;
    private static final String PHRASE = "bbb";
    private static final String PARAM1 = "333";
    private static final String PARAM2 = "444";
    private static final PriorityEnum PRIORITY = PriorityEnum.HIGH;
    private static final Long BID = 5000000L;
    private static final Long CONTEXT_BID = 4000000L;
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.TEN;

    private KeywordsAddRequestConverter converter;
    private KeywordAddItem item;

    @Before
    public void before() {
        converter = new KeywordsAddRequestConverter(mock(ClientService.class), mock(AdGroupService.class),
                mock(FeatureService.class));

        item = new KeywordAddItem()
                .withAdGroupId(ADGROUP_ID)
                .withKeyword(PHRASE)
                .withStrategyPriority(PRIORITY)
                .withBid(BID)
                .withContextBid(CONTEXT_BID)
                .withUserParam1(PARAM1)
                .withUserParam2(PARAM2)
                .withAutotargetingCategories(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.YES),
                        new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                .withValue(YesNoEnum.NO));
    }

    @Test
    public void convertKeywordItem_FullObject() {
        Keyword keyword = converter.convertKeywordItem(item, null, DEFAULT_PRICE).getKeyword();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(keyword.getAdGroupId()).isEqualTo(ADGROUP_ID);
            softly.assertThat(keyword.getPhrase()).isEqualTo(PHRASE);
            softly.assertThat(keyword.getAutobudgetPriority())
                    .isEqualTo(convertStrategyPriority(PRIORITY));
            softly.assertThat(keyword.getPrice()).isEqualTo(convertToDbPrice(BID));
            softly.assertThat(keyword.getPriceContext()).isEqualTo(convertToDbPrice(CONTEXT_BID));
            softly.assertThat(keyword.getHrefParam1()).isEqualTo(PARAM1);
            softly.assertThat(keyword.getHrefParam2()).isEqualTo(PARAM2);
        });
    }

    @Test
    public void convertRelevanceMatchItem_FullObject_BaseType() {
        RelevanceMatch relevanceMatch = converter.convertRelevanceMatchItem(
                item, null, BigDecimal.TEN, false, AdGroupType.BASE).getRelevanceMatch();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(relevanceMatch.getAdGroupId()).isEqualTo(ADGROUP_ID);
            softly.assertThat(relevanceMatch.getAutobudgetPriority())
                    .isEqualTo(convertStrategyPriority(PRIORITY));
            softly.assertThat(relevanceMatch.getPrice()).isEqualTo(convertToDbPrice(BID));
            softly.assertThat(relevanceMatch.getPriceContext()).isEqualTo(convertToDbPrice(CONTEXT_BID));
            softly.assertThat(relevanceMatch.getHrefParam1()).isEqualTo(PARAM1);
            softly.assertThat(relevanceMatch.getHrefParam2()).isEqualTo(PARAM2);
            softly.assertThat(relevanceMatch.getRelevanceMatchCategories())
                    .isEqualTo(asSet(RelevanceMatchCategory.alternative_mark, RelevanceMatchCategory.exact_mark,
                            RelevanceMatchCategory.broader_mark, RelevanceMatchCategory.competitor_mark));
        });
    }

    @Test
    public void convertRelevanceMatchItem_FullObject_MobileContentType() {
        RelevanceMatch relevanceMatch = converter.convertRelevanceMatchItem(
                item, null, BigDecimal.TEN, false, AdGroupType.MOBILE_CONTENT).getRelevanceMatch();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(relevanceMatch.getAdGroupId()).isEqualTo(ADGROUP_ID);
            softly.assertThat(relevanceMatch.getAutobudgetPriority())
                    .isEqualTo(convertStrategyPriority(PRIORITY));
            softly.assertThat(relevanceMatch.getPrice()).isEqualTo(convertToDbPrice(BID));
            softly.assertThat(relevanceMatch.getPriceContext()).isEqualTo(convertToDbPrice(CONTEXT_BID));
            softly.assertThat(relevanceMatch.getHrefParam1()).isEqualTo(PARAM1);
            softly.assertThat(relevanceMatch.getHrefParam2()).isEqualTo(PARAM2);
            softly.assertThat(relevanceMatch.getRelevanceMatchCategories())
                    .isEqualTo(asSet(RelevanceMatchCategory.exact_mark, RelevanceMatchCategory.accessory_mark));
        });
    }

    @Test
    public void convertRelevanceMatchItem_RelevanceMatchSupportContextPrice_FillEmptyContextPrice() {
        DbStrategy strategy = new DbStrategy();
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        strategy.withPlatform(CampaignsPlatform.BOTH);
        RelevanceMatch relevanceMatch = converter.convertRelevanceMatchItem(
                item.withContextBid(null), strategy, DEFAULT_PRICE, true, AdGroupType.BASE).getRelevanceMatch();
        assertThat(relevanceMatch.getPriceContext()).isEqualTo(DEFAULT_PRICE);
    }
}
