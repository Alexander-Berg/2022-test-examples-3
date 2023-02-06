package ru.yandex.mail.tests.akita;

import java.util.HashMap;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.Account;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.BbResponse;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.rules.BbResponseUploader;
import ru.yandex.mail.tests.akita.generated.AccountInformation;
import ru.yandex.mail.tests.akita.generated.AuthResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.credentials.UserCredentials.cookies;

@Aqua.Test
@Title("[AUTH] Мультикука. Тесты на авторизацию, current_uid")
@Description("Различные тесты на мультикуку")
@Issues({@Issue("DARIA-40262"), @Issue("DARIA-41770")})
public class MulticookieAccountInfoTest extends AkitaBaseTest {
    AccountWithScope mainUser() {
        return Accounts.multiCookieAccount1;
    }

    private UserCredentials authClient2 = new UserCredentials(Accounts.multiCookieAccount2);
    private UserCredentials authClient3 = new UserCredentials(Accounts.multiCookieAccount3);

    private String uid1 = authClient.account().uid();
    private String uid2 = authClient2.account().uid();
    private String uid3 = authClient3.account().uid();

    private String unexistingUid() {
        return "4611686018427387904";
    }

    private static List<Account> normalPair = asList(
            Accounts.multiCookieAccount1.get(),
            Accounts.multiCookieAccount2.get()
    );

    private static List<Account> fiveAccounts = asList(
            Accounts.multiCookieAccount1.get(),
            Accounts.multiCookieAccount2.get(),
            Accounts.multiCookieAccount3.get(),
            Accounts.multiCookieAccount4.get(),
            Accounts.multiCookieAccount5.get()
    );

    private static List<Account> withPdd = asList(
            Accounts.multiCookieAccount1.get(),
            Accounts.multiCookieAccount6.get()
    );

    private static String name(List<Account> ee) {
        String name = "";
        for (Account a : ee) {
            name += a.login();
        }

        return name;
    }

    @ClassRule
    public static BbResponseUploader uploadPairs = new BbResponseUploader(new HashMap<String, BbResponse>(){{
        put(name(normalPair),   BbResponse.from("fakebb/normal_pair.xml"));
        put(name(fiveAccounts), BbResponse.from("fakebb/five_accounts.xml"));
        put(name(withPdd),      BbResponse.from("fakebb/with_pdd.xml"));
    }});

    @Test
    @Title("Один обычный дочерний аккаунт")
    public void shouldShowOneChildAccount() {
        assertThat("Неверное число дочерних аккаунтов",
                auth(cookies(normalPair))
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getChildAccounts(),
                hasSize(1)
        );
    }

    @Test
    @Title("Обычный аккаунт и пдд")
    public void shouldShowOnePddChildAccount() {
        assertThat("Неверное число дочерних аккаунтов",
                auth(cookies(withPdd))
                        .get(shouldBe(ok200()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getChildAccounts(),
                hasSize(1)
        );
    }

    @Test
    @Title("Четыре обычных дочерних аккаунта")
    public void shouldShowFourChildAccounts() {
        assertThat("Неверное число дочерних аккаунтов",
                auth(cookies(fiveAccounts))
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getChildAccounts(),
                hasSize(4)
        );
    }

    @Test
    @Title("Current_uid = uid дочернего аккаунта")
    @Description("Делаем запрос с мультикукой для с current_uid = uid дочернего аккаунта." +
            "Ожидаемый результат: дочерний аккаунт станет главным, а главный дочерним")
    public void shouldSwitchMainAccountToChildOneIfProperCurrentUidIsSet() {
        AccountInformation info = auth(cookies(normalPair))
                .withCurrentUid(uid1)
                .get(shouldBe(okAuth()))
                .as(AuthResponse.class)
                .getAccountInformation();


        assertThat("Неверное значение uid у главного аккаунта",
                info.getAccount().getUserId(),
                equalTo(uid1)
        );

        assertThat("Неверное значение uid у дочернего аккаунта",
                info.getChildAccounts().get(0).getAccount().getUserId(),
                equalTo(uid2)
        );
    }

    @Test
    @Title("Current_uid = uid по умолчанию")
    @Description("Делаем запрос с мультикукой для с current_uid с аккаунтом по умолчанию выдача не должна измениться")
    public void shouldNotSwitchMainAccountIfCurrentUidIsEqualsToMainUid() {
        AccountInformation info = auth(cookies(normalPair))
                .withCurrentUid(uid2)
                .get(shouldBe(okAuth()))
                .as(AuthResponse.class)
                .getAccountInformation();


        assertThat("Неверное значение uid у главного аккаунта",
                info.getAccount().getUserId(),
                equalTo(uid2)
        );

        assertThat("Неверное значение uid у дочернего аккаунта",
                info.getChildAccounts().get(0).getAccount().getUserId(),
                equalTo(uid1)
        );
    }

    @Test
    @Title("Current_uid = несуществующий uid")
    @Description("Делаем запрос с мультикукой для с current_uid = неизвестный uid" +
            "Ожидаемый результат: ничего не меняется")
    public void shouldNotSwitchMainAccountInCaseOfNonExistentCurrentUid() {
        AccountInformation info = auth(cookies(normalPair))
                .withCurrentUid(unexistingUid())
                .get(shouldBe(okAuth()))
                .as(AuthResponse.class)
                .getAccountInformation();


        assertThat("Неверное значение uid у главного аккаунта",
                info.getAccount().getUserId(),
                equalTo(uid2)
        );

        assertThat("Неверное значение uid у дочернего аккаунта",
                info.getChildAccounts().get(0).getAccount().getUserId(),
                equalTo(uid1)
        );
    }

    @Test
    @Title("Current_uid = существующий, но не дочерний uid")
    @Description("Делаем запрос с мультикукой для с current_uid = не дочерний, но существующий uid" +
            "Ожидаемый результат: ничего не меняется")
    public void shouldNotSwitchMainAccountInCaseOfImproperUid() {
        AccountInformation info = auth(cookies(normalPair))
                .withCurrentUid(uid3)
                .get(shouldBe(okAuth()))
                .as(AuthResponse.class)
                .getAccountInformation();


        assertThat("Неверное значение uid у главного аккаунта",
                info.getAccount().getUserId(),
                equalTo(uid2)
        );

        assertThat("Неверное значение uid у дочернего аккаунта",
                info.getChildAccounts().get(0).getAccount().getUserId(),
                equalTo(uid1)
        );
    }
}
