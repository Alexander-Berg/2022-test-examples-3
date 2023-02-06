package ru.yandex.autotests.innerpochta.imap.search;

import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.SearchRequest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.ANSWERED;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.DRAFT;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.FLAGGED;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.SEEN;
import static ru.yandex.autotests.innerpochta.imap.consts.folders.Folders.INBOX;
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
@RunWith(Parameterized.class)
public class SearchByFlagsTest extends BaseTest {
    private static Class<?> currentClass = SearchByFlagsTest.class;


    public static String message1 = "1";

    public static String message2 = "2";

    public static String message3 = "3";
    @ClassRule
    public static ImapClient imap = withCleanBefore(newLoginedClient(currentClass));
    @Parameterized.Parameter(0)
    public SearchRequest request;
    @Parameterized.Parameter(1)
    public SearchRequest unRequest;
    @Parameterized.Parameter(2)
    public String flag;

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(
                new Object[]{search().seen(), search().unseen(), SEEN.value()},
                new Object[]{search().draft(), search().undraft(), DRAFT.value()},
                new Object[]{search().answered(), search().unanswered(), ANSWERED.value()},
                new Object[]{search().flagged(), search().unflagged(), FLAGGED.value()}
        );
    }

    @BeforeClass
    public static void setUp() throws Exception {
        imap.append().appendRandomMessage(INBOX, imapMessage());
        imap.append().appendRandomMessage(INBOX, imapMessage());
        imap.append().appendRandomMessage(INBOX, imapMessage());
        imap.select().inbox();
    }

    @Test
    @Description("Ищем по ключу флаг. Находим по обычному ключу (например seen)\n" +
            "и не находим по противоположному (соответственно unseen) ключу.\n" +
            "Удаляем флаг у сообщения. Ожидание меняется.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("526")
    public void shouldSearchBySystemFlag() {
        imap.request(store(message1, PLUS_FLAGS, flag)).shouldBeOk();
        imap.request(request).shouldBeOk().shouldSeeMessages(message1);
        imap.request(unRequest).shouldBeOk().shouldSeeMessages(message2, message3);

        imap.request(store(message1, MINUS_FLAGS, flag)).shouldBeOk();
        imap.request(request).shouldBeOk().shouldBeEmpty();
        imap.request(unRequest).shouldBeOk().shouldSeeMessages(message1, message2, message3);
    }
}
