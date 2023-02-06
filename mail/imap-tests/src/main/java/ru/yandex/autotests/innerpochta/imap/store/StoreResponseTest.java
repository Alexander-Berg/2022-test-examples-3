package ru.yandex.autotests.innerpochta.imap.store;

import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.StoreRequest;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CopyRequest.copy;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.ExpungeRequest.expunge;
import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created by nmikutskiy on 30.05.16.
 * <p>
 * [MPROTO-2787]
 */

@Aqua.Test
@Title("Команда STORE. Проверяем корректность ответа сервера для команды store")
@Features({ImapCmd.STORE})
@Stories("Имитация работы пользователя")
@Description("После работы с письмами в новой папке")
@Issue("MPROTO-2787")
public class StoreResponseTest extends BaseTest {
    private static Class<?> currentClass = StoreResponseTest.class;


    @ClassRule
    public static final ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Title("Добжен возвращать верный номер сообщения в response после выставления флага на удаление")
    public void checkMessageNumberInResponseAfterCopy() throws Exception {

        String folderName = Utils.generateName();

        imap.request(create(folderName)).shouldBeOk();
        imap.select().folder(folderName);

        imap.append().appendRandomMessage(folderName);
        imap.request(copy("1", folderName)).shouldBeOk();

        imap.store().deletedOnSequence("1");
        imap.request(expunge()).shouldBeOk();
        imap.request(fetch("2").uid(true).flags()).shouldBeOk().numberShouldBe(1);

        imap.append().appendRandomMessage(folderName);
        imap.request(store("2", StoreRequest.PLUS_FLAGS, MessageFlags.DELETED.value()).uid(true)).shouldBeOk().numberShouldBe(1);

    }
}
