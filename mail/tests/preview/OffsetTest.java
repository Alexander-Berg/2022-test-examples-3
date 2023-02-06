package ru.yandex.autotests.innerpochta.yfurita.tests.preview;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.*;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.FileNotFoundException;
import java.util.*;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.MessageIndexedMatcher.messageIndexed;

/**
 * User: stassiak
 * Date: 05.06.12
 * У тестового пользователя имеется папка YFURITA
 * + выставлена настройка "В списке писем 200 по  писем на странице"
 */
@Feature("Yfurita.Preview")
@Aqua.Test(title = "Тестирование параметра offset запроса preview",
        description = "У пользователя yfurita.user@yandex.ru 30 писем, удовлетворяющих условиям фильтра. Проверяем " +
                "preview запрос для следующих значений параметра offset: -5, 0, 5, 10, 20 , 30, 35")
@Title("OffsetTest.Тестирование параметра offset запроса preview")
@Description("У пользователя yfurita.user@yandex.ru 30 писем, удовлетворяющих условиям фильтра. Проверяем " +
        "preview запрос для следующих значений параметра offset: -5, 0, 5, 10, 20 , 30, 35")
@RunWith(Parameterized.class)
public class OffsetTest {
    private static final int NUMBER_OF_TEST_MSGS = 33;
    private static final int LENGTH_IN_PREVIEW_REQUEST = 30;
    private static FilterUser fUser;
    private static String filterId; //     //2290000000000069075
    private static ArrayList<String> msgsSubjs = new ArrayList<String>();
    private static ArrayList<String> midsFromInbox = new ArrayList<String>();
    private YFuritaPreviewResponse respEtalon;

    @Credentials(loginGroup = "OffsetTest")
    public static User testUser;

    @Parameterized.Parameter(0)
    public int offsetParamValue;
    @Parameterized.Parameter(1)
    public List<String> mids;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule;
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        userInitializationRule = new UserInitializationRule();
        userInitializationRule.initUsers(OffsetTest.class.getFields());
        fUser = new FilterUser(testUser);
//        fUser.createFolder("YFURITA");
        fUser.clearAll();
        fUser.removeAllFilters();
        fUser.sendMessageWithFilterOff(getMsgs());

        HashMap<String, String> params = new HashMap<String, String>();
        params.put(NAME.getName(), randomAlphanumeric(20));
        params.put(LOGIC.getName(), "1");
        params.put(FIELD1.getName(), "Subject");
        params.put(FIELD2.getName(), "3");
        params.put(FIELD3.getName(), "YFURITA");
        params.put(CLICKER.getName(), FilterSettings.CLIKER_MOVE);
        params.put(MOVE_FOLDER.getName(), fUser.getFid("YFURITA"));
        filterId = fUser.createFilter(params);

        for (String subj : msgsSubjs) {
            String mid = fUser.getMidOfMessageWithSubject(subj);
            assumeThat(mid,
                    withWaitFor(messageIndexed(fUser), MINUTES.toMillis(3), SECONDS.toMillis(10)));
            midsFromInbox.add(mid);
        }
        Collections.sort(midsFromInbox);
        Collections.reverse(midsFromInbox);

        Collection<Object[]> data = new LinkedList<Object[]>();
        for (int i : new int[]{0, 5, 10, 20, 30, 34}) {
            data.add(new Object[]{i, getMids(i)});
        }

        return data;
    }

    @Before
    public void countExpectedData(){
        respEtalon = new YFuritaPreviewResponse(mids.toArray(new String[mids.size()]));
    }

   @Test
    public void testJson() throws Exception {
        YFuritaPreviewResponse response =
                    new YFuritaPreviewResponse(fUser.previewFilter(filterId,
                            LENGTH_IN_PREVIEW_REQUEST, offsetParamValue, YFuritaUtils.JSON).jsonPath());
        assertThat(respEtalon.print(), equalTo(response.print()));
    }

    @AfterClass
    public static void disableAllFilters() throws Exception {
        fUser.disableAllFilters();
    }

    private static Message[] getMsgs() throws MessagingException, FileNotFoundException {
        Message[] msgs = new Message[NUMBER_OF_TEST_MSGS];
        for (int i = 0; i < NUMBER_OF_TEST_MSGS; i++) {
            TestMessage m = new TestMessage();
            m.setFrom("yantester@yandex.ru");
            m.setRecipient(testUser.getLogin());
            m.setSubject("YFURITA: " + randomAlphanumeric(15));
            m.setText(randomAlphanumeric(30));
            m.saveChanges();

            msgs[i] = m;
            msgsSubjs.add(msgs[i].getSubject());
        }
        return msgs;
    }

    private static LinkedList<String> getMids(int n) {
        LinkedList<String> result = new LinkedList<String>();
        for (int i = n; i < n + LENGTH_IN_PREVIEW_REQUEST && i < NUMBER_OF_TEST_MSGS; i++) {
            result.add(midsFromInbox.get(i));
        }
       // Collections.sort(result);

        return result;
    }
}
