package ru.yandex.direct.api.v5.common;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.general.AutotargetingCategoriesEnum;
import com.yandex.direct.api.v5.general.AutotargetingCategory;
import com.yandex.direct.api.v5.general.AutotargetingCategoryArray;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertNull;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.api.v5.common.RelevanceMatchCategoriesConverter.autotargetingCategoriesFromCore;

@RunWith(Parameterized.class)
public class RelevanceMatchCategoriesConverterAutotargetingCategoriesFromCoreTest {

    private static final Comparator<AutotargetingCategory> AUTOTARGETING_CATEGORY_COMPARATOR =
            Comparator.comparing(AutotargetingCategory::getCategory);

    @Parameterized.Parameter
    public String desc;

    @Parameterized.Parameter(1)
    public Set<RelevanceMatchCategory> relevanceMatchCategories;

    @Parameterized.Parameter(2)
    public AdGroupType adGroupType;

    @Parameterized.Parameter(3)
    public List<AutotargetingCategory> expected;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"one category",
                        asSet(RelevanceMatchCategory.exact_mark),
                        AdGroupType.BASE,
                        asList(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
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
                                        .withValue(YesNoEnum.NO))},
                {"two categories",
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark),
                        AdGroupType.BASE,
                        asList(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.NO))},
                {"all categories",
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.BASE,
                        asList(new AutotargetingCategory()
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
                                        .withValue(YesNoEnum.YES))},
                {"empty in db",
                        emptySet(),
                        AdGroupType.BASE,
                        asList(new AutotargetingCategory()
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
                                        .withValue(YesNoEnum.YES))},
                {"one category, wrong type",
                        asSet(RelevanceMatchCategory.exact_mark),
                        AdGroupType.MOBILE_CONTENT,
                        null},
                {"two categories, wrong type",
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark),
                        AdGroupType.MOBILE_CONTENT,
                        null},
                {"all categories, wrong type",
                        asSet(RelevanceMatchCategory.alternative_mark,
                                RelevanceMatchCategory.exact_mark,
                                RelevanceMatchCategory.broader_mark,
                                RelevanceMatchCategory.accessory_mark,
                                RelevanceMatchCategory.competitor_mark),
                        AdGroupType.MOBILE_CONTENT,
                        null},
                {"empty in db, wrong type",
                        emptySet(),
                        AdGroupType.MOBILE_CONTENT,
                        null},
        };
    }

    @Test
    public void test() {
        AutotargetingCategoryArray actual = autotargetingCategoriesFromCore(relevanceMatchCategories, adGroupType);
        if (expected == null) {
            assertNull(actual);
        } else {
            actual.getItems().sort(AUTOTARGETING_CATEGORY_COMPARATOR);
            expected.sort(AUTOTARGETING_CATEGORY_COMPARATOR);
            assertThat(actual.getItems(), beanDiffer(expected));
        }
    }
}
