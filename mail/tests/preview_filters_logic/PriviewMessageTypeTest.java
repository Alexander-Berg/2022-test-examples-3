package ru.yandex.autotests.innerpochta.yfurita.tests.preview_filters_logic;

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
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.UserInitializationRule;
import ru.yandex.autotests.innerpochta.yfurita.util.YFuritaPreviewResponse;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.*;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.CLIKER_DELETE;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.CLIKER_MOVEL;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.getMidsTableFromMailBox;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.getPreviewResponseEtalon;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.sendAllTestMsgsFromResourceFolder;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.MessageIndexedMatcher.messageIndexed;

/*
1.eml label=SystMetkaSO:news
2.eml label=SystMetkaSO:news
3.eml label=SystMetkaSO:regular
4.eml label=SystMetkaSO:regular From: news@newsnewsnews.ru, subject:news
5.eml label=SystMetkaSO:s_news
6.eml label=SystMetkaSO:s_news label=SystMetkaSO:news
7.eml без типов, но упоминание news в  заголовках и теле
 */

@Aqua.Test(title = "Тестирование preview фильтров на совпадение типа письма",
        description = "Тестирование preview фильтров на совпадение типа письма")
@Title("PriviewMessageTypeTest.Тестирование preview фильтров на совпадение типа письма [MAILDEV-851]")
@Description("Тестирование preview фильтров на совпадение типа письма")
@Feature("Yfurita.Preview")
@RunWith(Parameterized.class)
public class PriviewMessageTypeTest {
    private static final String FOLDER_WITH_TEST_LETTERS = "message-type-test";
    private static final long INDEX_ADDITIONAL_TIMEOUT = 30000;
    private static String filterId;
    private static FilterUser fUser;
    private static HashMap<String, TestMessage> fileNamesAndTestMsgs = new HashMap<String, TestMessage>();
    private static HashMap<String, String> fileNamesAndMids = new HashMap<String, String>();
    private HashMap<String, String> params = new HashMap<String, String>();
    private Logger log = LogManager.getLogger(this.getClass());

    @Credentials(loginGroup = "PriviewMessageTypeTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @Parameterized.Parameter(0)
    public String field1;
    @Parameterized.Parameter(1)
    public String field2;
    @Parameterized.Parameter(2)
    public String field3;
    @Parameterized.Parameter(3)
    public List<String> expectedTestLettersNames;


    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return asList(
                new Object[]{"type", "1", "news", asList("1.eml", "2.eml","6.eml")},
                new Object[]{"type", "2", "news", asList("3.eml", "4.eml", "5.eml","7.eml")},
                new Object[]{"type", "1", "s_news", asList("5.eml", "6.eml")},
                new Object[]{"type", "2", "s_news", asList("1.eml", "2.eml","3.eml", "4.eml","7.eml")},
                new Object[]{"type", "1", "s_aviaticket", asList()}
        );
    }

    @BeforeClass
    public static void initFilterUserAndParams() throws Exception {
        fUser = new FilterUser(testUser);
        fUser.removeAllFilters();
        fUser.clearAll();

        fileNamesAndTestMsgs = sendAllTestMsgsFromResourceFolder(FOLDER_WITH_TEST_LETTERS, testUser);
        fileNamesAndMids = getMidsTableFromMailBox(fUser, fileNamesAndTestMsgs);

        Thread.sleep(INDEX_ADDITIONAL_TIMEOUT);
    }

    @Before
    public void createFilter() throws Exception {
        log.info(fileNamesAndMids);
        params.put(NAME.getName(), randomAlphanumeric(20));
        params.put(LOGIC.getName(), "1");
        params.put(FIELD1.getName(), field1);
        params.put(FIELD2.getName(), field2);
        params.put(FIELD3.getName(), field3);
        params.put(CLICKER.getName(), CLIKER_DELETE);

        filterId = fUser.createFilter(params);
    }

    @Test
    @Title("Проверяем работу фильров на СООТВЕТСТВИЕ ТИПУ письма (preview по id фильтра)")
    @Issues({@Issue("MAILDEV-851")})
    @Description("Проверяем работу фильров на СООТВЕТСТВИЕ ТИПУ письма через preview запрос по id фильтра")
    public void shouldSeeCorrectPreviewForExistedFilter() throws Exception {
        YFuritaPreviewResponse response =
                new YFuritaPreviewResponse(fUser.previewFilter(filterId));
        assertThat("Некоректный вывод preview по id-фильтра",
                response,
                equalTo(getPreviewResponseEtalon(expectedTestLettersNames, fileNamesAndMids)));
    }

    @Test
    @Title("Проверяем работу фильров на СООТВЕТСТВИЕ ТИПУ письма (preview по условиям)")
    @Issues({@Issue("MAILDEV-851")})
    @Description("Проверяем работу фильров на СООТВЕТСТВИЕ ТИПУ письма через preview запрос условий")
    public void shouldSeeCorrectPreviewForConditions() throws Exception {
        YFuritaPreviewResponse response =
                new YFuritaPreviewResponse(fUser.previewFilter(params));
        assertThat("Некоректный вывод preview по условиям",
                response,
                equalTo(getPreviewResponseEtalon(expectedTestLettersNames, fileNamesAndMids)));
    }

    @AfterClass
    public static void disableAllFilters() throws Exception {
        fUser.disableAllFilters();
    }
}
