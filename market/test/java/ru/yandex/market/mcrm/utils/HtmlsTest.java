package ru.yandex.market.mcrm.utils;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.mcrm.utils.html.Htmls;
import ru.yandex.market.mcrm.utils.html.SafeUrlService;

public class HtmlsTest {

    private static final Htmls htmls;

    static {
        var mock = Mockito.mock(SafeUrlService.class);
        Mockito.when(mock.toSafeUrl(Mockito.any(String.class))).then(inv -> inv.getArgument(0) + "_");
        htmls = new Htmls(mock);
    }

    @Test
    public void safeHtml() {
        String text = "<div><strong>Восточный экспресс</strong> (<a href=\"" +
                "https://ru.wikipedia.org/wiki/Французский_язык" +
                "\" title=\"Французский язык\">фр.</a> <em lang=\"fr\" " +
                "style=\"font-style:italic\">Orient-Express</em>) —" +
                " <a href=\"" +
                "https://ru.wikipedia.org/wiki/Пассажирский_поезд" +
                "\" title=\"Пассажирский поезд\">пассажирский поезд</a> класса «люкс» частной компании Orient-Express" +
                " " +
                "Hotels, курсирующий между <a href=\"" +
                "https://ru.wikipedia.org/wiki/Париж" +
                "\" title=\"Париж\">Парижем</a> и Константинополем (<a href=\"" +
                "https://ru.wikipedia.org/wiki/Стамбул" +
                "\" title=\"Стамбул\">Стамбулом</a>) с <a href=\"" +
                "https://ru.wikipedia.org/wiki/1883_год" +
                "\" title=\"1883 год\">1883 года</a>.</div><img src=\"" +
                "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Aff_ciwl_orient_express4_jw.jpg/" +
                "408px-Aff_ciwl_orient_express4_jw.jpg" +
                "\" />";
        String expected = "<div><strong>Восточный экспресс</strong> (<a href=\"" +
                "https://ru.wikipedia.org/wiki/Французский_язык_" +
                "\" target=\"_blank\" rel=\"noopener noreferrer\">фр.</a> <em>Orient-Express</em>) — <a href=\"" +
                "https://ru.wikipedia.org/wiki/Пассажирский_поезд_" +
                "\" target=\"_blank\" rel=\"noopener noreferrer\">пассажирский поезд</a> класса «люкс» частной " +
                "компании " +
                "Orient-Express Hotels, курсирующий между <a href=\"" +
                "https://ru.wikipedia.org/wiki/Париж_" +
                "\" target=\"_blank\" rel=\"noopener noreferrer\">Парижем</a> и Константинополем (<a href=\"" +
                "https://ru.wikipedia.org/wiki/Стамбул_" +
                "\" target=\"_blank\" rel=\"noopener noreferrer\">Стамбулом</a>) с <a href=\"" +
                "https://ru.wikipedia.org/wiki/1883_год_" +
                "\" target=\"_blank\" rel=\"noopener noreferrer\">1883 года</a>.</div><img src=\"" +
                "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Aff_ciwl_orient_express4_jw.jpg/" +
                "408px-Aff_ciwl_orient_express4_jw.jpg_" +
                "\" />";
        Assertions.assertEquals(expected, eraseSign(htmls.safeHtml(text)));
    }

    @Test
    public void toTrimText() {
        String text = "&nbsp;     123";
        String expected = "      123";
        Assertions.assertEquals(expected, htmls.toText(text));
    }

    @Test
    public void twoTextWithSpaceTest() {
        String text = "<div>Заказ <a href=\"https://sba.yandex.net/redirect?url&#61;https%3A%2F%2Fow.tst.market" +
                ".yandex-team.ru%2Forder%2F4489334&amp;client&#61;market_support&amp;sign&#61;" +
                "52f52820fa293aab31e773a76a520f7a\" target=\"_blank\" rel=\"noopener " +
                "noreferrer\">4489334</a></div>";
        String expected = "Заказ 4489334\n";
        Assertions.assertEquals(expected, htmls.toText(text));
    }

    @Test
    public void toText() {
        String text = "<p><div><div><div></div></div></div><div><div><div><div><div>HTML тело письма</div><div>из" +
                "</div><div><div>нескольких</div></div><div>строк</div></div></div></div></div><div><div><div>" +
                "HTML тело письма</div><div>из</div><div><div>нескольких</div></div><div>строк</div></div></div><p>";
        String expected = "HTML тело письма\nиз\nнескольких\n\nстрок\n\nHTML тело письма\nиз\nнескольких\n\nстрок\n\n";
        Assertions.assertEquals(expected, htmls.toText(text));
    }

    @Test
    public void safeHtmlLink() {
        String text = "<a href=\"" +
                "https://ru.wikipedia.org/wiki/Бумтанг#Административное_деление" +
                "\" style=\"background-image:none;color:rgb( 11 , 0 , 128 );text-decoration-line:none\"><span " +
                "style=\"" +
                "color:#222222;padding-left:0px;padding-right:0.5em;text-decoration-color:inherit;" +
                "text-decoration-line:" +
                "inherit;text-decoration-style:inherit\">1</span><span style=\"text-decoration-color:inherit;" +
                "text-decoration-line:inherit;text-decoration-style:inherit\">География</span></a><img src=\"" +
                "https://yandex.ru" +
                "\">";
        String expected = "<a href=\"" +
                "https://ru.wikipedia.org/wiki/Бумтанг#Административное_деление_" +
                "\" target=\"_blank\" rel=\"noopener noreferrer\">1 География </a><img src=\"" +
                "https://yandex.ru_" +
                "\" />";
        Assertions.assertEquals(expected, eraseSign(htmls.safeHtml(text)));
    }

