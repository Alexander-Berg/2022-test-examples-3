package ru.yandex.autotests.direct.cmd.phrases.minuswords.intersection.negative;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
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

@Aqua.Test
@Description("Предупреждения о пересечении ключевых фраз и минус слов в попапе на странице редактирования группы")
@Stories(TestFeatures.Phrases.AJAX_CHECK_BANNERS_MINUS_WORDS)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.AJAX_CHECK_BANNERS_MINUS_WORDS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class AjaxCheckBannersMinusWordsNegative2Test  extends AjaxCheckMinusWordsNegativeBase {

    @Parameterized.Parameters(name = "Должно появиться предупреждение при пересечение КС {0} и КС {1}:")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"билеты [из Москвы в Париж]", "Москва"},
                {"билеты [из Москвы в Париж]", "+Москва"},
                {"билеты [из Москвы в Париж]", "[Москва]"},
                {"билеты [из Москвы в Париж]", "+из"},

                {"[билеты]", "билеты"},
                {"[билеты]", "[билеты]"},

                {"билеты [из !Москвы в Париж]", "!Москвы"},
                {"билеты [из !Москвы в Париж]", "Москва"},
                {"билеты [из !Москвы в Париж]", "Москвы"},
                {"билеты [из !Москвы в Париж]", "[Москвы]"},
                {"билеты [из !Москвы в Париж]", "[!Москвы]"},
                {"билеты [из !Москвы в Париж]", "Москва"},

                {"[!Москва]", "Москва"},
                {"[!Москва]", "!Москва"},
                {"[!Москва]", "[!Москва]"},
        });
    }
    @Test
    @Description("Предупреждение при пересечение ключевых слов и минус слов на странице группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10732")
    public void ajaxCheckBannersMinusWords() {
        super.ajaxCheckBannersMinusWords();
    }
}
