package ru.yandex.direct.api.v5.common;

import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.general.AutotargetingCategoriesEnum;
import com.yandex.direct.api.v5.general.AutotargetingCategory;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.direct.api.v5.common.RelevanceMatchCategoriesConverter.mergeAutotargetingCategoriesFromRequestWithDb;

@RunWith(Parameterized.class)
public class RelevanceMatchCategoriesConverterMergeAutotargetingCategoriesFromRequestWithDbTest {
    @Parameterized.Parameter
    public String desc;

    @Parameterized.Parameter(1)
    public List<AutotargetingCategory> autotargetingCategories;

    @Parameterized.Parameter(2)
    public Set<RelevanceMatchCategory> relevanceMatchCategories;

    @Parameterized.Parameter(3)
    public AdGroupType adGroupType;

    @Parameterized.Parameter(4)
    public Set<RelevanceMatchCategory> expected;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"one category, value = yes, empty in db",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.YES)),
                        emptySet(),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"one category, value = no, empty in db",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"two categories, values = yes, yes, empty in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES)),
                        emptySet(),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"two categories, values = no, no, empty in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"two categories, values = yes, no, empty in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"all categories, all values = yes, empty in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.YES)),
                        emptySet(),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"all categories, all values = no, empty in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.BASE,
                        emptySet()},
                {"all categories, different values, empty in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.accessory_mark)},
                {"empty list, empty in db",
                        emptyList(),
                        emptySet(),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"one category, value = yes, full in db",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.YES)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"one category, value = no, full in db",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.NO)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"two categories, values = yes, yes, full in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"two categories, values = no, no, full in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"two categories, values = yes, no, full in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"all categories, all values = yes, full in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.YES)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"all categories, all values = no, full in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.NO)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        emptySet()},
                {"all categories, different values, full in db",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.NO)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.accessory_mark)},
                {"empty list, full in db",
                        emptyList(),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"one category is absent in db, it in request, value = yes",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.YES)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"one category is absent in db, it in request, value = no",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.NO)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"one category is absent in db, another in request, value = yes",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                .withValue(YesNoEnum.YES)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"one category is absent in db, another in request, value = no",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                .withValue(YesNoEnum.NO)),
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"one category, value = yes, wrong type",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.YES)),
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        asSet(RelevanceMatchCategory.exact_mark)},
                {"one category, value = no, wrong type",
                        List.of(new AutotargetingCategory()
                                .withCategory(AutotargetingCategoriesEnum.EXACT)
                                .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        asSet(RelevanceMatchCategory.exact_mark)},
                {"two categories, values = yes, yes, wrong type",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES)),
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark)},
                {"two categories, values = no, no, wrong type",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark)},
                {"two categories, values = yes, no, wrong type",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark)},
                {"all categories, all values = yes, wrong type",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.YES)),
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"all categories, all values = no, wrong type",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"all categories, different values, wrong type",
                        List.of(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.NO)),
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark)},
                {"empty list, wrong type",
                        emptyList(),
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        null},
        };
    }

    @Test
    public void test() {
        Set<RelevanceMatchCategory> actual = mergeAutotargetingCategoriesFromRequestWithDb(autotargetingCategories,
                relevanceMatchCategories, adGroupType);
        assertThat(actual).isEqualTo(expected);
    }
}
