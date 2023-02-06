package ru.yandex.autotests.innerpochta.akita;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.akita.AccountInformation;
import ru.yandex.autotests.innerpochta.beans.akita.AuthResponse;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.qatools.allure.annotations.*;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.CommonUtils.authWith;

/**
 * User: lanwen
 * Date: 27.10.14
 * Time: 18:16
 */
@Aqua.Test
@Title("[AUTH] Мультикука. Тесты на авторизацию, current_uid")
@Description("Различные тесты на мультикуку")
@Features(MyFeatures.AKITA)
@Stories(MyStories.AUTH)
@Issues({@Issue("DARIA-40262"), @Issue("DARIA-41770")})
@Credentials(loginGroup = MulticookieAccountInfoTest.LOGIN_GROUP1)
public class MulticookieAccountInfoTest extends AkitaBaseTest {

    static final String LOGIN_GROUP1 = "AuthMulticookieTest1";
    private static final String LOGIN_GROUP2 = "AuthMulticookieTest2";
    private static final String LOGIN_GROUP3 = "AuthMulticookieTest3";
    private static final String LOGIN_GROUP4 = "AuthMulticookieTest4";
    private static final String LOGIN_GROUP5 = "AuthMulticookieTest5";
    private static final String LOGIN_GROUP6 = "AuthMulticookieTest6";


    private static String uid1;
    private static String uid2;
    private static String uid3;

    @ClassRule
    public static HttpClientManagerRule authClient2 = HttpClientManagerRule.auth().with(LOGIN_GROUP2);

    @ClassRule
    public static HttpClientManagerRule authClient3 = HttpClientManagerRule.auth().with(LOGIN_GROUP3);

    @BeforeClass
    public static void getUid() {
        uid1 = authClient.account().uid();
        uid2 = authClient2.account().uid();
        uid3 = authClient3.account().uid();
    }

    String unexistingUid() {
        return "4611686018427387904";
    }

    private static final List<Account> NORMAL_PAIR = asList(
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
                auth(authWith(NORMAL_PAIR))
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
                auth(authWith(PDD, NORMAL))
                        .get(shouldBe(okAuth()))
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
                auth(authWith(FIVE_PAIRS))
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
        AccountInformation info = auth(authWith(NORMAL_PAIR))
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
        AccountInformation info = auth(authWith(NORMAL_PAIR))
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
        AccountInformation info = auth(authWith(NORMAL_PAIR))
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
        AccountInformation info = auth(authWith(NORMAL_PAIR))
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
