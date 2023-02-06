package ru.yandex.market.partner.content.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.yandex.market.partner.content.common.utils.StringUtil.URL_PATTERN;
import static ru.yandex.market.partner.content.common.utils.StringUtil.URL_PATTERN_WITH_WL_VENDORS;

public class StringUtilTest {
    private static Pattern NEG_URL_PATTERN_WITH_WL_VENDORS =
        Pattern.compile(StringUtil.negateUrlRegex(URL_PATTERN_WITH_WL_VENDORS.toString()),
            Pattern.CASE_INSENSITIVE);
    private static Pattern NEG_URL_PATTERN = Pattern.compile(
        StringUtil.negateUrlRegex(URL_PATTERN.toString()), Pattern.CASE_INSENSITIVE);

    @Test
    public void containsCDATA() {
        Assert.assertTrue(StringUtil.containsCDATA("<![CDATA[значение]]>"));
        Assert.assertTrue(StringUtil.containsCDATA("\u003c![CDATA[значение]]\u003e"));
        Assert.assertTrue(StringUtil.containsCDATA("\\u003c![CDATA[значение]]\\u003e"));

        Assert.assertFalse(StringUtil.containsCDATA("CDATA-1"));
        Assert.assertFalse(StringUtil.containsCDATA("[CDATA]"));
        Assert.assertFalse(StringUtil.containsCDATA(""));
    }

    @Test
    public void replaceNewLineTagsWithBR() throws Exception {
        Assert.assertEquals(
            "test text<br>\nnew line",
            StringUtil.replaceNewLineTagsWithBR("<p>test   text</p>\n<P> new line </P> ")
        );
    }

    @Test
    public void lineBreakTagsAreAllowedForDescription() {
        Assert.assertFalse(StringUtil.containsHtmlTags(true, " test <br>"));
        Assert.assertFalse(StringUtil.containsHtmlTags(true, " test <br/>"));
        Assert.assertFalse(StringUtil.containsHtmlTags(true, " test <br></br>"));
        Assert.assertFalse(StringUtil.containsHtmlTags(true, " test <p></br>"));
    }

    @Test
    public void nonLineBreakTagsAreProhibitedInDescription() {
        Assert.assertTrue(StringUtil.containsHtmlTags(true, " test <img> text "));

        Assert.assertTrue(StringUtil.containsHtmlTags(true, "<strong Сумка мужская Stampa Brown."));
    }

    @Test
    public void anyHtmlTagIsProhibitedForNonDescription() {
        Assert.assertTrue(StringUtil.containsHtmlTags(false, " test <img> text "));
        Assert.assertTrue(StringUtil.containsHtmlTags(false, " test <br>"));
        Assert.assertTrue(StringUtil.containsHtmlTags(false, " test <br/>"));
        Assert.assertTrue(StringUtil.containsHtmlTags(false, " test <br></br>"));
        Assert.assertTrue(StringUtil.containsHtmlTags(false, " <p> input "));
    }

    @Test
    public void notHtmlTagsAreAllowed() {
        Assert.assertFalse(StringUtil.containsHtmlTags(true, "  input <> dsd"));
        Assert.assertFalse(StringUtil.containsHtmlTags(false, "  input <> dsd"));

        Assert.assertFalse(StringUtil.containsHtmlTags(true, "  input <///ttt> dsd"));
        Assert.assertFalse(StringUtil.containsHtmlTags(false, "  input <> dsd"));
    }

    @Test
    public void normalizeSpace() throws Exception {
        Assert.assertEquals(
            "test text\n\nnew line",
            StringUtil.normalizeSpace("\ntest   text \n\n  \n \r\t new line\n ")
        );
    }

    @Test
    public void cleanStringWithoutTags() {
        Assert.assertEquals("", StringUtil.cleanStringWithoutTags("  \t"));
        Assert.assertEquals("", StringUtil.cleanStringWithoutTags("  \n \t \n    "));
        Assert.assertEquals("cat", StringUtil.cleanStringWithoutTags("  \n cat\t \n    "));

        Assert.assertEquals("", StringUtil.cleanStringWithoutTags("<br>"));
        Assert.assertEquals("", StringUtil.cleanStringWithoutTags("<br/>"));
        Assert.assertEquals("", StringUtil.cleanStringWithoutTags("<p>"));
        Assert.assertEquals("happy cat", StringUtil.cleanStringWithoutTags(" happy <b>  cat <p>"));
    }

