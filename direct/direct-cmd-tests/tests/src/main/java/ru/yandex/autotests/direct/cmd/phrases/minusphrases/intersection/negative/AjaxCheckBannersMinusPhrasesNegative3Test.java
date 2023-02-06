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
public class AjaxCheckBannersMinusPhrasesNegative3Test  extends AjaxCheckMinusPhrasesNegativeBase {
    @Parameterized.Parameters(name = "Должно появиться предупреждение при пересечение КС {0} и МФ {1}:")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                //" "
                {"\"цветы в горшках\"", "цветы в горшках"},
                {"\"цветы в горшках\"", "цветы !в горшках"},
                {"\"цветы в горшках\"", "цветы +в горшках"},
                {"\"цветы в горшках\"", "цветы [в] горшках"},
                {"\"цветы в горшках\"", "цветы горшок"},
                {"\"цветы в горшках\"", "\"цветы в горшках\""},
                {"\"цветы в горшках\"", "\"цветы +в горшках\""},
                {"\"цветы в горшках\"", "\"цветы !в горшках\""},
                {"\"цветы в горшках\"", "\"в горшках цветы\""},
                {"\"цветы в горшках\"", "\"!в горшках цветы\""},
                {"\"цветы в горшках\"", "\"цветок в горшке\""},

                //" " и +
                {"\"цветы +в горшках розы\"", "цветы в горшках розы"},
                {"\"цветы +в горшках розы\"", "цветы !в горшках розы"},
                {"\"цветы +в горшках розы\"", "цветы +в горшках розы"},
                {"\"цветы +в горшках розы\"", "цветы +в горшках"},
                {"\"цветы +в горшках розы\"", "цветы [в] горшках"},
                {"\"цветы +в горшках розы\"", "\"цветы в горшках розы\""},
                {"\"цветы +в горшках розы\"", "\"цветы +в горшках розы\""},
                {"\"цветы +в горшках розы\"", "\"цветы !в горшках розы\""},
                {"\"цветы +в горшках розы\"", "\"розы в горшках цветы\""},
                {"\"цветы +в горшках розы\"", "\"цветки +в горшках розы\""},

                {"\"одежда +и обувь +для мужчины\"", "одежда и обувь для мужчины"},
                {"\"одежда +и обувь +для мужчины\"", "одежда !и обувь !для мужчины"},
                {"\"одежда +и обувь +для мужчины\"", "одежда +и обувь +для мужчины"},
                {"\"одежда +и обувь +для мужчины\"", "одежда [и] обувь [для] мужчины"},
                {"\"одежда +и обувь +для мужчины\"", "\"одежда и обувь для мужчины\""},
                {"\"одежда +и обувь +для мужчины\"", "\"одежда и обувь +для мужчины\""},
                {"\"одежда +и обувь +для мужчины\"", "\"одежда +и обувь для мужчины\""},
                {"\"одежда +и обувь +для мужчины\"", "\"одежда +и обувь +для мужчины\""},
                {"\"одежда +и обувь +для мужчины\"", "\"одежда +и обувь +для мужчин\""},
                {"\"одежда +и обувь +для мужчины\"", "одежда +и обувь +для"},

        });
    }
    @Test
    @Description("Предупреждение при пересечение ключевых слов и минус-фразы на странице группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10705")
    public void ajaxCheckBannersMinusPhrases() {
        super.ajaxCheckBannersMinusPhrases();
    }
}
