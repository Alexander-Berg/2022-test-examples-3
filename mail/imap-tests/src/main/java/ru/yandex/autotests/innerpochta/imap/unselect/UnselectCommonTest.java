package ru.yandex.autotests.innerpochta.imap.unselect;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.04.14
 * Time: 15:32
 */
@Aqua.Test
@Title("Команда UNSELECT. Общие тесты")
@Features({ImapCmd.UNSELECT})
@Stories(MyStories.COMMON)
@Description("Общие тесты на UNSELECT. UNSELECT без параметров")
public class UnselectCommonTest extends BaseTest {
    private static Class<?> currentClass = UnselectCommonTest.class;


    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));


    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Description("Делаем просто UNSELECT на пустом ящике без выбора папки")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("644")
    public void simpleUnselectShouldSeeBad() {
        imap.request(unselect()).shouldBeBad();
    }

    @Description("Делаем дважды UNSELECT на пустом ящике без выбора папки")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("645")
    public void doubleUnselectShouldSeeBad() {
        imap.request(unselect()).shouldBeBad();
        imap.request(unselect()).shouldBeBad();
    }
}
