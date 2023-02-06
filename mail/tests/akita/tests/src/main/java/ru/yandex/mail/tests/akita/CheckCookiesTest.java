package ru.yandex.mail.tests.akita;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.properties.Scope;
import ru.yandex.mail.common.properties.Scopes;
import ru.yandex.mail.tests.akita.generated.CheckCookies;
import ru.yandex.mail.tests.akita.generated.CheckCookiesResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;


@Aqua.Test
@Title("[Akita] Ручка проверки кук для ксивы check_cookies")
@Issue("MAILPG-1749")
public class CheckCookiesTest extends AkitaBaseTest {
    AccountWithScope mainUser() {
        return Accounts.checkCookiesTest;
    }

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
    @Scope({Scopes.TESTING, Scopes.PRODUCTION, Scopes.INTRANET_PRODUCTION})
    @Description("Ожидаемый результат ошибка с кодом 2001 и http200")
    public void checkCookiesWithTrashInsteadCookies() {
        checkCookies(new Cookies(new Cookie.Builder("Session_id", "42").build()))
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
        assertThat("Неверное значение аттрибута 'timeZone'", resp.getTimeZone(), not(""));
        assertThat("Неверное значение аттрибута 'offset'", resp.getOffset(), not(""));
        assertThat("Неверное значение аттрибута 'uid'", resp.getUid(), equalTo(authClient.account().uid()));
        assertThat("Неверное значение аттрибута 'childUids'", resp.getChildUids().size(), equalTo(0));
    }
}
