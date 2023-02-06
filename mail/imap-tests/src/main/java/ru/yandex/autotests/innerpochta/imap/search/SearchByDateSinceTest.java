package ru.yandex.autotests.innerpochta.imap.search;

import java.util.List;

import javax.mail.MessagingException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 14.07.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по дате с автогенерацией даты")
@Features({ImapCmd.SEARCH})
@Stories({"#поиск по дате"})
@Description("Ищем письма по датам после определённой даты (since) на любом юзере.\n" +
        "Автоматически генерирурем даты для тестов. since (>=)")
public class SearchByDateSinceTest extends BaseTest {
    private static Class<?> currentClass = SearchByDateSinceTest.class;

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
    @Title("Ищем письма после максимальной даты")
    @Description("Ищем письма после максимальной существующей даты +1 день в ящике - находим ничего.\n" +
            "Берём просто дату с максимальным значением - находим крайние елементы.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("469")
    public void searchDateSinceRightBorder() throws MessagingException {
        imap.request(search().since(rightBorderAfter)).shouldBeOk().shouldBeEmpty();
        imap.request(search().since(rightBorder)).shouldBeOk().shouldContain(searchAll.get(allCount - 1));
    }

    @Test
    @Title("Ищем письма после максимальной даты")
    @Description("Ищем письма после минимально известной даты - находим крайние элементы.\n" +
            "Ищем всё что строго больше минимальной даты - также находим всё.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("470")
    public void searchDateSinceLeftBorder() throws MessagingException {
        imap.request(search().since(leftBorder)).shouldBeOk().shouldContain(searchAll);
        imap.request(search().since(leftBorderBefore)).shouldBeOk().shouldContain(searchAll);
    }
}
