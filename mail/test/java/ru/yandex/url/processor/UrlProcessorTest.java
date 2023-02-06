package ru.yandex.url.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ru.yandex.collection.LazyList;
import ru.yandex.function.CollectionConsumer;
import ru.yandex.net.uri.fast.FastUri;
import ru.yandex.net.uri.fast.FastUriParser;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.string.StringUtils;

public class UrlProcessorTest extends TestBase {
    public UrlProcessorTest() {
        super(false, 0L);
    }

    private static List<UrlInfo> extractUrls(final String str) {
        return extractUrls(str, ImmutableUrlProcessorConfig.DEFAULT_CONFIG);
    }

    private static List<UrlInfo> extractUrls(
        final String str,
        final ImmutableUrlProcessorConfig config)
    {
        List<UrlInfo> urls = new ArrayList<>();
        UrlProcessor processor =
            new UrlProcessor(new CollectionConsumer<>(urls), config);
        processor.process(str.toCharArray());
        processor.process();
        return urls;
    }

    private static List<String> extractUrlsAsStrings(final String str) {
        return extractUrlsAsStrings(
            str,
            ImmutableUrlProcessorConfig.DEFAULT_CONFIG);
    }

    private static List<String> extractUrlsAsStrings(
        final String str,
        final ImmutableUrlProcessorConfig config)
    {
        return new LazyList<>(
            extractUrls(str, config),
            urlInfo -> urlInfo.url().toString(true));
    }

    private static void assertExtractedSame(final String... urls) {
        YandexAssert.check(
            YandexAssert.checkersFor(urls),
            new LazyList<>(
                extractUrls(StringUtils.join(urls, ' ')),
                urlInfo -> urlInfo.url().toString(true)));
    }

    private static FastUri parse(final String uri) throws Exception {
        return new FastUriParser(uri).parse();
    }

    @Test
    public void testFullUrls() {
        assertExtractedSame(
            "http://yandex.ru",
            "http://mail.google.com/some?user=potapov.d@gmail.com#hash",
            "http://http.over.https:443",
            "https://sendsay.some.com",
            "ftp://user@base.com/file.jpg");
    }

    @Test
    public void testCustomSchemes() {
        assertExtractedSame(
            "mailto:potapov.d@gmail.com",
            "twitch://directory/all/tags/",
            "tel:+79267227664",
            "svn+ssh://a.yandex-team.ru");
    }

    @Test
    public void testPositions() throws Exception {
        YandexAssert.assertEquals(
            Arrays.asList(
                new UrlInfo(15, 37, parse("https://gmail.com")),
                new UrlInfo(54, 67, parse("http://www.yandex.ru")),
                new UrlInfo(
                    68,
                    106,
                    parse("ftp://user@base.com/?path=here#hash")),
                new UrlInfo(107, 120, parse("https://ya.ru"))),
            extractUrls(
                "la-la-la-la-la https://gmail.com:443/ " // 38
                + "2a.la-la.labuda www.yandex.ru " // 26
                + "ftp://user@base.com:21/?path=here#hash https://ya.ru"));
        YandexAssert.assertEquals(
            Arrays.asList(new UrlInfo(0, 13, parse("https://ya.ru"))),
            new StringUrlsExtractor().extractUrls("https://ya.ru"));

        List<UrlInfo> urls = new ArrayList<>();
        UrlProcessor processor =
            new UrlProcessor(new CollectionConsumer<>(urls));
        processor.process(
            Arrays.copyOf("https://ya.ru".toCharArray(), 200),
            0,
            13);
        processor.process();
        YandexAssert.assertEquals(
            Arrays.asList(new UrlInfo(0, 13, parse("https://ya.ru"))),
            urls);
    }

    // Underscores allowed only on third level domains and deeper
    @Test
    public void testUnderscoreUrls() {
        YandexAssert.assertEquals(
            Arrays.asList(
                "http://www.send_say.google.ru",
                "http://mail_.google.com/some?user=pov.d@gmail.com#hash",
                "https://www._myhost_.ru",
                "https://www._a_.myhost.ru",
                "http://www.ru",
                "http://_.example.com",
                "http://a_b.xn--a___b.ru",
                "http://www._a_.my--host.example"),
            extractUrlsAsStrings(
                "www.send_say.google.ru "
                + "http://mail_.google.com:80/some?user=pov.d@gmail.com#hash "
                + "https://www._myhost_.ru "
                + "https://www._a_.myhost.ru "
                + "www.ru "
                + "http://_.example.com "
                + "http://a_b.xn--a___b.ru "
                + "www._a_.my--host.example:80"));
    }

    @Test
    public void testBareHosts() {
        YandexAssert.check(
            YandexAssert.checkersFor(
                "http://bit.ly",
                "http://яндекс.рф/?arg",
                "http://_a_.b.გე/#info",
                "http://яндекс.рф",
                "http://яндекс.рф/s",
                "http://tracker2.postman.i2p",
                "mailto:potapov.d@gmail.com",
                "mailto:www."
                + "%D0%BB%D0%B5%D0%BD%D0%B8%D0%BD%D0%B3%D1%80%D0%B0%D0%B4"
                + "@%D1%81%D0%BF%D0%B1.%D1%80%D1%83"),
            extractUrlsAsStrings(
                "bit.ly/ яндекс.рф/?arg _a_.b.xn--node/#info "
                + "xn--d1acpjx3f.xn--p1ai http://яндекс.рф/s "
                + "Д.А.Потапов tracker2.postman.i2p/ "
                + "potapov.d@gmail.com www.ленинград@спб.ру"));
    }

