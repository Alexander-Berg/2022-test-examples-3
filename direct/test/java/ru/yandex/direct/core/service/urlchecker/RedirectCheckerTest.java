package ru.yandex.direct.core.service.urlchecker;

import java.util.List;

import io.netty.handler.codec.http.HttpHeaderNames;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.asynchttpclient.Response;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.zorafetcher.ZoraResponse;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class RedirectCheckerTest {

    public static List<Object[]> parametersForExtractLocationTest() {
        return asList(new Object[][]{
                { // https://st.yandex-team.ru/DIRECT-106870
                        "обработка относительных путей в Location",
                        "https://mc.admetrica.ru/show?cmn_id=1474&plt_id=4984&crv_id=6724&evt_t=click&ad_type=banner",
                        "/show?cmn_id=1474&plt_id=4984&crv_id=6724&evt_t=click&ad_type=banner&redir=1",
                        "https://mc.admetrica.ru/show?cmn_id=1474&plt_id=4984&crv_id=6724&evt_t=click&ad_type=banner" +
                                "&redir=1"
                },
                { // https://st.yandex-team.ru/DIRECT-108816
                        "Location в кодировке ISO-8859-1",
                        "https://3145.xg4ken.com/media/redir.php",
                        "https://www.farfetch.com/ru/shopping/women/pierre-hardy/items" +
                                ".aspx?category=136301&title=Ð¾Ð±Ñ\u0083Ð²Ñ\u008C pierre hardy",
                        "https://www.farfetch.com/ru/shopping/women/pierre-hardy/items" +
                                ".aspx?category=136301&title=%D0%BE%D0%B1%D1%83%D0%B2%D1%8C%20pierre%20hardy"
                },
                {
                        "Location с линком до приложения (google play)",
                        "https://app.appsflyer.com",
                        "market://details?id=com.pmco.luckyfruits",
                        "https://play.google.com/store/apps/details?id=com.pmco.luckyfruits",
                },
//                TODO: доразбираться с itms-apps и itms-appss - почему-то Uri.create() считает эти урлы относительными путями
//                {
//                        "Location с линком до приложения (app store)",
//                        "https://app.appsflyer.com",
//                        "itms-apps://apps.apple.com/ru/app/carx-drift-racing-2/id1198510863",
//                        "https://apps.apple.com/ru/app/carx-drift-racing-2/id1198510863",
//                },
//                {
//                        "Location с линком до приложения (app store, itms-appss)",
//                        "https://app.appsflyer.com",
//                        "itms-appss://apps.apple.com/ru/app/carx-drift-racing-2/id1198510863",
//                        "https://apps.apple.com/ru/app/carx-drift-racing-2/id1198510863",
//                },
        });
    }

    @Test
    @TestCaseName("{0}")
    @Parameters
    public void extractLocationTest(
            @SuppressWarnings("unused") String testName,
            String contextUrl,
            String redirectUrl,
            String expected
    ) {
        Response response = mock(Response.class);
        when(response.getHeader(eq(HttpHeaderNames.LOCATION))).thenReturn(redirectUrl);
        ZoraResponse res = mock(ZoraResponse.class);
        when(res.getResponse()).thenReturn(response);

        String actual = RedirectChecker.extractLocation(contextUrl, res);
        assertThat(actual).isEqualTo(expected);
    }
}
