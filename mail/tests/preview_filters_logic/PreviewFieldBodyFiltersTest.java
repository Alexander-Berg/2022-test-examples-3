package ru.yandex.autotests.innerpochta.yfurita.tests.preview_filters_logic;

import com.google.common.collect.ImmutableMap.Builder;
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
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static com.google.common.collect.Maps.newHashMap;
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

@Aqua.Test(title = "Тестирование preview фильтров на параметр body",
        description = "Тестирование preview фильтров на параметр body")
@Title("PreviewFieldBodyFiltersTest.Тестирование preview фильтров на параметр body")
@Description("Тестирование preview фильтров на параметр body")
@Feature("Yfurita.Preview")
@RunWith(Parameterized.class)
public class PreviewFieldBodyFiltersTest {
    private static final String FOLDER_WITH_TEST_LETTERS = "body-test";
    private static final long INDEX_ADDITIONAL_TIMEOUT = 30000;
    private static String filterId;
    private static FilterUser fUser;
    private static HashMap<String, TestMessage> fileNamesAndTestMsgs = new HashMap<String, TestMessage>();
    private static HashMap<String, String> fileNamesAndMids = new HashMap<String, String>();
    private Logger log = LogManager.getLogger(this.getClass());

    @Credentials(loginGroup = "PreviewFieldBodyFiltersTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @Parameterized.Parameter(0)
    public HashMap params;
    @Parameterized.Parameter(1)
    public List<String> expectedTestLettersNames;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return asList(
                new Object[]{
                        newHashMap(new Builder<String, String>()
                                .put(FIELD1.getName(), "body")
                                .put(FIELD2.getName(), "3")
                                .put(FIELD3.getName(), "simpletext")
                                .build()),
                        asList("3.eml")},
                new Object[]{
                        newHashMap(new Builder<String, String>()
                                .put(FIELD1.getName(), "body")
                                .put(FIELD2.getName(), "3")
                                .put(FIELD3.getName(), "Уважаемый")
                                .build()),
                        asList("1.eml", "2.eml")},
                new Object[]{
                        newHashMap(new Builder<String, String>()
                                .put(FIELD1.getName(), "body")
                                .put(FIELD2.getName(), "3")
                                .put(FIELD3.getName(), "Уважаемый клиент!")
                                .put(ATTACHMENT.getName(), "1")
                                .build()),
                        asList("1.eml")},
                new Object[]{
                        newHashMap(new Builder<String, String>()
                                .put(FIELD1.getName(), "body")
                                .put(FIELD2.getName(), "3")
                                .put(FIELD3.getName(), "Уважаемый клиент!")
                                .put(ATTACHMENT.getName(), "2")
                                .build()),
                        asList("2.eml")}//,
//                new Object[]{ //сейчас при "равен" и "содержит" одинаковый запрос в поиск!!!
//                        newHashMap(new Builder<String, String>()
//                                .put(FIELD1.getName(), "body")
//                                .put(FIELD2.getName(), "1")
//                                .put(FIELD3.getName(), "Уважаемый клиент!")
//                                .build()),
//                        asList()}
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
        params.put(CLICKER.getName(), CLIKER_DELETE);

        filterId = fUser.createFilter(params);
    }

    @Test
    @Title("Проверяем работу фильров на СООТВЕТСТВИЕ SUBJECT в письме (preview по id фильтра)")
    @Description("Проверяем работу фильров на СООТВЕТСТВИЕ SUBJECT в письме через preview запрос по id фильтра")
    public void shouldSeeCorrectPreviewForExistedFilter() throws Exception {
        YFuritaPreviewResponse response =
                new YFuritaPreviewResponse(fUser.previewFilter(filterId));
        assertThat("Некоректный вывод preview по id-фильтра",
                response,
                equalTo(getPreviewResponseEtalon(expectedTestLettersNames, fileNamesAndMids)));
    }

    @Test
    @Title("Проверяем работу фильров на СООТВЕТСТВИЕ SUBJECT в письме (preview по условиям)")
    @Description("Проверяем работу фильров на СООТВЕТСТВИЕ SUBJECT в письме через preview запрос по условиям")
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