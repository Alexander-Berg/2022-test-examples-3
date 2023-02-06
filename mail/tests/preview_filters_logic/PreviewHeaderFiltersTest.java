package ru.yandex.autotests.innerpochta.yfurita.tests.preview_filters_logic;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.CLIKER_DELETE;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.CLIKER_MOVEL;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.*;

/**
 * Created by alex89 on 20.07.17.
 */
@Aqua.Test(title = "Тестирование preview фильтров на соответствие заголовков",
        description = "Тестирование preview фильтров на соответствие заголовков")
@Title("PreviewHeaderFiltersTest.Тестирование preview фильтров на соответствие заголовков [MAILDEV-809]")
@Description("Тестирование preview фильтров на соответствие заголовков")
@Feature("Yfurita.Preview")
@RunWith(Parameterized.class)
public class PreviewHeaderFiltersTest {
    private static final String FOLDER_WITH_TEST_LETTERS = "header-test2";
    private static final long INDEX_ADDITIONAL_TIMEOUT = 30000;
    private static String filterId;
    private static FilterUser fUser;
    private static HashMap<String, TestMessage> fileNamesAndTestMsgs = new HashMap<String, TestMessage>();
    private static HashMap<String, String> fileNamesAndMids = new HashMap<String, String>();
    private HashMap<String, String> params = new HashMap<String, String>();
    private Logger log = LogManager.getLogger(this.getClass());

    @Credentials(loginGroup = "PreviewHeaderFiltersTest")
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
                new Object[]{"Return-Path", "1", "yantester22222222@yandex.ru", asList("1.eml")},
                new Object[]{"Peterburg", "1", "Peterburg", asList("1.eml", "2.eml", "3.eml")},
                new Object[]{"Peterburg", "2", "Peterburg", asList("4.eml", "5.eml", "6.eml", "7.eml")},
                new Object[]{"Peterburg", "1", "Peterburg2", asList("3.eml")},
                new Object[]{"Subject", "3", "cat",
                        asList("1.eml", "2.eml", "3.eml", "4.eml", "5.eml")},
                new Object[]{"Subject", "4", "cat", asList("6.eml", "7.eml")},
               // new Object[]{"Subject2", "3", "ильтры", asList("6.eml")},
                new Object[]{"Bla-bla", "1", "Кот", asList()},
                new Object[]{"x-job", "3", "4732_576", asList("7.eml")}
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
    @Title("Проверяем работу фильров на CУЩЕСТВОВАНИЕ ЗАГОЛОВКОВ в письме")
    @Issues({@Issue("MAILDEV-809")})
    @Description("Проверяем работу фильров на CУЩЕСТВОВАНИЕ ЗАГОЛОВКОВ в письме через preview запрос")
    public void shouldSeeCorrectPreviewForExistedFilter() throws Exception {
        YFuritaPreviewResponse response =
                new YFuritaPreviewResponse(fUser.previewFilter(filterId));
        assertThat("Некоректный вывод preview по id-фильтра",
                response,
                equalTo(getPreviewResponseEtalon(expectedTestLettersNames, fileNamesAndMids)));
    }

    @Test
    @Title("Проверяем работу фильров на CУЩЕСТВОВАНИЕ ЗАГОЛОВКОВ в письме")
    @Issues({@Issue("MAILDEV-809")})
    @Description("Проверяем работу фильров на CУЩЕСТВОВАНИЕ ЗАГОЛОВКОВ в письме через preview запрос")
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
