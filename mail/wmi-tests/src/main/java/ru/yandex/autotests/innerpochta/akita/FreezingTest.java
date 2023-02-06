package ru.yandex.autotests.innerpochta.akita;


import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.akita.AuthUserIsFrozenResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.CommonUtils.authWith;

@Aqua.Test
@Title("[Akita] Ручки авторизации auth")
@Description("Проверка формата ответа для неактивных пользователей")
@Credentials(loginGroup = "AkitaFrozenUser")
@Features(MyFeatures.AKITA)
@Stories(MyStories.AUTH)
@Issue("MAILPG-3879")
public class FreezingTest extends AkitaBaseTest  {
    private static final String ARCHIVED_LOGIN_GROUP = "AkitaArchivedUser";

    @Test
    @Title("Проверяем наличие в ответе всех нужных полей для замороженного пользователя")
    public void shouldHaveAllAttributesForFrozenUser() {
        AuthUserIsFrozenResponse attrs = auth()
                .get(shouldBe(frozenAuth()))
                .as(AuthUserIsFrozenResponse.class);
    }

    @Test
    @Title("Проверяем наличие в ответе всех нужных полей для заархивированного пользователя")
    public void shouldHaveAllAttributesForArchivedUser() {
        AuthUserIsFrozenResponse attrs = auth(authWith(props().account(ARCHIVED_LOGIN_GROUP)))
                .get(shouldBe(frozenAuth()))
                .as(AuthUserIsFrozenResponse.class);
    }
}
