package ru.yandex.autotests.direct.cmd.phrases.minusphrases.intersection.negative;

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
@Description("Предупреждения о пересечении ключевых фраз и минус-фраз в попапе на странице редактирования группы")
@Stories(TestFeatures.Phrases.AJAX_CHECK_BANNERS_MINUS_WORDS)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.AJAX_CHECK_BANNERS_MINUS_WORDS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class AjaxCheckBannersMinusPhrasesNegative9Test  extends AjaxCheckMinusPhrasesNegativeBase {

    @Parameterized.Parameters(name = "Должно появиться предупреждение при пересечение КС {0} и МФ {1}:")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                //Дефис , " " и !
                {"\"!интернет-магазин мебели\"", "!интернет-магазин мебели"},
                {"\"!интернет-магазин мебели\"", "\"!интернет-магазин мебели\""},
                {"\"!интернет-магазин мебели\"", "!интернет-магазин"},
                {"\"!интернет-магазин мебели\"", "[!интернет-магазин] мебели"},

                {"\"!по-английски !говорить\"", "по-английски говорить"},
                {"\"!по-английски !говорить\"", "по-английски !говорить"},
                {"\"!по-английски !говорить\"", "!по-английски говорить"},
                {"\"!по-английски !говорить\"", "!по-английски !говорить"},
                {"\"!по-английски !говорить\"", "\"по-английски говорить\""},
                {"\"!по-английски !говорить\"", "\"по-английски !говорить\""},
                {"\"!по-английски !говорить\"", "\"!по-английски говорить\""},
                {"\"!по-английски !говорить\"", "\"!по-английски !говорить\""},
                {"\"!по-английски !говорить\"", "английски !говорить"},

                //Дефис и []
                {"скачать [англо-русский переводчик]", "скачать [англо-русский переводчик]"},
                {"скачать [англо-русский переводчик]", "[англо-русский переводчик]"},

                {"говорить [по-английски]", "говорить [по-английски]"},
                {"говорить [по-английски]", "говорить по-английски"},
                {"говорить [по-английски]", "говорить +по-английски"},
                {"говорить [по-английски]", "[по-английски]"},
                {"говорить [по-английски]", "говорить английски"},
        });
    }
    @Test
    @Description("Предупреждение при пересечение ключевых слов и минус-фразы на странице группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10711")
    public void ajaxCheckBannersMinusPhrases() {
        super.ajaxCheckBannersMinusPhrases();
    }
}
