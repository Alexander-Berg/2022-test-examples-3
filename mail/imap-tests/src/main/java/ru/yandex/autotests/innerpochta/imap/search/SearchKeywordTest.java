package ru.yandex.autotests.innerpochta.imap.search;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.MINUS_FLAGS_SILENT;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.PLUS_FLAGS_SILENT;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanFlagsRule.withCleanFlagsBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;


/**
 * Created by kurau on 18.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Поиск по KEYWORD")
@Features({ImapCmd.SEARCH})
@Stories(MyStories.COMMON)
@Description("Выбираем письмо по номеру. Ставим ему флаг с помощью STORE. Ищем по этому флагу.\n" +
        "В ящике должно быть минимум 9 сообщений")
public class SearchKeywordTest extends BaseTest {
    private static Class<?> currentClass = SearchKeywordTest.class;

    public static final String MESSAGE = "5";
    public static final String MESSAGE_SEQUENCE = "5,7,9";
    public static final String FLAG = MessageFlags.random();
    public static final int MESSAGES_COUNT = 10;


    @Rule
    public ImapClient imap = withCleanFlagsBefore(newLoginedClient(currentClass));

    @Before
    public void setUp() {
        imap.select().inbox();
    }

    @Test
    @Description("Ставим письму флаг. Ищем по этому флагу. Удаляем. Снова ищем.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("555")
    public void searchKeyword() {
        imap.request(store(MESSAGE, PLUS_FLAGS_SILENT, roundBraceList(FLAG)));
        imap.request(search().keyword(FLAG)).shouldBeOk().shouldHasSize(1).shouldContain(MESSAGE);
        imap.request(search().unkeyword(FLAG)).shouldBeOk().shouldHasSize(MESSAGES_COUNT - 1);
        imap.request(store(MESSAGE, MINUS_FLAGS_SILENT, roundBraceList(FLAG))).shouldBeOk();
        imap.request(search().keyword(FLAG)).shouldBeOk().shouldHasSize(0);
        imap.request(search().unkeyword(FLAG)).shouldBeOk().shouldHasSize(MESSAGES_COUNT);
    }

    @Test
    @Description("Ставим нескольким письмам флаг. Ищем. Убираем флаг. Снова ищем, уже не находим.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("556")
    public void searchKeywordMessageSequence() {
        imap.request(store(MESSAGE_SEQUENCE, PLUS_FLAGS_SILENT, roundBraceList(FLAG))).shouldBeOk();
        imap.request(search().keyword(FLAG)).shouldBeOk().shouldHasSize(3);
        imap.request(search().unkeyword(FLAG)).shouldBeOk().shouldHasSize(MESSAGES_COUNT - 3);
        imap.request(store(MESSAGE_SEQUENCE, MINUS_FLAGS_SILENT, roundBraceList(FLAG))).shouldBeOk();
        imap.request(search().keyword(FLAG)).shouldBeOk().shouldHasSize(0);
        imap.request(search().unkeyword(FLAG)).shouldBeOk().shouldHasSize(MESSAGES_COUNT);
    }
}
