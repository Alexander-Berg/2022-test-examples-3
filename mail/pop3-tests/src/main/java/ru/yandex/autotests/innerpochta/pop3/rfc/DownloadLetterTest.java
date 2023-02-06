package ru.yandex.autotests.innerpochta.pop3.rfc;

import java.io.IOException;
import java.util.Collection;

import javax.mail.MessagingException;

import org.apache.commons.net.pop3.POP3MessageInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.base.Pop3Cmd;
import ru.yandex.autotests.innerpochta.imap.core.pop3.Pop3Client;
import ru.yandex.autotests.innerpochta.pop3.base.BaseTest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.model.SeverityLevel;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

@Aqua.Test
@Title("Четыре способа скачать письма по pop3")
@Features({MyStories.POP3, MyStories.RFC})
@Stories({Pop3Cmd.USER, Pop3Cmd.PASS, Pop3Cmd.QUIT, Pop3Cmd.TOP})
@Description("Скачиваем письма четырёх разных пользователях")
@Issue("MPROTO-1799")
@Web
@RunWith(Parameterized.class)
public class DownloadLetterTest extends BaseTest {

    public static final String ONE_FOLDER = "DownloadOneFolder";
    public static final String TWO_FOLDER = "DownloadTwoFolder";
    public static final String ONE_FOLDER_401 = "DownloadOneFolder401";
    public static final String TWO_FOLDER_401 = "DownloadTwoFolder401";

    @Parameterized.Parameter(0)
    public String loginGroup;

    @Parameterized.Parameter(1)
    public int lettersCount;
    public Pop3Client pop3 = new Pop3Client();
    public Pop3Client prodPop3 = new Pop3Client();

    @Parameterized.Parameters(name = "u: {0} letters: {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {ONE_FOLDER, 2},
                {TWO_FOLDER, 3},
                {ONE_FOLDER_401, 2},
                {TWO_FOLDER_401, 3}
        });
    }

    @Before
    public void connect() {
        pop3.pop3(loginGroup).connect();
        prodPop3.pop3(loginGroup).connect();
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Stories("Загрузка мисем по POP3")
    @ru.yandex.qatools.allure.annotations.TestCaseId("669")
    public void downloadLetters() throws IOException, MessagingException {
        POP3MessageInfo[] messages = pop3.list();
        POP3MessageInfo[] prodMessages = prodPop3.list();

        assertThat(format("В ящике не %s письма", lettersCount), messages.length, equalTo(lettersCount));

        for (int i = 0; i < messages.length; i++) {
            assertThat(format("Неправильный размер письма #%s", i), messages[i].size, allOf(equalTo(prodMessages[i].size),
                    not(equalTo(0))));
        }

        POP3MessageInfo message = pop3.list(messages[0].number);
        POP3MessageInfo prodMessage = pop3.list(messages[0].number);
        assertThat("Неправильный размер письма", message.size, allOf(equalTo(prodMessage.size), not(equalTo(0))));
        assertThat("Неправильный номер письма", message.number, equalTo(1));

        for (int i = 1; i <= messages.length; i++) {
            assertThat("Тело первого письма различается с продакшеном", pop3.retr(i),
                    hasSameItemsAsList(prodPop3.retr(i)));
        }
    }

    @After
    public void disconnect() {
        pop3.disconnect();
        prodPop3.disconnect();
    }

}
