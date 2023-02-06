package ru.yandex.autotests.innerpochta.imap.subscribe;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.autotests.innerpochta.imap.responses.SubscribeResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.SubscribeRequest.subscribe;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.rules.UnsubscribeRule.withUnsubscribeBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 09.04.14
 * Time: 20:08
 */
@Aqua.Test
@Title("Команда SUBSCRIBE. Подписываемся папки")
@Features({ImapCmd.SUBSCRIBE})
@Stories(MyStories.COMMON)
@Description("Общие тесты на SUBSCRIBE. SUBSCRIBE без параметров")
public class SubscribeCommonTest extends BaseTest {
    private static Class<?> currentClass = SubscribeCommonTest.class;


    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));


    @Rule
    public ImapClient prodImap = withUnsubscribeBefore(withCleanBefore(newLoginedClient(currentClass)));

    @Description("SUBSCRIBE без параметров\n" +
            "Должны увидеть: BAD")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("633")
    public void subscribeWithoutParamShouldSeeBad() {
        imap.request(subscribe("")).shouldBeBad().statusLineContains(ImapResponse.COMMAND_SYNTAX_ERROR);
    }

    @Description("SUBSCRIBE кириллической папки без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("634")
    public void subscribeCyrillicFolderShouldSeeBad() {
        imap.request(subscribe(Utils.cyrillic())).shouldBeBad()
                .statusLineContains(SubscribeResponse.FOLDER_ENCODING_ERROR);
    }
}