    @Test
    public void normalizeImageCid() {
        String cid = "10498061561615651@sas1-d856b3d759c7.qloud-c.yandex.net";
        String name = "file.txt";
        String url = "yandex.ru";
        String text = "<div>TEST<img src=\"cid:" + cid + "\" />TEST</div>";

        String normalized = htmls.normalizeImageCid(text, cid, url);
        text = "<div>TEST<img src=\"" + url + "\" />TEST</div>";
        Assertions.assertEquals(text, normalized);

        normalized = htmls.safeHtml(text, ImmutableMap.of(url, name));
        text = "<div>TEST " + name + " (см. вложения) TEST</div>";
        Assertions.assertEquals(text, normalized);
    }

    @Test
    public void removeQuotes() {
        String text = "<div>test with blockquote</div><blockquote>blockquoted text</blockquote><div>test</div>";
        String normalized = htmls.removeQuotes(text);
        text = "<div>test with blockquote</div><div>test</div>";
        Assertions.assertEquals(text, normalized);
    }

    @Test
    public void hideQuotes() {
        String text = "<div>test with blockquote</div><blockquote>blockquoted text</blockquote>";
        String normalized = htmls.hideQuotes(text);
        text = "<div>test with blockquote</div><button></button><blockquote>blockquoted text</blockquote>";
        Assertions.assertEquals(text, normalized);
    }

    @Test
    public void hideButtonAttributes() {
        String text = "<div>test with blockquote</div><button type=\"submit\" " +
                "style='123'>123</button><blockquote>blockquoted text</blockquote>";
        String normalized = htmls.safeHtml(text);
        text = "<div>test with blockquote</div><button></button><blockquote>blockquoted text</blockquote>";
        Assertions.assertEquals(text, normalized);
    }

    @Test
    public void whiteSpaceDiv() {
        String text = "<p>123</p>";
        String normalized = htmls.toHtml(text);
        normalized = htmls.safeHtml(normalized);
        text = "<div style=\"" + Htmls.WHITE_SPACE + "\">&lt;p&gt;123&lt;/p&gt;</div>";
        Assertions.assertEquals(text, normalized);
    }

    @Test
    public void removeDivAttributes() {
        String text = "<div style=\"background:black\"><p>123</p></div>";
        String normalized = htmls.safeHtml(text);
        text = "<div><p>123</p></div>";
        Assertions.assertEquals(text, normalized);
    }

    @Test
    public void glueBlanks() {
        String text = "<br>text<br>text<br>";
        String expected = "text<br />text";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "<br><br>text<br><br>text<br><br>";
        expected = "text<br />text";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "<hr>text<hr>text<hr>";
        expected = "text<hr />text";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "<hr><br><hr>text<br><hr><br>text<hr><br><hr>";
        expected = "text<br />text";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "<p>text</p><p>text</p>";
        expected = "<p>text</p><p>text</p>";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "<p><p>text</p><p><p>text</p>";
        expected = "<p>text</p><p>text</p>";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "<p><br><hr>text<p><br><hr>text<hr><br><p>text<hr><br><p>";
        expected = "<p>text</p><p>text</p><hr />text";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "<p><br><hr>text<p>\n<br>\n<hr>text<hr>\n<br>\n<p>text<hr>\n<br>\n<p>";
        expected = "<p>text</p><p>text</p><hr />text";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "<div><br></div><div><br></div>text<div><br></div><div><br></div>text<div><br></div>text<div><br></div" +
                "><div><br></div>";
        expected = "<div></div><div></div>text<div><br /></div><div></div>text<div><br /></div>text<div><br " +
                "/></div><div></div>";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "text<div><br></div><div><br></div><div><br></div><div><br></div>text";
        expected = "text<div><br /></div><div></div><div></div><div></div>text";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "text<div> </div><div> </div><div> </div><div> </div>text";
        expected = "text<div></div><div></div><div></div><div></div>text";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));

        text = "text<div>&nbsp;</div><div>&nbsp;</div><div>&nbsp;</div><div>&nbsp;</div>text";
        expected = "text<div>\u00A0</div><div></div><div></div><div></div>text";
        Assertions.assertEquals(expected, htmls.glueBlanks(text));
    }

    @Test
    public void limitHtml() {
        String text = "<div></div><div><div><p>HTML</p>тело<div>письма</div><div>из" +
                "</div><div><div>нескольких</div></div></div></div><br>строк";
        String expected = "<div></div><div><div><p>HTML</p>тело<div>письма</div><div>из" +
                "</div><div><div>нескольк</div></div></div></div>";
        Assertions.assertEquals(expected, htmls.limitHtml(text, 24));
    }

    @Test
    public void formatPreTag() {
        String text = "<div></div>\n" +
                      "<div><pre style=\"align-items:baseline\">\n" +
                      "    <div><p>HTML</p>тело\n" +
                      "        <div>письма</div>\n" +
                      "        <pre>Ещё один тег pre</pre>\n" +
                      "    </div>\n" +
                      "</pre>\n" +
                      "<blockquote>blockquoted text</blockquote>\n" +
                      "</div>строк";
        String actual = htmls.addExtraStyles(text, "PrE", List.of("white-space:pre-line", "white-space:wrap"));
        Assertions.assertEquals(2, StringUtils.countMatches(actual, "white-space:pre-line;white-space:wrap"));
    }

    private String eraseSign(String text) {
        return text.replaceAll("&amp;sign&#61;\\w+", "");
    }
}
