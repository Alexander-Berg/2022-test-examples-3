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

import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 19.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск с неправильным форматом размера")
@Features({ImapCmd.SEARCH})
@Stories({"#негативные тесты", "#поиск по размеру письма"})
@Description("#поиск по размеру письма. Смотрим различные размеры в параметрах поиска. Смотрим чтобы не было OK")
@RunWith(Parameterized.class)
public class SearchBySizeDifferentDataTest extends BaseTest {
    private static Class<?> currentClass = SearchBySizeDifferentDataTest.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private String request;

    public SearchBySizeDifferentDataTest(String request) {
        this.request = request;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(
                new Object[]{"-1"},
                new Object[]{"--1"},
                new Object[]{"sdfg"},
                new Object[]{"*"},
                new Object[]{"+"},
                new Object[]{"-"},
                new Object[]{"рп"},
                new Object[]{"$"},
                new Object[]{"%"},
                new Object[]{"&"},
                new Object[]{"#"},
                new Object[]{"@"},
                new Object[]{"6q"},
                new Object[]{"2-"},
                new Object[]{"9876543210"}
        );
    }

    @BeforeClass
    public static void setUp() {
        imap.select().inbox();
    }

    @Test
    @Description("Просто делаем запрос с разными типами размеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("530")
    public void searchSizeValidInputData() {
        imap.request(search().larger(request)).shouldBeBad();
        imap.request(search().smaller(request)).shouldBeBad();
    }
}
