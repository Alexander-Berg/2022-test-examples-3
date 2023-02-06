package ru.yandex.autotests.innerpochta.yfurita.tests.apply;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.SendMessageProvider;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.*;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.FuritaConsts.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.MessageIndexedMatcher.messageIndexed;

/**
 * User: stassiak
 * Date: 05.06.12
 * Здесь имеется несколько шероховатостей.
 * 1) Письма,упавшие в СПам - не индексируются.
 * 2) Для корпа этот тест не работает при apply фильтров из-за того,
 * что есть письмо в "Удаленных".
 * Фильтр не применяется к этому письму и ко всем остальным.
 */

@Feature("Yfurita.Base")
@Aqua.Test(title = "Тестирование фильтрации писем, находящихся в разных папках",
        description = "У пользователя yfurita.user@yandex.ru в каждой из папок лежит письмо, удовлетворяющее " +
                "условию фильтра. Проверяем, что письма находятся запросом preview и фильтруются запросом apply")
@Title("FilteredFromOtherFoldersTest.Тестирование фильтрации писем, находящихся в разных папках")
@Description("У пользователя yfurita.user@yandex.ru в каждой из папок лежит письмо, удовлетворяющее " +
        "условию фильтра. Проверяем, что письма находятся запросом preview и фильтруются запросом apply")
public class FilteredFromOtherFoldersTest {
    private static final String CUSTOM_FOLDER_NAME = "YFURITA";
    private static final String CUSTOM_LABEL_NAME = "YFURITA";
    private static final List<String> FOLDERS_PG = asList(PG_FOLDER_DEFAULT, PG_FOLDER_DRAFT,
            /*PG_FOLDER_SPAM, [MAILDEV-926]PG_FOLDER_DELETED,*/ PG_FOLDER_OUTBOX/*, CUSTOM_FOLDER_NAME*/);

    private static FilterUser fUser;
    private static HashMap<String, String> folderNameAndMid = new HashMap<String, String>();
    private static HashMap<String, TestMessage> folderNameAndTestMsgs = new HashMap<String, TestMessage>();
    private static YFuritaPreviewResponse previewRespEtalon;
    private static String lid;
    private static String filterId;
    private Logger log = LogManager.getLogger(this.getClass());
    private HashMap<String, String> params = new HashMap<String, String>();

