package ru.yandex.autotests.innerpochta.akita;


import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.akita.AuthResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Scope;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes.TESTING;


@Aqua.Test
@Title("[Akita] Ручки авторизации auth")
@Description("Проверяем, что есть атрибут haveYaplus")
@Credentials(loginGroup = "AkitaYaplus")
@Features(MyFeatures.AKITA)
@Stories(MyStories.AUTH)
@Issue("MAILPG-1820")
@Scope(TESTING)
public class YaplusTest extends AkitaBaseTest {
    @Test
    @Title("Проверяем наличие в ответе auth атрибута haveYaplus")
    public void shouldHaveYaplusAttribute() {
        assertThat("Неверное значение аттрибута 'haveYaplus'."
                        + "Последний раз выставлялось с помощью @dlihatskiy",
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
