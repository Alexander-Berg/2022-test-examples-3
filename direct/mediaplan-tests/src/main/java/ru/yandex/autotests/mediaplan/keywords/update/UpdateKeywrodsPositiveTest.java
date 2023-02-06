package ru.yandex.autotests.mediaplan.keywords.update;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.Keyword;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_bids_set_auto.Bid;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_update_keywords.ParamsApi5UpdateKeywords;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.KEYWORDS_UPDATE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.KeyWordsFactory.*;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(KEYWORDS_UPDATE)
@Description("Обновление ключевых слов для медиаплана")
@RunWith(Parameterized.class)
public class UpdateKeywrodsPositiveTest {
    public List<Keyword> keywords;

    public List<Keyword> keywordsChanges;

    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одно ключевое слово", oneKeyWord(), oneKeyWord()},
                {"Два ключевых слова", twoKeyWords(), twoKeyWords()},
                {"200 ключевых слова", maxKeyWords(), maxKeyWords()},
                {"Одно максимально длинное ключевое слово", oneLongKeyWord(), oneLongKeyWord()},
        });
    }

    @Rule
    public AdgroupRule adgroupRule;

    public UpdateKeywrodsPositiveTest(String text, List<Keyword> keywords, List<Keyword> keywordsChanges) {
        adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup()).withKeywords(keywords);
        this.keywordsChanges = keywordsChanges;
    }

    @Test
    public void updateKeyWords() {
        keywordsChanges = IntStream.range(0, keywordsChanges.size()).mapToObj(x -> keywordsChanges.get(x)
                .withId(adgroupRule.getKeywordsIds().get(x))).collect(Collectors.toList());
        keywordsChanges.stream().map(x -> x.withAdGroupId(adgroupRule.getAdGroupId()))
                .collect(Collectors.toList());
        adgroupRule.getUserSteps().keywordsSteps().api5KeywordsUpdate(new ParamsApi5UpdateKeywords()
                .withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp()).withKeywords(keywordsChanges));
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        keywordsChanges.stream().map(x -> x.withPosition(Bid.Position.P_11))
                .collect(Collectors.toList());
        assertThat("Сохраненные ключевые слова соотвествуют ожиданиям", changes.getModified().getKeywords(), equalTo(keywordsChanges));
    }

}
