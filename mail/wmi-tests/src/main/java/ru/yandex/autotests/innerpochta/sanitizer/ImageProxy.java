package ru.yandex.autotests.innerpochta.sanitizer;

import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.net.URLEncoder;
import java.nio.charset.Charset;

import static com.google.common.hash.Hashing.md5;
import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.sanitizer.SanitizerApi.sanitizer;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.fromClasspath;

/**
 * Как строится хеш:
 * Хеш строится как мд5 от урла+СЕКРЕТНЫЙ_КЛЮЧ
 * Предварительно экранируя в урле все спецсимволы кроме /;,?:@&=+$-_.!~*'()#
 * <p>
 * <p>
 * Хэш в текущий момент считается так:
 * echo -n 'mx.voevodyno-group.com/thumb.php?src=uploads/mails/0007/007_1.jpg%2526w=540%2526h=158<SECRET_KEY>' | md5sum
 * Секретный хранится в конфиге санитайзера
 */
@Aqua.Test
@Title("Проксирование картинки через кешер")
@Description("Проверка, что картинки проксируются через resize.yandex.net")
@Features(MyFeatures.SANITIZER)
@Stories(MyStories.SANITIZER)
@Credentials(loginGroup = "ImageProxy")
@Ignore("MAILDEV-1108")
public class ImageProxy {

    public static String SECRET_KEY = "cb3bc5fb1542f6aab0c80eb84a17bad9";

    public static final String RESIZE_MAIL = "https://resize.yandex.net/mailservice?url=";

//    use for debug portForwarding.local():
//    @ClassRule
//    public static SshLocalPortForwardingRule portForwarding = viaRemoteHost(props().betaURI())
//            .forwardTo(props().betaURI())
//            .withForwardToPort(props().getSanitizerPort()).onLocalPort(localPortForMocking());

