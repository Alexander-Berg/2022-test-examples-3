package org.owasp.html;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HtmlsOptimizeTest {

    private final String message;
    private final String html;
    private final String expected;

    public HtmlsOptimizeTest(String message, String html, String expected) {
        this.message = message;
        this.html = html;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "Должны склейт пробелы",
                        "<a>  </a>",
                        "<a> </a>"
                },
                {
                        "Должны удалить лишний пробел перед закрывающим тэгом",
                        "<a />",
                        "<a/>"
                },
                {
                        "Должны удалить лишний пробел перед закрывающим тэгом",
                        "<a ></a>",
                        "<a></a>"
                },
                {
                        "Должны удалить лишний пробелы перед присваиванием значения аттрибуту",
                        "<a href = \"http://value/\"/>",
                        "<a href=\"http://value/\"/>"
                },
                {
                        "Должны сохранять пробелы в значениях параметров",
                        "<a alt=\"  bug   afix  \"/>",
                        "<a alt=\"  bug   afix  \"/>"
                },
                {
                        "Должны удалять пустые строки",
                        "<a/>\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "<b/>",
                        "<a/>\n" +
                                "<b/>"
                },
                {
                        "Должны оставлять DOCTYPE и в нем удалять лишние пробелы",
                        "<!DOCTYPE    HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3" +
                                ".org/TR/html4/loose.dtd\">\n" +
                                "<html>" +
                                "</html>",
                        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3" +
                                ".org/TR/html4/loose.dtd\">\n" +
                                "<html>" +
                                "</html>"
                },
                {
                        "Должны удалять комментарии",
                        "<!-- will be removed -->",
                        ""
                },
                {
                        "Должны оставлять комментарии комментарии начинающиеся с <!--[ и в них удалять лишние пробелы",
                        "<!--[if (gte mso 9)|(IE)]>\n" +
                                "    <a>  </a>\n" +
                                "    <![endif]-->\n",
                        "<!--[if (gte mso 9)|(IE)]>\n" +
                                "<a> </a>\n" +
                                "<![endif]-->\n"
                },
                {
                        "Должны оставлять комментарии комментарии начинающиеся с <!--< и в них удалять лишние пробелы",
                        "<!--<if (gte mso 9)|(IE)]>\n" +
                                "    <a>        </a>\n" +
                                "    <![endif]-->\n",
                        "<!--<if (gte mso 9)|(IE)]>\n" +
                                "<a> </a>\n" +
                                "<![endif]-->\n"
                },
        });
    }

    @Test
    public void doCheck() {
        String result = Htmls.optimize(html);
        Assert.assertEquals(message, expected, result);
    }
}
