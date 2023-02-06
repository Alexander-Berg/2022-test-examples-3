package ru.yandex.autotests.innerpochta.imap.store;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.StoreRequest;
import ru.yandex.autotests.innerpochta.imap.responses.FetchStoreResponse;
import ru.yandex.autotests.innerpochta.imap.steps.SmtpSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.model.SeverityLevel;

import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.random;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.randomCyrillic;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.FLAGS;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.MINUS_FLAGS;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 19:48
 * [MAILPROTO-1745]
 * [MAILPROTO-2315]
 */
@Aqua.Test
@Title("Команда STORE. Общие тесты")
@Features({ImapCmd.STORE})
@Stories(MyStories.COMMON)
@Description("Общие тесты на STORE")
public class StoreCommonTest extends BaseTest {
    private static Class<?> currentClass = StoreCommonTest.class;



    @ClassRule
    public static final ImapClient imap = newLoginedClient(currentClass);
    private final SmtpSteps smtp = new SmtpSteps(currentClass.getSimpleName());
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Before
    public void addOneMessage() throws Exception {
        prodImap.append().appendRandomMessageInInbox();
        imap.select().waitMsgs(Folders.INBOX, 1);
    }

    @Test
    @Title("Должны возвращать текущие флаги даже если ничего не удалили с помощью -flags")
    @ru.yandex.qatools.allure.annotations.TestCaseId("610")
    public void shouldReturnUntaggedFetchResponseAfterDeleteNonExistFlag() {
        String flag = random();
        imap.select().inbox();
        imap.request(store("1", MINUS_FLAGS, flag)).shouldBeOk()
                .flagsShouldBe(MessageFlags.RECENT.value());
    }

    @Description("Пытаемся поставить пустой (\"\") флаг")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("609")
    public void storeNullFlagsShouldSeeOk() throws Exception {
        imap.select().inbox();
        imap.request(store("1", FLAGS, roundBraceList(""))).shouldBeOk();
        imap.fetch().flagShouldBe("1", MessageFlags.RECENT.value());
    }

    @Description("STORE кириллической папки без энкодинга [MAILPROTO-2141][MAILPROTO-2315][MAILPROTO-2324]\n" +
            "Должны увидеть: Ok и флаг с русскими символами")
    @Stories({MyStories.JIRA, MyStories.СYRILLIC_SYMBOLS})
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("612")
    public void storeCyrillicFlagShouldSeeOk() {
        imap.select().inbox();
        String cyrillicFlag = randomCyrillic();
        imap.request(store("1", FLAGS, roundBraceList(cyrillicFlag))).shouldBeOk()
                .flagsShouldBe(MessageFlags.RECENT.value(), cyrillicFlag);
    }

    @Description("STORE без селекта\n" +
            "Ожидаемый результат: BAD")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("613")
    public void storeWithoutSelectShouldSeeBad() {
        imap.request(unselect());
        imap.request(store("1", FLAGS, roundBraceList(random()))).shouldBeBad()
                .statusLineContains(FetchStoreResponse.STORE_WRONG_SESSION_STATE);
    }

    @Description("STORE с (())\n" +
            "Ожидаемый результат: BAD")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("614")
    public void withDoubleRoundBraceShouldSeeBad() {
        imap.select().inbox();
        imap.request(store("1", FLAGS, roundBraceList(roundBraceList(random())))).shouldBeBad()
                .statusLineContains(FetchStoreResponse.COMMAND_SYNTAX_ERROR);
    }

    @Description("STORE c EXAMINE\n" +
            "Ожидаемый результат: BAD, read-only folder.")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("615")
    public void storeWithExamineShouldSeeBad() {
        imap.examine().inbox();
        imap.request(store("1", FLAGS, roundBraceList(random()))).shouldBeBad()
                .statusLineContains(FetchStoreResponse.STORE_READ_ONLY_FOLDER);
    }

    @Description("STORE с двумя ОДИНАКОВЫМИ кириллическими флагами [MAILPROTO-2324][MAILPROTO-2190][MAILPROTO-2315]\n" +
            "Ожидаемый результат: 1 флаг + \\Recent")
    @Stories({MyStories.JIRA, MyStories.СYRILLIC_SYMBOLS})
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("616")
    public void storeDoubleFlag() {
        String flag = randomCyrillic();
        imap.select().inbox();
        imap.request(store("1", FLAGS, roundBraceList(flag, flag))).shouldBeOk()
                .flagsShouldBe(MessageFlags.RECENT.value(), flag);
    }

    @Description("STORE с двумя разными флагами, один из которых кириллический\n" +
            "[MAILPROTO-2324][MAILPROTO-2181][MAILPROTO-2315]\n" +
            "Ожидаемый результат: 2 флага + \\Recent")
    @Severity(SeverityLevel.BLOCKER)
    @Stories({MyStories.JIRA, MyStories.СYRILLIC_SYMBOLS})
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("617")
    public void storeTwoFlags() {
        String flag = randomCyrillic();
        String flag2 = random();
        imap.select().inbox();
        imap.request(store("1", FLAGS, roundBraceList(flag, flag2))).shouldBeOk()
                .flagsShouldBe(MessageFlags.RECENT.value(), flag, flag2);
    }

    @Description("Ставим флаг \\answered, снимаем его.\n " +
            "Не должны увидеть флагов. [MAILPROTO-1745]")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("618")
    public void afterAnsweredShouldNotSeeSeenFlag() {
        imap.select().inbox();
        imap.fetch().flagShouldBe("1", MessageFlags.RECENT.value());

        imap.request(store("1", FLAGS, roundBraceList(MessageFlags.ANSWERED.value()))).shouldBeOk()
                .flagShouldBe(MessageFlags.ANSWERED.value());

        //снимаем флаги:
        imap.request(store("1", StoreRequest.MINUS_FLAGS, MessageFlags.ANSWERED.value())).shouldBeOk();

        imap.select().inbox();
        imap.fetch().shouldBeNoFlags("1");
    }
}
