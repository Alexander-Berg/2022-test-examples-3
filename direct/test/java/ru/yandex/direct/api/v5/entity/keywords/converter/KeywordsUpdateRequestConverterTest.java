package ru.yandex.direct.api.v5.entity.keywords.converter;

import java.util.Collections;
import java.util.Map;

import com.yandex.direct.api.v5.general.AutotargetingCategoriesEnum;
import com.yandex.direct.api.v5.general.AutotargetingCategory;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.keywords.KeywordUpdateItem;
import com.yandex.direct.api.v5.keywords.ObjectFactory;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.keywords.container.UpdateInputItem;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptySet;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class KeywordsUpdateRequestConverterTest {
    private static final long KEYWORD_ID = 22L;
    private static final long AD_GROUP_ID = 33L;
    private static final String PHRASE = "bbb";
    private static final String PARAM1 = "333";
    private static final String PARAM2 = "444";

    private KeywordsUpdateRequestConverter converter;
    private KeywordUpdateItem item;
    private RelevanceMatch relevanceMatch;
    private Map<Long, AdGroup> adGroupsMap;

    @Before
    public void before() {
        converter = new KeywordsUpdateRequestConverter(mock(RelevanceMatchService.class), mock(AdGroupService.class));

        ObjectFactory objectFactory = new ObjectFactory();
        item = new KeywordUpdateItem()
                .withId(KEYWORD_ID)
                .withKeyword(PHRASE)
                .withUserParam1(objectFactory.createKeywordUpdateItemUserParam1(PARAM1))
                .withUserParam2(objectFactory.createKeywordUpdateItemUserParam2(PARAM2));

        relevanceMatch = new RelevanceMatch()
                .withId(KEYWORD_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withHrefParam1(PARAM1)
                .withHrefParam2(PARAM2)
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.exact_mark,
                        RelevanceMatchCategory.accessory_mark));

        adGroupsMap = Map.of(AD_GROUP_ID, new AdGroup()
                .withId(AD_GROUP_ID)
                .withType(AdGroupType.BASE));
    }

    @Test
    public void convertKeywordItem_FullObject() {
        ModelChanges<Keyword> changes = converter.convertKeywordItem(item);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(Keyword.PHRASE)).isEqualTo(PHRASE);
            softly.assertThat(changes.getChangedProp(Keyword.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(Keyword.HREF_PARAM2)).isEqualTo(PARAM2);
        });
    }

    @Test
    public void convertRelevanceMatch_FullObject() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                .withAutotargetingCategories(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.YES),
                        new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                .withValue(YesNoEnum.YES)), relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.exact_mark, RelevanceMatchCategory.accessory_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_AddOneCategoryWithOthers() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.YES)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.exact_mark, RelevanceMatchCategory.accessory_mark,
                            RelevanceMatchCategory.competitor_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_AddOneCategoryWithoutOthers() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                .withValue(YesNoEnum.YES)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.exact_mark, RelevanceMatchCategory.accessory_mark,
                            RelevanceMatchCategory.competitor_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_AddOneCategoryWithFullSetOfCategories() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.NO)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.exact_mark, RelevanceMatchCategory.accessory_mark,
                            RelevanceMatchCategory.competitor_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_RemoveOneCategoryWithOthers() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.accessory_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_RemoveOneCategoryWithoutOthers() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.NO)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.accessory_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_RemoveOneCategoryWithFullSetOfCategories() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.NO)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.accessory_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_RemoveAllCategories() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.NO)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(emptySet());
        });
    }

    @Test
    public void convertRelevanceMatch_AddAllCategories() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.YES)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.accessory_mark, RelevanceMatchCategory.exact_mark,
                            RelevanceMatchCategory.competitor_mark, RelevanceMatchCategory.broader_mark,
                            RelevanceMatchCategory.alternative_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_ReplaceOneCategory() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.YES)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.accessory_mark, RelevanceMatchCategory.competitor_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_ReplaceTwoCategories() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES)),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.alternative_mark, RelevanceMatchCategory.competitor_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_AddOneWithEmptyInDb() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.YES)),
                relevanceMatch.withRelevanceMatchCategories(emptySet()), AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.accessory_mark, RelevanceMatchCategory.exact_mark,
                            RelevanceMatchCategory.competitor_mark, RelevanceMatchCategory.broader_mark,
                            RelevanceMatchCategory.alternative_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_RemoveOneWithEmptyInDb() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.NO)),
                relevanceMatch.withRelevanceMatchCategories(emptySet()), AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.accessory_mark, RelevanceMatchCategory.competitor_mark,
                            RelevanceMatchCategory.broader_mark, RelevanceMatchCategory.alternative_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_AddOneCategory_WrongType() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item
                        .withAutotargetingCategories(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                .withValue(YesNoEnum.YES)),
                relevanceMatch, AdGroupType.MOBILE_CONTENT);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.competitor_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_EmptyAutotargetingCategories() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item.withAutotargetingCategories(),
                relevanceMatch, AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.exact_mark, RelevanceMatchCategory.accessory_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_EmptyAutotargetingCategories_EmptyInDb() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item.withAutotargetingCategories(),
                relevanceMatch.withRelevanceMatchCategories(emptySet()), AdGroupType.BASE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(asSet(RelevanceMatchCategory.accessory_mark, RelevanceMatchCategory.exact_mark,
                            RelevanceMatchCategory.competitor_mark, RelevanceMatchCategory.broader_mark,
                            RelevanceMatchCategory.alternative_mark));
        });
    }

    @Test
    public void convertRelevanceMatch_EmptyAutotargetingCategories_WrongType() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item.withAutotargetingCategories(),
                relevanceMatch, AdGroupType.MOBILE_CONTENT);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(null);
        });
    }

    @Test
    public void convertRelevanceMatch_EmptyAutotargetingCategories_EmptyInDb_WrongType() {
        ModelChanges<RelevanceMatch> changes = converter.convertRelevanceMatch(item.withAutotargetingCategories(),
                relevanceMatch.withRelevanceMatchCategories(emptySet()), AdGroupType.MOBILE_CONTENT);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.getId()).isEqualTo(KEYWORD_ID);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM1)).isEqualTo(PARAM1);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.HREF_PARAM2)).isEqualTo(PARAM2);
            softly.assertThat(changes.getChangedProp(RelevanceMatch.RELEVANCE_MATCH_CATEGORIES))
                    .isEqualTo(null);
        });
    }

    @Test
    public void convertRelevanceMatch() {
        UpdateInputItem inputItem = converter.convertItem(item, Collections.singletonList(relevanceMatch), adGroupsMap);
        assertTrue(inputItem.hasRelevanceMatchChanges());
    }

    @Test
    public void convertKeyword() {
        UpdateInputItem inputItem = converter.convertItem(item,
                Collections.singletonList(relevanceMatch.withId(KEYWORD_ID + 1000)), adGroupsMap);
        assertTrue(inputItem.hasKeywordChanges());
    }

}
