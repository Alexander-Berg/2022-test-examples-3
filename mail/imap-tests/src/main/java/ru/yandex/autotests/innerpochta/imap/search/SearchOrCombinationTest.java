package ru.yandex.autotests.innerpochta.imap.search;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.SearchResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.String.valueOf;
import static org.junit.rules.RuleChain.emptyRuleChain;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 19.06.14.
 */
@Aqua.Test
@Title("Команда SEARCH. Запросы, используя OR")
@Features({ImapCmd.SEARCH})
@Stories(MyStories.COMMON)
@Description("В тесте комбинируем различные запросы с помощью команды OR")
@Issue("MPROTO-1355")
@Web
public class SearchOrCombinationTest extends BaseTest {
    private static Class<?> currentClass = SearchOrCombinationTest.class;


    public static final String LAST_MESSAGE = "2";

    public static final int NUMBER_OF_MESSAGES = 2;

    public static final int TEN_DAYS_IN_FEATURE = 10;

    public static String sendDate = "";

    public static String featureDate = "";

    public static int messageSize;

    public ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public RuleChain chain = emptyRuleChain().around(imap);

    @Before
    public void setUp() throws Exception {
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGES);

        sendDate = imap.fetch().getSentDate(LAST_MESSAGE);
        featureDate = imap.fetch().getSentDateNeighbourhood(LAST_MESSAGE, TEN_DAYS_IN_FEATURE);
        messageSize = imap.fetch().getSize(LAST_MESSAGE);

        imap.select().inbox();
    }

    @Test
    @Title("Должны найти сообщение которое удовлетворяет только одному условию OR")
    @ru.yandex.qatools.allure.annotations.TestCaseId("558")
    public void shouldSearchOrWithOneTrueCondition() {
        imap.request(search().or().on(sendDate).larger(valueOf(messageSize + 1)))
                .shouldHasSize(2)
                .shouldContain("1", "2");
    }

    @Test
    @Title("Не должны найти сообщения с двумя не верными условиями в OR")
    @ru.yandex.qatools.allure.annotations.TestCaseId("559")
    public void shouldSearchOrWithTwoFalseCondition() {
        imap.request(search().or().on(featureDate).larger(valueOf(messageSize + 1)))
                .shouldBeEmpty();
    }

    @Test
    @Issue("MPROTO-1355")
    @Title("Перестановка аргументов в SEARCH")
    @Description("Проверяем основные правила для ключа OR [MAILPROTO-2199]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("557")
    public void searchOr() {
        imap.request(search().or().on(sendDate).larger(valueOf(messageSize + 1)))
                .shouldBeEqualTo(imap.request(search().or().larger(valueOf(messageSize + 1)).on(sendDate)));
    }

    @Test
    @Title("Более 2-х аргументов в SEARCH")
    @Description("Проверяем основные правила для ключа OR с несколькими параметрами (больше двух параметров)\n" +
            "[MAILPROTO-2199]\n" +
            "SEARCH принимает ровно 2 аргумента. Всё остальное идёт по общим правилам, т.е AND")
    @ru.yandex.qatools.allure.annotations.TestCaseId("560")
    public void searchOrGreater() {
        imap.request(search().or().on(sendDate).on(sendDate).on(sendDate)).shouldContain(LAST_MESSAGE);
        imap.request(search().or().on(sendDate).on(sendDate).on(sendDate).larger(valueOf(messageSize + 1))
                .larger(valueOf(messageSize + 1))).shouldBeEmpty();

        imap.request(search().or().larger(valueOf(messageSize + 1)).on(sendDate)).shouldContain(LAST_MESSAGE);
        imap.request(search().or().on(sendDate).larger(valueOf(messageSize + 1)).flagged().seen().all().answered())
                .shouldBeOk().shouldBeEmpty();
    }

    @Test
    @Description("Используем команду OR в неправильном месте\n" +
            "Должно быть 2 ключа после OR")
    @ru.yandex.qatools.allure.annotations.TestCaseId("561")
    public void searchOrWithoutKey() {
        imap.request(search().seen().all().or()).shouldBeBad().statusLineContains(SearchResponse.COMMAND_SYNTAX_ERROR);
        imap.request(search().or().all()).shouldBeBad().statusLineContains(SearchResponse.COMMAND_SYNTAX_ERROR);
        imap.request(search().seen().or().all()).shouldBeBad().statusLineContains(SearchResponse.COMMAND_SYNTAX_ERROR);
    }
}
