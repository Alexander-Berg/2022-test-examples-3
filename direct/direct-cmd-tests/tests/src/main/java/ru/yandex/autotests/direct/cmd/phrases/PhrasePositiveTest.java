package ru.yandex.autotests.direct.cmd.phrases;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getGroupWithALotOfLongPhrases;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getGroupWithALotOfPhrases;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getGroupWithLongPhrase;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getGroupWithPhraseWithALotOfWords;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getGroupWithPhraseWithSameKeyWordsAndMinusWords;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;


@Aqua.Test
@Description("Позитивные тесты сохранения фраз")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class PhrasePositiveTest extends PhraseBaseTest {

    private List<Phrase> expectedPhrases;

    @Parameterized.Parameters(name = "Тест #{index}. {1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {getGroupWithLongPhrase(), "Проверяем фразу длиной 4096"},
                {getGroupWithALotOfPhrases(), "Проверяем группу с 200 фразами"},
                {getGroupWithPhraseWithALotOfWords(), "Проверяем фразу с 7 словами"},
                {getGroupWithALotOfLongPhrases(), "Проверяем группу с 200 фразами длиной 4096"},
                {getGroupWithPhraseWithSameKeyWordsAndMinusWords(),
                        "Проверяем группу с фразой cо словами пересекающимися с минус словами"},
        };
        return Arrays.asList(data);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9924")
    public void saveGroup() {
        group.setCampaignID(campaignRule.getCampaignId().toString());
        group.getBanners().stream().forEach(b -> b.withCid(campaignRule.getCampaignId()));
        GroupsParameters groupRequest = GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);

        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupRequest);
        expectedPhrases = group.getPhrases();
        List<Phrase> actualPhrases = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, String.valueOf(campaignRule.getCampaignId()))
                .getGroups().get(0).getPhrases();
        assertThat("Сохраненные фразы совпадает с ожидаемыми",
                actualPhrases,
                beanDiffer(expectedPhrases).
                        useCompareStrategy(DefaultCompareStrategies.onlyFields(BeanFieldPath.newPath(".*", "phrase")))
        );
    }

    @Test
    @Description("Проверяем отсутствие предупреждлений")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9925")
    public void saveGrouptoCheckWarnings() {
        group.setCampaignID(campaignRule.getCampaignId().toString());
        group.getBanners().stream().forEach(b -> b.withCid(campaignRule.getCampaignId()));
        GroupsParameters groupRequest = GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);

        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupRequest);
        expectedPhrases = group.getPhrases();
        ShowCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, String.valueOf(campaignRule.getCampaignId()));
        assertThat("полученный флаг предупрежедения 'does_phrase_exceed_max_length' совпадает с ожидаемым",
                actualResponse.getGroups().get(0).getPhraseExceedMaxLength(), equalTo(""));
        assertThat("полученный флаг предупрежедения 'does_phrase_exceed_max_words'  совпадает с ожидаемым",
                actualResponse.getGroups().get(0).getPhraseExceedMaxWordsCount(), equalTo(""));
    }


}
