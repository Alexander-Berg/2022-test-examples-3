package ru.yandex.mail.tests.akita;

import java.util.HashMap;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.Account;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.BbResponse;
import ru.yandex.mail.common.rules.BbResponseUploader;
import ru.yandex.mail.tests.akita.generated.CheckCookiesResponse;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.credentials.UserCredentials.cookies;

@Aqua.Test
@Title("[AUTH] Мультикука. Тесты на ручку check_cookies")
@Issue("MAILPG-1749")
public class MulticookieCheckCookiesTest extends AkitaBaseTest {
    AccountWithScope mainUser() {
        return Accounts.multiCookieAccount10;
    }

    private static List<Account> normalPair = asList(
            Accounts.multiCookieAccount10.get(),
            Accounts.multiCookieAccount11.get()
    );

    private static List<Account> fiveAccounts = asList(
            Accounts.multiCookieAccount10.get(),
            Accounts.multiCookieAccount11.get(),
            Accounts.multiCookieAccount12.get(),
            Accounts.multiCookieAccount13.get(),
            Accounts.multiCookieAccount14.get()
    );

    private static List<Account> withPdd = asList(
            Accounts.multiCookieAccount10.get(),
            Accounts.multiCookieAccount6.get()
    );

    private static String name(List<Account> ee) {
        StringBuilder name = new StringBuilder();
        for (Account a : ee) {
            name.append(a.login());
        }

        return name.toString();
    }

    @ClassRule
    public static BbResponseUploader uploadPairs = new BbResponseUploader(new HashMap<String, BbResponse>(){{
        put(name(normalPair),   BbResponse.from("fakebb/normal_pair.json"));
        put(name(fiveAccounts), BbResponse.from("fakebb/five_accounts.json"));
        put(name(withPdd),      BbResponse.from("fakebb/with_pdd.json"));
    }});

    @Test
    @Title("Один обычный дочерний аккаунт")
    public void shouldShowOneChildAccount() {
        assertThat("Неверное число дочерних аккаунтов",
                checkCookies(cookies(normalPair))
                        .get(shouldBe(ok200()))
                        .as(CheckCookiesResponse.class)
                        .getCheckCookies()
                        .getChildUids(),
                hasSize(1)
        );
    }

    @Test
    @Title("Обычный аккаунт и пдд")
    public void shouldShowOnePddChildAccount() {
        assertThat("Неверное число дочерних аккаунтов",
                checkCookies(cookies(withPdd))
                        .get(shouldBe(ok200()))
                        .as(CheckCookiesResponse.class)
                        .getCheckCookies()
                        .getChildUids(),
                hasSize(1)
        );
    }

    @Test
    @Title("Четыре обычных дочерних аккаунта")
    public void shouldShowFourChildAccounts() {
        assertThat("Неверное число дочерних аккаунтов",
                checkCookies(cookies(fiveAccounts))
                        .get(shouldBe(ok200()))
                        .as(CheckCookiesResponse.class)
                        .getCheckCookies()
                        .getChildUids(),
                hasSize(4)
        );
    }
}
