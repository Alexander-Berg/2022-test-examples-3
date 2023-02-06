package ru.yandex.autotests.mediaplan.keywords.add;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.Api5AddKeywordsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.Keyword;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.ParamsApi5AddKeywords;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_bids_set_auto.Bid;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.KEYWORDS_ADD;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.KeyWordsFactory.tooLongKeyWord;
import static ru.yandex.autotests.mediaplan.datafactories.KeyWordsFactory.tooMuchKeyWords;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(KEYWORDS_ADD)
@Description("Создание некорректных ключевых слов для медиаплана")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class AddKeyWordsNegativeTest {
    @Parameterized.Parameter(value = 1)
    public List<Keyword> keywords;

    @Parameterized.Parameter(value = 0)
    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"201 ключевых слов", tooMuchKeyWords()},
                {"Одно слишком длинное ключевое слово", tooLongKeyWord()},
        });
    }
    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup());

    @Test
    public void addKeyWords() {
        keywords.stream().map(x -> x.withAdGroupId(adgroupRule.getAdGroupId())).collect(Collectors.toList());
        Api5AddKeywordsResult ids = adgroupRule.getUserSteps().keywordsSteps().api5KeywordsAdd(new ParamsApi5AddKeywords().withMediaplanId(adgroupRule.getMediaplanId())
                .withClientId(getClient()).withTimestamp(adgroupRule.getLastUpdateTimestamp()).withKeywords(keywords));
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        List<Keyword> expectedKeywords = IntStream.range(0, keywords.size() - 1).mapToObj(x -> keywords.get(x)
                .withId(ids.getAddResults().get(x).getId()).withPosition(Bid.Position.P_11))
                .collect(Collectors.toList());
        assertThat("Сохраненные ключевые слова соотвествуют ожиданиям", changes.getModified().getKeywords(), equalTo(expectedKeywords));
    }


}
