package ru.yandex.autotests.innerpochta.sanitizer;

import com.google.common.base.Charsets;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.net.URLEncoder;

import static com.google.common.hash.Hashing.md5;
import static java.lang.String.format;
import static org.cthul.matchers.chain.AndChainMatcher.and;
import static org.cthul.matchers.object.ContainsPattern.matchesPattern;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.sanitizer.SanitizerApi.sanitizer;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.fromClasspath;


@Aqua.Test
@Title("Тестирование санитайзера")
@Description("Отправка html напрямую в санитайзер, затем сравнение полученного с ожидаемым")
@Features(MyFeatures.SANITIZER)
@Stories(MyStories.SANITIZER)
@Ignore("MAILDEV-1110")
public class SanitizerTest {

//    use for debug portForwarding.local():
//    @ClassRule
//    public static SshLocalPortForwardingRule portForwarding = viaRemoteHost(props().betaURI())
//            .forwardTo(props().betaURI())
//            .withForwardToPort(props().getSanitizerPort()).onLocalPort(localPortForMocking());

    @Test
    @Title("Проверка эскейпинга амперсанда & в %26")
    @Issue("DARIA-16433")
    public void shouldEscapeAmp() throws Exception {
        String send = fromClasspath("sanitizer/ampescape.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);

        assertThat("Символ & не заэскейпился", content,
                both(containsString("&amp;")).and(not(containsString("r&c"))));
    }

    @Test
    @Title("Проверка энкодинга русских букв")
    @Issue("SANITIZER-120")
    public void shouldEncodeRussianLanguageTwice() throws IOException {
        String send = fromClasspath("sanitizer/russianencode.htm");
        String russianLetterInSend = "ф";

        String content = sanitizer(props().sanitizerUri()).secproxy(send);
        String russianLetterEncodedOnce = URLEncoder.encode(russianLetterInSend, "UTF-8");
        String russianLetterEncodedTwice = URLEncoder.encode(russianLetterEncodedOnce, "UTF-8");
        assertThat("Русские буквы не заэнкодились", content,
                both(not(containsString(russianLetterEncodedOnce))).and(containsString(russianLetterEncodedTwice)));
    }

    @Test
    @Title("Проверка свойства стиля direction: rtl;")
    @Description("Тестирование Санитайзера на предмет неудаления свойства direction")
    @Issue("DARIA-13782")
    public void testSanitizerStyleDirection() throws Exception {
        String attrib = "direction:ltl";
        String send = fromClasspath("sanitizer/direction.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat(format("Атрибут стиля %s не должен быть вырезан", attrib), content, containsString(attrib));
    }


    @Test
    @Issue("DARIA-18857")
    public void testSanitizerRemoveJS() throws Exception {
        String send = fromClasspath("sanitizer/jsinhref.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        // Конкретная проверка
        assertThat("JS не вырезан", content, not(containsString("alert")));
    }


    @Test
    @Title("Удаление «data:» схемы из нежелательных тегов")
    @Issue("SANITIZER-12")
    public void sanitizerShouldRemoveDataWithWrongContentType() throws Exception {
        String send = fromClasspath("sanitizer/data-base64.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("[SANITIZER-12] Неверный атрибут [data:...] не вырезан", content, not(containsString("data")));
    }


    @Test
    @Title("Схема «data:» должна оставаться в картинках")
    @Issue("SANITIZER-12")
    public void sanitizerShouldNOTRemoveDataWithRightContentType() throws Exception {
        String send = fromClasspath("sanitizer/dataattr.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("[SANITIZER-12] Верный атрибут data вырезан у картинки", content, containsString("data"));
    }


    @Test
    @Issue("DARIA-18872")
    public void testSanitizerRemoveCSSActiveContent() throws Exception {
        String send = fromClasspath("sanitizer/css-active.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("[DARIA-18872] JS не вырезан", content, not(containsString("cookie")));
    }

    @Test
    @Title("Должны оставить свойство стиля !important;")
    @Issue("DARIA-9636")
    public void testSanitizerImportantInStyle() throws Exception {
        String send = fromClasspath("sanitizer/important.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("Атрибут стиля !important вырезан", content, containsString("!important"));
    }

    @Test
    @Title("Должны вырезать отрицательный text-indent")
    public void testSanitizerStyleTextIndent() throws Exception {
        String attrib = "text-indent";
        String send = fromClasspath("sanitizer/text-indent.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat(format("Атрибут стиля %s не вырезан", attrib), content, not(containsString(attrib)));
    }

    @Test
    @Title("Должны удалять всё что может сделать тег video активным")
    @Issues({@Issue("DARIA-13058"), @Issue("SANITIZER-76")})
    public void testSanitizerRemoveVideo() throws Exception {
        String send = fromClasspath("sanitizer/video.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("Тег video  не был очищен", content,
                both(not(containsString("<video")))
                        .and(not(containsString("src=")))
                        .and(not(containsString("source")))
                        .and(containsString("Your browser does not support the video tag."))
        );
    }

    @Test
    @Title("Должны вырезать тег embed")
    @Issue("DARIA-11699")
    public void testSanitizerRemoveEmbed() throws Exception {
        String send = fromClasspath("sanitizer/embed.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("Тег embed  не был очищен", content, not(containsString("embed")));
    }


    @Test
    @Title("Не должны удалять margin атрибуты")
    @Description("Картинка прилипает к краю из-за вырезки тега margin")
    @Issue("DARIA-14197")
    public void testSanitizerDontRemoveMargins() throws Exception {
        String send = fromClasspath("sanitizer/margins.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("Свойство margin было неверно вырезано", content, containsString("margin:0px 20px 10px -20px;"));
    }


    @Test
    @Title("Проверка правильного закрытия colgroup")
    @Issues({@Issue("DARIA-11368"), @Issue("DARIA-7563")})
    public void testSanitizerCloseColgroup() throws Exception {
        String send = fromClasspath("sanitizer/colgroup.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("Тег colgroup не был закрыт правильно", content, containsString("</colgroup><tbody><tr><td>" +
                "lala</td></tr></tbody></table>"));
    }

    @Test
    @Title("Должны вырезать тег audio")
    @Issue("DARIA-13058")
    public void testSanitizerRemoveAudio() throws Exception {
        String send = fromClasspath("sanitizer/audio.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("В выдаче содержатся src трибуты", content, not(containsString("src=")));
        assertThat("Тег audio  не был очищен", content, not(containsString("audio")));
    }

    @Test
    @Title("Должны вырезать iframe теги")
    @Issue("DARIA-15320")
    public void testSanitizerRemoveIframe() throws Exception {
        String send = fromClasspath("sanitizer/iframe.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("В выдаче содержатся src трибуты", content, not(containsString("src=")));
        assertThat("Тег iframe  не был очищен", content, not(containsString("iframe")));
    }

    @Test
    @Title("Должны удалять object теги")
    @Issue("DARIA-15320")
    public void testSanitizerRemoveObject() throws Exception {
        String send = fromClasspath("sanitizer/object.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("В выдаче содержатся data трибуты", content, not(containsString("data=")));
        assertThat("Тег object  не был очищен", content, not(containsString("object")));
    }


    @Test
    @Issues({@Issue("DARIA-8480"), @Issue("DARIA-13813")})
    @Title("Должны удалять тег style вместе с содержимым")
    public void testSanitizerRemoveStyleTag() throws Exception {
        String send = fromClasspath("sanitizer/style.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("Тег style  всё еще есть", content, allOf(
                not(containsString("<style")),
                not(containsString("</style>")),
                not(containsString("<style/>"))
        ));
        assertThat("В выдаче содержатся атрибуты стилей", content, allOf(
                not(containsString("#wrapper")),
                not(containsString("#content"))
        ));
    }

    @Test
    @Title("Пробелы между адресом ссылки и кавычкой не влияют на кликабельность")
    @Issue("DARIA-13084")
    public void testSanitizerSpacingInUrl() throws Exception {
        String send = fromClasspath("sanitizer/url-space.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        // Конкретная проверка
        assertThat("Ссылка была вырезана!", content, containsString("a href=\"http"));
        assertThat("Пробелы не были удалены!", content, not(matchesPattern(".*href=\"\\s+http.*")));

    }

    @Test
    @Title("Картинки с cid в адресе не должны ломаться")
    @Issues({@Issue("DARIA-13221"), @Issue("DARIA-17313"), @Issue("DARIA-14197")})
    public void testSanitizerCidInImgSrc() throws Exception {
        String send = fromClasspath("sanitizer/cid2.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        // Конкретная проверка
        assertThat("Текст за картинкой обрезается", content, containsString("Эта строка уже не отображается"));
        assertThat("Альт текст был вырезан", content, containsString("магазин натуральных продуктов"));
    }


    @Test
    @Title("Картинки с cid в адресе не должны ломаться")
    @Issue("DARIA-14197")
    public void testTestSanitizerCidInImgSrc() throws Exception {
        // String send = "<inline-image alt=\"Демотиваторы\">part10704080605070904gmail.com</inline-image>";

        String send = fromClasspath("sanitizer/cid.htm");

        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("Альт текст был вырезан", content, containsString("Московский Дом Книги - перейти на сайт"));
    }

    @Test
    @Title("Должны обрабатываться yandex_smile_ и возвращать cid-ы")
    @Issues({@Issue("DARIA-51363"), @Issue("SANITIZER-102")})
    public void testSanitizerSmileCidImg() throws Exception {
        String send = fromClasspath("sanitizer/cid-smile.htm");
        String content = sanitizer(props().sanitizerUri()).secproxy(send);
        assertThat(content,
                containsString("[{\"classValue\":\"yandex_smile_30\",\"position\":[5,200,10,194],\"type\":4}]"));
    }

    @Test
    @Title("Должны обрабатываться yandex_postcard_ и возврщать cid-ы")
    @Issues({@Issue("DARIA-51363"), @Issue("SANITIZER-102")})
    public void testSanitizerPostcardCidImg() throws Exception {
        String send = fromClasspath("sanitizer/cid-postcard.htm");
        String content = sanitizer(props().sanitizerUri()).secproxy(send);
        assertThat(content,
                containsString("[{\"classValue\":\"yandex_postcard_5362\",\"position\":[5,203,10,197],\"type\":4}]"));
    }

    @Test
    @Issue("DARIA-51719")
    @Title("Должны обрабатываться background и возвращать cid-ы")
    @Issues({@Issue("DARIA-51719")})
    public void testSanitizerBackgroundCidImg() throws Exception {
        String send = fromClasspath("sanitizer/cid-background.htm");
        String content = sanitizer(props().sanitizerUri()).secproxy(send);
        assertThat(content, containsString("background:#FFFDCD url('cid:f1cf9a12860b769cd5a6dceec270aeae');"));
    }

    @Test
    @Title("Не вырезаем фон ячеек в html-письмах с таблицами")
    @Issue("DARIA-27628")
    public void testSanitizerShouldNOTRemoveBackgroundInHtmlTable() throws Exception {
        String send = fromClasspath("sanitizer/tablebg.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("Background был вырезан", content, containsString("background"));
    }


    @Test
    @Issues({@Issue("DARIA-29487"), @Issue("SANITIZER-65")})
    public void sanitizerShouldNOTRemoveMinWidthAndMaxWidth() throws Exception {
        String send = fromClasspath("sanitizer/max-width.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("min-width был вырезан",
                content, and(containsString("min-width"), containsString("10px")));
        assertThat("max-width был вырезан",
                content, and(containsString("max-width"), containsString("100500px")));
    }

    @Test
    @Issue("SANITIZER-64")
    public void sanitizerShouldRemovePingAndDownloadOfA() throws Exception {
        String send = fromClasspath("sanitizer/ping-in-a.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("download не был вырезан", content, not(containsString("download")));
        assertThat("ping не был вырезан", content, not(containsString("ping")));
    }

    @Test
    @Title("Должны заменять рандомом id целевого элемента")
    @Description("* Проверяем, что санитайзер НЕ\n" +
            "* вырезает id у целевого элемента.\n" +
            "* Проверяем, что  заменяет их рандомными\n" +
            "* в a@href и element@id")
    @Issue("SANITIZER-21")
    public void testSanitizerWithAnchorLink() throws Exception {
        String send = fromClasspath("sanitizer/anchor.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        /* echo "#anchor7896" | md5sum
         считаем с переводом строки  */

        String md5 = md5().hashString("#anchor7896\n", Charsets.UTF_8).toString();
        System.out.println(content);

        assertThat("Сантайзер портит ссылку ",
                content, containsString(" <a href=\"http://anchor.com/"));
        assertThat("Сантайзер добавляет md5 к неякорной ссылке ",
                content, containsString("<p name=\"anchor\">here</p>"));
        assertThat("Сантайзер вырезает  href у целевого элемента ",
                content, containsString("<a href=\"#anchor" + md5));
        assertThat("Сантайзер вырезает id у целевого элемента ",
                content, containsString("<p id=\"anchor" + md5));
    }


    //__________________


    @Test
    @Title("Вырезаем src в спаме")
    @Issues({@Issue("SANITIZER-36"), @Issue("DARIA-22826")})
    @Stories("Обработка спама")
    public void testSanitizerIsSpamWithImgSrc() throws Exception {
        String send = fromClasspath("sanitizer/spam-img-src.htm");
        String content = sanitizer(props().sanitizerUri()).spam(send);
        assertThat("Src был не вырезан", content, not(containsString("src")));
    }

    @Test
    @Title("Вырезаем background у картинки в спаме")
    @Issues({@Issue("SANITIZER-36"), @Issue("DARIA-22826")})
    @Stories("Обработка спама")
    public void testSanitizerIsSpamWithImgBackground() throws Exception {
        String send = fromClasspath("sanitizer/spam-bg-img.htm");
        String content = sanitizer(props().sanitizerUri()).spam(send);
        assertThat("Background был не вырезан", content, not(containsString("background")));

    }

    @Test
    @Title("Вырезаем href у ссылок в спаме")
    @Issues({@Issue("DARIA-38740")})
    @Stories("Обработка спама")
    public void shouldRemoveHrefInSpam() throws Exception {
        String send = fromClasspath("sanitizer/url-space.htm");
        String content = sanitizer(props().sanitizerUri()).spam(send);
        assertThat("href был не вырезан", content, not(containsString("href")));
    }

    //__________________

    @Test
    @Title("Должны не показывать \"зло\" вместо интерфейса")
    @Description("Письмо с EVIL перекрывало интерфейс почты, вырезаем style")
    @Issue("DARIA-42322")
    public void shouldNotSeeEvil() throws IOException {
        String send = fromClasspath("sanitizer/evil.htm");
        String content = sanitizer(props().sanitizerUri()).prHttps(send);
        assertThat("Style у \"злого\" письма не был вырезан", content,
                not(containsString("<div style=\"color:'qwe;font-size:&quot;10';position: fixed;left:0;" +
                        "top:0;font-size:250px;color:red;z-index:10000;qwe:a&quot;;\">EVIL</div>")));

        assertThat("Вырезали <div>EVIL</div>", content, containsString("<div>EVIL</div>"));
    }

    @Test
    @Issue("DARIA-53789")
    @Title("Ручка mail_spam. Должны вырезать data-vdir-href и data-orig-href")
    public void shouldRemoveVdirHrefAndOriginHrefReqSpam() throws IOException {
        String send = fromClasspath("sanitizer/data-vdir-href.htm");
        String exp = "<a>click</a>";
        String content = sanitizer(props().sanitizerUri()).spam(send);
        assertThat("Должны вырезать все аттрибуты", content, containsString(exp));
        assertThat("Должны вырезать <data-vdir-href>", content, not(containsString("data-vdir-href")));
        assertThat("Должны вырезать <data-orig-href>", content, not(containsString("data-orig-href")));
    }

    @Test
    @Issue("DARIA-53789")
    @Title("Ручка mail_secproxy. Должны вырезать data-vdir-href и data-orig-href")
    public void shouldRemoveVdirHrefAndOriginHrefReqMailSecproxy() throws IOException {
        String send = fromClasspath("sanitizer/data-vdir-href.htm");
        String exp = "<a>click</a>";
        String content = sanitizer(props().sanitizerUri()).secproxy(send);
        assertThat("Должны вырезать все аттрибуты", content, containsString(exp));
        assertThat("Должны вырезать <data-vdir-href>", content, not(containsString("data-vdir-href")));
        assertThat("Должны вырезать <data-orig-href>", content, not(containsString("data-orig-href")));
    }

    @Test
    @Issue("DARIA-53789")
    @Title("Ручка mail_proxy. Должны вырезать data-vdir-href и data-orig-href")
    public void shouldRemoveVdirHrefAndOriginHrefReqMailProxy() throws IOException {
        String send = fromClasspath("sanitizer/data-vdir-href.htm");
        String exp = "<a>click</a>";
        String content = sanitizer(props().sanitizerUri()).proxy(send);
        assertThat("Должны вырезать все аттрибуты", content, containsString(exp));
        assertThat("Должны вырезать <data-vdir-href>", content, not(containsString("data-vdir-href")));
        assertThat("Должны вырезать <data-orig-href>", content, not(containsString("data-orig-href")));
    }

    @Test
    @Issue("DARIA-57065")
    @Title("Ручка mail_proxy. Удаление data-атрибутов и nanoislands")
    public void shouldRemoveDataReqMailProxy() throws IOException {
        String send = fromClasspath("sanitizer/data-nb.htm");
        String content = sanitizer(props().sanitizerUri()).proxy(send);
        assertThat("Должны вырезать <data-nb>", content, not(containsString("data-nb")));
        assertThat("Должны вырезать <data-nb-input>", content, not(containsString("data-nb-input")));
    }
}
