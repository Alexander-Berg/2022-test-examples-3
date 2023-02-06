package ru.yandex.autotests.innerpochta.imap.idle;

import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.StoreRequest;
import ru.yandex.autotests.innerpochta.imap.responses.IdleResponse;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.autotests.innerpochta.imap.steps.SmtpSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CopyRequest.copy;
import static ru.yandex.autotests.innerpochta.imap.requests.DoneRequest.done;
import static ru.yandex.autotests.innerpochta.imap.requests.ExpungeRequest.expunge;
import static ru.yandex.autotests.innerpochta.imap.requests.IdleRequest.idle;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 29.04.14
 * Time: 15:58
 * Ждем 2 секунды для того чтобы не ловить событие subscribed от ксивы в имап
 * <p/>
 */
@Aqua.Test
@Title("Команда IDLE. Подписывает клиента на обновления")
@Features({ImapCmd.IDLE})
@Stories(MyStories.COMMON)
@Issue("MAILPROTO-2198")
@Description("Команда IDLE. Общие тесты. Тесты с двумя тестами")
public class IdleCommonTest extends BaseTest {
    private static Class<?> currentClass = IdleCommonTest.class;


    public static final int WAIT_SECONDS = 2;
    @ClassRule
    public static final ImapClient imap2 = (newLoginedClient(currentClass));
    private final SmtpSteps smtp = new SmtpSteps(currentClass.getSimpleName());
    @Rule
    public ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Description("Делаем IDLE, затем DONE")
    @ru.yandex.qatools.allure.annotations.TestCaseId("217")
    public void simpleIdleTest() throws InterruptedException {
        imap.request(idle()).hasIdling(IdleResponse.IDLING);
        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
        imap.request(done()).shouldBeOk();
    }

    @Test
    @Description("Дважды делаем IDLE. Ожидаемый результат: BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("218")
    public void doubleIdleTest() throws InterruptedException {
        imap.request(idle()).hasIdling(IdleResponse.IDLING);
        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
        imap.request(idle()).shouldBeBad().statusLineContains(ImapResponse.COMMAND_SYNTAX_ERROR);
    }

    @Test
    @Stories(MyStories.TWO_SESSION)
    @Description("Выполняем IDLE с двумя сессиями, проверяем, что изменения сразу же подтягиваются." +
            "Необходимо заселектить папку перед IDLE")
    @ru.yandex.qatools.allure.annotations.TestCaseId("219")
    public void idleWithTwoSessionSendMessage() throws Exception {
        imap.select().inbox();
        imap.request(idle()).hasIdling(IdleResponse.IDLING);
        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));

        smtp.send();
        imap2.select().waitMsgInInbox();
        imap.readFuture(IdleResponse.class).existsShouldBe(1);
        imap.readFuture(IdleResponse.class).recentShouldBe(1);
        imap.request(done()).shouldBeOk();
    }

    @Ignore
    @Test
    @Stories(MyStories.TWO_SESSION)
    @Description("Тестируем IDLE с двумя сессиями: в одной включаем IDLE, в другой делаем EXPUNGE")
    @ru.yandex.qatools.allure.annotations.TestCaseId("221")
    public void idleDeleteFromTwoSessionTest() throws Exception {
        imap2.append().appendRandomMessageInInbox();
        imap.select().waitMsgsInInbox(1);
        // ставим флаг /Deleted на первое письмо
        imap2.select().inbox();
        imap2.store().deletedOnSequence("1");
        //врубаем IDLE
        imap.request(idle()).hasIdling(IdleResponse.IDLING);
        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
        //делаем EXPUNGE во второй сесии
        imap2.request(expunge()).shouldBeOk();
        //должны получить 1 EXPUNGE
        imap.readFuture(IdleResponse.class).expungeShouldBe(1);
        imap.request(done()).shouldBeOk();
    }

    @Test
    @Description("Аппендим письмо, проверяем, что рузультат приходит в IDLE")
    @ru.yandex.qatools.allure.annotations.TestCaseId("222")
    public void idleWithAppendShouldSeeExistAndRecent() throws Exception {
        imap.select().inbox();

        imap.request(idle()).hasIdling(IdleResponse.IDLING);
        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));

        imap2.append().appendRandomMessageInInbox();
        imap2.select().waitMsgsInInbox(1);

        imap.readFuture(IdleResponse.class).existsShouldBe(1);
        imap.readFuture(IdleResponse.class).recentShouldBe(1);
        imap.request(done()).shouldBeOk();
    }

    @Test
    @Issue("MPROTO-355")
    @Description("Копируем письмо, проверяем, что рузультат приходит в IDLE ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("223")
    public void idleWithCopyShouldSeeExistAndRecent() throws Exception {
        imap2.append().appendRandomMessageInInbox();
        imap.select().waitMsgsInInbox(1);

        imap.select().inbox();
        imap.request(idle()).hasIdling(IdleResponse.IDLING);
        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));

        imap2.select().inbox();
        imap2.request(copy("1", Folders.INBOX)).shouldBeOk();
        imap2.select().waitMsgsInInbox(2);

        imap.readFuture(IdleResponse.class).existsShouldBe(2);
        imap.readFuture(IdleResponse.class).recentShouldBe(2);
        imap.request(done()).shouldBeOk();
    }


    @Test
    @Web
    @Issues({@Issue("MAILORA-352")})
    @Description("Ставим флаг на письмо, должны увидеть fetch с флагом [MPROTO-355]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("224")
    public void idleWithStoreFlagsShouldSeeFetch() throws Exception {
        imap2.append().appendRandomMessageInInbox();
        imap.select().waitMsgsInInbox(1);
        imap2.select().inbox();

        imap.select().inbox();
        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
        imap.request(idle()).hasIdling(IdleResponse.IDLING);
        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
        //получаем response от феча, который должны увидеть в IDLE
        String response = imap2.request(store("*", StoreRequest.FLAGS, roundBraceList(MessageFlags.random())))
                .shouldBeOk().lines().get(0);
        imap.readFuture(IdleResponse.class).fetchShouldBe(response);
        imap.request(done()).shouldBeOk();
    }

    @Test
    @Web
    @Issues({@Issue("MAILORA-352")})
    @Description("Снимаем флаг с письма, должны увидеть fetch с флагом [MPROTO-355]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("225")
    public void idleWithStoreMinusFlagsShouldSeeFetch() throws Exception {
        imap2.append().appendRandomMessageInInbox();
        imap.select().waitMsgsInInbox(1);

        imap.select().inbox();
        //ставим флаг и снимаем /recent
        String flag = MessageFlags.random();
        imap.request(store("*", StoreRequest.FLAGS, roundBraceList(flag)))
                .shouldBeOk();

        imap2.select().inbox();

        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
        imap.request(idle()).hasIdling(IdleResponse.IDLING);
        Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
        String response = imap2.request(store("*", StoreRequest.MINUS_FLAGS, roundBraceList(flag)))
                .shouldBeOk().lines().get(0);
        imap.readFuture(IdleResponse.class).fetchShouldBe(response);
        imap.request(done()).shouldBeOk();
    }
}
