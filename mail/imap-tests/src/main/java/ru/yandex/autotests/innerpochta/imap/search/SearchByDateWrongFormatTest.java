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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.String.format;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 06.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Неправильные даты")
@Features({ImapCmd.SEARCH})
@Stories({"#поиск по дате", "#негативные тесты"})
@Description("Ищем по дате с неправильным форматом дня, месяца и года")
@RunWith(value = Parameterized.class)
public class SearchByDateWrongFormatTest extends BaseTest {
    private static Class<?> currentClass = SearchByDateWrongFormatTest.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private String request;

    public SearchByDateWrongFormatTest(String request) {
        this.request = request;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(
                new Object[]{""},
                new Object[]{"001"},
                new Object[]{"gaf"},
                new Object[]{"345667"},
                new Object[]{"*"},
                new Object[]{"буб"},
                new Object[]{"#$"}
        );
    }

    @BeforeClass
    public static void setUp() {
        imap.select().inbox();
    }

    @Test
    @Title("Ищем с неправильным форматом даты для Since")
    @ru.yandex.qatools.allure.annotations.TestCaseId("476")
    public void searchDateWrongFormatSince() {
        imap.request(search().since(format("%s-mar-2013", request))).shouldBeBad();
        imap.request(search().since(format("2-%s-2013", request))).shouldBeBad();
        imap.request(search().since(format("3-jan-%s", request))).shouldBeBad();
        imap.request(search().since(format("%s-%s-%s", request, request, request))).shouldBeBad();
    }

    @Test
    @Title("Ищем с неправильным форматом даты для Before")
    @ru.yandex.qatools.allure.annotations.TestCaseId("477")
    public void searchDateWrongFormatBefore() {
        imap.request(search().before(format("%s-mar-2013", request))).shouldBeBad();
        imap.request(search().before(format("2-%s-2013", request))).shouldBeBad();
        imap.request(search().before(format("3-jan-%s", request))).shouldBeBad();
        imap.request(search().before(format("%s-%s-%s", request, request, request))).shouldBeBad();
    }

    @Test
    @Title("Ищем с неправильным форматом даты для ON")
    @ru.yandex.qatools.allure.annotations.TestCaseId("475")
    public void searchDateWrongFormatOn() {
        imap.request(search().on(format("%s-mar-2013", request))).shouldBeBad();
        imap.request(search().on(format("2-%s-2013", request))).shouldBeBad();
        imap.request(search().on(format("3-jan-%s", request))).shouldBeBad();
        imap.request(search().on(format("%s-%s-%s", request, request, request))).shouldBeBad();
    }
}
