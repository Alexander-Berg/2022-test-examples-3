package ru.yandex.autotests.mediaplan.keywords.add;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.Api5AddKeywordsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.Keyword;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.ParamsApi5AddKeywords;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.KEYWORDS_ADD;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.KeyWordsFactory.oneCorrectOtherNot;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(KEYWORDS_ADD)
@Description("Создание ключевых слов для медиаплана, одно коррeктное второе нет")
@Tag(MasterTags.MASTER)
public class AddKeywordsOneCorrectOtherNot {
    public List<Keyword> keywords = oneCorrectOtherNot();

    public String text;
    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup());

    @Test
    public void addKeyWords() {
        keywords.stream().map(x -> x.withAdGroupId(adgroupRule.getAdGroupId())).collect(Collectors.toList());
        Api5AddKeywordsResult results = adgroupRule.getUserSteps().keywordsSteps().api5KeywordsAdd(new ParamsApi5AddKeywords().withMediaplanId(adgroupRule.getMediaplanId())
                .withClientId(getClient()).withTimestamp(adgroupRule.getLastUpdateTimestamp()).withKeywords(keywords));
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        List<Keyword> expectedKeywords = IntStream.range(0, keywords.size()).mapToObj(x -> keywords.get(x)
                .withId(results.getAddResults().get(x).getId())).collect(Collectors.toList()).subList(1, 2);
        assumeThat("При сохранении некорректного ключевого слова вернулась ошибка", results.getAddResults().stream()
                        .filter(x->!x.getErrors().isEmpty()).collect(Collectors.toList()), not(hasSize(0))
        );
        changes.getModified().getKeywords().get(0).setPosition(null);
        assertThat("Сохраненные ключевые слова соотвествуют ожиданиям", changes.getModified().getKeywords(),
                equalTo(expectedKeywords)
        );
    }
}
