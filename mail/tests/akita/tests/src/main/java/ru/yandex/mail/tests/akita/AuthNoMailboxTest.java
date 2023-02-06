package ru.yandex.mail.tests.akita;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;

/**
 * Нельзя заходить в ящик через в вэбинтерфейс ибо тест сломается.
 * Как отписать пользователя от ящика: https://passport-test.yandex.ru/passport?mode=unsubscribe&from=mail
 */
@Aqua.Test
@Title("[AUTH] Запросы с юзером, который не разу не заходил в вэбинтерфейс")
@Description("Делаем запрос у пользователя без почтового сида")
public class AuthNoMailboxTest extends AkitaBaseTest {
    AccountWithScope mainUser() {
        return Accounts.noMailboxTest;
    }

    @Test
    @Description("Делаем запросы с юзером без почтового сида")
    public void authWithNoMailboxTest() {
        auth()
                .get(shouldBe(noMailbox()));
    }
}
