package ru.yandex.mail.tests.akita;


import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.properties.Scope;
import ru.yandex.mail.tests.akita.generated.AuthResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.Scopes.DEVPACK;
import static ru.yandex.mail.common.properties.Scopes.TESTING;


@Aqua.Test
@Title("[Akita] Ручки авторизации auth")
@Description("Проверяем, что есть атрибут haveYaplus")
@Issue("MAILPG-1820")
@Scope({TESTING, DEVPACK})
public class YaplusTest extends AkitaBaseTest {
    AccountWithScope mainUser() {
        return Accounts.yplus;
    }

    @Test
    @Title("Проверяем наличие в ответе auth атрибута haveYaplus")
    public void shouldHaveYaplusAttribute() {
        assertThat("Неверное значение аттрибута 'haveYaplus'",
                auth()
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getAccount()
                        .getAttributes()
                        .getHaveYaplus(),
                is(true));
    }
}
