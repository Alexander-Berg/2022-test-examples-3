package ru.yandex.autotests.innerpochta.yfurita.tests.other;

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
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.YFuritaPreviewResponse;
import ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils;
import ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import java.util.Arrays;
import java.util.Collection;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.yfurita.util.FuritaConsts.PG_FOLDER_DEFAULT;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.MessageIndexedMatcher.messageIndexed;

/**
 * User: stassiak
 * Date: 01.02.12
 * Time: 11:59
 */
@Aqua.Test(title = "Тестирование фильтров с проставлением меток",
        description = "Тестирование preview и apply запроса")
@RunWith(value = Parameterized.class)
@Feature("Yfurita.Base")
public class YFuritaLabelFiltersTest {
    @Rule
    public LogConfigRule logRule = new LogConfigRule();
    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();

    private static Logger log = LogManager.getLogger(YFuritaLabelFiltersTest.class);
    private static YfuritaProperties props = new YfuritaProperties();
    private static final User TEST_USER = chooseUser(new User("yfurita.labelstest@yandex.ru", "12345678"),
            new User("furita-test-19@mail.yandex-team.ru", "s8Igl3mT/zjX"));
    private static FilterUser fUser;
    private String labelSubj;
    private String filterId;
    private String filterString;
    private String lId;
    private String mid;

    public YFuritaLabelFiltersTest(String fStr, String fId, String labelId) {
        filterString = fStr;
        filterId = fId;
        lId = labelId;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data;
        if (props.isCorp()) {
            data = new Object[][]{
                    new Object[]{"black_label", "2370000000000109291", "2370000000091698398"},
                    new Object[]{"red_label", "2370000000000109292", "2370000000091698424"},
                    new Object[]{"read_label", "2370000000000109293", "FAKE_SEEN_LBL"}};
        } else {
            data = new Object[][]{
                    new Object[]{"red_label", "152595", "9"},
                    new Object[]{"black_label", "152594", "10"},
                    new Object[]{"read_label", "152596", "FAKE_SEEN_LBL"}};
        }

        return Arrays.asList(data);
    }

    @BeforeClass
    public static void initFilterUser() throws Exception {
        // assumeThat("Данный тест подходит только для ORACLE!", yfuritaProps().isPg(), is(false));
        fUser = new FilterUser(TEST_USER);
        fUser.clearAll();
    }

    @Before
    public void sendMsgs() throws Exception {
        labelSubj = filterString + " " + randomAlphanumeric(20);
        TestMessage labelMessage = new TestMessage();
        labelMessage.setRecipient(TEST_USER.getLogin());
        labelMessage.setSubject(labelSubj);
        labelMessage.setFrom("devnull@yandex.ru");
        labelMessage.setText("Message with red label" + labelSubj);
        labelMessage.saveChanges();
        fUser.sendMessageWithFilterOff(labelMessage);
        mid = fUser.inFolder(PG_FOLDER_DEFAULT).getMidOfMessageWithSubject(labelSubj);
        assumeThat(mid, withWaitFor(messageIndexed(fUser), MINUTES.toMillis(3), SECONDS.toMillis(10)));
    }

    @Test
    public void testPreviewLabelFilter() throws Exception {
        YFuritaPreviewResponse respEtalon = new YFuritaPreviewResponse(new String[]{mid});
        YFuritaPreviewResponse response = new YFuritaPreviewResponse(fUser.previewFilter(filterId));

        assertThat("Expected:\n" + respEtalon.print() + "Actual:\n" + response.print(),
                respEtalon, equalTo(response));
    }

    @Test
    public void testJsonPreviewLabelFilter() throws Exception {
        YFuritaPreviewResponse respEtalon = new YFuritaPreviewResponse(new String[]{mid});
        YFuritaPreviewResponse response =
                new YFuritaPreviewResponse(fUser.previewFilter(filterId, YFuritaUtils.JSON).jsonPath());

        assertThat("Expected:\n" + respEtalon.print() + "Actual:\n" + response.print(),
                respEtalon, equalTo(response));
    }

    @Test
    public void testApplyLabelFilter() throws Exception {
        fUser.applyFilter(filterId);
        Thread.sleep(5000);
        fUser.shouldSeeLetterWithSubjectAndLabelWithLid(labelSubj, lId);
    }

    @Test
    public void testJsonApplyLabelFilter() throws Exception {
        fUser.applyFilter(filterId, YFuritaUtils.JSON);
        Thread.sleep(5000);
        fUser.shouldSeeLetterWithSubjectAndLabelWithLid(labelSubj, lId);
    }

    @After
    public void deleteMsgs() throws Exception {
        fUser.clearAll();
    }
}
