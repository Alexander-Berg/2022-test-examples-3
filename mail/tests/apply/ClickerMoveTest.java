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
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.FuritaConsts.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.MessageIndexedMatcher.messageIndexed;

/**
 * User: stassiak
 * Date: 05.06.12
 */

@Feature("Yfurita.Apply")
@Aqua.Test(title = "Тестирование apply запроса c clicker=move для писем, лежащих в разных папках",
        description = "Тестирование apply запроса c clicker=move для писем, лежащих в разных папках")
@Title("ClickerMoveTest.Тестирование фильтров на перекладку из папки в папку (clicker=move)")
@Description("Тестирование apply запроса c clicker=move для писем, лежащих в разных папках")
@RunWith(Parameterized.class)
public class ClickerMoveTest {
    private static final String CUSTOM_FOLDER_NAME = "YFURITA" + randomAlphanumeric(20);
    private static final List<String> FOLDERS_START = asList(PG_FOLDER_DEFAULT, PG_FOLDER_DRAFT,
            /*PG_FOLDER_SPAM, PG_FOLDER_DELETED, */ PG_FOLDER_OUTBOX/*, CUSTOM_FOLDER_NAME*/);
    private static final List<String> FOLDERS_MOVE = asList(PG_FOLDER_DEFAULT, PG_FOLDER_DRAFT,
            PG_FOLDER_SPAM, PG_FOLDER_DELETED, PG_FOLDER_OUTBOX/*, CUSTOM_FOLDER_NAME*/);
    private static HashMap<String, String> params = new HashMap<String, String>();
    private static FilterUser fUser;
    private static String filterId;
    private TestMessage testMessage;
    private Logger log = LogManager.getLogger(this.getClass());

    @Credentials(loginGroup = "ClickerMoveTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @Parameterized.Parameter(0)
    public String startFolder;
    @Parameterized.Parameter(1)
    public String moveFolder;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        LinkedList<Object[]> data = new LinkedList<Object[]>();
        for (String startFolder : FOLDERS_START) {
            for (String moveFolder : FOLDERS_MOVE) {
                data.add(new Object[]{startFolder, moveFolder});
            }
        }
        return data;
    }

    @BeforeClass
    public static void initFilterUser() throws Exception {
        fUser = new FilterUser(testUser);
        fUser.removeAllFilters();
        fUser.clearAll();
        params.put(NAME.getName(), randomAlphanumeric(20));
        params.put(LOGIC.getName(), "1");
        params.put(FIELD1.getName(), "subject");
        params.put(FIELD2.getName(), "3");
        params.put(FIELD3.getName(), "message");
        params.put(CLICKER.getName(), FilterSettings.CLIKER_MOVE);
//        fUser.createFolder(CUSTOM_FOLDER_NAME);
    }


    @Before
    public void initTest() throws Exception {
        fUser.disableAllFilters();
//        fUser.createFolder(startFolder);
//        fUser.createFolder(moveFolder);
//        fUser.createFolder(CUSTOM_FOLDER_NAME);

        testMessage = new TestMessage();
        testMessage.setFrom("yantester@yandex.ru");
        testMessage.setRecipient(testUser.getLogin());
        testMessage.setSubject(format("message for start_folder=%s %s", startFolder, randomAlphanumeric(5)));
        testMessage.setHeader("X-Yandex-Spam", "1");
        testMessage.setHeader("X-Yandex-Hint", encodeBase64String(("fid=" + fUser.getFid(startFolder)).getBytes()));
        testMessage.setText(randomAlphanumeric(20));
        testMessage.saveChanges();
        SendMessageProvider.send(testMessage, yfuritaProps().getMxServer(), yfuritaProps().getNwsmtpPort());


        String mid = fUser.inFolder(startFolder).getMidOfMessageWithSubject(testMessage.getSubject());
        assumeThat(mid, withWaitFor(messageIndexed(fUser), SECONDS.toMillis(30), SECONDS.toMillis(10)));

        params.put(MOVE_FOLDER.getName(), fUser.getFid(moveFolder));
    }

    @Test
    @Title("Проверяем apply фильра на переладку письма из папки в папку")
    @Description("Проверяем apply фильра  на переладку письма из папки в папку")
    public void shouldSeeCorrectApplyResultForClickerMove() throws Exception {
        filterId = fUser.createFilter(params);
        fUser.applyFilter(filterId);

        fUser.inFolder(moveFolder).shouldSeeLetterWithSubject(testMessage.getSubject());
    }


}