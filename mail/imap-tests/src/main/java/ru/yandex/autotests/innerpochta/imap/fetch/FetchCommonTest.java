package ru.yandex.autotests.innerpochta.imap.fetch;

import javax.mail.MessagingException;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.append.AppendMessages;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.FetchRequest;
import ru.yandex.autotests.innerpochta.imap.responses.FetchStoreResponse;
import ru.yandex.autotests.innerpochta.imap.steps.SmtpSteps;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.AppendRequest.append;
import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getFilledMessageWithAttachFromEML;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getMessage;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 20.05.14
 * Time: 14:09
 */
@Aqua.Test
@Title("Команда FETCH. Общие тесты")
@Features({ImapCmd.FETCH})
@Stories(MyStories.COMMON)
@Description("Общие тесты на FETCH.")
public class FetchCommonTest extends BaseTest {
    private static Class<?> currentClass = FetchCommonTest.class;


    @ClassRule
    public static final ImapClient imap = newLoginedClient(currentClass);
    private final SmtpSteps smtp = new SmtpSteps(currentClass.getSimpleName());
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Description("Делаем FETCH на пустом ящике первого сообщения.\n" +
            "Должны увидеть (no messages)")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("194")
    public void fetchOnEmptyMailboxShouldBeNoMessages() {
        imap.select().waitNoMessagesInInbox();
        imap.select().inbox();
        imap.request(fetch("1").flags()).shouldBeOk().statusLineContains(FetchStoreResponse.FETCH_NO_MESSAGES);
    }

    @Description("Отсылаем сообщение.\n" +
            "Ожидаемый результат: должны увидеть флаг /Recent")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("195")
    public void sendMessageShouldSeeRecentFlag() throws MessagingException {
        smtp.subj(Utils.cyrillic()).text(Utils.generateName()).send();

        imap.select().waitMsgInInbox();
        imap.select().inbox();

        imap.request(fetch("1").flags()).shouldBeOk().flagShouldBe(MessageFlags.RECENT.value());
    }

    @Description("Аппендим рандомное сообщение.\n" +
            "Ожидаемый результат: должны увидеть флаг /Recent и /Unseen")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("196")
    public void appendMessageShouldSeeRecentFlag() throws Exception {
        prodImap.append().appendRandomMessageInInbox();
        imap.select().waitMsgInInbox();
        imap.select().inbox();

        imap.request(fetch("1").flags()).shouldBeOk().flagShouldBe(MessageFlags.RECENT.value());
    }

    @Test
    @Description("Аппендим рандомное сообщение.\n" +
            "Ожидаемый результат: должны увидеть флаг /Recent и /Unseen")
    @ru.yandex.qatools.allure.annotations.TestCaseId("197")
    public void doubleFetchShouldTakeOffRecentFlag() throws Exception {
        prodImap.append().appendRandomMessageInInbox();
        imap.select().waitMsgInInbox();
        imap.select().inbox();

        imap.request(fetch("1").flags()).shouldBeOk().flagShouldBe(MessageFlags.RECENT.value());
        imap.request(fetch("1").flags()).shouldBeOk().shouldBeEmptyFlags();
    }


    @Description("Отсылаем сообщение.\n" +
            "Ожидаемый результат: UIDNEXT должен быть такой же как в статусе")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("191")
    public void fetchShouldSeeUidNext() throws MessagingException {
        int uidNext = prodImap.status().getUidNext(Folders.INBOX);
        smtp.subj(Utils.cyrillic()).text(Utils.generateName()).send();

        imap.select().waitMsgInInbox();
        imap.select().inbox();

        imap.request(fetch("1").uid()).shouldBeOk().uidShouldBe(uidNext);
    }

    @Description("FETCH c EXAMINE\n" +
            "Ожидаемый результат: OK")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("198")
    public void fetchWithExamineShouldSeeOk() throws Exception {
        prodImap.append().appendRandomMessageInInbox();
        imap.select().waitMsgInInbox();

        imap.examine().inbox();
        imap.request(fetch("1").flags()).shouldBeOk().flagShouldBe(MessageFlags.RECENT.value());
    }

    @Description("Фетчим простое сообщение находясь не в папке\n" +
            "Ожидаемый результат: BAD")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("199")
    public void fetchWithUnselectedFolder() {
        imap.request(unselect());
        imap.request(fetch("1:*").fast()).shouldBeBad().statusLineContains(FetchStoreResponse.FETCH_WRONG_SESSION_STATE);
    }

    /**
     * Если у письма есть, например, части, 1, 1.1 и 1.2 (и часть 1.1, например, имеет размер 2 байта),
     * то доступ к этим частям с точки зрения imap делается как FETCH BODY[], FETCH BODY[1], FETCH BODY[2].
     * Т.е. без первой единицы.
     * Так вот, некоторые клиенты пытались доступиться к первой части неправильно и делали FETCH BODY[1.1].
     * Мулька в ответ, естественно ругалась ошибкой.
     * Egorp сделал для imap-а опцию, которая позволяет игнорировать ошибки мульки и отдавать в ответ на такие запросы
     * (BODY[1.1] NIL).
     * Так, например, поступает gmail.
     */
    @Ignore("[MAILPROTO-2164] <tolerate_mulca_errors>1</tolerate_mulca_errors> нужно включать руками")
    @Title("Команда FETCH. Извлекаем часть 1.1 из тела письма [MAILPROTO-2164]")
    @Features({ImapCmd.FETCH})
    @Stories({"BODY"})
    @Description("Извлекаем различные данные из сообщения с помощью команды BODY и BODY.PEEK")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("193")
    public void fetchEmptyBodyTest() throws Exception {
        TestMessage emptyMsg = getFilledMessageWithAttachFromEML
                (AppendMessages.class.getResource("/messages/empty1.1part.eml").toURI());

        prodImap.request(append(Folders.INBOX,
                literal(getMessage(emptyMsg)))).shouldBeOk();
        prodImap.select().waitMsgsInInbox(1);

        imap.select().inbox();
        FetchRequest fetchBody = fetch("1").body("1.1");
        imap.request(fetchBody).shouldBeOk()
                .shouldBeEqualTo(prodImap.request(fetchBody).shouldBeOk());
    }

    @Test
    @Title("Команда FETCH. Имя аттача")
    @Stories({"BODYSTUCTURE"})
    @Description("Проверяем с помощью FETCH BODYSTRUCTURE, что не декодируем значение заголовка filename.")
    @Issue("DARIA-45831")
    @ru.yandex.qatools.allure.annotations.TestCaseId("200")
    public void fetchFilenameTest() throws Exception {
        TestMessage msgWithAttach = getFilledMessageWithAttachFromEML
                (AppendMessages.class.getResource("/messages/filename.eml").toURI());

        prodImap.request(append(Folders.INBOX,
                literal(getMessage(msgWithAttach)))).shouldBeOk();
        prodImap.select().waitMsgsInInbox(1);

        imap.select().inbox();
        FetchRequest fetchBody = fetch("1").bodystructure();
        imap.request(fetchBody).shouldBeOk()
                .shouldBeEqualTo(prodImap.request(fetchBody).shouldBeOk());
    }

}