    @Credentials(loginGroup = "FilteredFromOtherFoldersTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();

    @BeforeClass
    public static void initTest() throws Exception {
        fUser = new FilterUser(testUser);
        fUser.removeAllFilters();
        fUser.clearAll();

//        fUser.createFolder(CUSTOM_FOLDER_NAME);
//        lid = fUser.createLabel(CUSTOM_LABEL_NAME);
//
        lid = fUser.getLid(CUSTOM_LABEL_NAME);

        for (String folderName : FOLDERS_PG) {
            String fid = fUser.getFid(folderName);
            TestMessage testMessage = new TestMessage();
            testMessage.setFrom("yantester@yandex.ru");
            testMessage.setRecipient(testUser.getLogin());
            testMessage.setSubject("message for folder=" + folderName + " " + randomAlphanumeric(5));
            testMessage.setHeader("X-Yandex-Spam", "1");
            testMessage.setHeader("X-Yandex-Hint", encodeBase64String(("fid=" + fid).getBytes()));
            testMessage.setText(randomAlphanumeric(20));
            testMessage.saveChanges();
            SendMessageProvider.send(testMessage, yfuritaProps().getMxServer(), yfuritaProps().getNwsmtpPort());
            folderNameAndTestMsgs.put(folderName, testMessage);
        }

        for (String folderName : folderNameAndTestMsgs.keySet()) {
            fUser.inFolder(folderName).shouldSeeLetterWithSubject(folderNameAndTestMsgs.get(folderName).getSubject());
            String mid = fUser.inFolder(folderName)
                    .getMidOfMessageWithSubject(folderNameAndTestMsgs.get(folderName).getSubject());
            folderNameAndMid.put(folderName, mid);
            assumeThat(mid,
                    withWaitFor(messageIndexed(fUser), MINUTES.toMillis(3), SECONDS.toMillis(10)));
        }

        previewRespEtalon =
                new YFuritaPreviewResponse(folderNameAndMid.values().toArray(new String[folderNameAndMid.size()]));
    }

    @Before
    public void createFilters() throws Exception {
        log.info(folderNameAndMid);
        params.put(NAME.getName(), randomAlphanumeric(20));
        params.put(LOGIC.getName(), "1");
        params.put(FIELD1.getName(), "subject");
        params.put(FIELD2.getName(), "3");
        params.put(FIELD3.getName(), "message");
        params.put(CLICKER.getName(), FilterSettings.CLIKER_MOVEL);
        params.put(MOVE_LABEL.getName(), lid);

        filterId = fUser.createFilter(params);
    }

    @Test
    @Title("Проверяем apply фильров к письмам с разных папок")
    @Issues({@Issue("MPROTO-177")})
    @Description("Проверяем apply фильров к письмам с разных папок")
    public void shouldSeeCorrectApplyResultPq() throws Exception {
        fUser.applyFilter(filterId);
//        fUser.inFolder(CUSTOM_FOLDER_NAME)
//                .shouldSeeLetterWithSubjectAndLabel(folderNameAndTestMsgs.get(CUSTOM_FOLDER_NAME).getSubject(),
//                        CUSTOM_LABEL_NAME);
        fUser.inFolder(PG_FOLDER_OUTBOX)
                .shouldSeeLetterWithSubjectAndLabel(folderNameAndTestMsgs.get(PG_FOLDER_OUTBOX).getSubject(),
                        CUSTOM_LABEL_NAME);
        fUser.inFolder(PG_FOLDER_DRAFT)
                .shouldSeeLetterWithSubjectAndLabel(folderNameAndTestMsgs.get(PG_FOLDER_DRAFT).getSubject(),
                        CUSTOM_LABEL_NAME);
        fUser.inFolder(PG_FOLDER_DEFAULT)
                .shouldSeeLetterWithSubjectAndLabel(folderNameAndTestMsgs.get(PG_FOLDER_DEFAULT).getSubject(),
                        CUSTOM_LABEL_NAME);
        //fUser.inFolder(PG_FOLDER_SPAM)
        //        .shouldSeeLetterWithSubjectAndWithoutLabel(folderNameAndTestMsgs.get(PG_FOLDER_SPAM).getSubject(),
        //               CUSTOM_LABEL_NAME);
//        fUser.inFolder(PG_FOLDER_DELETED)
//                .shouldSeeLetterWithSubjectAndLabel(folderNameAndTestMsgs.get(PG_FOLDER_DELETED).getSubject(),
//                        CUSTOM_LABEL_NAME);
    }

    @Test
    @Title("Проверяем наличие mid-ов с разных папок в ответе на preview-запроса по условиям")
    @Description("Проверяем наличие mid-ов с разных папок в ответе на preview-запроса по условиям")
    public void shouldSeeCorrectPreviewForExistedFilter() throws Exception {
        YFuritaPreviewResponse response =
                new YFuritaPreviewResponse(fUser.previewFilter(filterId));
        assertThat("Некоректный вывод preview по id-фильтра",
                response,
                equalTo(previewRespEtalon));
    }

    @Test
    @Title("Проверяем наличие mid-ов с разных папок в ответе на preview-запроса по id-фильтра")
    @Description("Проверяем наличие mid-ов с разных папок в ответе на preview-запроса по id-фильтра")
    public void shouldSeeCorrectPreviewForConditions() throws Exception {
        YFuritaPreviewResponse response =
                new YFuritaPreviewResponse(fUser.previewFilter(params));
        assertThat("Некоректный вывод preview по условиям",
                response,
                equalTo(previewRespEtalon));
    }
}