    @Test
    public void testIDNccTLD() {
        YandexAssert.check(
            YandexAssert.checkersFor(
                "https://яндекс.рф",
                "http://www.gov.ελ",
                "https://_a_.b.გე",
                "http://мир.тест"),
            extractUrlsAsStrings(
                "https://яндекс.рф www.gov.ελ https://_a_.b.xn--node/ "
                + "https://my-domain.p BDRİRLLİLLR.DÜZLLLLME мир.xn--e1aybc"));
    }

    @Test
    public void testBrandTLD() {
        YandexAssert.check(
            YandexAssert.checkersFor(
                "http://ozon.travel",
                "http://my.travel",
                "http://www.my.labuda"),
            extractUrlsAsStrings(
                "ozon.travel my.travel/ www.my.labuda"));
    }

    @Test
    public void testFtp() {
        YandexAssert.check(
            YandexAssert.checkersFor(
                "ftp://with.port",
                "ftp://with.poort"),
            extractUrlsAsStrings(
                "ftp://with.port ftp://with.poort:21/"));
    }

    @Test
    public void testNormalization() {
        YandexAssert.check(
            YandexAssert.checkersFor("http://oauth.com/%D0%BF"),
            extractUrlsAsStrings("http://@oauth.com/п"));
    }

    @Test
    public void testStricterConfig() throws Exception {
        YandexAssert.check(
            YandexAssert.checkersFor(
                "https://www.yandex.ru",
                "https://тест.ru"),
            extractUrlsAsStrings(
                "www.yandex.ru yandex.ru xn--e1aybc.ru",
                new UrlProcessorConfigBuilder()
                    .defaultScheme("https")
                    .requireSchemeForNonMailto(true)
                    .build()));
    }

    @Test
    public void testTurkish() {
        assertExtractedSame("https://hıliki.com.tr");
        YandexAssert.check(
            YandexAssert.checkersFor("http://hilıki.com"),
            extractUrlsAsStrings("HİLIKİ.COM"));
    }

    @Test
    public void testIp() {
        // Bare IPs requires at least scheme or path
        YandexAssert.check(
            YandexAssert.checkersFor(
                "https://92.168.0.2",
                "http://192.168.0.4:8080/path"),
            extractUrlsAsStrings(
                "192.168.0.1 https://92.168.0.2 192.168.0.3:8080 "
                + "192.168.0.4:8080/path"));
    }

    @Test
    public void testSchemefulLinks() {
        // See DARIA-64178
        YandexAssert.check(
            YandexAssert.checkersFor(
                "https://appⵏe.com",
                "https://apple.com",
                "http://appl.com"),
            extractUrlsAsStrings(
                "hi https://appⵏe.com и https://apple.com. http://appl.com,"));
    }

    @Test
    public void testNullPath() {
        YandexAssert.check(
            YandexAssert.checkersFor(
                "https://apple.com#%D1%84%D1%80%D0%B0%D0%B3%D0%BC%D0%B5%D1%82",
                "https://apple.com?%D0%BA%D0%B2%D0%B5%D1%80%D1%8F",
                "https://apple.com/path"),
            extractUrlsAsStrings(
                "https://apple.com#фрагмет https://apple.com?кверя "
                + "https://apple.com:/path"));
    }

    @Test
    public void testPunct() {
        YandexAssert.check(
            YandexAssert.checkersFor(
                "https://apple.com",
                "http://apple.com/token",
                "mailto:a@gmail.com",
                "https://ru.wikipedia.org/wiki/Kiss_"
                + "(%D0%B7%D0%BD%D0%B0%D1%87%D0%B5%D0%BD%D0%B8%D1%8F)",
                "https://yandex.ru"),
            extractUrlsAsStrings(
                "https://apple.com. http://apple.com/token, a@gmail.com... "
                + "https://ru.wikipedia.org/wiki/Kiss_(значения) "
                + "[something](https://yandex.ru)"));
    }

    @Test
    public void testBareParentheses() {
        YandexAssert.check(
            YandexAssert.checkersFor("mailto:overlord@radrad.ru"),
            extractUrlsAsStrings("Test (overlord@radrad.ru) made"));
    }

    @Test
    public void testIpObfuscation() {
        YandexAssert.check(
            YandexAssert.checkersFor(
                "http://yandex.net@151.248.115.22/path1",
                "http://151.248.115.22",
                "http://151.248.115.22/path3",
                "http://151.248.115.22/path4",
                "http://4.0.0.8",
                "tel:89267227664"),
            extractUrlsAsStrings(
                "http://yandex.net@0x97f87316/path1"
                + " http://2549642006"
                + " http://022776071426/path3"
                + " 0227.0370.0X73.22/path4"
                + " http://4.8"
                + " tel:89267227664"));
    }

    @Test
    public void testNbsp() {
        YandexAssert.check(
            YandexAssert.checkersFor("mailto:test@example.com"),
            extractUrlsAsStrings("test@example.com|\u00a0"));
    }

    @Test
    public void testNFKC() throws Exception {
        // due to unskippable NFKC normalization in IDN (JDK) and IDNA (ICU)
        // punycoded <ﷺ> will become <xn--   -oze6dh5a3fcaccnvdrg0a>,
        // which is not a valid hostname, so, just skip such hostnames
        YandexAssert.assertEmpty(extractUrls("ﷺ.tt"));
    }
}

