package ru.yandex.autotests.innerpochta.imap.search;

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
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created by kurau on 11.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по дате с автогенерацией даты")
@Features({ImapCmd.SEARCH})
@Stories({"#поиск по дате"})
@Description("Ищем письма с ключами sentSince/On/Before. По ним поиск не поддерживается.\n" +
        "Ожидаем, что ничего не сломается - OK")
public class SearchByDateSentTest extends BaseTest {
    private static Class<?> currentClass = SearchByDateSentTest.class;


    public static String anyDate = "1-Feb-2020";

    @ClassRule
    public static ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @BeforeClass
    public static void setUp() throws MessagingException {
        imap.select().inbox();
    }

    @Test
    @Description("Не поддерживается поиск по этим ключам. Формально проверяем, что ничего не ломается")
    @ru.yandex.qatools.allure.annotations.TestCaseId("468")
    public void searchDateSentSince() throws MessagingException {
        imap.request(search().sentSince(anyDate)).shouldBeOk().shouldBeEmpty();
        imap.request(search().sentBefore(anyDate)).shouldBeOk().shouldBeEmpty();
        imap.request(search().sentOn(anyDate)).shouldBeOk().shouldBeEmpty();
    }
}
