package ru.yandex.autotests.directintapi.tests.soap.setcampdaybudgetlimitstoptime;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: xy6er
 * https://jira.yandex-team.ru/browse/TESTIRT-1251
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.SOAP_SET_CAMP_DAY_BUDGET_LIMIT_STOP_TIME)
public class SetCampDayBudgetLimitStopTimeTest {
    protected static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(SetCampDayBudgetLimitStopTimeTest.class);

    private static final String ZERO_TIME = "0000-00-00 00:00:00";
    private static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static DateTimeFormatter apiFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private static Long campaignId;
    private static int orderID;

    private DateTime dateTime;
    private Map<Integer, String> request;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Before
    public void init() {
        log.info("Подготовка данных для теста");
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderID = api.userSteps.campaignFakeSteps().setRandomOrderID(campaignId);

        dateTime = DateTime.now();
        request = new HashMap<>();
    }


    @Test
    public void setCampDayBudgetLimitStopTimeTest() {
        log.info("вызываем метод setCampDayBudgetLimitStopTime для одной кампании");
        request.put(orderID, formatter.print(dateTime));
        darkSideSteps.getSoapClientSteps().setCampDayBudgetLimitStopTime(request);

        CampaignFakeInfo campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("Неверное значения для поля DayBudgetStopTime",
                campaignFakeInfo.getDayBudgetStopTime(), equalTo(apiFormatter.print(dateTime)));
        assertThat("Неверное значения для поля DayBudgetNotificationStatus",
                campaignFakeInfo.getDayBudgetNotificationStatus(), equalTo(Status.READY));
    }

    @Test
    public void setCampDayBudgetLimitStopTimeForMultipleCampaignTest() {
        log.info("вызываем метод setCampDayBudgetLimitStopTime для нескольких кампаний");
        Long campaignId2 = api.userSteps.campaignSteps().addDefaultTextCampaign();
        int orderID2 = api.userSteps.campaignFakeSteps().setRandomOrderID(campaignId2);
        DateTime dateTime2 = dateTime.plusDays(1);
        request.put(orderID, formatter.print(dateTime));
        request.put(orderID2, formatter.print(dateTime2));
        darkSideSteps.getSoapClientSteps().setCampDayBudgetLimitStopTime(request);

        CampaignFakeInfo campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("Неверное значения для поля DayBudgetStopTime",
                campaignFakeInfo.getDayBudgetStopTime(), equalTo(apiFormatter.print(dateTime)));
        assertThat("Неверное значения для поля DayBudgetNotificationStatus",
                campaignFakeInfo.getDayBudgetNotificationStatus(), equalTo(Status.READY));

        campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId2);
        assertThat("Неверное значения для поля DayBudgetStopTime",
                campaignFakeInfo.getDayBudgetStopTime(), equalTo(apiFormatter.print(dateTime2)));
        assertThat("Неверное значения для поля DayBudgetNotificationStatus",
                campaignFakeInfo.getDayBudgetNotificationStatus(), equalTo(Status.READY));
    }

    @Test
    public void setCampDayBudgetLimitStopTimeWithZeroTimeValueTest() {
        log.info("вызываем метод setCampDayBudgetLimitStopTime c нулевым значением времени - для возобновления показов");
        request.put(orderID, formatter.print(dateTime));
        darkSideSteps.getSoapClientSteps().setCampDayBudgetLimitStopTime(request);

        request.put(orderID, "0");
        darkSideSteps.getSoapClientSteps().setCampDayBudgetLimitStopTime(request);

        CampaignFakeInfo campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("Неверное значения для поля DayBudgetStopTime",
                campaignFakeInfo.getDayBudgetStopTime(), equalTo(ZERO_TIME));
    }

}
