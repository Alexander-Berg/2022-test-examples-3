package ru.yandex.autotests.innerpochta.imap.unsubscribe;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.autotests.innerpochta.imap.responses.UnsubscribeResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.UnsubscribeRequest.unsubscribe;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.rules.UnsubscribeRule.withUnsubscribeBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 09.04.14
 * Time: 20:08
 */
@Aqua.Test
@Title("Команда UNSUBSCRIBE. Отписываемся от папок")
@Features({ImapCmd.UNSUBSCRIBE})
@Stories(MyStories.COMMON)
@Description("Общие тесты на UNSUBSCRIBE. UNSUBSCRIBE без параметров")
public class UnsubscribeCommonTest extends BaseTest {
    private static Class<?> currentClass = UnsubscribeCommonTest.class;


    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));


    @Rule
    public ImapClient prodImap = withUnsubscribeBefore(withCleanBefore(newLoginedClient(currentClass)));

    @Description("Делаем просто UNSUBSCRIBE на пустом ящике без выбора папки")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("651")
    public void unsubscribeWithoutParamShouldSeeBad() {
        imap.request(unsubscribe("")).shouldBeBad().statusLineContains(ImapResponse.COMMAND_SYNTAX_ERROR);
    }

    @Description("SUBSCRIBE кириллической папки без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("652")
    public void unsubscribeCyrillicFolderShouldSeeBad() {
        imap.request(unsubscribe(Utils.cyrillic())).shouldBeBad()
                .statusLineContains(UnsubscribeResponse.FOLDER_ENCODING_ERROR);
    }
}
