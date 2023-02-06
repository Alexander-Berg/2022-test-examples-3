package ru.yandex.autotests.innerpochta.mbody;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendmessage.ApiSendMessage;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Attachment;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.function.Function.identity;
import static org.cthul.matchers.CthulMatchers.both;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.NO_VDIRECT_LINKS_WRAP;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.fromClasspath;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * User: lanwen
 * Date: 06.11.14
 * Time: 20:21
 */
@Aqua.Test
@Title("[MBODY] Тестирование хайлайтера")
@Description("Отправка различных html писем, затем сравнение полученного с ожидаемым")
@Features(MyFeatures.MBODY)
@Stories("#хайлайтер")
@Credentials(loginGroup = "Sanitizertest")
public class HilighterTest extends MbodyBaseTest {

    public static final String VDIRECT_URL_PREFIX = "https://mail.yandex.ru/re.jsx";

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("«Ё@...ый» не должно подсвечиваться как мыло")
    @Description("Тестирование хайлайтера. Смотрим что собака и много точек после нее" +
            " не превращаются в мыло в веб-интерфейсе. Например когда отсылаем \"Ё@...ый\"")
    public void shouldNotHilightAnyStringWithAT() throws Exception {
        String send = "Ё@...ый";
        String content = sendAndReturn(send, "");
        assertThat("Собака и несколько точек подсвечивается как email", content, not(containsString("wmi-mailto")));
    }

    @Test
    @Title("Неразрывный пробел сразу после ссылки не должен тянуть под ссылку текст")
    @Description("Тестирование хайлайтера. Смотрим что неразрывный пробел, добавленный сразу после ссылки" +
            " не тянет за собой под ссылку текст после этого пробела:" +
            " <div>parampampam<br>http://www.site.ru&nbsp;text</div>")
    @Issues({@Issue("DARIA-10501"), @Issue("WMI-257"), @Issue("WMI-442")})
    public void shouldNotHilightTextAfterLinkWithNbsp() throws Exception {
        String send = "<div>parampampam<br>http://www.site.ru&nbsp;text</div>";
        String content = sendAndReturn(send, "");
        assertThat("Текст и неразрывный пробел находится внутри ссылки, ожидалось - &nbsp; не потянет за собой текст",
                content, both(not(containsString("text</span>"))).and(not(containsString("www.site.ru text\">")))
        );
    }

    @Test
    @Title("Заворачиваем area в vdirect")
    @Issue("WMI-597")
    public void shouldWrapWithVdirectAreaHref() throws Exception {
        String send = fromClasspath("hilighter/area.html");
        String content = sendAndReturn(send, "");
        assertThat("Ссылки должны быть завернуты в vdirect",
                content, allOf(containsString(VDIRECT_URL_PREFIX),
                        not(containsString("http://boutique.us4")))
        );
    }

    @Test
    @Title("Не заворачиваем ссылки в vdirect при параметре novdirect")
    @Issue("DARIA-47387")
    public void shouldNotWrapLinksToVdirectIfNovdirectParam() throws Exception {
        String send = fromClasspath("hilighter/area.html");
        String content = sendAndReturn(send, NO_VDIRECT_LINKS_WRAP.toString());
        assertThat("Ссылки не должны быть завернуты в вдирект",
                content, allOf(not(containsString(VDIRECT_URL_PREFIX)),
                        containsString("http://boutique.us4"))
        );
    }

    @Test
    @Issues({@Issue("WMI-442"), @Issue("WMI-246")})
    @Title("Текст, стоящий до собаки через пробел, не должен подсвечиваться как e-mail")
    @Description("Например: «много текста @ya.ru»")
    public void shouldNotHilightNotEmailTextSpaceAtDomain() throws Exception {
        String send = "много текста @ya.ru";
        String content = sendAndReturn(send, "");
        assertThat("Текст [пробел] @ya.ru объединились в e-mail", content, not(containsString("wmi-mailto")));
        assertThat("\"много текста\" оказались разнесены", content, containsString("много текста"));
    }

    @Attachment("Содержимое тега content")
    private String sendAndReturn(String send, String mbodyFlags) throws Exception {
        String mid = sendWith(authClient)
                .viaProd()
                .subj("Sanitizer - " + Util.getRandomString())
                .text(send)
                .html(ApiSendMessage.HtmlParam.YES.value())
                .send()
                .waitDeliver()
                .getMid();
        Mbody mbody = apiMbody().message().withMid(mid).withUid(uid()).withAuthDomain(".yandex.ru").withSecure("1")
                .withFlags(mbodyFlags)
                .get(identity()).peek().as(Mbody.class);
        return mbody.getBodies().get(0).getTransformerResult().getTextTransformerResult().getContent();
    }

}
