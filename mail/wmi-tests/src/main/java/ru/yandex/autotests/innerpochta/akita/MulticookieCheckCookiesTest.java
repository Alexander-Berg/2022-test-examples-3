package ru.yandex.autotests.innerpochta.akita;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.akita.CheckCookiesResponse;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.CommonUtils.authWith;

@Aqua.Test
@Title("[AUTH] Мультикука. Тесты на ручку check_cookies")
@Features(MyFeatures.AKITA)
@Stories(MyStories.AUTH)
@Issue("MAILPG-1749")
@Credentials(loginGroup = MulticookieCheckCookiesTest.LOGIN_GROUP1)
public class MulticookieCheckCookiesTest extends AkitaBaseTest {

    static final String LOGIN_GROUP1 = "AuthMulticookieTest1";
    private static final String LOGIN_GROUP2 = "AuthMulticookieTest2";
    private static final String LOGIN_GROUP3 = "AuthMulticookieTest3";
    private static final String LOGIN_GROUP4 = "AuthMulticookieTest4";
    private static final String LOGIN_GROUP5 = "AuthMulticookieTest5";
    private static final String LOGIN_GROUP6 = "AuthMulticookieTest6";

    private static final List<Account>
        NORMAL_PAIR = asList(
            props().account(LOGIN_GROUP1),
            props().account(LOGIN_GROUP2));

    private static final List<Account> FIVE_PAIRS = asList(
            props().account(LOGIN_GROUP3),
            props().account(LOGIN_GROUP4),
            props().account(LOGIN_GROUP5),
            props().account(LOGIN_GROUP1),
            props().account(LOGIN_GROUP2)
    );

    private static final Account NORMAL = props().account(LOGIN_GROUP1);
    private static final Account PDD = props().account(LOGIN_GROUP6);

    @Test
    @Title("Один обычный дочерний аккаунт")
    public void shouldShowOneChildAccount() {
        assertThat("Неверное число дочерних аккаунтов",
                checkCookies(authWith(NORMAL_PAIR))
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
                checkCookies(authWith(PDD, NORMAL))
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
                checkCookies(authWith(FIVE_PAIRS))
                        .get(shouldBe(ok200()))
                        .as(CheckCookiesResponse.class)
                        .getCheckCookies()
                        .getChildUids(),
                hasSize(4)
        );
    }
}
