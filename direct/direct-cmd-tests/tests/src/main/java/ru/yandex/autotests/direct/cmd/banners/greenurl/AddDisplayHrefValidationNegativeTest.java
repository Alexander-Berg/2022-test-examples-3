package ru.yandex.autotests.direct.cmd.banners.greenurl;

import org.junit.Ignore;
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
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Валидация при добавлении отображаемой ссылки в баннер (saveTextAdGroups) (негативные кейсы)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
@Ignore("https://st.yandex-team.ru/DIRECT-53906")
public class AddDisplayHrefValidationNegativeTest extends DisplayHrefBaseTest {

    public String displayHref;

    public AddDisplayHrefValidationNegativeTest(String displayHref, String bannerText, Geo geo) {
        this.displayHref = displayHref;
        bannersRule = new TextBannersRule().
                overrideBannerTemplate(new Banner().
                        withTitle(bannerText).
                        withBody(bannerText)).
                overrideGroupTemplate(new Group().withGeo(geo.getGeo())).
                withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Отображаемая ссылка: {0}; гео: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // длина
                {"abcdefghijklmnopqrstu", "Русский текст", Geo.RUSSIA},
                // позиция недопустимого символа
                {"\\bcdefghijklmnopqrs", "Русский текст", Geo.RUSSIA},
                {"abcdefghijklmnopqrs]", "Русский текст", Geo.RUSSIA},
                {"abcdefghijk;mnopqrst", "Русский текст", Geo.RUSSIA},
                // недопустимые символы
                {"abcde.", "Русский текст", Geo.RUSSIA},
                {"abcde\\", "Русский текст", Geo.RUSSIA},
                {"abcde?", "Русский текст", Geo.RUSSIA},
                {"abcde_", "Русский текст", Geo.RUSSIA},
                {"abc de", "Русский текст", Geo.RUSSIA},
                // украинский язык
                {"абвгґії", "русский текст", Geo.RUSSIA},
                {"абвгґії", "Қазақ мәтін", Geo.KAZAKHSTAN},
                {"абвгґії", "Türk şarkı sözleri", Geo.TURKEY},
                // казахский язык
                {"әғқңөұүһі", "русский текст", Geo.RUSSIA},
                {"әғқңөұүһі", "український текст", Geo.UKRAINE},
                {"әғқңөұүһі", "Türk şarkı sözleri", Geo.TURKEY},
                // турецкий язык
                {"çğıiöşü", "русский текст", Geo.RUSSIA},
                {"çğıiöşü", "український текст", Geo.UKRAINE},
                {"çğıiöşü", "Қазақ мәтін", Geo.KAZAKHSTAN}
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Валидация при добавлении отображаемой ссылки в баннер (saveTextAdGroups) (негативные кейсы)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9190")
    public void testAddDisplayHrefValidationNegative() {
        editDisplayHref();
    }

    @Override
    protected String getDisplayHrefToAddToCreatedBanner() {
        return displayHref;
    }
}
