package ru.yandex.autotests.innerpochta.imap.search;

import java.util.List;

import javax.mail.MessagingException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import ru.lanwen.verbalregex.VerbalExpression;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.lanwen.verbalregex.VerbalExpression.regex;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created by kurau on 14.07.14.
 * [MAILPROTO-2332]
 */
@Aqua.Test
@Title("Команда SEARCH. Поля сообщения")
@Features({ImapCmd.SEARCH})
@Stories("#поиск по полям сообщения")
@Description("Поиск по полю TEXT\n" +
        "Берём произвольное письмо, вытаскиваем из него поле TEXT и смотрим, что в поиске по такому полю не пусто")
public class SearchByFieldTextTest extends BaseTest {
    private static Class<?> currentClass = SearchByFieldTextTest.class;
    public static final String BAD_RESP = "bvcxt";
    public static String leftInterval = "";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private static String text;
    private static List<String> messages;
    private static String messageId;

    @BeforeClass
    public static void setUp() throws MessagingException {
        imap.select().inbox();
        messages = imap.request(search().all()).shouldBeOk().shouldNotBeEmpty().getMessages();
        messageId = String.valueOf(messages.size());
        text = imap.fetch().getText(messageId).get(0);
        VerbalExpression ve = regex().startOfLine().then("<").anything().then(">")
                .capt().anything().endCapt()
                .then("<").then("/").anything().then(">").build();
        if (ve.test(text)) {
            text = ve.getText(text, 1);
        }
        leftInterval = text.substring(text.length() / 2);
    }
}

