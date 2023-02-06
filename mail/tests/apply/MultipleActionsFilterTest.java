package ru.yandex.autotests.innerpochta.yfurita.tests.apply;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.Credentials;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.UserInitializationRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.tests.unstable.SendMessageProvider.send;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.yfurita.util.FuritaConsts.PG_FOLDER_DEFAULT;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.createFilterWithHttpClient;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.MessageIndexedMatcher.messageIndexed;

/**
 * User: alex89
 * Date: 11.09.13
 */
@Feature("Yfurita.Base")
@Aqua.Test(title = "Тестирование фильтров  несколькими действиями",
        description = "Создаём фильтр с 3 действиями и проверяем его работу")
@Title("MultipleActionsFilterTest.Тестирование фильтров  несколькими действиями")
@Description("Создаём фильтр с 3 действиями и проверяем его работу")
public class MultipleActionsFilterTest {
    private static final String FAKE_SEEN_LBL = "FAKE_SEEN_LBL";
    private static String labelId;
    private static String folderId;
    private static FilterUser fUser;
    private Logger log = LogManager.getLogger(this.getClass());
    private String filterId;
    private String params;
    private TestMessage testMessage;

    @Credentials(loginGroup = "MultipleActionsFilterTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();


    @BeforeClass
    public static void initTestUser() throws Exception {
        fUser = new FilterUser(testUser);
    }

    @Before
    public void createFilterAndPrepareTestMessage() throws Exception {
        fUser.clearAll();
        labelId = fUser.createLabel("LABEL_" + randomAlphabetic(10));
        folderId = fUser.createFolder("FOLDER_" + randomAlphabetic(10));
        fUser.removeAllFilters();
        this.params = new StringBuilder().append("?attachment=")
                .append("&field1=subject")
                .append("&field2=3")
                .append("&field3=FILTER_ON")
                .append("&logic=1")
                .append("&clicker=movel")
                .append("&move_label=").append(labelId)
                .append("&clicker=movel")
                .append("&move_label=").append("lid_read")
                .append("&clicker=move")
                .append("&move_folder=").append(folderId)
                .append("&name=").append(randomAlphabetic(10))
                .append("&order=1")
                .append("&noconfirm=1")
                .append("&uid=").append(fUser.getUid()).toString();

        log.info("Создаём фильтр с параметрами" + params);
        filterId = createFilterWithHttpClient(params, log);
        log.info(filterId);
        fUser.disableAllFilters();

        testMessage = new TestMessage();
        testMessage.setRecipient(testUser.getLogin());
        testMessage.setSubject("FILTER_ON_" + randomAlphabetic(10));
        testMessage.setFrom("yantester@ya.ru");
        testMessage.setText("Message text");
        testMessage.saveChanges();
    }


    @Test
    public void testFilterAppliesCorrectly() throws Exception {
        log.info(format("Отправляем письмо с темой %s.", testMessage.getSubject()));
        send(testMessage, yfuritaProps().getMxServer(), yfuritaProps().getNwsmtpPort());
        assumeThat(fUser.inFolder(PG_FOLDER_DEFAULT).getMidOfMessageWithSubject(testMessage.getSubject()),
                withWaitFor(messageIndexed(fUser), MINUTES.toMillis(3), SECONDS.toMillis(10)));
        fUser.applyFilter(filterId);
        fUser.inFolderWithFid(folderId)
                .shouldSeeLetterWithSubjectAndLabelWithLid(testMessage.getSubject(), labelId)
                .shouldSeeLetterWithSubjectAndLabel(testMessage.getSubject(), FAKE_SEEN_LBL);
    }

    @Test
    public void testFilterNotAppliesCorrectly() throws Exception {
        testMessage.setSubject("FILTER_OFF_" + randomAlphabetic(10));
        testMessage.saveChanges();
        log.info(format("Отправляем письмо с темой %s.", testMessage.getSubject()));
        send(testMessage, yfuritaProps().getMxServer(), yfuritaProps().getNwsmtpPort());
        assumeThat(fUser.inFolder(PG_FOLDER_DEFAULT).getMidOfMessageWithSubject(testMessage.getSubject()),
                withWaitFor(messageIndexed(fUser), MINUTES.toMillis(3), SECONDS.toMillis(10)));
        fUser.applyFilter(filterId);
        fUser.inFolder(PG_FOLDER_DEFAULT)
                .shouldSeeLetterWithSubjectAndWithoutLabelWithLid(testMessage.getSubject(), labelId)
                .shouldSeeLetterWithSubjectAndWithoutLabel(testMessage.getSubject(), FAKE_SEEN_LBL);
    }

    @AfterClass
    public static void disableAllFilters() throws Exception {
        fUser.disableAllFilters();
    }
}