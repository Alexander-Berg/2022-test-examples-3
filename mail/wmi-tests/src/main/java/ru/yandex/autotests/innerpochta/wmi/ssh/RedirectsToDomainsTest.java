package ru.yandex.autotests.innerpochta.wmi.ssh;

import com.jayway.restassured.specification.RequestSpecification;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.GetBestLanguage;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static com.google.common.net.HttpHeaders.HOST;
import static com.jayway.restassured.RestAssured.given;
import static gumi.builders.UrlBuilder.fromString;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.any;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.other.KcufCompTimeTest.X_REAL_IP_HEADER;

@Aqua.Test
@Title("[SSH] Тестирование редиректов")
@Description("Тест работает по ssh, дергая запрос курлом, передавая в заголовке ip")
@Features(MyFeatures.WMI)
@Stories({MyStories.SSH, MyStories.REDIRECTS})
@Issue("MAILPG-1294")
public class RedirectsToDomainsTest extends BaseTest {

    public static final int PORT = 8079;
    public static final String SIMPLE_JSX = "/jsxapi/simple.jsx";


    public static final String TURKEY_IP = "81.214.36.71";
    //62.149.23.243
    public static final String UKRAINE_IP = "193.19.110.11";
    public static final String BELARUS_IP = "87.252.251.255";
    public static final String KZ_IP = "88.204.255.255";
    public static final String CRIMEA_IP = "109.200.128.255";

    public static final String MAIL_YANDEX_RU = "mail.yandex.ru";
    public static final String MAIL_YANDEX_UA = "mail.yandex.ua";
    public static final String MAIL_YANDEX_COM = "mail.yandex.com";
    public static final String MAIL_YANDEX_COM_TR = "mail.yandex.com.tr";

    public static final String DOMAIN_RU = "<domain>ru</domain>";
    public static final String DOMAIN_UA = "<domain>ua</domain>";
    public static final String DOMAIN_KZ = "<domain>kz</domain>";
    public static final String DOMAIN_BY = "<domain>by</domain>";
    public static final String DOMAIN_COM = "<domain>com</domain>";
    public static final String DOMAIN_COM_TR = "<domain>com.tr</domain>";

    @ClassRule
    public static HttpClientManagerRule auth = auth().with("lanwen@kida-lo-vo.name", "testqa");

    @Test
    @Issue("DARIA-14358")
    @Description("Проверка, что с турецкого ip при заходе на com\n" +
            "осуществляется редирект на com.tr\n" +
            "DARIA-14358")
    public void testRedirToComTr() throws Exception {
        String ip = TURKEY_IP; // Турецкий ип
        String command = "curl -I -H 'Host: mail.yandex.com' -H 'X-Real-IP: " + ip +
                "' 'http://127.0.0.1:8080/host-root2/index.jsx'";
        String resp = sshAuthRule.ssh().cmd(command);
        assertThat("Заголовка с редиректом 302 Moved Temporary не найдено",
                resp, containsString("302 Moved Temporary"));
        assertThat("Редирект не на mail.yandex.com.tr", resp, containsString(MAIL_YANDEX_COM_TR));
    }

    @Test
    @Issues({@Issue("DARIA-39314"), @Issue("WMI-582")})
    @Description("https://wiki.yandex-team.ru/portal/international/lang\n" +
            "* Проверяем lang-detect\n" +
            "* [WMI-582][DARIA-39314]\n" +
            "* При заходе с пустыми куками с акцепт-ланг = ен на украинский хост,\n" +
            "* должен отдаваться укр язык")
    public void getBestLangForUkr() throws IOException {
        logger.warn("[WMI-582]");
        GetBestLanguage langOp = jsx(GetBestLanguage.class)
                .headers(new BasicHeader("Accept-Language", "en"),
                        new BasicHeader("Host", MAIL_YANDEX_UA));
        langOp.setHost(fromString(props().betaHost()).withPort(8079).withScheme("http").toString());

        assertEquals("Ответ содержит не ту страну",
                "uk", langOp.post().via(new DefaultHttpClient()).withDebugPrint().getLang());
    }

    @Test
    @Issue("DARIA-7156")
    @Description("Проверка, что с турецкого ip при заходе на com\n" +
            "методом выдается ком.тр\n" +
            "DARIA-7156")
    public void testAnswToComTr() throws Exception {
        String resp = getDomainToRedirectReq(TURKEY_IP, MAIL_YANDEX_COM).get().asString();
        assertThat("Ответ не содержит com.tr", resp, containsString(DOMAIN_COM_TR));
    }

    @Test
    @Title("C украинского ip при заходе на ру - отдает com")
    @Description("[DARIA-7156]")
    public void shouldGetComFromUaIpWithRuDomain() throws Exception {
        String resp = getDomainToRedirectReq(UKRAINE_IP, MAIL_YANDEX_RU).get().asString();
        assertThat("Ответ не содержит com", resp, containsString(DOMAIN_COM));
    }

    @Test
    @Issue("DARIA-7156")
    @Description("Проверка, что с белорусского ip при заходе на ру\n" +
            "метод отдает by")
    public void testAnswToBy() throws Exception {
        String resp = getDomainToRedirectReq(BELARUS_IP, MAIL_YANDEX_RU).get().asString();
        assertThat("Ответ не содержит by", resp, containsString(DOMAIN_BY));
    }

    @Test
    @Issue("DARIA-7156")
    @Description("Проверка, что с казахского ip при заходе на ру\n" +
            "метод отдает kz\n" +
            "DARIA-7156")
    public void testAnswToKz() throws Exception {
        String resp = getDomainToRedirectReq(KZ_IP, MAIL_YANDEX_RU).get().asString();
        assertThat("Ответ не содержит KZ", resp, containsString(DOMAIN_KZ));
    }

