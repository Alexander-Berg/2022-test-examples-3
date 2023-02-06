package ru.yandex.autotests.innerpochta.imap.search;

import java.util.List;

import javax.mail.MessagingException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
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

import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 14.07.14.
 * [MAILPROTO-2322]
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по дате с автогенерацией даты")
@Features({ImapCmd.SEARCH})
@Stories({"#поиск по ключу BEFORE"})
@Description("Ищем письма по датам на любом юзере.\n" +
        "Автоматически гененирурем двты для тестов. before (<)")
public class SearchByDateBeforeTest extends BaseTest {
    private static Class<?> currentClass = SearchByDateBeforeTest.class;

    public static List<String> searchAll;
    public static int leftCount;
    public static int allCount;
    public static String leftBorder = "";
    public static String rightBorder = "";
    public static String leftBorderBefore = "";
    public static String rightBorderAfter = "";

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @BeforeClass
    public static void setUp() throws MessagingException {
        imap.select().inbox();
        searchAll = imap.request(search().all()).shouldNotBeEmpty().getMessages();
        allCount = searchAll.size();
        leftBorder = imap.fetch().getSentDate("1");
        rightBorder = imap.fetch().getSentDate(String.valueOf(allCount));
        leftBorderBefore = imap.fetch().getSentDateNeighbourhood("1", -1);
        rightBorderAfter = imap.fetch().getSentDateNeighbourhood(String.valueOf(allCount), 1);
        leftCount = imap.request(search().on(rightBorder)).shouldBeOk().getMessages().size();
    }

    @Test
    @Title("Ищем письма до максимальной даты в ящике [MAILPROTO-2322]")
    @Description("Ищем письма до максимальной существующей даты +1 день в ящике - находим всё.\n" +
            "Берём просто дату с максимальным значением в ящике - не находим крайнего элемента.")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("463")
    public void searchDateBeforeRightBorder() throws MessagingException {
        imap.request(search().before(rightBorderAfter)).shouldBeOk().shouldContain(searchAll);
        imap.request(search().before(rightBorder)).shouldBeOk().shouldNotContain(searchAll.get(allCount - 1));
    }

    @Test
    @Title("Ищем письма до минимальной даты в ящике [MAILPROTO-2322]")
    @Description("Ищем письма до минимально известной даты - находим крайние элементы.\n" +
            "Ищем всё что строго меньше минимальной даты - ничего не находим.")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("464")
    public void searchDateBeforeLeftBorder() throws MessagingException {
        imap.request(search().before(leftBorder)).shouldBeOk().shouldNotContain(searchAll.get(0));
        imap.request(search().before(leftBorderBefore)).shouldBeOk().shouldBeEmpty();
    }

}
