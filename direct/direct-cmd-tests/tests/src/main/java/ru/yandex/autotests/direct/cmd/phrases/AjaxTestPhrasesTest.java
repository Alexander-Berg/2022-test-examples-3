package ru.yandex.autotests.direct.cmd.phrases;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.phrases.AjaxTestPhrasesXmlResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.correctedPhrasesWithDuplicates;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.correctedPhrasesWithWhitespaces;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.normalPhrases;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.phraseWithCommaAndWhiteSpaceInPrefix;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.phraseWithCommaInPrefix;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.phraseWithoutCommas;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.phrasesWithDuplicates;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.phrasesWithWhitespaces;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

// таск : https://st.yandex-team.ru/DIRECT-54215
@Aqua.Test
@Description("Проверка ручки ajaxTestPhrases на возможность приема сырых данных")
@Stories(TestFeatures.Phrases.AJAX_TEST_PHRASES)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.AJAX_TEST_PHRASES)
@Tag(ObjectTag.PHRASE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AjaxTestPhrasesTest {
    protected static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT);

    @Parameterized.Parameter(value = 0)
    public String savingPhrases;
    @Parameterized.Parameter(value = 1)
    public String expectedPhrases;

    @Parameterized.Parameters(name = "Редактирование фидов под {0} у {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {phrasesWithWhitespaces(), correctedPhrasesWithWhitespaces()},
                {phraseWithCommaInPrefix(), phraseWithoutCommas()},
                {phraseWithCommaAndWhiteSpaceInPrefix(), phraseWithoutCommas()},
                {phrasesWithDuplicates(), correctedPhrasesWithDuplicates()},
                {normalPhrases(), normalPhrases()}
        });
    }



    @Test
    @Description("Отправляем фразы на проверку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9917")
    public void testPhrases() {
        AjaxTestPhrasesXmlResponse actualResponse = cmdRule.cmdSteps().phrasesSteps().ajaxTestPhrases(savingPhrases);
        String actualPhrases = String.join(", ", actualResponse.getPhrase());
        assertThat("Полученные фразы соотвествуют ожиданиям", actualPhrases, equalTo(expectedPhrases));
    }

}
