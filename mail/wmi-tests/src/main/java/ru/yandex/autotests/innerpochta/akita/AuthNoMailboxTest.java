package ru.yandex.autotests.innerpochta.akita;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;

/**
 * Нельзя заходить в ящик через в вэбинтерфейс ибо тест сломается.
 * Как отписать пользователя от ящика: https://passport-test.yandex.ru/passport?mode=unsubscribe&from=mail
 */
@Aqua.Test
@Title("[AUTH] Запросы с юзером, который не разу не заходил в вэбинтерфейс")
@Description("Делаем запрос у пользователя без почтового сида")
@Features(MyFeatures.AKITA)
@Stories(MyStories.AUTH)
@Credentials(loginGroup = "AuthNoMailboxTest")
public class AuthNoMailboxTest extends AkitaBaseTest {
    @Test
    @Description("Делаем запросы с юзером без почтового сида")
    public void authWithNoMailboxTest() {
        auth()
                .get(shouldBe(noMailbox()));
    }
}
