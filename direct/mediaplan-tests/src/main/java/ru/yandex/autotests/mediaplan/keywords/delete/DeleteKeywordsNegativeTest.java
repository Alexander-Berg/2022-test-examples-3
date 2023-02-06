package ru.yandex.autotests.mediaplan.keywords.delete;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.Keyword;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_keywords.Api5DeleteKeywordsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_keywords.ParamsApi5DeleteKeywords;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_keywords.SelectionCriteria;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.KEYWORDS_DELETE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.KeyWordsFactory.twoKeyWords;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(KEYWORDS_DELETE)
@Description("Некорректное удаление ключевых слов для медиаплана")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class DeleteKeywordsNegativeTest {
    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Два ключевых слова", twoKeyWords()},
        });
    }
    @Rule
    public AdgroupRule adgroupRule;

    public DeleteKeywordsNegativeTest(String text, List<Keyword> keywords) {
        adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup()).withKeywords(keywords);
    }

    @Test
    public void deleteKeyWords() {
        //где ошибки
        adgroupRule.getUserSteps().keywordsSteps().api5KeywordsDelete(new ParamsApi5DeleteKeywords()
                .withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                .withSelectionCriteria(new SelectionCriteria().withAdGroupId(adgroupRule.getAdGroupId())
                        .withIds(adgroupRule.getKeywordsIds())));
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        assumeThat("Ключевые слова удалились", changes.getModified().getKeywords(), hasSize(0));
        Api5DeleteKeywordsResult results = adgroupRule.getUserSteps().keywordsSteps().api5KeywordsDelete(new ParamsApi5DeleteKeywords()
                .withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                .withSelectionCriteria(new SelectionCriteria().withAdGroupId(adgroupRule.getAdGroupId())
                        .withIds(adgroupRule.getKeywordsIds())));
        assertThat("ошибки что слова удалились", results.getDeleteResults().stream().filter(x -> !x.getErrors().isEmpty())
                        .collect(Collectors.toList()),
                not(hasSize(0)));

    }

}
