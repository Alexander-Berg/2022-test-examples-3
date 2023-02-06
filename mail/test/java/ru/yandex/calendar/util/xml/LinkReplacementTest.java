package ru.yandex.calendar.util.xml;

import lombok.val;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;
import org.junit.Test;
import org.springframework.web.util.HtmlUtils;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.jdom.JdomUtils;

/**
 * @author dbrylev
 */
public class LinkReplacementTest {

    @Test
    public void replace() {
        Assert.equals("", replaceAsText(""));
        Assert.equals("<a href=\"http://mail.ru\">mail.ru</a>", replaceAsText("mail.ru"));

        Assert.equals("\t<a href=\"http://ya.ru/xxx?x=y#47\">http://ya.ru/xxx?x=y#47</a>\t",
                replaceAsText("\thttp://ya.ru/xxx?x=y#47\t"));

        Assert.equals("<a href=\"http://www.1.ru\">www.1.ru</a>\t<a href=\"http://www.2.ru\">www.2.ru</a>",
                replaceAsText("www.1.ru\twww.2.ru"));
    }

    @Test
    public void ascii() {
        Assert.equals("<a href=\"http://xn--80a1acny.xn--p1ai/%D0%BF%D0%BE%D1%81%D1%8B%D0%BB%D0%BA%D0%B0" +
                        "?%D0%B8%D0%B4=1#статус\">почта.рф/посылка?ид=1#статус</a>",
                replaceAsText("почта.рф/посылка?ид=1#статус"));
    }

    @Test
    public void startrek() {
        Assert.equals("(<a href=\"https://st.ru/NAVI/updated/filter?resolution=empty()\">" +
                "https://st.ru/NAVI/updated/filter?resolution=empty()</a>)",
                replaceAsText("(https://st.ru/NAVI/updated/filter?resolution=empty())"));
    }

    @Test
    public void miro() {
        Assert.equals("(<a href=\"https://miro.com/app/board/o0J_IX74Hcc=/\">" +
                "https://miro.com/app/board/o0J_IX74Hcc=/</a>)",
                replaceAsText("(https://miro.com/app/board/o0J_IX74Hcc=/)"));
    }

    @Test
    public void lookAround() {
        Assert.equals(":<a href=\"http://ya.ru\">ya.ru</a>:", replaceAsText(":ya.ru:"));
        Assert.equals("\n<a href=\"http://ya.ru\">ya.ru</a>\n", replaceAsText("\nya.ru\n"));

        Assert.equals("xhttp://ya.ru", replaceAsText("xhttp://ya.ru"));
        Assert.equals("httpx://ya.ru", replaceAsText("httpx://ya.ru"));
        Assert.equals("ya.rux", replaceAsText("ya.rux"));

        Assert.equals("xwww.ya.rux", replaceAsText("xwww.ya.rux"));
        Assert.equals("<a href=\"http://www.ya.rux\">www.ya.rux</a>", replaceAsText("www.ya.rux"));
    }

    @Test
    public void comaEnd() {
        Assert.equals("<a href=\"http://ya.ru/page\">ya.ru/page</a>,", replaceAsText("ya.ru/page,"));
        Assert.equals("<a href=\"http://ya.ru/page?x=y\">ya.ru/page?x=y</a>,", replaceAsText("ya.ru/page?x=y,"));
    }

    @Test
    public void mailto() {
        Assert.equals(":<a href=\"mailto:s.om_e@ya.ru\">s.om_e@ya.ru</a>:", replaceAsText(":s.om_e@ya.ru:"));
        Assert.equals("<a href=\"mailto:печкин@почта.рф\">печкин@почта.рф</a>", replaceAsText("печкин@почта.рф"));
    }

    //GREG-258
    @Test
    public void trailingSlash() {
        val expected = "<a href=\"https://a-s-m.bitrix24.ru/crm/deal/details/90175/\">" +
                            "https://a-s-m.bitrix24.ru/crm/deal/details/90175/</a>";
        val actual = replaceAsText("https://a-s-m.bitrix24.ru/crm/deal/details/90175/");
        Assert.equals(expected, actual);
    }

    private static String replaceAsText(String text) {
        return asText(LinkReplacement.replace(text));
    }

    private static String asText(ListF<Content> contents) {
        StringBuilder sb = new StringBuilder();

        for (Content c : contents) {
            if (c instanceof Element) {
                sb.append(JdomUtils.I.writeElementToString((Element) c));
            } else if (c instanceof Text) {
                sb.append(HtmlUtils.htmlEscape(((Text) c).getText()));
            } else {
                throw new IllegalStateException("Unexpected content type " + c.getClass());
            }
        }
        return sb.toString();

    }
}
