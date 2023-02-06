package ru.yandex.autotests.innerpochta.akita;

import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.akita.CheckCookies;
import ru.yandex.autotests.innerpochta.beans.akita.CheckCookiesResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiAkitaWithoutAuth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.xOriginalHost;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;


@Aqua.Test
@Title("[Akita] Ручка проверки кук для ксивы check_cookies")
@Credentials(loginGroup = "AkitaCheckCookies")
@Features(MyFeatures.AKITA)
@Stories(MyStories.AUTH)
@Issue("MAILPG-1749")
public class CheckCookiesTest extends AkitaBaseTest {
    @Test
    @Title("Проверяем куки без кук")
    @Description("Проверяем куки без кук. Ожидаемый результат ошибка с кодом 2001 и http400")
    public void checkCookiesWithoutCookies() {
        apiAkitaWithoutAuth().checkCookies()
                .withXoriginalhostHeader(xOriginalHost())
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Проверяем мусор вместо кук")
    @Description("Ожидаемый результат ошибка с кодом 2001 и http200")
    public void checkCookiesWithTrashInsteadCookies() {
        checkCookies(new Cookies(new Cookie.Builder("Session_id", getRandomString()).build()))
            .withXoriginalhostHeader(xOriginalHost())
            .get(shouldBe(ok200()));
    }

    @Test
    @Title("Проверяем наличие всех полей в ответе check_cookies")
    public void shouldHaveAllAttributes() {
        CheckCookies resp = checkCookies()
            .get(shouldBe(ok200()))
            .as(CheckCookiesResponse.class)
            .getCheckCookies();

        assertThat("Неверное значение аттрибута 'bbConnectionId'", resp.getBbConnectionId(), not(""));
        assertThat("Неверное значение аттрибута 'bbLoginId'", resp.getBbLoginId(), not(""));
        assertThat("Неверное значение аттрибута 'timeZone'", resp.getTimeZone(), not(""));
        assertThat("Неверное значение аттрибута 'uid'", resp.getUid(), equalTo(authClient.account().uid()));
        assertThat("Неверное значение аттрибута 'childUids'", resp.getChildUids().size(), equalTo(0));
    }
}