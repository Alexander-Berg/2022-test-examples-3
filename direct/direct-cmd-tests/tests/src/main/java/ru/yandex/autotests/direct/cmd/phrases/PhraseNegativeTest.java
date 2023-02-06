package ru.yandex.autotests.direct.cmd.phrases;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.PhraseErrorsEnum;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.*;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Негативные тесты сохранения фраз")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class PhraseNegativeTest extends PhraseBaseTest {

    @Parameterized.Parameter(value = 2)
    public String errorText;

    @Parameterized.Parameters(name = "Тест #{index}. {1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {getGroupWithTooLongWordPhrase(), "Проверяем фразу со словом больше 35 букв", PhraseErrorsEnum.TOO_LONG_WORD.toString()},
                {getGroupWithTooLongPhrase(), "Проверяем фразу длиной 4097", PhraseErrorsEnum.TOO_LONG_PHRASE.toString()},
                {getGroupWithTooMuchPhrases(), "Проверяем группу с более чем 201 фраз", PhraseErrorsEnum.TOO_MANY_PHRASES.toString()},
                {getGroupWithPhraseWithTooMuchWords(), "Проверяем фразу с более чем 7 словами", PhraseErrorsEnum.TOO_MANY_WORDS_IN_PHRASE.toString()},
                {getGroupWithPhraseFromSpaces(), "Проверяем фразу из пробелов", PhraseErrorsEnum.PHRASE_TEXT_NOT_FOUND.toString()},
                {getGroupWithPhraseFromMinusWords(), "Проверяем фразу из минус слов", PhraseErrorsEnum.WRONG_ONLY_MINUS_WORDS.toString()},
                {getGroupWithPhraseWithSeparateDots(), "Проверяем фразу c отдельно стоящими точками", PhraseErrorsEnum.WRONG_DOT_POSITION.toString()},
                {getGroupWithPhraseWithBannedWords(), "Проверяем фразу с запрещенными символами", PhraseErrorsEnum.WRONG_SYMBOLS.toString()},
                {getGroupWithPhraseStartsFromDot(), "Проверяем фразу начинающуюся с точки", PhraseErrorsEnum.WRONG_START_SYMBOL.toString()},
                {getGroupWithPhraseWithMunisWordsInQuotes(), "Проверяем фразу c минус словом в кавычках", PhraseErrorsEnum.WRONG_QUOTE_USE.toString()},
                {getGroupWithPhraseWithEmptyAbPriority(), "Проверяем фразу c пустым autobudget_priority", CommonErrorsResource.WRONG_INPUT_DATA.toString()}
        };
        return Arrays.asList(data);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9921")
    public void saveGroup() {
        group.setCampaignID(campaignRule.getCampaignId().toString());
        group.getBanners().stream().forEach(b -> b.withCid(campaignRule.getCampaignId()));
        GroupsParameters groupRequest = GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);

        ErrorResponse actualError = cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(groupRequest);
        assertThat("Ошибка соотвествует ожиданиям", actualError.getError(), containsString(errorText));
    }
}
