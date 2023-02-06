package ru.yandex.autotests.innerpochta.akita;

import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.akita.Aliases;
import ru.yandex.autotests.innerpochta.beans.akita.Attributes;
import ru.yandex.autotests.innerpochta.beans.akita.AuthResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.*;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiAkitaWithoutAuth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.xOriginalHost;


@Aqua.Test
@Title("[Akita] Ручки авторизации auth")
@Description("В выдаче akita в ручках auth поле defaultEmail будет пустым если не запрошено получение всех адресов")
@Credentials(loginGroup = "AkitaAuth")
@Features(MyFeatures.AKITA)
@Stories(MyStories.AUTH)
@Issue("MAILPG-1370")
public class AuthTest extends AkitaBaseTest {
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
    @Title("Проверяем, что auth возвращает UserTicket")
    public void shouldReturnUserTicket() {
        assertThat("Пустой тикет",
                auth()
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getUserTicket(),
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
    @Issue("MAILPG-3418")
    @Title("Должны вернуть ошибку если запрошен несуществующий атрибут")
    public void shouldReturnErrorOnUnexistingAttribute() {
        auth()
                .withAttributesToCheck("1111111")
                .get(shouldBe(internalError()));
    }

    @Test
    @Issue("MAILPG-3418")
    @Title("Проверяем наличие в ответе auth атрибутов ЧЯ")
    public void shouldReturnAttributes() {
        String json = auth()
                .withAttributesToCheck("33")
                .withAttributesToCheck("34")
                .withAttributesToCheck("1013")
                .withAttributesToCheck("1015")
                .get(shouldBe(okAuth()))
                .asString();

        assertThat(JsonPath.read(json, "$.account_information.attributes.1013"), equalTo("16"));
        assertThat(JsonPath.read(json, "$.account_information.attributes.34"), equalTo("ru"));
        assertThat(JsonPath.read(json, "$.account_information.attributes.33"), equalTo("Europe/Moscow"));
        try {
            JsonPath.read(json, "$.account_information.attributes.1015");
            fail("Атрибута 1015 не должно быть у пользователя");
        } catch (Exception e) { }
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
    @Issue("MAILPG-4592")
    @Title("Проверяем наличие login_id от ЧЯ в ответе auth")
    public void shouldHaveBlackboxLoginId() {
        assertThat("Неверное значение аттрибута 'bbLoginId'",
                auth()
                        .get(shouldBe(okAuth()))
                        .as(AuthResponse.class)
                        .getAccountInformation()
                        .getAccount()
                        .getBbLoginId(),
                not(""));
    }

    @Test
    @Title("Проверяем существование ручки ninja_auth")
    public void shouldFindNinjaAuthHandler() {
        ninjaAuth()
                .get(shouldBe(okAuth()));
    }

    @Test
    @Title("Логинимся с помощью OAuth токена")
    public void shouldLoginWithOAuthToken() {
        oauth()
                .get(shouldBe(okAuth()));
    }
}