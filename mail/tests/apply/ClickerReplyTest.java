package ru.yandex.autotests.innerpochta.yfurita.tests.apply;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.*;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;


import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.tests.unstable.SendMessageProvider.send;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.MessageIndexedMatcher.messageIndexed;

/**
 * User: alex89
 * Date: 23.08.17
 */
@Feature("Yfurita.Base")
@Aqua.Test(title = "Тестирование создания и работы фильтра на автоответ с clicker=reply",
        description = "Тестирование создания и работы фильтра на автоответ с clicker=reply")
@Title("ClickerReplyTest.Тестирование создания и работы фильтра на автоответ с clicker=reply")
@Description("Тестирование создания и работы фильтра на автоответ с clicker=reply")
public class ClickerReplyTest {
    private static FilterUser fUser;
    private static FilterUser toUser;
    private String filterId;
    private TestMessage testMessage;
    private String mid;

    @Credentials(loginGroup = "ClickerReplyTest1")
    public static User testUser;

    @Credentials(loginGroup = "ClickerReplyTest2")
    public static User testUser2;

    @Rule
    public LogConfigRule logRule = new LogConfigRule();
    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();

    @BeforeClass
    public static void createFilters() throws Exception {
        fUser = new FilterUser(testUser);
        toUser = new FilterUser(testUser2);
        fUser.removeAllFilters();
    }

    @Before
    public void createFilter() throws Exception {
        toUser.clearAll();
        fUser.clearAll();
        FilterSettings filterSettings = new FilterSettings();
        filterSettings.setLetter(FilterSettings.LETTER_NOSPAM);
        filterSettings.setClicker(FilterSettings.CLIKER_REPLY);
        filterSettings.setAutoAnswer("autoreply");
        filterSettings.setLogic(FilterSettings.LOGIC_OR);
        filterSettings.setStop(FilterSettings.STOP_YES);
        filterSettings.setFromConfirm(testUser.getLogin());
        filterSettings.setConfirmLang(FilterSettings.LANG_RU);
        filterSettings.setConfirmDomain("mail.yandex.ru");
        filterSettings.setAuthDomain("yandex.ru");
        filterSettings.setField1("subject");
        filterSettings.setField2("3");
        filterSettings.setField3("reply");

        filterId = fUser.createFilter(filterSettings.getParams());

        fUser.disableFilter(filterId);
    }


    @Before
    public void initTest() throws Exception {
        testMessage = new TestMessage();
        testMessage.setFrom(testUser2.getLogin());
        testMessage.setRecipient(testUser.getLogin());
        testMessage.setSubject(format("message for reply %s", randomAlphanumeric(5)));
        testMessage.setHeader("X-Yandex-Spam", "1");
        testMessage.setText(randomAlphanumeric(20));
        testMessage.saveChanges();
        send(testMessage, yfuritaProps().getMxServer(), yfuritaProps().getNwsmtpPort());


        mid = fUser.getMidOfMessageWithSubject(testMessage.getSubject());
        assumeThat(mid, withWaitFor(messageIndexed(fUser), SECONDS.toMillis(30), SECONDS.toMillis(10)));
    }

    @Test
    @Title("Проверяем работу apply фильтра на автоответ")
    @Description("Проверяем работу apply фильтра на автоответ")
    @Issues({@Issue("MPROTO-3890")})
    public void shouldSeeApplyResultOfReplyFilter() throws Exception {
        fUser.applyFilter(filterId);
        //todo доделать после правки MPROTO-3890
//        fUser.shouldNotSeeLetterWithSubject(testMessage.getSubject());
//        toUser.shouldSeeLetterWithSubject(testMessage.getSubject());
    }


    @Test
    @Title("Проверяем работу preview фильтра на автоответ")
    @Description("Проверяем работу preview фильтра на автоответ")
    public void shouldSeeLetterMidInPreviewOfReplyFilter() throws Exception {
        YFuritaPreviewResponse response = new YFuritaPreviewResponse(fUser.previewFilter(filterId));
        assertThat("Некоректный вывод preview", response, equalTo(new YFuritaPreviewResponse(asList(mid))));
    }


}