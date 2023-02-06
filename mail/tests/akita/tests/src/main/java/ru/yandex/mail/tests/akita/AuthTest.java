package ru.yandex.mail.tests.akita;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.tests.akita.generated.auth.ApiAuth;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.properties.Scope;
import ru.yandex.mail.tests.akita.generated.Attributes;
import ru.yandex.mail.tests.akita.generated.AuthResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.Scopes.INTRANET_PRODUCTION;
import static ru.yandex.mail.common.properties.Scopes.PRODUCTION;
import static ru.yandex.mail.common.properties.Scopes.TESTING;


@Aqua.Test
@Title("[Akita] Ручки авторизации auth")
@Description("В выдаче akita в ручках auth поле defaultEmail будет пустым если не запрошено получение всех адресов")
@Issue("MAILPG-1370")
public class AuthTest extends AkitaBaseTest {
    AccountWithScope mainUser() {
        return Accounts.authTest;
    }

    @Test
    @Title("auth с email=yes")
    public void authWithEmailYesDefaultAddressTest() {
        assertThat("Неправильный адрес по умолчанию",
                auth()
                        .withEmails(ApiAuth.EmailsParam.YES)
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getAddresses()
                        .getDefaultAddress(),
                equalTo(mainUser().get().email())
        );
    }

    @Test
    @Title("auth с email=no")
    public void authWithoutEmailDefaultAddressTest() {
        assertThat("Неправильный адрес по умолчанию",
                auth()
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getAddresses()
                        .getDefaultAddress(),
                equalTo("")
        );
    }

    @Test
    @Title("auth с email=")
    public void authWithNoDefaultAddressTest() {
        assertThat("Неправильный адрес по умолчанию",
                auth()
                        .withEmails(ApiAuth.EmailsParam.NO)
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getAddresses()
                        .getDefaultAddress(),
                equalTo("")
        );
    }

    @Test
    @Title("Авторизация без кук")
    @Description("Авторизируемся без кук. Ожидаемый результат ошибка с кодом 2001")
    public void authWithoutCookies() {
        apiAkitaWithoutAuth().auth()
                .withXoriginalhostHeader(xOriginalHost())
                .get(shouldBe(noAuth()));
    }

    @Test
    @Issue("MAILPG-823")
    @Title("Проверяем, что auth возвращает TVM тикет")
    @Scope({PRODUCTION, INTRANET_PRODUCTION, TESTING})
    public void shouldReturnTicket() {
        assertThat("Пустой tvm1 тикет",
                auth()
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getTicket(),
                not("")
        );
    }

    @Test
    @Issue("MAILPG-1195")
    @Title("Проверяем наличие в ответе auth атрибутов ЧЯ")
    public void shouldHaveBlackboxAttributes() {
        Attributes attrs = auth()
                .get(shouldBe(okAuth()))
                .as(AuthResponse.class)
                .getAccountInformation()
                .getAccount()
                .getAttributes();

        assertThat("Неверное значение аттрибута 'haveOrganizationName'",
                attrs.getHaveOrganizationName(),
                equalTo(false)
        );

        assertThat("Неверное значение аттрибута 'haveYaplus'",
                attrs.getHaveYaplus(),
                equalTo(false)
        );

        assertThat("Неверное значение аттрибута 'securityLevel'",
                attrs.getSecurityLevel(),
                equalTo("16")
        );
    }

    @Test
    @Issue("MAILPG-1491")
    @Title("Проверяем наличие connection_id от ЧЯ в ответе auth")
    public void shouldHaveBlackboxConnectionId() {
        assertThat("Неверное значение аттрибута 'bbConnectionId'",
                auth()
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getAccount()
                        .getBbConnectionId(),
                not(""));
    }

    @Test
    @Title("Проверяем существование ручки ninja_auth")
    public void shouldFindNinjaAuthHandler() {
        ninjaAuth()
                .get(shouldBe(okAuth()));
    }
}
