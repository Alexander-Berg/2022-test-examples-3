package ru.yandex.autotests.innerpochta.imap.append;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.AppendRequest.append;
import static ru.yandex.autotests.innerpochta.imap.requests.NoOpRequest.noOp;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getRandomMessage;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;

public class AppendDoubleSent extends BaseTest {
    private static Class<?> currentClass = AppendDoubleSent.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient imap2 = withCleanBefore(newLoginedClient(currentClass));

    private static final String PATH_TO_EML = "/messages/complicated_message.eml";
    private static String message;

    @BeforeClass
    public static void prepareEml() throws Exception {
        message = getRandomMessage();
    }

    @Test
    @Description("Проверяем что при аппенде дубликата в параллельной сессии приходит корректный статус апдейт")
    public void testDoubleAppendResponses() throws Exception {
        String literalMessage = literal(message);
        String folder = systemFolders().getSent();

        imap2.select().folder(folder);
        imap2.request(append(folder, literalMessage)).shouldBeOk();

        imap.request(select(folder)).existsShouldBe(1);
        Integer uidValidity = imap2.request(select(folder)).uidValidity();
        Integer uidNext = imap2.request(select(folder)).uidNext();
        imap2.request(append(folder, literalMessage)).shouldBeOk().uidvalidityShouldBe(uidValidity).uidShouldBe(uidNext);

        imap.request(noOp()).expungeShouldBe(1).existsShouldBe(1);
    }
}
