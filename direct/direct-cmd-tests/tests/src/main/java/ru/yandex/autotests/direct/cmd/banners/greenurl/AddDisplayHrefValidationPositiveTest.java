package ru.yandex.autotests.direct.cmd.banners.greenurl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
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

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Валидация при добавлении отображаемой ссылки в баннер (saveTextAdGroups) (позитивные кейсы)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class AddDisplayHrefValidationPositiveTest extends DisplayHrefBaseTest {

    public String displayHref;

    public AddDisplayHrefValidationPositiveTest(String displayHref, String bannerText, Geo geo) {
        this.displayHref = displayHref;
        bannersRule = new TextBannersRule().
                overrideBannerTemplate(new Banner().
                        withTitle(bannerText).
                        withBody(bannerText)).
                overrideGroupTemplate(new Group().withGeo(geo.getGeo())).
                withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Отображаемая ссылка: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // длина
                {"a", "Русский текст", Geo.RUSSIA},
                {"abcdefghijklmnopqrst", "Русский текст", Geo.RUSSIA},
                // заглавные буквы
                {"абВгд", "Русский текст", Geo.RUSSIA},
                {"aBcde", "Русский текст", Geo.RUSSIA},
                {"абвгҐії", "український текст", Geo.UKRAINE},
                {"әғқңҢөұүһі", "Қазақ мәтін", Geo.KAZAKHSTAN},
                {"çğıiöÖşü", "Türk şarkı sözleri", Geo.TURKEY},
                // допустимые символы
                {"-/№%#0123456789", "Русский текст", Geo.RUSSIA},
                {"-abc/def№abc%tyhf#", "Русский текст", Geo.RUSSIA},
                // русский язык
                {"абвгдеёжзийклмнопрст", "Русский текст", Geo.RUSSIA},
                {"уфхцчшщьыъэюя", "Русский текст", Geo.UKRAINE},
                // английский язык
                {"abcdefghijklmnopqrst", "English text", Geo.RUSSIA},
                {"uvwxyz", "English text", Geo.KAZAKHSTAN},
                // украинский + беларусский язык
                {"абвгґії", "український текст", Geo.UKRAINE},
                // казахский язык
                {"әғқңөұүһі", "Қазақ мәтін", Geo.KAZAKHSTAN},
                // турецкий язык
                {"çğıiöşü", "Türk şarkı sözleri", Geo.TURKEY},
        });
    }

    @Test
    @Description("Валидация при добавлении отображаемой ссылки в баннер (saveTextAdGroups) (позитивные кейсы)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9191")
    public void testAddDisplayHrefValidationPositive() {
        editDisplayHref();
        assertThat("отображаемая ссылка сохранилась правильно", getDisplayHref(), equalTo(displayHref));
    }

    @Override
    protected String getDisplayHrefToAddToCreatedBanner() {
        return displayHref;
    }
}
