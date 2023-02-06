package ru.yandex.autotests.innerpochta.yfurita.tests.apply;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.SendMessageProvider;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.Credentials;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.UserInitializationRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.LETTER_ALL;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.LETTER_CLEARSPAM;
import static ru.yandex.autotests.innerpochta.yfurita.util.FuritaConsts.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.MessageIndexedMatcher.messageIndexed;

/**
 * User: stassiak
 * Date: 05.06.12
 */

@Feature("Yfurita.Apply")
@Aqua.Test(title = "Тестирование фильтров на удаление (clicker=delete)",
        description = "Тестирование apply запроса c clicker=delete для писем, лежащих в разных папках")
@Title("ClickerDeleteTest.Тестирование фильтров на удаление (clicker=delete)")
@Description("Тестирование apply запроса c clicker=delete для писем, лежащих в разных папках")
@RunWith(Parameterized.class)
public class ClickerDeleteTest {
    private static final String CUSTOM_FOLDER_NAME = "YFURITA" + randomAlphanumeric(20);
    private static final List<String> FOLDERS_PG = asList(PG_FOLDER_DEFAULT, PG_FOLDER_DRAFT,
           /* PG_FOLDER_SPAM,*/ PG_FOLDER_DELETED, PG_FOLDER_OUTBOX/*, CUSTOM_FOLDER_NAME*/);
    private static HashMap<String, String> params = new HashMap<String, String>();
    private static FilterUser fUser;
    private static String filterId;
    private TestMessage testMessage;
    private Logger log = LogManager.getLogger(this.getClass());

    @Credentials(loginGroup = "ClickerDeleteTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @Parameterized.Parameter(0)
    public String startFolder;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        for (String startFolder : FOLDERS_PG) {
            data.add(new Object[]{startFolder});
        }
        return data;
    }

    @BeforeClass
    public static void initFilterUser() throws Exception {
        fUser = new FilterUser(testUser);
        fUser.removeAllFilters();
        fUser.clearAll();
        params.put(LETTER.getName(), LETTER_ALL);
        params.put(NAME.getName(), randomAlphanumeric(20));
        params.put(LOGIC.getName(), "1");
        params.put(FIELD1.getName(), "subject");
        params.put(FIELD2.getName(), "3");
        params.put(FIELD3.getName(), "message");
        params.put(CLICKER.getName(), FilterSettings.CLIKER_DELETE);
        filterId = fUser.createFilter(params);
        fUser.disableFilter(filterId);
//        fUser.createFolder(CUSTOM_FOLDER_NAME);
    }

    @Before
    public void initTest() throws Exception {
        log.info("Работаем с письмом из папки: " + startFolder);
        testMessage = new TestMessage();
        testMessage.setFrom("yantester@yandex.ru");
        testMessage.setRecipient(testUser.getLogin());
        testMessage.setSubject("message for start_folder=" + startFolder + " " + randomAlphanumeric(5));
        testMessage.setHeader("X-Yandex-Spam", "1");
        testMessage.setHeader("X-Yandex-Hint", encodeBase64String(("fid=" + fUser.getFid(startFolder)).getBytes()));
        testMessage.setText(randomAlphanumeric(20));
        testMessage.saveChanges();
        SendMessageProvider.send(testMessage, yfuritaProps().getMxServer(), yfuritaProps().getNwsmtpPort());


        String mid = fUser.inFolder(startFolder).getMidOfMessageWithSubject(testMessage.getSubject());
        assumeThat(mid, withWaitFor(messageIndexed(fUser), MINUTES.toMillis(1), SECONDS.toMillis(10)));
    }

    @Test
    @Title("Проверяем apply фильра на удаление к письмам с разных папок")
    @Description("Проверяем apply фильра на удаление к письмам с разных папок")
    public void shouldSeeLetterInFolderDeleteAfterFilterApplying() throws Exception {
        fUser.applyFilter(filterId);

        fUser.inFolder(PG_FOLDER_DELETED)
                .shouldSeeLetterWithSubject(testMessage.getSubject());
    }
}