    @Test
    @Issue("DARIA-7156")
    @Description("Проверка, что с казахского ip при заходе на ua\n" +
            "метод отдает ua (редиректа нет)")
    public void testAnswToKzFromUa() throws Exception {
        String resp = getDomainToRedirectReq(KZ_IP, MAIL_YANDEX_UA).get().asString();
        assertThat("Ответ не содержит UA", resp, containsString(DOMAIN_UA));
    }

    @Test
    @Title("Из Крыма на домен ru без cr/со значением ru в cr - остался на домене ru")
    @Description("[DARIA-37780]")
    public void shouldStayRuFromCrWithCrRu() throws Exception {
        getDomainToRedirectReq(CRIMEA_IP, MAIL_YANDEX_RU).get().then()
                .assertThat()
                .body(containsString(DOMAIN_RU));

        getDomainToRedirectReq(CRIMEA_IP, MAIL_YANDEX_RU)
                .cookie("ys", "cr.ru")
                .get().then()
                .assertThat()
                .body(containsString(DOMAIN_RU));
    }

    @Test
    @Title("Из Крыма на домен ua без cr/со значением ua - остался на домене ua")
    @Description("[DARIA-37780]")
    public void shouldStayUaFromCrWithCrUa() throws Exception {
        getDomainToRedirectReq(CRIMEA_IP, MAIL_YANDEX_UA).get().then()
                .assertThat()
                .body(containsString(DOMAIN_UA));

        getDomainToRedirectReq(CRIMEA_IP, MAIL_YANDEX_UA)
                .cookie("ys", "cr.ua")
                .get().then()
                .assertThat()
                .body(containsString(DOMAIN_UA));
    }

    @Test
    @Title("Из Крыма на домен ru со значением ua в cr - получил редирект на com")
    @Description("[DARIA-37780]")
    public void shouldGoComFromRuWithCrUa() throws Exception {
        getDomainToRedirectReq(CRIMEA_IP, MAIL_YANDEX_RU)
                .cookie("ys", "cr.ua")
                .get().then()
                .assertThat()
                .body(containsString(DOMAIN_COM));
    }

    @Test
    @Title("Из Крыма на домен ru с значением ua в cr - получил редирект на com (контейнер YP)")
    @Description("[DARIA-37780]")
    public void shouldGoComFromRuWithCrUaForYPContainer() throws Exception {
        getDomainToRedirectReq(CRIMEA_IP, MAIL_YANDEX_RU)
                .cookie("yp", "1730713267.cr.ua")
                .get().then()
                .assertThat()
                .body(containsString(DOMAIN_COM));
    }

    @Test
    @Title("Из Крыма на домен ru со значением ua в cr - получил редирект на com (приоритет контейнеров YS над YP)")
    @Description("[DARIA-37780]")
    public void shouldGoComFromRuWithCrUaForBothYPAndYSContainer() throws Exception {
        getDomainToRedirectReq(CRIMEA_IP, MAIL_YANDEX_RU)
                .cookie("yp", "1730713267.cr.ru")
                .cookie("ys", "cr.ua")
                .get().then()
                .assertThat()
                .body(containsString(DOMAIN_COM));
    }

    @Test
    @Title("Из Крыма на домен ua со значением ru в cr - получил редирект на ru")
    @Description("[DARIA-37780][DARIA-39316]")
    public void shouldGoUaFromCrWithCrRu() throws Exception {
        getDomainToRedirectReq(CRIMEA_IP, MAIL_YANDEX_UA)
                .cookie("ys", "cr.ru")
                .get().then()
                .assertThat()
                .body(containsString(DOMAIN_RU));
    }

    @Test
    @Title("Из любой другой части Украины на домен ru с любым значением в cr/без него - получил редирект на com")
    @Description("[DARIA-37780]")
    public void shouldGoComFromAnyOtherUaWithAnyCr() throws Exception {
        getDomainToRedirectReq(UKRAINE_IP, MAIL_YANDEX_RU)
                .cookie("ys", "cr.ru")
                .get().then()
                .assertThat()
                .body(containsString(DOMAIN_COM));

        getDomainToRedirectReq(UKRAINE_IP, MAIL_YANDEX_RU)
                .cookie("ys", "cr.ua")
                .get().then()
                .assertThat()
                .body(containsString(DOMAIN_COM));
    }

    @Test
    @Title("Остаемся на com.tr с крымским ипом")
    @Description("[DARIA-37780]")
    public void shouldIgnoreCrWhenGoToComTr() throws Exception {
        getDomainToRedirectReq(CRIMEA_IP, MAIL_YANDEX_COM_TR)
                .cookie("ys", "cr.ua")
                .get().then()
                .assertThat()
                .body(containsString(DOMAIN_COM_TR));
    }

    /**
     * Возвращает ответ от ручки get_domain_to_redirect
     *
     * @param ip - ип, передаваемый в заголовке запроса
     * @return - ответ от ручки
     * @throws Exception
     */
    private RequestSpecification getDomainToRedirectReq(String ip, String host) {
        logger.warn("Проверка ручки get_domain_to_redirect [DARIA-7156]");
        return given().relaxedHTTPSValidation().filter(log())
                .header(HOST, host)
                .header(X_REAL_IP_HEADER, ip)
                .queryParam("wmi-method", "get_domain_to_redirect")
                .baseUri(fromUri(props().betaHost()).port(8079)
                        .scheme(HttpSchemes.HTTP).path(SIMPLE_JSX).build().toString());
    }
}

