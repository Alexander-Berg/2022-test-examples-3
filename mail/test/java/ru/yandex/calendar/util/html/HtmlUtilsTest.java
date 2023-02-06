package ru.yandex.calendar.util.html;

import ru.yandex.misc.test.Assert;
import org.junit.Test;

/**
 * @author gutman
 */
public class HtmlUtilsTest {

    @Test
    public void html2Text() {
        String s = "Hello, world!";
        String html1 = "<a>Hello, world!</a>";
        String html2 = "<x>Hello, world!</x>";
        String html3 = "Hello,<br> world!";
        String html4 = "Hello,<br>world!";

        Assert.A.equals(s, HtmlUtils.html2text(html1));
        Assert.A.equals(s, HtmlUtils.html2text(html2));
        Assert.A.equals(s, HtmlUtils.html2text(html3));
        Assert.A.equals(s, HtmlUtils.html2text(html4));

        Assert.A.equals("Procter&Gamble", HtmlUtils.html2text("Procter&amp;Gamble"));
        String withHead =
                "<head>" +
                    "<style>.stl { margin-left: 1pt; padding-left: 4pt;}</style>\n" +
                "</head>\n" +
                "<body>" +
                    "Превед" +
                "</body>";
        Assert.A.equals("Превед", HtmlUtils.html2text(withHead));
    }
}