    @Test
    public void sanitizeString() throws Exception {
        Assert.assertNull(StringUtil.sanitizeString(null));

        Assert.assertNull(StringUtil.sanitizeString("\n    \n\n  \n \r\t  \n "));

        Assert.assertEquals(
            "test",
            StringUtil.sanitizeString("test  \u0003")
        );

        Assert.assertEquals(
                "test me",
                StringUtil.sanitizeString("test \tme")
        );

        Assert.assertEquals(
            "test text new line",
            StringUtil.sanitizeString("\ntest   text \n\n  \n \r\t new line\n ")
        );
    }

    @Test
    public void checkForUrl() throws Exception {
        String url = "http://ya.ru";
        String url2 = "Товары с OZON.ru";
        String url3 = "НЕ ЕШЬТЕ ЭТИ ТРИ ПРОДУКТА ЧТОБЫ... http://agp.fish/media/products/18.12.07/1500x1500_Udochka_zimnyaya_Pirs_50___B_ABSPK_zhelto_chernaya.jpg and more";
        String url4 = "ВРАЧИ В ШОКЕ! https://yadi.sk/i/OvQnKvcc6Yz6gw";
        String url5 = "подробно www.educaborras.com В комплект входит сухой клей";
        String url6 = "WoodRing.Ru";

        // Non whitelist links are present
        String url7 = "http://ya.ru а также mail.ru";
        String url8 = "WoodRing.Ru а также mail.ru";

        // Link does not fully match the link in domain
        String url9 = "товар от http://subdomain.mail.ru";
        String url10 = "товар от mail.ru/coronavirusIsALie";
        String url11 = "товар от blahblahmail.ru";
        String url12 = "hm.mail.ru";

        String notUrl = "ozon ru";
        String notUrl2 = "товары .рф";
        String notUrl3 = "арт.RU156";
        String notUrlBecauseWhiteList = "товар от mail.ru ммм";
        String notUrlBecauseWhiteList2 = "mail.ru";
        String notUrlBecauseWhiteList3 = "товар от hB-tex.Ru ммм, его не ешь, подумой";

        List<String> urls = Arrays.asList(url, url2, url3, url4, url5, url6, url7, url8, url9, url10, url11, url12);
        List<String> notUrls = Arrays.asList(notUrl, notUrl2, notUrl3);
        List<String> vendorUrls =
            Arrays.asList(notUrlBecauseWhiteList, notUrlBecauseWhiteList2, notUrlBecauseWhiteList3);

        for (String u : urls) {
            Assert.assertTrue("For \"" + u + "\":", StringUtil.checkForUrl(u, true));
            Assert.assertTrue("For \"" + u + "\":", StringUtil.checkForUrl(u, false));
            Assert.assertFalse("For \"" + u + "\":", checkForUrlNeg(u, true));
            Assert.assertFalse("For \"" + u + "\":", checkForUrlNeg(u, false));
        }

        for (String u : notUrls) {
            Assert.assertFalse("For \"" + u + "\":", StringUtil.checkForUrl(u, true));
            Assert.assertFalse("For \"" + u + "\":", StringUtil.checkForUrl(u, false));
            Assert.assertTrue("For \"" + u + "\":", checkForUrlNeg(u, true));
            Assert.assertTrue("For \"" + u + "\":", checkForUrlNeg(u, false));
        }

        for (String u : vendorUrls) {
            Assert.assertFalse("For \"" + u + "\":", StringUtil.checkForUrl(u, true));
            Assert.assertTrue("For \"" + u + "\":", StringUtil.checkForUrl(u, false));
            Assert.assertTrue("For \"" + u + "\":", checkForUrlNeg(u, true));
            Assert.assertFalse("For \"" + u + "\":", checkForUrlNeg(u, false));
        }
    }

    private static boolean checkForUrlNeg(String rawParamValue, boolean allowWhiteListedDomains) {
        Matcher matcher = allowWhiteListedDomains
            ? NEG_URL_PATTERN_WITH_WL_VENDORS.matcher(rawParamValue)
            : NEG_URL_PATTERN.matcher(rawParamValue);
        return org.apache.commons.lang3.StringUtils.isEmpty(rawParamValue)
            || matcher.find();
    }

    @Test
    public void sanitizeDescription() {
        Assert.assertEquals(
            "I m a lit\\t&lt;///dont remove&gt;le\ntest",
            StringUtil.sanitizeDescription("<qwerty>I  <mnb>m\t a l</bebebe>it\\t<///dont remove>le\ntest\n")
        );
    }

    @Test
    public void stripInvalidChars() {
        Assert.assertEquals(
            "Бейсболка Очки",
            StringUtil.stripInvalidChars("\uD83E\uDDE2Бейсболка \uD83D\uDC53Очки")
        );
    }
}
