package ru.yandex.autotests.innerpochta.imap.search;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.consts.folders.Folders.INBOX;
import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.MINUS_FLAGS;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.PLUS_FLAGS;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.ImapMessage.imapMessage;

/**
 * Created by kurau on 14.07.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по флагам")
@Features({ImapCmd.SEARCH})
@Stories("#поиск по флагам")
@Description("Для каждого флага есть свой уникальный ключ. В первую очередь ищем по этим ключам.\n" +
        "Добавляем и убираем флаги у сообщения. Ищем это сообщение по ключу.")
public class SearchByFlagsMissTest extends BaseTest {
    private static Class<?> currentClass = SearchByFlagsMissTest.class;


    public static String message1 = "1";

    public static String message2 = "2";

    @ClassRule
    public static ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @BeforeClass
    public static void setUp() throws Exception {
        imap.append().appendRandomMessage(INBOX, imapMessage());
        imap.append().appendRandomMessage(INBOX, imapMessage());
        imap.select().inbox();
    }

    @Test
    @Description("Проверяем поиск по ключу NEW = RECENT + UNSEEN. Просто RECENT + SEEN не находим.\n" +
            "Убираем SEEN и находим.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("524")
    public void searchFlagsNew() {
        imap.request(store(message1, PLUS_FLAGS, MessageFlags.SEEN.value())).shouldBeOk();
        imap.request(search().newMessages()).shouldBeOk().shouldSeeMessages(message2);
        imap.request(store(message1, MINUS_FLAGS, MessageFlags.SEEN.value())).shouldBeOk();
        imap.request(search().newMessages()).shouldBeOk().shouldSeeMessages(message1, message2);
    }

    @Test
    @Title("Ищем по OLD = NOT + RECENT.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("525")
    public void searchFlagsOld() {
        imap.request(fetch(message1).body()).shouldBeOk();
        imap.request(fetch(message1).flags()).shouldBeEmptyFlags();
        imap.request(search().recent()).shouldBeOk().shouldSeeMessages(message2);
        imap.request(search().old()).shouldBeOk().shouldSeeMessages(message1);
    }
}
