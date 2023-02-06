package ru.yandex.autotests.innerpochta.wmi.uatraits;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 11.09.12
 * Time: 13:23
 * <p/>
 * Unmodify
 */
@Aqua.Test
@Title("[Uatraits] Проверка метода mobile_get_info")
@Description("[DARIA-20838]")
@Features(MyFeatures.WMI)
@Stories(MyStories.UATRAITS)
@Credentials(loginGroup = "Zoo")
@Issue("DARIA-20838")
public class MobileGetInfoTest extends BaseTest {

    @Test
    @Issue("DARIA-45808")
    @Title("Uatraits с iphone")
    public void uatraitsCanParseUAgent() throws Exception {
        jsx(ru.yandex.autotests.innerpochta.wmi.core.oper.MobileGetInfo.class).userAgent("IPhone").post().via(hc)
                .shouldBe().mobileUserAgent("phone").oSFamily("iOS");

        jsx(ru.yandex.autotests.innerpochta.wmi.core.oper.MobileGetInfo.class).userAgent("Mozilla/5.0 " +
                "(Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/40.0.2214.115 YaBrowser/15.2.2214.3643 Safari/537.36")
                .post().via(hc).shouldBe().browserName("YandexBrowser").isMobile(false).oSFamily("MacOS");
    }

    @Test
    @Issue("MAILPG-898")
    @Title("uatraits с iphone 10.0")
    @Description("Раньше парсили 10.0-ую iOS версию как 1")
    public void uatraitsIOS10Test() throws Exception {
        jsx(ru.yandex.autotests.innerpochta.wmi.core.oper.MobileGetInfo.class).userAgent("Mozilla/5.0 " +
                "(iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.32" +
                " (KHTML, like Gecko) Version/10.0 Mobile/14A5261v Safari/602.1")
                .post().via(hc).shouldBe().oSFamily("iOS").oSVersion("10.0");
    }


    @Test
    @Title("Uatraits: BrowserBase и BrowserBaseVersion")
    @Issue("DARIA-45808")
    public void uatraitsBrowserBaseAnBrowserBaseVersionTest() throws IOException {
        jsx(ru.yandex.autotests.innerpochta.wmi.core.oper.MobileGetInfo.class).userAgent("Mozilla/5.0 " +
                "(Linux; Android 4.4.4; Nexus 5 Build/KTU84P) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/36.0.1985.131 Mobile Safari/537.36:::/touch/")
                .post().via(hc).shouldBe().browserBase("Chromium").browserBaseVersion("36.0.1985.131")
        .browserName("ChromeMobile").oSFamily("Android").isMobile(true);

    }
}