    @Test
    @Title("Должны проксировать src в img через кешер, даже с переводом строки")
    @Description("Проверка, что картинки содержащие перевод строки в адресе" +
            " проксируются через resize.yandex.net")
    public void httpProxy() throws Exception {
        String text = fromClasspath("sanitizer/proxy/new-line-src2.htm");

        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net/mail/",
                result, startsWith(RESIZE_MAIL));
    }


    @Test
    @Title("Не должны проксировать через кешер maps.googleapis.com")
    @Description("* Отправляем картинку как тег img содержащую в src хост maps.googleapis.com\n" +
            "* Выцепляем ее src и смотрим, что адресс не прокируется через кешер\n")
    @Issues({@Issue("DARIA-26469"), @Issue("SANITIZER-58")})
    public void notHttpProxyForGoogleapis() throws Exception {
        String text = fromClasspath("sanitizer/proxy/google-apis.htm");

        String result = getImgSrc(text);
        assertThat("[DARIA-26469] Картинка c maps.googleapis.com проксируется через кешер resize.yandex.net/mail/",
                result, not(containsString("resize.yandex.net")));

        assertThat("[SANITIZER-58] Картинка c maps.googleapis.com имеет неправильный src",
                result, containsString("https://maps.googleapis.com/cb0d41bdf5.jpg"));
    }

    @Test
    @Title("Картинка с переводом строки в адресе должна проксироваться через кешер")
    @Issue("DARIA-16505")
    public void httpProxyImgWithBrInSrc() throws Exception {
        String text = fromClasspath("sanitizer/proxy/new-line-src.htm");

        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net",
                result, startsWith(RESIZE_MAIL));
    }

    @Test
    @Title("Картинки проксируются, если содержат пробелы в урле")
    @Issue("DARIA-18015")
    public void httpProxyImageWithBspInUrl() throws Exception {
        String text = fromClasspath("sanitizer/proxy/src-space.htm");

        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net",
                result, startsWith(RESIZE_MAIL));
    }

    @Test
    @Title("Должны проксировать картинки с очень длинным урлом")
    @Issue("DARIA-14413")
    public void httpProxyLongUrl() throws Exception {
        String text = "<html><body>" +
                "<img src='http://img7-fotki.yandex.net/get/6207/140147405.10/0_STATIC8e8e7_bd93dbfc_L?"
                + Util.getLongString() + Util.getLongString() + Util.getLongString() + "1'></img>" +
                "</body></html>";

        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net",
                result, startsWith(RESIZE_MAIL));
    }

    @Test
    @Title("Должны кешировать картинку с русскими символами в урле")
    public void httpProxyRusUrl() throws Exception {
        String text = fromClasspath("sanitizer/proxy/rus-url.htm");

        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net",
                result, startsWith(RESIZE_MAIL));
    }

    @Test
    @Issue("MAILADM-4048")
    @Title("Должны эскейпить определенные спецсимволы в урле")
    public void httpProxyNotEncodedUrl() throws Exception {
        String text = fromClasspath("sanitizer/proxy/escaped-symbols.htm");
        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net",
                result, startsWith(RESIZE_MAIL));

        assertThat("Неправильно эскейпим картинку [MAILADM-4048]", result,
                containsString("http%3A%2F%2Fimage.klm-mail.com%2Flogo_simple.svg%3Ffoo%3Dbar%2Fbar*bar%5C%2F%252F" +
                        "%2F%24%2F99157073600d7c74%2Fm%2F1%2FKL_RU_04072012_ru_05.jpg"));
    }

    @Test
    @Title("Должны проксировать ссылку с заэнкоженными символами, не энкодя ее дважды")
    @Issues({@Issue("DARIA-15877"), @Issue("DARIA-15878"), @Issue("DARIA-19087")})
    public void httpProxyRusEncodedUrl() throws Exception {
        String url = "http://zvezda-receptov.ru/wp-content/uploads/2012/05/2-%D0%BF%D0%B8%D1%80%D0%BE%D0%B6%D0%BA%" +
                "D0%B8-%D0%B1%D0%BB%D0%B8%D0%BD%D1%87%D0%B0%D1%82%D1%8B%D0%B5-%D0%B2%D0%BE-%D1%84%D1%80%D0%B8%D" +
                "1%82%D1%8E%D1%80%D0%B5.jpg";
        String text = format("<html><body><img src='%s'></img></body></html>", url);

        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net",
                result, startsWith(RESIZE_MAIL));
        assertThat("Неправильно эскейпим картинку [MAILADM-4048]", result,
                containsString("=http%3A%2F%2Fzvezda-receptov.ru%2Fwp-content%2Fuploads%2F2012%2F05%2F2-%25D0" +
                        "%25BF%25D0%25B8%25D1%2580%25D0%25BE%25D0%25B6%25D0%25BA%25D0%25B8-%25D0%25B1%25D0" +
                        "%25BB%25D0%25B8%25D0%25BD%25D1%2587%25D0%25B0%25D1%2582%25D1%258B%25D0%25B5-%25D0" +
                        "%25B2%25D0%25BE-%25D1%2584%25D1%2580%25D0%25B8%25D1%2582%25D1%258E%25D1%2580%25D0%25B5.jpg"));
    }

    @Test
    @Title("Санитайзер не должен лишний раз превращать '%20' в '+'")
    @Issue("SANITIZER-57")
    public void sanitizerDontReplace20ToPlus() throws Exception {
        String text = fromClasspath("sanitizer/proxy/escaped-space.htm");

        String result = getImgSrc(text);
        assertThat("[SANITIZER-57] В пути лишний раз %20 заменяется на +", result, not(containsString("+")));
    }

    @Test
    @Title("Картинки с заэнкоженными символами в урле должны кешироваться и не экранироваться дважды")
    @Issues({@Issue("DARIA-15877"), @Issue("DARIA-15878"), @Issue("DARIA-18428")})
    public void httpProxyEncodedUrl() throws Exception {
        //String url = "http://mx.voevodyno-group.com/thumb.php?src=uploads%2Fmails%2F0007%2F007_1.jpg&w=540&h=158";
        String url = "http://pokupon.by/uploaded/campaign_pictures/116561/data/" +
                "madmimi_additional/001%20%281%29.jpg?1346395985";
        String text = format("<html><body><img src='%s'></img></body></html>", url);

        String result = getImgSrc(text);
        System.out.println(url.replace("http://", "").replaceAll("&", "%26"));

        assertThat("Картинка не проксируется через кешер resize.yandex.net или неправильно эскейпится",
                result,
                allOf(startsWith(RESIZE_MAIL),
                        containsString("http%3A%2F%2Fpokupon.by%2Fuploaded%2Fcampaign_pictures%2F116561%2Fdata" +
                                "%2Fmadmimi_additional%2F001%2520%25281%2529.jpg")));
    }

    @Test
    @Title("Проверяем формируемый email")
    @Issues({@Issue("SANITIZER-99"), @Issue("SANITIZER-109")})
    public void newProxyImageTest() throws Exception {
        String url = "https://shop.mascotte.ru/images/newsletters/2015-07-30/7.jpg";
        String text = format("<html><body><img src='%s'></img></body></html>", url);
        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net или неправильно эскейпится",
                result, containsString(getResize(url)));

    }

    @Test
    @Title("Проверяем формируемый ключ. Не должны энкодить спец. символы")
    @Issues({@Issue("SANITIZER-107")})
    public void specSymbolsProxyImageTest() throws Exception {
        String text = fromClasspath("sanitizer/proxy/spec-symbols.htm");
        String url = "http://latex.codecogs.com/";
        String symbols = "-_.!~*()'";

        String expected = StringUtils.replace(RESIZE_MAIL + URLEncoder.encode(url) + symbols + "&proxy=yes&key="
                + md5().hashString("url=" + URLEncoder.encode(url) + symbols + "/proxy=yes" + SECRET_KEY,
                Charset.defaultCharset()).toString(), "&", "&amp;");

        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net или неправильно эскейпится",
                result, containsString(expected));
    }

    /**
     * "https://resize.yandex.net/mailservice?url=" + urlencode(http://i.imgur.com/5QllJFz.gif) +
     * "&proxy=yes&key=" + md5("url=" + urlencode(http://i.imgur.com/5QllJFz.gif) + "/proxy=yes" + SECRET_KEY )
     *
     * @return
     * @throws Exception
     */
    public static String getResize(String url) {
        String encodedUrl = URLEncoder.encode(url);

        return StringUtils.replace(RESIZE_MAIL + encodedUrl + "&proxy=yes&key=" + md5().hashString("url=" + encodedUrl + "/proxy=yes" + SECRET_KEY,
                Charset.defaultCharset()).toString(), "&", "&amp;");
    }


    private String getImgSrc(String text) throws Exception {
        return StringUtils.substringAfter(sanitizer(props().sanitizerUri()).prHttps(text), "src=\"");
    }

    @Test
    @Issue("SANITIZER-116")
    @Title("Заворачиваем две картинки в srcset")
    public void shouldResizeSrcset() throws Exception {
        String text = fromClasspath("sanitizer/srcset.htm");
        String result = sanitizer(props().sanitizerUri()).prHttps(text);

        assertThat("Не верно заворачиваем две картинки одну в <srcset> [SANITIZER-116]",
                result, containsString("https://resize.yandex.net/mailservice?" +
                        "url=http%3A%2F%2Fs.auto.drom.ru%2Fi24195%2Fs%2F" +
                        "photos%2F21490%2F21489924%2Fttn_320_167295801.jpg&amp;proxy=yes" +
                        "&amp;key=67549e578cb4f2e078acef533b10e98d"));
    }

    @Test
    @Issue("LIB-675")
    @Title("Ссылка от googleapis должна оборачиваться")
    public void hrefChartGoogleApisTest() throws Exception {
        String text = fromClasspath("sanitizer/proxy/chart-googleapis.htm");
        String result = getImgSrc(text);
        assertThat("Картинка не проксируется через кешер resize.yandex.net",
                result, containsString("https://resize.yandex.net/mailservice?url=https%3A%2F%2Fchart.googleapis.com"));
    }
}
