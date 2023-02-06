package ru.yandex.autotests.mediaplan.setauto;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_keywords.Keyword;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_bids_set_auto.Api5BidsSetAutoResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_bids_set_auto.Bid;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_bids_set_auto.ParamsApi5BidsSetAuto;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.KeyWordsFactory.oneKeyWord;
import static ru.yandex.autotests.mediaplan.datafactories.KeyWordsFactory.twoKeyWords;
import static ru.yandex.autotests.mediaplan.TestFeatures.BIDS_SET_AUTO;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(BIDS_SET_AUTO)
@Issue("https://st.yandex-team.ru/MEDIAPLAN-124")
@Description("Расчитываем ставки")
@RunWith(Parameterized.class)
public class BidsSetAutoPositiveTest {
    private List<Keyword> keywords;

    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одно ключевое слово", oneKeyWord()},
                {"Два ключевых слова", twoKeyWords()},
        });
    }
    @Rule
    public AdgroupRule adgroupRule;

    public BidsSetAutoPositiveTest(String text, List<Keyword> keywords) {
        this.keywords = keywords;
        adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup()).withKeywords(keywords);

    }

    @Test
    public void setAuto() {
        keywords.stream().map(x->x.withPosition(Bid.Position.P_11)).collect(Collectors.toList());
        keywords.stream().map(x -> x.withAdGroupId(adgroupRule.getAdGroupId())).collect(Collectors.toList());
        keywords = IntStream.range(0, keywords.size()).mapToObj(x -> keywords.get(x).withKeyword(null)
                .withKeywordId(adgroupRule.getKeywordsIds().get(x))).collect(Collectors.toList());
        Api5BidsSetAutoResult bids = adgroupRule.getUserSteps().bidsSteps().api5BidsSetAuto(new ParamsApi5BidsSetAuto().withBids(keywords)
                .withClientId(getClient()).withMediaplanId(adgroupRule.getMediaplanId()).withTimestamp(adgroupRule.getLastUpdateTimestamp()));
        assertThat("Число ставок соотвествует ожиданиям", bids.getSetAutoResults(), hasSize(keywords.size()));
    }




}
